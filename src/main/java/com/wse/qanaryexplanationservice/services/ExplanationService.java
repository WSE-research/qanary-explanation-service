package com.wse.qanaryexplanationservice.services;

import com.wse.qanaryexplanationservice.exceptions.ExplanationException;
import com.wse.qanaryexplanationservice.helper.ExplanationHelper;
import com.wse.qanaryexplanationservice.helper.dtos.ComposedExplanationDTO;
import com.wse.qanaryexplanationservice.helper.dtos.ExplanationMetaData;
import com.wse.qanaryexplanationservice.helper.dtos.QanaryExplanationData;
import com.wse.qanaryexplanationservice.helper.pojos.*;
import com.wse.qanaryexplanationservice.repositories.QanaryRepository;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.ResourceFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
public class ExplanationService {

    private final Logger logger = LoggerFactory.getLogger(ExplanationService.class);
    private final String SELECT_PIPELINE_INFORMATION = "/queries/select_pipeline_information.rq";
    private final String METHOD_EXPLANATION_TEMPLATE = "/explanations/methods/";
    private final String SELECT_CHILD_PARENT_METHODS = "/queries/fetch_child_parent_methods.rq";
    private final String ASK_IF_CHILDS_EXIST = "/queries/ask_if_method_has_childs.rq";

    private final TemplateExplanationsService templateService;
    private final GenerativeExplanationsService generativeService;
    private final QanaryRepository qanaryRepository;
    private final GenerativeExplanationsService generativeExplanationsService;

    public ExplanationService(TemplateExplanationsService templateService, GenerativeExplanationsService generativeService, QanaryRepository qanaryRepository, GenerativeExplanationsService generativeExplanationsService) {
        this.templateService = templateService;
        this.generativeService = generativeService;
        this.qanaryRepository = qanaryRepository;
        this.generativeExplanationsService = generativeExplanationsService;
    }

    public String getQaSystemExplanation(String header, String graphUri) throws Exception {
        return templateService.explainQaSystem(header, graphUri);
    }

    public String getTemplateComponentExplanation(String graphUri, QanaryComponent component, String header)
            throws Exception {
        return templateService.explainComponentAsRdf(graphUri, component, header);
    }

    public String getTemplateComponentInputExplanation(String graphUri, QanaryComponent component) throws IOException {
        return templateService.createInputExplanation(graphUri, component);
    }

    /**
     * Controller called method to start the process explaining several components
     * with both approaches;
     * the rulebased and the generative one.
     */
    public ComposedExplanation composedExplanationsForOutputData(ComposedExplanationDTO composedExplanationDTO) {
        ComposedExplanation composedExplanation = new ComposedExplanation();
        GenerativeExplanationRequest generativeExplanationRequest = composedExplanationDTO
                .getGenerativeExplanationRequest();

        generativeExplanationRequest.getQanaryComponents().forEach(component -> {
            try {
                String templatebased = templateService.createOutputExplanation( // compute template based explanation
                        composedExplanationDTO.getGraphUri(),
                        component,
                        "en");

                GenerativeExplanationObject generativeExplanationObject = generativeService.createGenerativeExplanation(
                        component,
                        generativeExplanationRequest.getShots(),
                        composedExplanationDTO.getGraphUri());

                String prompt = generativeService.createPrompt(
                        generativeExplanationRequest.getShots(),
                        generativeExplanationObject);

                String generativeExplanation = generativeService.sendPrompt(prompt,
                        generativeExplanationRequest.getGptModel());

                composedExplanation.addExplanationItem(component.getComponentName(), templatebased, prompt,
                        generativeExplanation, generativeExplanationObject.getTestComponent().getDataSet());
            } catch (Exception e) {
                logger.error("{}", e.getMessage());
            }
        });
        return composedExplanation;
    }

