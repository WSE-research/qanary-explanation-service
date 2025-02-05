package com.wse.qanaryexplanationservice.services;

import com.wse.qanaryexplanationservice.helper.dtos.ComposedExplanationDTO;
import com.wse.qanaryexplanationservice.helper.dtos.QanaryExplanationData;
import com.wse.qanaryexplanationservice.helper.pojos.*;
import com.wse.qanaryexplanationservice.repositories.QanaryRepository;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.ResourceFactory;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class ExplanationService {

    private final Logger logger = LoggerFactory.getLogger(ExplanationService.class);
    private final String SELECT_PIPELINE_INFORMATION = "/queries/select_pipeline_information.rq";
    private final String SELECT_ALL_LOGGED_METHODS = "/queries/fetch_all_logged_methods.rq";
    private final String SELECT_ONE_METHOD = "/queries/fetch_one_method.rq";
    private final String METHOD_EXPLANATION_TEMPLATE = "/explanations/methods/";
    private final String SELECT_CHILD_PARENT_METHODS = "/queries/fetch_child_parent_methods.rq";
    @Autowired
    private TemplateExplanationsService tmplExpService;
    @Autowired
    private GenerativeExplanationsService genExpService;
    @Autowired
    private QanaryRepository qanaryRepository;

    public String getQaSystemExplanation(String header, String graphUri) throws Exception {
        return tmplExpService.explainQaSystem(header, graphUri);
    }

    public String getTemplateComponentExplanation(String graphUri, QanaryComponent component, String header) throws Exception {
        return tmplExpService.explainComponentAsRdf(graphUri, component, header);
    }

    public String getTemplateComponentInputExplanation(String graphUri, QanaryComponent component) throws IOException {
        return tmplExpService.createInputExplanation(graphUri, component);
    }

    public String explainComponentMethods(ExplanationMetaData explanationMetaData) throws Exception {
        QuerySolutionMap qsm = new QuerySolutionMap();
        AtomicReference<String> prefixExplanation = new AtomicReference<>();
        AtomicInteger i = new AtomicInteger(1);
        StringBuilder explanationItems = new StringBuilder();

        qsm.add("graph", ResourceFactory.createResource(explanationMetaData.getGraph().toASCIIString()));
        String query = explanationMetaData.getRequestQuery() == null ?
                QanaryTripleStoreConnector.readFileFromResourcesWithMap(SELECT_ALL_LOGGED_METHODS, qsm) :
                transformQueryStringToQuery(explanationMetaData.getRequestQuery(), qsm);

        logger.debug("Query: {}", query);
        ResultSet loggedMethodsResultSet = qanaryRepository.selectWithResultSet(query);
        List<String> variables = loggedMethodsResultSet.getResultVars();

        if (!explanationMetaData.getGptRequest().isDoGenerative()) {
            loggedMethodsResultSet.forEachRemaining(querySolution -> {
                if(prefixExplanation.get() == null) {
                    prefixExplanation.set(tmplExpService.replacePlaceholdersWithVarsFromQuerySolution(querySolution, variables, explanationMetaData.getPrefixTemplate()));
                }
                explanationItems.append("\n" + i + ". " + tmplExpService.replacePlaceholdersWithVarsFromQuerySolution(querySolution, variables, explanationMetaData.getItemTemplate()));
                i.getAndIncrement();
            });
        } else explanationItems.append(genExpService.explain(explanationMetaData, loggedMethodsResultSet));

        return prefixExplanation + explanationItems.toString();
    }

    // TODO: Later, combine both approaches (single method explanation as well as multiple method explanations)
    // THis class in general sets up everything relevant to make explanations easy in responsible classes
    public String explainComponentMethod(ExplanationMetaData explanationMetaData) throws Exception {
        String query = QanaryTripleStoreConnector.readFileFromResources(SELECT_ONE_METHOD)
                .replace("?graph", "<" + explanationMetaData.getGraph().toASCIIString() + ">")
                .replace("?methodName", "\"" + explanationMetaData.getMethodName() + "\"")
                .replace("?component", "<" + explanationMetaData.getQanaryComponent().getPrefixedComponentName() + ">");

        logger.info("Requesting query: {}", query);

        ResultSet resultSet = qanaryRepository.selectWithResultSet(query);
        if (!resultSet.hasNext()) {
            return "SPARQL query returned no results. Therefore, no explanation can be provided.";
        }

        try {
            if(explanationMetaData.getItemTemplate() == null) {
                explanationMetaData.setItemTemplate(
                        TemplateExplanationsService.getStringFromFile(METHOD_EXPLANATION_TEMPLATE + "item/" + explanationMetaData.getLang())
                );
            }
            if(explanationMetaData.getPrefixTemplate() == null) {
                explanationMetaData.setPrefixTemplate(
                        TemplateExplanationsService.getStringFromFile(METHOD_EXPLANATION_TEMPLATE + "prefix/" + explanationMetaData.getLang())
                );
            }
        } catch (IOException e) {
            return "For language " + explanationMetaData.getLang() + " no template exists.";
        }

        return explanationMetaData.getGptRequest().isDoGenerative() ?
                genExpService.explain(explanationMetaData, resultSet) :
                tmplExpService.explain(explanationMetaData, resultSet);
    }



    /**
     * Replaces passed variables within the SPARQL query
     * @param queryString Query with variables
     * @param querySolutionMap Variable mappings
     * @return Final query
     */
    public static String transformQueryStringToQuery(String queryString, QuerySolutionMap querySolutionMap) {
        ParameterizedSparqlString pq = new ParameterizedSparqlString(queryString, querySolutionMap);
        Query query = QueryFactory.create(pq.toString());
        return query.toString();
    }

    /**
     * Controller called method to start the process explaining several components with both approaches;
     * the rulebased and the generative one.
     */
    public ComposedExplanation composedExplanationsForOutputData(ComposedExplanationDTO composedExplanationDTO) {
        ComposedExplanation composedExplanation = new ComposedExplanation();
        GenerativeExplanationRequest generativeExplanationRequest = composedExplanationDTO.getGenerativeExplanationRequest();

        generativeExplanationRequest.getQanaryComponents().forEach(component -> {
            try {
                String templatebased = tmplExpService.createOutputExplanation(   // compute template based explanation
                        composedExplanationDTO.getGraphUri(),
                        component,
                        "en"
                );

                GenerativeExplanationObject generativeExplanationObject = genExpService.createGenerativeExplanation(
                        component,
                        generativeExplanationRequest.getShots(),
                        composedExplanationDTO.getGraphUri()
                );

                String prompt = genExpService.createPrompt(
                        generativeExplanationRequest.getShots(),
                        generativeExplanationObject
                );

                String generativeExplanation = genExpService.sendPrompt(prompt, generativeExplanationRequest.getGptModel());

                composedExplanation.addExplanationItem(component.getComponentName(), templatebased, prompt, generativeExplanation, generativeExplanationObject.getTestComponent().getDataSet());
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("{}", e.toString());
            }
        });
        return composedExplanation;
    }

    public ComposedExplanation composedExplanationForInputData(ComposedExplanationDTO composedExplanationDTO) throws Exception {
        List<QanaryComponent> components = composedExplanationDTO.getGenerativeExplanationRequest().getQanaryComponents();
        String graph = composedExplanationDTO.getGraphUri();
        ComposedExplanation composedExplanation = new ComposedExplanation();
        for (QanaryComponent component : components) {

            String sparqlQuery = bindingForGraphAndComponent(graph, component, TemplateExplanationsService.INPUT_DATA_SELECT_QUERY);
            ResultSet results = qanaryRepository.selectWithResultSet(sparqlQuery);
            String query = getBodyFromResultSet(results);

            String templatebasedExplanation = query == null ? "This component didn't used any query" : tmplExpService.createExplanationForQuery(query, graph, component);

            String prompt = genExpService.getInputDataExplanationPrompt(
                    component,
                    query == null ? "" : query,
                    composedExplanationDTO.getGenerativeExplanationRequest().getShots()
            );
            String gptExplanation = genExpService.sendPrompt(prompt, composedExplanationDTO.getGenerativeExplanationRequest().getGptModel());
            composedExplanation.addExplanationItem(component.getComponentName(), templatebasedExplanation, prompt, gptExplanation, query);
        }
        return composedExplanation;
    }

    // TODO: Later, refactor existing methods which add bindings (and execute the query?)
    public String bindingForGraphAndComponent(String graph, QanaryComponent component, String plainQueryPath) throws IOException {
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
        return tmplExpService.getPipelineOutputExplanation(results, graphUri);
    }

    public String explainPipelineOutput(String graphUri, Map<String,String> explanations) throws IOException {
        return tmplExpService.getPipelineOutputExplanation(explanations, graphUri);
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
        return tmplExpService.getPipelineInputExplanation(question);
    }

    // Caching candidate
    public ResultSet getPipelineInformation(String graphUri) throws IOException {
        QuerySolutionMap querySolutionMap = new QuerySolutionMap();
        querySolutionMap.add("graph", ResourceFactory.createResource(graphUri));
        String sparql = QanaryTripleStoreConnector.readFileFromResourcesWithMap(SELECT_PIPELINE_INFORMATION, querySolutionMap);
        return qanaryRepository.selectWithResultSet(sparql);
    }

    public String getPipelineExplanation(String graph) throws IOException {
        return tmplExpService.getPipelineOutputExplanation(
                this.getPipelineInformation(graph),
                graph
        );
    }

    public String getComposedExplanation(QanaryExplanationData body) throws IOException {
        String graph = body.getGraph();
        String component = body.getComponent();
        String explanation = null;
        String inputExplanation = null;
        String outputExplanation = null;
        if (component == null) {
            inputExplanation = explainPipelineInput(graph);
            outputExplanation = explainPipelineOutput(graph);
            return getPipelineExplanation(graph);
        } else {
            QanaryComponent qanaryComponent = new QanaryComponent(component);
            inputExplanation = getTemplateComponentInputExplanation(graph, qanaryComponent);
            outputExplanation = getTemplateComponentOutputExplanation(graph, qanaryComponent, "en");
        }
        return tmplExpService.composeInputAndOutputExplanations(inputExplanation, outputExplanation, component);
    }

    public String getTemplateComponentOutputExplanation(String graph, QanaryComponent component, String lang) throws IOException {
        return tmplExpService.createOutputExplanation(graph, component, lang);
    }

    protected String getComponentExplanation(String graph, QanaryComponent qanaryComponent) throws IOException {
        return tmplExpService.composeInputAndOutputExplanations(
                getTemplateComponentInputExplanation(graph, qanaryComponent),
                getTemplateComponentOutputExplanation(graph, qanaryComponent, "en"),
                qanaryComponent.getComponentName()
        );
    }

    protected String getPipelineExplanation(String graph, Map<String,String> explanations) throws IOException {
        return tmplExpService.composeInputAndOutputExplanations(
                explainPipelineInput(graph),
                explainPipelineOutput(graph, explanations),
                null
        );
    }

    public String explain(QanaryExplanationData data) throws IOException {
        logger.info("Explaining ...");
        if(data.getExplanations() == null || data.getExplanations().isEmpty()) { // componentName, questionId and graph provided // component-based explanation
            QanaryComponent qanaryComponent = new QanaryComponent(data.getComponent());
            try {
                return getComponentExplanation(data.getGraph(), qanaryComponent); // TODO: Add lang-support
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else if (data.getComponent() != "" || data.getComponent() != null){ // componentName, componentExplanations, questionId and graph are provided // PaC-based explanation
            String explanationTemplate = tmplExpService.getStringFromFile("/explanations/pipeline_component/en_prefix");
            String components = StringUtils.join(data.getExplanations().keySet().toArray(), ", ");
            return explanationTemplate
                    .replace("${component}", data.getComponent())
                    .replace("${components}", components)
                    .replace("${question}", qanaryRepository.getQuestionFromQuestionId(data.getQuestionId() + "/raw"))
                    .replace("${questionId}", data.getQuestionId())
                    .replace("${graph}", data.getGraph())
                    .replace("${componentsAndExplanations}", composeComponentExplanations(data.getExplanations()));
        }
        else { // only questionId and graph are provided // System-based explanation
            // TODO: Implement. Extend pipeline with /explain or handle it here?
        }
        return null;
    }

    public String composeComponentExplanations(Map<String,String> componentAndExplanation) {
        StringBuilder composedExplanations = new StringBuilder();
        componentAndExplanation.forEach((k,v) -> {
            composedExplanations.append (k + ": " + v + "\n\n");
        });
        return composedExplanations.toString();
    }

    // Explanation Tree
    public JSONObject getAggregatedExplanations(String graph, String methodId) throws IOException {
        QuerySolutionMap qsm = new QuerySolutionMap();
        qsm.add("methodId", ResourceFactory.createResource(methodId));
        qsm.add("graph", ResourceFactory.createResource(graph));
        String query = QanaryTripleStoreConnector.readFileFromResourcesWithMap(SELECT_CHILD_PARENT_METHODS, qsm);
        ResultSet childParentPairs = qanaryRepository.selectWithResultSet(query);

        // Decide which explanation mode to use

    }

    /*
    * 1st step: Compute for all involved methods either data or explanations (atomic axplanations)
    * 2nd step: Recreate Child/Parent structure
     */






}

