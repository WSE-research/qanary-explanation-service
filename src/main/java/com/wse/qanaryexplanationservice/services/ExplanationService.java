package com.wse.qanaryexplanationservice.services;

import com.wse.qanaryexplanationservice.helper.dtos.ComposedExplanationDTO;
import com.wse.qanaryexplanationservice.helper.pojos.ComposedExplanation;
import com.wse.qanaryexplanationservice.helper.pojos.GenerativeExplanationObject;
import com.wse.qanaryexplanationservice.helper.pojos.GenerativeExplanationRequest;
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

    public String getQaSystemExplanation(String header, String graphUri) throws Exception {
        return tmplExpService.explainQaSystem(header, graphUri);
    }

    public String getTemplateComponentExplanation(String graphUri, String componentUri, String header) throws Exception {
        return tmplExpService.explainSpecificComponent(graphUri, componentUri, header);
    }

    public String getTemplateComponentInputExplanation(String graphUri, String componentUri) throws IOException {
        return tmplExpService.createInputExplanation(graphUri, componentUri);
    }

    /**
     * Controller called method to start the process explaining several components with both approaches;
     * the rulebased and the generative one.
     *
     * @param composedExplanationDTO
     */
    public ComposedExplanation composedExplanationsForOutputData(ComposedExplanationDTO composedExplanationDTO) {
        logger.info("Request object: ", composedExplanationDTO);
        ComposedExplanation composedExplanation = new ComposedExplanation();
        GenerativeExplanationRequest generativeExplanationRequest = composedExplanationDTO.getGenerativeExplanationRequest();

        generativeExplanationRequest.getQanaryComponents().forEach(component -> {
            try {
                List<String> annotationTypesUsedByComponent = tmplExpService.fetchAllAnnotations(composedExplanationDTO.getGraphUri(), component.getComponentName());
                String templatebased = tmplExpService.createTextualExplanation(   // compute template based explanation
                        composedExplanationDTO.getGraphUri(),
                        component.getComponentName(),
                        "en",
                        annotationTypesUsedByComponent
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
        List<String> components = composedExplanationDTO.getGenerativeExplanationRequest().getQanaryComponents().stream().map(component -> component.getComponentName()).toList();
        String graph = composedExplanationDTO.getGraphUri();
        ComposedExplanation composedExplanation = new ComposedExplanation();
        for (String component : components) {

            QuerySolutionMap bindings = new QuerySolutionMap();
            bindings.add("graph", ResourceFactory.createResource(graph));
            bindings.add("component", ResourceFactory.createResource("urn:qanary:" + component));
            String query = QanaryTripleStoreConnector.readFileFromResourcesWithMap(tmplExpService.INPUT_DATA_SELECT_QUERY, bindings);
            ResultSet results = QanaryRepository.selectWithResultSet(query);

            QuerySolution querySolution = results.next(); // TODO: Add component to composedExplanation
            String resultQuery = querySolution.get("body").toString();

            String templatebasedExplanation = tmplExpService.createExplanationForQuery(querySolution, graph, component);

            String generativeExplanation = genExpService.createGenerativeExplanationInputDataForOneComponent(
                    component,
                    resultQuery,
                    composedExplanationDTO.getGenerativeExplanationRequest().getShots(),
                    composedExplanationDTO.getGenerativeExplanationRequest().getGptModel()
            );
            composedExplanation.addExplanationItem(component, templatebasedExplanation, "", generativeExplanation, resultQuery);
        }
        return composedExplanation;

    }

}
