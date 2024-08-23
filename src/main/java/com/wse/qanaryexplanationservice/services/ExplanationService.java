package com.wse.qanaryexplanationservice.services;

import com.wse.qanaryexplanationservice.helper.dtos.ComposedExplanationDTO;
import com.wse.qanaryexplanationservice.helper.dtos.QanaryExplanationData;
import com.wse.qanaryexplanationservice.helper.pojos.ComposedExplanation;
import com.wse.qanaryexplanationservice.helper.pojos.GenerativeExplanationObject;
import com.wse.qanaryexplanationservice.helper.pojos.GenerativeExplanationRequest;
import com.wse.qanaryexplanationservice.helper.pojos.QanaryComponent;
import com.wse.qanaryexplanationservice.repositories.QanaryRepository;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class    ExplanationService {

    private final Logger logger = LoggerFactory.getLogger(ExplanationService.class);
    @Autowired
    private QanaryRepository qanaryRepository;
    private final String SELECT_PIPELINE_INFORMATION = "/queries/select_pipeline_information.rq";
    private final String SYSTEM_EXPLANATION_TEMPLATE = "/explanations/pipeline_component/en_prefix";
    @Autowired
    private GenerativeExplanationsService generativeExplanationsService;
    @Autowired
    private TemplateExplanationsService tmplExplanationService;


    /**
     * Explains the input data of a certain component
     * @param graph KG
     * @param qanaryComponent If null, the pipeline's input is being explained
     * @return Template-based explanation (textual)
     * @throws IOException
     */
    public String getInputTemplateExplanation(String graph, QanaryComponent qanaryComponent) throws IOException {
        if(qanaryComponent != null)
            return tmplExplanationService.createInputExplanation(graph, qanaryComponent);
        else
            return explainPipelineInput(graph);
    }

    /**
     * Explain the output data of a certain component
     * @param graph KG
     * @param qanaryComponent If null, the pipeline's output is being explained
     * @return Template-based explanation (textual)
     * @throws IOException
     */
    public String getOutputTemplateExplanation(String graph, QanaryComponent qanaryComponent) throws IOException {
        if(qanaryComponent != null)
            return tmplExplanationService.createOutputExplanation(graph, qanaryComponent, "en");
        else
            return explainPipelineOutput(graph); // TODO: Extend languages for pipeline explanation
    }

    // TODO: Should explain the input data with generative AI
    public String getInputGenerativeExplanation() {
        return null;
    }

    // TODO: Should explain the output data with generative AI
    public String getOutputGenerativeExplanation() {
        return null;
    }

    /**
     * Explains a concrete component by composing the input and output explanation
     * @return Textual explanation
     */
    public String getComposedTemplateExplanation(String graph, QanaryComponent qanaryComponent) throws IOException {
        return tmplExplanationService.composeInputAndOutputExplanations(
                this.getInputTemplateExplanation(graph,qanaryComponent),
                this.getOutputTemplateExplanation(graph,qanaryComponent),
                qanaryComponent.getComponentName()
        );
    }

    /**
     * Explains the system by explaining the pipeline data and attach the subcomponents' explanations
     * @param data Contains component name, questionId, graph, explanations for subcomponents
     * @return Explanation (textual)
     */
    public String getSystemExplanation(QanaryExplanationData data) {
        String explanationTemplate = TemplateExplanationsService.getStringFromFile(SYSTEM_EXPLANATION_TEMPLATE);
        String components = StringUtils.join(data.getExplanations().keySet().toArray(), ", ");
        return explanationTemplate
                .replace("${component}", data.getComponent())
                .replace("${components}", components)
                .replace("${question}", qanaryRepository.getQuestionFromQuestionId(data.getQuestionId() + "/raw"))
                .replace("${questionId}", data.getQuestionId())
                .replace("${graph}", data.getGraph())
                .replace("${componentsAndExplanations}", this.concatComponentAndExplanation(data.getExplanations()));
    }

    /**
     * Creates a pair of input-data explanations (Generative and template-based) with provided settings
     * @param composedExplanationDTO
     * @return Explanation
     */
    public ComposedExplanation templateAndGenerativeInputExplanation(ComposedExplanationDTO composedExplanationDTO) throws Exception {
        List<QanaryComponent> components = composedExplanationDTO.getGenerativeExplanationRequest().getQanaryComponents();
        String graph = composedExplanationDTO.getGraphUri();
        ComposedExplanation composedExplanation = new ComposedExplanation();
        for (QanaryComponent component : components) {

            String sparqlQuery = bindingForGraphAndComponent(graph, component, TemplateExplanationsService.INPUT_DATA_SELECT_QUERY);
            ResultSet results = qanaryRepository.selectWithResultSet(sparqlQuery);
            QuerySolution querySolution = results.next();
            String query = querySolution.get("body").toString();

            String templateBasedExplanation = tmplExplanationService.createExplanationForQuery(query, graph, component);

            String prompt = generativeExplanationsService.getInputDataExplanationPrompt(
                    component,
                    query,
                    composedExplanationDTO.getGenerativeExplanationRequest().getShots()
            );
            String gptExplanation = generativeExplanationsService.sendPrompt(prompt, composedExplanationDTO.getGenerativeExplanationRequest().getGptModel());
            composedExplanation.addExplanationItem(component.getComponentName(), templateBasedExplanation, prompt, gptExplanation, query);
        }
        return composedExplanation;
    }

    /**
     * Creates a pair of output-data explanations (Generative and template-based) with provided settings
     * @param composedExplanationDTO
     * @return Explanation
     */
    public ComposedExplanation templateAndGenerativeOutputExplanation(ComposedExplanationDTO composedExplanationDTO) {
        ComposedExplanation composedExplanation = new ComposedExplanation();
        GenerativeExplanationRequest generativeExplanationRequest = composedExplanationDTO.getGenerativeExplanationRequest();

        generativeExplanationRequest.getQanaryComponents().forEach(component -> {
            try {
                String templateBased = getOutputTemplateExplanation(composedExplanationDTO.getGraphUri(), component);

                GenerativeExplanationObject generativeExplanationObject = generativeExplanationsService.createGenerativeExplanation(
                        component,
                        generativeExplanationRequest.getShots(),
                        composedExplanationDTO.getGraphUri()
                );

                String prompt = generativeExplanationsService.createPrompt(
                        generativeExplanationRequest.getShots(),
                        generativeExplanationObject
                );

                String generativeExplanation = generativeExplanationsService.sendPrompt(prompt, generativeExplanationRequest.getGptModel());

                composedExplanation.addExplanationItem(component.getComponentName(), templateBased, prompt, generativeExplanation, generativeExplanationObject.getTestComponent().getDataSet());
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("{}", e.toString());
            }
        });
        return composedExplanation;
    }

    /**
     * Returns the wholesome explanation for a component or a pipeline
     * @param data
     * @return
     */
    public String explain(QanaryExplanationData data) throws RuntimeException, IOException {
        logger.info("Explaining {}", data.getComponent());
        if (data.getExplanations() == null || data.getExplanations().isEmpty()) { // componentName, questionId and graph provided // component-based explanation
            QanaryComponent qanaryComponent = new QanaryComponent(data.getComponent());
            try {
                return this.getComposedTemplateExplanation(data.getGraph(), qanaryComponent);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (data.getComponent() != "" || data.getComponent() != null) { // Composes components' explanations
            return getSystemExplanation(data);
        }
        String errormsg = "Error while computing explanations: " + data.getComponent() == null ? "No component passed" : "No explanations for sub-components given";
        logger.error(errormsg);
        throw new RuntimeException(errormsg);
    }

    /* ************ HELPER FUNCTIONS ************** */

    /**
     * Wrapper for template service, fetches required information and utilizes the ResultSet to return the explanation
     * @param graphUri KG
     * @return Explanation (textual)
     * @throws IOException
     */
    public String explainPipelineOutput (String graphUri) throws IOException {
        ResultSet results = getPipelineInformation(graphUri);
        return tmplExplanationService.getPipelineOutputExplanation(results, graphUri);
    }

    /**
     * Fetches the pipelines' used components and the questionId
     * @param graphUri KG
     * @return ResultSet containing used components and the questionId
     * @throws IOException
     */
    public ResultSet getPipelineInformation (String graphUri) throws IOException {
        QuerySolutionMap querySolutionMap = new QuerySolutionMap();
        querySolutionMap.add("graph", ResourceFactory.createResource(graphUri));
        String sparql = QanaryTripleStoreConnector.readFileFromResourcesWithMap(SELECT_PIPELINE_INFORMATION, querySolutionMap);
        return qanaryRepository.selectWithResultSet(sparql);
    }

    /**
     * Concat the components with their corresponding explanation
     * @param explanations Key: Component, Value: Explanation
     * @return String-representation of the map in the manner: Component: Explanation \n\n
     */
    public String concatComponentAndExplanation (Map < String, String > explanations){
        StringBuilder composedExplanations = new StringBuilder();
        explanations.forEach((k, v) -> {
            composedExplanations.append(k + ": " + v + "\n\n");
        });
        return composedExplanations.toString();
    }

    /**
     * Computes the explanation for the pipeline's input data
     * @param graphUri KG
     * @return Explanation (textual)
     * @throws IOException
     */
    public String explainPipelineInput (String graphUri) throws IOException {
        ResultSet results = getPipelineInformation(graphUri);
        String questionId = "";
        while (results.hasNext()) {
            QuerySolution result = results.next();
            if (result.contains("questionId"))
                questionId = result.get("questionId").toString();
        }
        String question = qanaryRepository.getQuestionFromQuestionId(questionId);
        return tmplExplanationService.getPipelineInputExplanation(question);
    }

    /**
     * Binds graph and component to passed plain query
     * @param graph KG
     * @param component Qanary component
     * @param plainQueryPath Plain SPARQL query with variables ?graph and ?component
     * @return Executable SPARQL query with replaced variables
     * @throws IOException
     */
    public String bindingForGraphAndComponent (String graph, QanaryComponent component, String plainQueryPath) throws IOException {
        QuerySolutionMap bindings = new QuerySolutionMap();
        bindings.add("graph", ResourceFactory.createResource(graph));
        bindings.add("component", ResourceFactory.createResource(component.getPrefixedComponentName()));
        return QanaryTripleStoreConnector.readFileFromResourcesWithMap(plainQueryPath, bindings);
    }





}