    public ComposedExplanation composedExplanationForInputData(ComposedExplanationDTO composedExplanationDTO)
            throws Exception {
        List<QanaryComponent> components = composedExplanationDTO.getGenerativeExplanationRequest()
                .getQanaryComponents();
        String graph = composedExplanationDTO.getGraphUri();
        ComposedExplanation composedExplanation = new ComposedExplanation();
        for (QanaryComponent component : components) {

            String sparqlQuery = bindingForGraphAndComponent(graph, component,
                    TemplateExplanationsService.INPUT_DATA_SELECT_QUERY);
            ResultSet results = qanaryRepository.selectWithResultSet(sparqlQuery);
            String query = getBodyFromResultSet(results);

            String templatebasedExplanation = query == null ? "This component didn't used any query"
                    : templateService.createExplanationForQuery(query, graph, component);

            String prompt = generativeService.getInputDataExplanationPrompt(
                    component,
                    query == null ? "" : query,
                    composedExplanationDTO.getGenerativeExplanationRequest().getShots());
            String gptExplanation = generativeService.sendPrompt(prompt,
                    composedExplanationDTO.getGenerativeExplanationRequest().getGptModel());
            composedExplanation.addExplanationItem(component.getComponentName(), templatebasedExplanation, prompt,
                    gptExplanation, query);
        }
        return composedExplanation;
    }

    // TODO: Later, refactor existing methods which add bindings (and execute the
    // query?)
    public String bindingForGraphAndComponent(String graph, QanaryComponent component, String plainQueryPath)
            throws IOException {
        QuerySolutionMap bindings = new QuerySolutionMap();
        bindings.add("graph", ResourceFactory.createResource(graph));
        bindings.add("component", ResourceFactory.createResource(component.getPrefixedComponentName()));
        return QanaryTripleStoreConnector.readFileFromResourcesWithMap(plainQueryPath, bindings);
    }

    public String getBodyFromResultSet(ResultSet resultSet) {
        if (resultSet.hasNext()) {
            QuerySolution querySolution = resultSet.next();
            return querySolution.get("body").toString();
        } else {
            return null;
        }
    }

    /**
     * Similar to a system's explanation
     */
    public String explainPipelineOutput(String graphUri) throws IOException {
        ResultSet results = getPipelineInformation(graphUri);
        return templateService.getPipelineOutputExplanation(results, graphUri);
    }

    public String explainPipelineOutput(String graphUri, Map<String, String> explanations) throws IOException {
        return templateService.getPipelineOutputExplanation(explanations, graphUri);
    }

    public String explainPipelineInput(String graphUri) throws IOException {
        ResultSet results = getPipelineInformation(graphUri);
        String questionId = "";
        while (results.hasNext()) {
            QuerySolution result = results.next();
            if (result.contains("questionId"))
                questionId = result.get("questionId").toString();
        }
        String question = qanaryRepository.getQuestionFromQuestionId(questionId);
        return templateService.getPipelineInputExplanation(question);
    }

    // Caching candidate
    public ResultSet getPipelineInformation(String graphUri) throws IOException {
        QuerySolutionMap querySolutionMap = new QuerySolutionMap();
        querySolutionMap.add("graph", ResourceFactory.createResource(graphUri));
        String sparql = QanaryTripleStoreConnector.readFileFromResourcesWithMap(SELECT_PIPELINE_INFORMATION,
                querySolutionMap);
        return qanaryRepository.selectWithResultSet(sparql);
    }

    public String getPipelineExplanation(String graph) throws IOException {
        return templateService.getPipelineOutputExplanation(
                this.getPipelineInformation(graph),
                graph);
    }

    public String getComposedExplanation(QanaryExplanationData body) throws IOException {
        String graph = body.getGraph();
        String component = body.getComponent();
        String inputExplanation;
        String outputExplanation;
        if (component == null) {
            return getPipelineExplanation(graph);
        } else {
            QanaryComponent qanaryComponent = new QanaryComponent(component);
            inputExplanation = getTemplateComponentInputExplanation(graph, qanaryComponent);
            outputExplanation = getTemplateComponentOutputExplanation(graph, qanaryComponent, "en");
        }
        return templateService.composeInputAndOutputExplanations(inputExplanation, outputExplanation, component);
    }

    public String getTemplateComponentOutputExplanation(String graph, QanaryComponent component, String lang)
            throws IOException {
        return templateService.createOutputExplanation(graph, component, lang);
    }

    protected String getComponentExplanation(String graph, QanaryComponent qanaryComponent) throws IOException {
        return templateService.composeInputAndOutputExplanations(
                getTemplateComponentInputExplanation(graph, qanaryComponent),
                getTemplateComponentOutputExplanation(graph, qanaryComponent, "en"),
                qanaryComponent.getComponentName());
    }

    protected String getPipelineExplanation(String graph, Map<String, String> explanations) throws IOException {
        return templateService.composeInputAndOutputExplanations(
                explainPipelineInput(graph),
                explainPipelineOutput(graph, explanations),
                null);
    }

