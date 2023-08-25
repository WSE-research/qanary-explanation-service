package com.wse.qanaryexplanationservice.controller;

import com.wse.qanaryexplanationservice.services.ExplanationService;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@RestController
@ControllerAdvice
public class ExplanationController {

    private static final String DBpediaSpotlight_SPARQL_QUERY = "/queries/explanation_for_dbpediaSpotlight_sparql_query.rq";
    private static final String GENERAL_EXPLANATION_SPARQL_QUERY = "/queries/general_explanation.rq";
    private final Logger logger = LoggerFactory.getLogger(ExplanationController.class);

    @Autowired
    private ExplanationService explanationService;


    @CrossOrigin
    @GetMapping(value = {"/explanations/{graphURI}", "/explanations/{graphURI}/{componentURI}"}, produces = {
            "application/rdf+xml",
            "text/turtle",
            "application/ld+json",
            "*/*"})
    @Operation(
            summary = "Get either the explanation for a the whole QA-system on a graphURI"
                    + "or the explanation for a specific component by attaching the componentURI",
            description = "This endpoint currently offers two sort of requests: "
                    + "\n 1. Explanation for a QA-system by providing a graphURI and "
                    + "\n 2. Explanation for a component within a QA-process by providing the graphURI"
                    + "\n as well as the URI for the component"
                    + "\n Note: You must at least provide a graphURI to use this endpoint"
    )
    public ResponseEntity<?> getExplanations(
            @PathVariable(required = true) String graphURI,
            @PathVariable(required = false) String componentURI,
            @RequestHeader(value = "accept", required = false) String acceptHeader) throws Exception {
        if (componentURI == null) {
            String result = explanationService.explainQaSystem(graphURI, GENERAL_EXPLANATION_SPARQL_QUERY, acceptHeader);
            if (result != null)
                return new ResponseEntity<>(result, HttpStatus.OK);
            else
                return new ResponseEntity<>(null, HttpStatus.NOT_ACCEPTABLE);
        } else {
            String result = this.explanationService.explainSpecificComponent(graphURI, componentURI, GENERAL_EXPLANATION_SPARQL_QUERY, acceptHeader);
            if (result != null)
                return new ResponseEntity<>(result, HttpStatus.OK);
            else
                return new ResponseEntity<>(result, HttpStatus.BAD_REQUEST);
        }

    }

}
