package com.wse.qanaryexplanationservice.services;

import com.wse.qanaryexplanationservice.helper.dtos.ComposedExplanationDTO;
import com.wse.qanaryexplanationservice.helper.pojos.ComposedExplanation;
import com.wse.qanaryexplanationservice.helper.pojos.GenerativeExplanationObject;
import com.wse.qanaryexplanationservice.helper.pojos.GenerativeExplanationRequest;
import com.wse.qanaryexplanationservice.helper.pojos.QanaryComponent;
import com.wse.qanaryexplanationservice.repositories.QanaryRepository;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
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

@Service
public class ExplanationService {

    private final Logger logger = LoggerFactory.getLogger(ExplanationService.class);

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

    public String getTemplateComponentInputExplanation(String graphUri, String componentUri) throws IOException {
        return tmplExpService.createInputExplanation(graphUri, componentUri);
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
                String templatebased = tmplExpService.explainComponentAsText(   // compute template based explanation
                        composedExplanationDTO.getGraphUri(),
                        component.getComponentName(),
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

            QuerySolutionMap bindings = new QuerySolutionMap();
            bindings.add("graph", ResourceFactory.createResource(graph));
            bindings.add("component", ResourceFactory.createResource(component.getPrefixedComponentName()));
            String query = QanaryTripleStoreConnector.readFileFromResourcesWithMap(TemplateExplanationsService.INPUT_DATA_SELECT_QUERY, bindings);
            ResultSet results = qanaryRepository.selectWithResultSet(query);

            QuerySolution querySolution = results.next(); // TODO: Add component to composedExplanation
            String resultQuery = querySolution.get("body").toString();

            String templatebasedExplanation = tmplExpService.createExplanationForQuery(querySolution, graph, component.getComponentName());

            String prompt = genExpService.getInputDataExplanationPrompt(
                    component.getComponentName(),
                    resultQuery,
                    composedExplanationDTO.getGenerativeExplanationRequest().getShots()
            );
            String gptExplanation = genExpService.sendPrompt(prompt, composedExplanationDTO.getGenerativeExplanationRequest().getGptModel());
            composedExplanation.addExplanationItem(component.getComponentName(), templatebasedExplanation, prompt, gptExplanation, resultQuery);
        }
        return composedExplanation;
    }

}
