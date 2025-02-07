package com.wse.qanaryexplanationservice.services;

import com.wse.qanaryexplanationservice.exceptions.ExplanationException;
import com.wse.qanaryexplanationservice.exceptions.GenerativeExplanationException;
import com.wse.qanaryexplanationservice.helper.ExplanationHelper;
import com.wse.qanaryexplanationservice.helper.dtos.ComposedExplanationDTO;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class ExplanationService {

    private final Logger logger = LoggerFactory.getLogger(ExplanationService.class);
    private final String SELECT_PIPELINE_INFORMATION = "/queries/select_pipeline_information.rq";
    private final String SELECT_ALL_LOGGED_METHODS = "/queries/fetch_all_logged_methods.rq";
    private final String SELECT_ONE_METHOD_WITH_ID = "/queries/fetch_one_method_id.rq";
    private final String METHOD_EXPLANATION_TEMPLATE = "/explanations/methods/";
    private final String SELECT_CHILD_PARENT_METHODS = "/queries/fetch_child_parent_methods.rq";

    private final TemplateExplanationsService templateService;
    private final GenerativeExplanationsService generativeService;
    private final QanaryRepository qanaryRepository;

    public ExplanationService(TemplateExplanationsService templateService, GenerativeExplanationsService generativeService, QanaryRepository qanaryRepository) {
        this.templateService = templateService;
        this.generativeService = generativeService;
        this.qanaryRepository = qanaryRepository;
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

    public String explainComponentMethods(ExplanationMetaData explanationMetaData) throws Exception {
        QuerySolutionMap qsm = new QuerySolutionMap();
        AtomicReference<String> prefixExplanation = new AtomicReference<>();
        AtomicInteger i = new AtomicInteger(1);
        StringBuilder explanationItems = new StringBuilder();

        qsm.add("graph", ResourceFactory.createResource(explanationMetaData.getGraph().toASCIIString()));
        String query = QanaryTripleStoreConnector.readFileFromResourcesWithMap(SELECT_ALL_LOGGED_METHODS, qsm);
        logger.debug("Query: {}", query);

        ResultSet loggedMethodsResultSet = qanaryRepository.selectWithResultSet(query);
        List<String> variables = loggedMethodsResultSet.getResultVars();

        if (!explanationMetaData.getGptRequest().isDoGenerative()) {
            loggedMethodsResultSet.forEachRemaining(querySolution -> {
                if (prefixExplanation.get() == null) {
                    prefixExplanation.set(templateService.replacePlaceholdersWithVarsFromQuerySolution(querySolution,
                            variables, explanationMetaData.getPrefixTemplate()));
                }
                explanationItems.append("\n" + i + ". " + templateService.replacePlaceholdersWithVarsFromQuerySolution(
                        querySolution, variables, explanationMetaData.getItemTemplate()));
                i.getAndIncrement();
            });
        } else
            explanationItems.append(generativeService.explainSingleMethod(explanationMetaData, loggedMethodsResultSet));

        return prefixExplanation + explanationItems.toString();
    }

    public String select_one_method(ExplanationMetaData explanationMetaData) throws IOException {
        QuerySolutionMap qsm = new QuerySolutionMap();
        qsm.add("methodIdentifier", ResourceFactory.createResource(explanationMetaData.getMethod()));
        qsm.add("graph", ResourceFactory.createResource(explanationMetaData.getGraph().toASCIIString()));
        qsm.add("component", ResourceFactory.createResource(explanationMetaData.getQanaryComponent().getPrefixedComponentName()));

        String query = QanaryTripleStoreConnector.readFileFromResourcesWithMap(SELECT_ONE_METHOD_WITH_ID, qsm);
        logger.debug("Query: {}", query);
        return query;
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
                logger.error("{}", e.toString());
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
                e.printStackTrace();
            }
        } else if (data.getComponent() != "" || data.getComponent() != null) { // componentName, componentExplanations,
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
        componentAndExplanation.forEach((k, v) -> {
            composedExplanations.append(k + ": " + v + "\n\n");
        });
        return composedExplanations.toString();
    }

    // Explanation Tree
    public String getAggregatedExplanations(ExplanationMetaData data) throws Exception {
        QuerySolutionMap qsm = new QuerySolutionMap();
        qsm.add("methodId", ResourceFactory.createResource(data.getMethod()));
        qsm.add("graph", ResourceFactory.createResource(data.getGraph().toASCIIString()));
        String query = QanaryTripleStoreConnector.readFileFromResourcesWithMap(SELECT_CHILD_PARENT_METHODS, qsm);
        ResultSet childParentPairs = qanaryRepository.selectWithResultSet(query);
        Map<String, List<String>> childrenMap = new HashMap<>();
        Map<String, MethodItem> allMethods = new HashMap<>();
        String root = null;

        while (childParentPairs.hasNext()) {
            QuerySolution qs = childParentPairs.next();
            String childId = qs.get("leaf").toString();
            String parentId = qs.get("parent").toString();
            String rootId = qs.get("root").toString();
            childrenMap.putIfAbsent(parentId, new ArrayList<>());
            childrenMap.get(parentId).add(childId);
            if (root == null)
                root = rootId;
            allMethods.putIfAbsent(childId, requestMethodItem(data,  childId));
            allMethods.putIfAbsent(parentId, requestMethodItem(data, parentId));
        }

        JSONObject json = buildJsonTree(childrenMap, root, data, allMethods);
        return json.toString();
    }

    public MethodItem requestMethodItem(ExplanationMetaData data, String method)
            throws Exception {
        data.setMethod(method);
        String query = select_one_method(data);
        ResultSet result = qanaryRepository.selectWithResultSet(query);
        return transformQuerySolutionToMethodItem(result.next());
    }

    // New helper method to safely retrieve a variable from QuerySolution
    private String safeGetString(QuerySolution qs, String key) {
        if (qs.contains(key) && qs.get(key) != null) {
            return qs.get(key).toString();
        }
        return null;
    }

    public MethodItem transformQuerySolutionToMethodItem(QuerySolution qs) {
        String caller = safeGetString(qs, "caller");
        String callerName = safeGetString(qs, "callerName");
        String method = safeGetString(qs, "method");
        String annotatedAt = safeGetString(qs, "annotatedAt");
        String annotatedBy = safeGetString(qs, "annotatedBy");
        String outputDataType = safeGetString(qs, "outputDataType");
        String outputDataValue = safeGetString(qs, "outputDataValue");
        String inputDataTypes = safeGetString(qs, "inputDataTypes");
        String inputDataValues = safeGetString(qs, "inputDataValues");

        return new MethodItem(
                caller,
                callerName,
                method,
                outputDataType, outputDataValue, inputDataTypes, inputDataValues,
                annotatedAt,
                annotatedBy);
    }

    public JSONObject buildJsonTree(Map<String, List<String>> childrenMap, String root, ExplanationMetaData data,
            Map<String, MethodItem> allMethods) throws Exception { // TODO: Add decision between gen and
                                                                   // tmpl-explanation
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("parent", root);

        JSONArray childrenArray = new JSONArray();
        if (childrenMap.containsKey(root)) {
            for (String child : childrenMap.get(root)) {
                if (childrenMap.containsKey(child)) {
                    childrenArray.put(buildJsonTree(childrenMap, child, data, allMethods));
                } else {
                    // <-- Explain atomic methods
                    data.setMethod(child);
                    JSONObject childExplanationObj = new JSONObject();
                    childExplanationObj.put("id", child);
                    childExplanationObj.put("explanation", explainMethodSingle(data));
                    childExplanationObj.put("method", allMethods.get(child).getMethodName());
                    childrenArray.put(childExplanationObj);
                }
            }
        }
        // <-- Explain aggregated methods
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("id", root);
        jsonObj.put("explanation", data.getGptRequest().isDoGenerative() ? generativeService.explainAggregatedMethods(
                aggregateExplanationsToOneExplanation(childrenArray),
                data, allMethods.get(root)) : "Summarized template explanation are not yet supported.");
        jsonObj.put("method", allMethods.get(root).getMethodName());
        jsonObject.put("parent", jsonObj);
        jsonObject.put("children", childrenArray);
        return jsonObject;
    }

    /**
     * Aggregated all explanations with two newlines in between
     * 
     * @param explanations JSONArray of JSONObjects that contain the key
     *                     "explanation" if atomic, otherwise "parent" ->
     *                     "explanation"
     * @return Explanations separated with \n\n
     */
    public String aggregateExplanationsToOneExplanation(JSONArray explanations) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < explanations.length(); i++) {
            JSONObject explanationObj = explanations.getJSONObject(i);
            if (explanationObj.has("parent"))
                explanationObj = explanationObj.getJSONObject("parent");
            logger.debug("Json object: {}", explanationObj);
            stringBuilder.append(explanationObj.get("explanation").toString() + "\n\n");
        }
        return stringBuilder.toString();
    }

    public String explainMethodSingle(ExplanationMetaData data) throws ExplanationException, GenerativeExplanationException, Exception {
        String query = select_one_method(data);
        ResultSet resultSet = qanaryRepository.selectWithResultSet(query);
        if (!resultSet.hasNext()) {
            return "SPARQL query returned no results. Therefore, no explanation can be provided.";
        }

        try {
            if (data.getItemTemplate() == null) {
                data.setItemTemplate(
                        ExplanationHelper.getStringFromFile(
                                METHOD_EXPLANATION_TEMPLATE + "item/" + data.getLang()));
            }
        } catch (IOException e) {
            throw new ExplanationException("Template for language" + data.getLang() + " not found.", e);
        }

        return data.getGptRequest().isDoGenerative()
                ? generativeService.explainSingleMethod(data, resultSet)
                : templateService.explain(data, resultSet);
    }

    /**
     * Consists of prefix and numerated list of explanations
     * @return
     */
    public String explainMethodsAsList() {
        return null;
    }

    /**
     * Respect variants: Explanations or data? Which explanations? 
     * @return
     */
    public String explainMethodsAggregated() {
        return null;
    }

}
