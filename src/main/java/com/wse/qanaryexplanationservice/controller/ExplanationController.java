package com.wse.qanaryexplanationservice.controller;

import com.wse.qanaryexplanationservice.exceptions.ExplanationException;
import com.wse.qanaryexplanationservice.helper.dtos.ComposedExplanationDTO;
import com.wse.qanaryexplanationservice.helper.dtos.QanaryExplanationData;
import com.wse.qanaryexplanationservice.helper.pojos.ComposedExplanation;
import com.wse.qanaryexplanationservice.helper.pojos.QanaryComponent;
import com.wse.qanaryexplanationservice.exceptions.ExplanationExceptionComponent;
import com.wse.qanaryexplanationservice.exceptions.ExplanationExceptionPipeline;
import com.wse.qanaryexplanationservice.services.ExplanationService;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@ControllerAdvice
public class ExplanationController {

    private final Logger logger = LoggerFactory.getLogger(ExplanationController.class);
    @Autowired
    private ExplanationService explanationService;

    /**
     * Computes the explanations for (currently) the output data for a specific graph and/or component
     *
     * @param component    @see QanaryComponent
     * @param acceptHeader The answer is formatted as RDF, possibly in RDF/XML, TTL or JSON-LD.
     * @return Explanation for system or component as RDF
     */
    @CrossOrigin
    @GetMapping(value = {"/explanations/{graphURI}", "/explanations/{graphURI}/{component}"}, produces = {
            "application/rdf+xml",
            "text/turtle",
            "application/ld+json",
            "*/*"})
    @Operation(
            summary = "Get either the explanation for a the whole QA-system on a graphURI"
                    + "or the explanation for a specific component by attaching the componentURI",
            description = """
                    This endpoint currently offers two sort of requests:\s
                     1. Explanation for a QA-system by providing a graphURI and\s
                     2. Explanation for a component within a QA-process by providing the graphURI
                     as well as the URI for the component
                     Note: You must at least provide a graphURI to use this endpoint"""
    )
    public ResponseEntity<?> getExplanations(
            @PathVariable String graphURI,
            @PathVariable(required = false) QanaryComponent component,
            @RequestHeader(value = "accept", required = false) String acceptHeader) throws Exception {
        if (component == null) {
            String result = explanationService.getQaSystemExplanation(graphURI, acceptHeader);
            if (result != null)
                return new ResponseEntity<>(result, HttpStatus.OK);
            else
                return new ResponseEntity<>(null, HttpStatus.NOT_ACCEPTABLE);
        } else {
            String result = this.explanationService.getTemplateComponentExplanation(graphURI, component, acceptHeader);
            if (result != null)
                return new ResponseEntity<>(result, HttpStatus.OK);
            else
                return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
    }

    @CrossOrigin
    @GetMapping(value = {"/inputdata/{graphURI}/{componentURI}"}, produces = {
            "application/rdf+xml",
            "text/turtle",
            "application/ld+json",
            "*/*"})
    @Operation(
            summary = "Computes the rulebased explanation for a specific component",
            description = """
                    This endpoint doesn't require a body and only takes the graph and component as path variables.
                    The component must include the prefixes, e.g. 'urn:qanary:'
                    """
    )
    public ResponseEntity<?> getInputExplanation(
            @PathVariable String graphURI,
            @PathVariable(required = false) QanaryComponent component) throws IOException {
        if (component != null) {
            return new ResponseEntity<>(this.explanationService.getTemplateComponentInputExplanation(graphURI, component), HttpStatus.OK);
        } else
            try {
                String explanation = explanationService.explainPipelineInput(graphURI);
                return new ResponseEntity<>(explanation, HttpStatus.OK);
            } catch (Exception e) {
                return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
            }
    }

    @CrossOrigin
    @GetMapping(value = {"/outputdata/{graphURI}/{componentURI}"}, produces = {
            "application/rdf+xml",
            "text/turtle",
            "application/ld+json",
            "*/*"
    })
    @Operation(
            summary = "Computes the rulebased explanation for a specific component",
            description = """
                    This endpoint doesn't require a body and only takes the graph and component as path variables.
                    The component must include the prefixes, e.g. 'urn:qanary:'
                    """
    )
    public ResponseEntity<?> getOutputExplanation(
            @PathVariable String graphURI,
            @PathVariable(required = false) QanaryComponent component,
            @RequestHeader(value = "accept", required = false) String acceptHeader) {
        String explanation = "";
        try {
            if (component == null) {
                explanation = explanationService.getTemplateComponentOutputExplanation(graphURI, component, acceptHeader);
            } else {
                explanation = explanationService.explainPipelineOutput(graphURI);
            }
        } catch (Exception e) {
            logger.error("{}", e.getMessage());
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(explanation, HttpStatus.OK);
    }

    @CrossOrigin
    @PostMapping(value = {"/composedexplanations/inputdata"}, produces = {
            "application/rdf+xml",
            "text/turtle",
            "application/ld+json",
            "*/*"})
    @Operation(
            summary = "Computes the rulebased and generative input-data explanations for all components included in the body.",
            description = """
                    The Request body should follow this structure:
                            {
                                "graphUri": "",
                                "generativeExplanationRequest": {
                                    "shots": 1,
                                    "gptModel": "gpt3.5",
                                    "qanaryComponents": [
                                        {
                                            "componentName": null,
                                            "componentMainType": "AnnotationOfInstance"
                                        },
                                        {
                                        ...
                                        },
                                        ...
                                    ]
                                }
                            }
                    """
    )
    public ResponseEntity<?> getComposedExplanationInputData(@RequestBody ComposedExplanationDTO composedExplanationDTO) {
        try {
            ComposedExplanation composedExplanationInputData = this.explanationService.composedExplanationForInputData(composedExplanationDTO);
            return new ResponseEntity<>(composedExplanationInputData, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("{}", e.toString());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }

    }

    @PostMapping(value = {"/composedexplanations/outputdata"})
    @Operation(
            summary = "Computes the rulebased and generative output-data explanations for all components included in the body.",
            description = """
                    The Request body should follow this structure:
                            {
                                "graphUri": "",
                                "generativeExplanationRequest": {
                                    "shots": 1,
                                    "gptModel": "gpt3.5",
                                    "qanaryComponents": [
                                        {
                                            "componentName": null,
                                            "componentMainType": "AnnotationOfInstance"
                                        },
                                        {
                                        ...
                                        },
                                        ...
                                    ]
                                }
                            }
                    """
    )
    public ResponseEntity<?> getComposedExplanationOutputData(@RequestBody ComposedExplanationDTO composedExplanationDTO) {
        try {
            ComposedExplanation composedExplanationInputData = this.explanationService.composedExplanationsForOutputData(composedExplanationDTO);
            return new ResponseEntity<>(composedExplanationInputData, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("{}", e.toString());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Endpoint explaining a component / pipeline input and output data
     * @param body TODO
     * @return
     * @throws IOException
     */
    @PostMapping(value = {"/explain"})
    @Operation()
    public ResponseEntity<?> getComposedExplanation(@RequestBody QanaryExplanationData body) throws ExplanationException { // TODO: Extend methods
        try {
            logger.info(body.getComponent());
            String explanation = explanationService.explain(body);
            return new ResponseEntity<>(explanation, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}