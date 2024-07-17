package com.wse.qanaryexplanationservice.services;

import com.wse.qanaryexplanationservice.helper.dtos.ComposedExplanationDTO;
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
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

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
    @Autowired
    private GenerativeExplanations generativeExplanations;

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

    public String getComposedExplanation(String graph, String component) throws IOException {
        String explanation = null;
        String inputExplanation = null;
        String outputExplanation = null;
        if(component == null)
            return explainPipeline(graph);
        if (component == "pipeline") {
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

    public ArrayList<String> getInputExplanationDataset(QanaryComponent qanaryComponent, String graph) throws IOException {
        String sparqlQuery = bindingForGraphAndComponent(graph, qanaryComponent, TemplateExplanationsService.INPUT_DATA_SELECT_QUERY);
        ResultSet results = qanaryRepository.selectWithResultSet(sparqlQuery);
        ArrayList list = new ArrayList<>();
        while(results.hasNext()) {
            list.add(results.next().get("body").toString());
        }
        return list;
    }

    public String getOutputExplanationDataset(QanaryComponent qanaryComponent, String graph) throws Exception {
        return generativeExplanations.createDataset(qanaryComponent, graph, null);
    }

    public String explainPipeline(String graphUri) throws IOException {
        try {
            String explanation = QanaryTripleStoreConnector.readFileFromResources("/explanations/pipeline_system/en_prefix");
            String questionUri = getQuestionFromGraph(graphUri);
            String question = qanaryRepository.getQuestionFromQuestionId(questionUri);
            List<String> componentExplanations = getComponentExplanations(graphUri);

            return explanation
                    .replace("${question}", question)
                    .replace("${questionId}", questionUri)
                    .replace("${graph}", graphUri)
                    .replace("${components}", StringUtils.join(componentExplanations, "\n\n").toString());
        } catch(Exception e) {
            e.printStackTrace();
            return "Explanation for graph " + graphUri + " couldn't be computed with error: " + e.getMessage();
        }
    }

    public String getQuestionFromGraph(String graph) throws IOException {
        QuerySolutionMap qsm = new QuerySolutionMap();
        qsm.add("graph", ResourceFactory.createResource(graph));
        String sparql = QanaryTripleStoreConnector.readFileFromResourcesWithMap("/queries/question_query.rq", qsm);
        ResultSet result = qanaryRepository.selectWithResultSet(sparql);
        return result.next().get("source").toString();
    }

    public List<QanaryComponent> getUsedComponents(String graph) throws IOException {
        QuerySolutionMap qsm = new QuerySolutionMap();
        qsm.add("graph", ResourceFactory.createResource(graph));
        String sparql = QanaryTripleStoreConnector.readFileFromResourcesWithMap("/queries/components_sparql_query.rq", qsm);
        ResultSet components = qanaryRepository.selectWithResultSet(sparql);
        List<QanaryComponent> componentList = new ArrayList<>();
        while(components.hasNext()) {
            QuerySolution qs = components.next();
            componentList.add(new QanaryComponent(qs.get("component").toString()));
        }
        return componentList;
    }

    public List<String> getComponentExplanations(String graph) throws IOException {
        List<QanaryComponent> qanaryComponents = getUsedComponents(graph);
        List<String> explanations = new ArrayList<>();

        qanaryComponents.forEach(component -> {
            try {
                explanations.add(getComposedExplanation(graph, component.getComponentName()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        return explanations;
    }

}