    public String explain(QanaryExplanationData data) throws IOException {
        if (data.getExplanations() == null || data.getExplanations().isEmpty()) { // componentName, questionId and graph
            // provided // component-based
            // explanation
            QanaryComponent qanaryComponent = new QanaryComponent(data.getComponent());
            try {
                return getComponentExplanation(data.getGraph(), qanaryComponent); // TODO: Add lang-support
            } catch (IOException e) {
                logger.error(e.toString());
            }
        } else if (!data.getComponent().isEmpty() || data.getComponent() != null) { // componentName, componentExplanations,
            // questionId and graph are provided //
            // PaC-based explanation
            String explanationTemplate = ExplanationHelper.getStringFromFile("/explanations/pipeline_component/en_prefix");
            String components = StringUtils.join(data.getExplanations().keySet().toArray(), ", ");
            return explanationTemplate
                    .replace("${component}", data.getComponent())
                    .replace("${components}", components)
                    .replace("${question}", qanaryRepository.getQuestionFromQuestionId(data.getQuestionId() + "/raw"))
                    .replace("${questionId}", data.getQuestionId())
                    .replace("${graph}", data.getGraph())
                    .replace("${componentsAndExplanations}", composeComponentExplanations(data.getExplanations()));
        } else { // only questionId and graph are provided // System-based explanation
            // TODO: Implement. Extend pipeline with /explain or handle it here?
        }
        return null;
    }

    public String composeComponentExplanations(Map<String, String> componentAndExplanation) {
        StringBuilder composedExplanations = new StringBuilder();
        componentAndExplanation.forEach((k, v) -> composedExplanations.append(k).append(": ").append(v).append("\n\n"));
        return composedExplanations.toString();
    }

    /**
     * Wrapper method that decides whether the target method is atomic or a parent of other methods. Based on this result, the path to the dark or bright side is chosen.
     */
    public String explainMethod(ExplanationMetaData metaData) throws Exception {
        QuerySolutionMap qsm = new QuerySolutionMap();
        qsm.add("graph", ResourceFactory.createResource(metaData.getGraph().toASCIIString()));
        qsm.add("methodId", ResourceFactory.createResource(metaData.getMethod()));
        boolean doChildrenExist = qanaryRepository.askQuestion(QanaryTripleStoreConnector.readFileFromResourcesWithMap(ASK_IF_CHILDS_EXIST, qsm));

        return !doChildrenExist ? explainMethodSingle(metaData).getExplanation() : explainMethodAggregated(metaData);
    }

    public String explainMethodAggregated(ExplanationMetaData metaData) throws Exception {
        QuerySolutionMap qsm = new QuerySolutionMap();
        qsm.add("methodId", ResourceFactory.createResource(metaData.getMethod()));
        qsm.add("graph", ResourceFactory.createResource(metaData.getGraph().toASCIIString()));
        String query = QanaryTripleStoreConnector.readFileFromResourcesWithMap(SELECT_CHILD_PARENT_METHODS, qsm);
        ResultSet childParentPairs = qanaryRepository.selectWithResultSet(query);
        Map<Method, List<Method>> childParentPairsMap = createParentChildrenMap(childParentPairs);
        childParentPairsMap = explainAllLeafs(childParentPairsMap, metaData);
        childParentPairsMap = recursiveParentExplanation(childParentPairsMap, metaData);
        Method root = childParentPairsMap.keySet().stream()
                .filter(method -> method.getId().equals(metaData.getMethod()))
                .findFirst()
                .orElse(null);

        return metaData.getTree() // Maybe add a feature that return either the id(method)/explanation tree or the all-information containing tree
                ? convertExplanationsToTree(childParentPairsMap, root)
                : Objects.requireNonNull(root).getExplanation();
    }

    /**
     * Takes a ResultSet containing leaf, parent, root and hasChilds variables.
     * It creates mappings of parents and their childs. For the latter it additionally checks if the child is atomic, i.e. a leaf.
     * This distinction is relevant, as
     */
    public Map<Method, List<Method>> createParentChildrenMap(ResultSet childParentPairs) {
        Map<Method, List<Method>> childrenMap = new HashMap<>(); // Contains all 1-level subtree's
        while (childParentPairs.hasNext()) {
            QuerySolution qs = childParentPairs.next();
            Method child = new Method(qs.get("leaf").toString(), qs.get("hasChilds").asLiteral().getInt() == 0);
            Method parent = new Method(qs.get("parent").toString(), false);
            childrenMap.putIfAbsent(parent, new ArrayList<>());
            childrenMap.get(parent).add(child);
        }
        return childrenMap;
    }

