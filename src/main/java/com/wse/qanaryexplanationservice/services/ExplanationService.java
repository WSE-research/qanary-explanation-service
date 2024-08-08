package com.wse.qanaryexplanationservice.services;

import com.wse.qanaryexplanationservice.exceptions.ExplanationExceptionComponent;
import com.wse.qanaryexplanationservice.exceptions.ExplanationExceptionPipeline;
import com.wse.qanaryexplanationservice.helper.dtos.ComposedExplanationDTO;
import com.wse.qanaryexplanationservice.helper.dtos.QanaryExplanationData;
import com.wse.qanaryexplanationservice.helper.pojos.ComposedExplanation;
import com.wse.qanaryexplanationservice.helper.pojos.GenerativeExplanationObject;
import com.wse.qanaryexplanationservice.helper.pojos.GenerativeExplanationRequest;
import com.wse.qanaryexplanationservice.helper.pojos.QanaryComponent;
import com.wse.qanaryexplanationservice.repositories.QanaryRepository;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
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
public class ExplanationService {

    private final Logger logger = LoggerFactory.getLogger(ExplanationService.class);
    private final String SELECT_PIPELINE_INFORMATION = "/queries/select_pipeline_information.rq";
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

            String templatebasedExplanation = tmplExpService.createExplanationForQuery(query, graph, component);

            String prompt = genExpService.getInputDataExplanationPrompt(
                    component,
                    query,
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
        QuerySolution querySolution = resultSet.next();
        return querySolution.get("body").toString();
    }

    /**
     * Similar to a system's explanation
     */
    public String explainPipelineOutput(String graphUri) throws IOException {
        ResultSet results = getPipelineInformation(graphUri);
        return tmplExpService.getPipelineOutputExplanation(results, graphUri);
    }

    public String explainPipelineOutput(String graphUri, Map<String,String> explanations) {
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

    public String getComposedExplanation(QanaryExplanationData body) throws IOException {
        String graph = body.getGraph();
        String component = body.getComponent();
        String explanation = null;
        String inputExplanation = null;
        String outputExplanation = null;
        if (component == null) {
            inputExplanation = explainPipelineInput(graph);
            outputExplanation = explainPipelineOutput(graph);
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

    // Deprecated, alt. approach
    /*
    public String explain(QanaryExplanationData explanationData) throws ExplanationExceptionComponent, ExplanationExceptionPipeline {
        if(explanationData.getExplanations() != null) { // It's a pipeline (as component) -> Composes explanations
            try {
                return getPipelineExplanation(
                  explanationData.getGraph(),
                  explanationData.getExplanations()
                );
            } catch(Exception e) {
                throw new ExplanationExceptionPipeline();
            }
        }
        else { // It's a component
            QanaryComponent qanaryComponent = new QanaryComponent(explanationData.getComponent());
            try {
                return getComponentExplanation(explanationData.getGraph(), qanaryComponent);
            } catch (IOException e) {
                e.printStackTrace();
                throw new ExplanationExceptionComponent();
            }
        }
    }
     */

    public String explain(QanaryExplanationData data) {
        logger.info("Explaining  component {}", data.getComponent());
        if(data.getExplanations() == null || data.getExplanations().isEmpty()) { // componentName, questionId and graph provided // component-based explanation
            QanaryComponent qanaryComponent = new QanaryComponent(data.getComponent());
            try {
                return getComponentExplanation(data.getGraph(), qanaryComponent); // TODO: Add lang-support
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else { // componentName, componentExplanations, questionId and graph are provided // PaC-based explanation
            String explanationTemplate = tmplExpService.getStringFromFile("/explanations/pipeline_component/en_prefix");
            String components = StringUtils.join(data.getExplanations().keySet().toArray(), ", ");
            String question = qanaryRepository.getQuestionFromQuestionId(data.getQuestionId() + "/raw");
            return explanationTemplate
                    .replace("${question}", question)
                    .replace("${questionId}", data.getQuestionId())
                    .replace("${graph}", data.getGraph())
                    .replace("${component}", data.getComponent())
                    .replace("${components}", components)
                    .replace("${componentsAndExplanations}", composeComponentExplanations(data.getExplanations()));
        }
/*
        else { // only questionId and graph are provided // System-based explanation
            // TODO: Implement. Extend pipeline with /explain or handle it here?
        }
        return null;

 */
        return "Explanation couldn't be created.";
    }

    public String composeComponentExplanations(Map<String,String> componentAndExplanation) {
        StringBuilder composedExplanations = new StringBuilder();
        componentAndExplanation.forEach((k,v) -> {
            composedExplanations.append (k + ": " + v + "\n\n");
        });
        return composedExplanations.toString();
    }

}