    public String convertExplanationsToTree(Map<Method, List<Method>> childParentPairsMap, Method root) {
        JSONObject jsonObject = new JSONObject();

        // Create the root node JSON object
        JSONObject rootObject = root.toJson();

        JSONArray jsonArray = new JSONArray();
        if (childParentPairsMap.containsKey(root)) {
            for (Method child : childParentPairsMap.get(root)) {
                if (childParentPairsMap.containsKey(child)) {
                    jsonArray.put(new JSONObject(convertExplanationsToTree(childParentPairsMap, child)));
                } else {
                    jsonArray.put(child.toJson());
                }
            }
        }
        jsonObject.put("parent", rootObject);
        jsonObject.put("children", jsonArray);
        logger.debug("JSON: {}", jsonObject);

        return jsonObject.toString();
    }

    public Map<Method, List<Method>> recursiveParentExplanation(Map<Method, List<Method>> childParentPairsMap, ExplanationMetaData data) throws Exception {
        boolean updated;

        do {
            updated = false;
            List<Method> parents = new ArrayList<>(childParentPairsMap.keySet());
            for (int i = 0; i < parents.size(); i++) {
                Method parent = parents.get(i);
                List<Method> childs = childParentPairsMap.get(parent);

                // Only process parents that don't have an explanation yet
                if (parent.getExplanation() == null && childs.stream().allMatch(child -> child.getExplanation() != null)) {
                    MethodItem parentItem = qanaryRepository.requestMethodItem(data, parent.getId());
                    Method newParent = Objects.equals(data.getAggregationSettings().getApproach(), "generative")
                            ? (Objects.equals(data.getAggregationSettings().getType(), "data")
                            ? generativeExplanationsService.explainMethodAggregatedWithDataReturnMethod(parentItem, data)
                            : generativeExplanationsService.explainAggregatedMethodWithExplanationsReturnMethod(parentItem, childs, data))
                            : templateService.explainAggregatedMethodWithExplanationsReturnMethod(parentItem, childs, data);

                    childParentPairsMap.remove(parent);
                    childParentPairsMap.put(newParent, childs);
                    parents.set(i, newParent);

                    updated = true;

                    // Propagate parent explanation to child-occurrences  // TODO: Not very efficient
                    for (Map.Entry<Method, List<Method>> entry : childParentPairsMap.entrySet()) {
                        List<Method> children = entry.getValue();
                        for (int j = 0; j < children.size(); j++) {
                            if (children.get(j).equals(parent)) {
                                children.set(j, newParent);
                            }
                        }
                    }
                }
            }
        } while (updated);
        return childParentPairsMap;
    }

    public Map<Method, List<Method>> explainAllLeafs(Map<Method, List<Method>> childrenMap, ExplanationMetaData metaData) throws Exception {
        for (Method parent : childrenMap.keySet()) {
            List<Method> children = childrenMap.get(parent);
            for (int i = 0; i < children.size(); i++) {
                Method child = children.get(i);
                if (child.isLeaf()) {
                    logger.info("Child: {}", child.getId());
                    metaData.setMethod(child.getId());
                    children.set(i, explainMethodSingle(metaData));
                }
            }
        }
        return childrenMap;
    }

    public Method explainMethodSingle(ExplanationMetaData data) throws Exception { // TODO: Maybe return Method instead of explanation only?
        MethodItem method = qanaryRepository.requestMethodItem(data, data.getMethod());

        try {
            if (data.getItemTemplate() == null) {
                data.setItemTemplate(
                        ExplanationHelper.getStringFromFile(
                                METHOD_EXPLANATION_TEMPLATE + data.getLang())); // TODO: Is it possible, to provide the placeholders somewhere where they can be seen from OpenAPI def. for example?
            }
        } catch (IOException e) {
            throw new ExplanationException("Template for language" + data.getLang() + " not found. Please use a different language or provide your own template with the designated json property.", e);
        }

        if (Objects.equals(data.getAggregationSettings().getLeafs(), "template"))
            return templateService.explainSingleMethodReturnMethod(data, method);
        else if (Objects.equals(data.getAggregationSettings().getLeafs(), "generative"))
            return generativeService.explainSingleMethodReturnMethod(data, method);
        else
            throw new ExplanationException("Please provide a valid value for \"leaf\": Either \"template\" or \"generative\".");
    }

}
