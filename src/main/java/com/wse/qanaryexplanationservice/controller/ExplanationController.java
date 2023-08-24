package com.wse.qanaryexplanationservice.controller;

import com.wse.qanaryexplanationservice.services.ExplanationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.annotation.*;

@ControllerAdvice
@RestController
public class ExplanationController {

    private static final String DBpediaSpotlight_SPARQL_QUERY = "/queries/explanation_for_dbpediaSpotlight_sparql_query.rq";
    private static final String GENERAL_EXPLANATION_SPARQL_QUERY = "/queries/general_explanation.rq";
    private final Logger logger = LoggerFactory.getLogger(ExplanationController.class);

    @Autowired
    private ExplanationService explanationService;

    @CrossOrigin
    @GetMapping(value = "/explanations/{graphURI}/{componentURI}", produces = {
            "application/rdf+xml",
            "text/turtle",
            "application/ld+json",
            "*/*"})
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

    @CrossOrigin
    @GetMapping(value = "/explainqasystem", produces = {
            "application/rdf+xml",
            "text/turtle",
            "application/ld+json"
    })
    public ResponseEntity<?> getQaSystemExplanation(@RequestParam String graphURI, @RequestHeader(value = "accept", required = false) String acceptHeader) throws Exception {
        String result = explanationService.explainQaSystem(graphURI, GENERAL_EXPLANATION_SPARQL_QUERY, acceptHeader);
        if (result != null)
            return new ResponseEntity<>(result, HttpStatus.OK);
        else
            return new ResponseEntity<>(null, HttpStatus.NOT_ACCEPTABLE);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<String> handleHttpMediaTypeNotAcceptableException() {
        return new ResponseEntity<>("Accepted headers: " +
                "application/rdf+xml, " +
                "text/turtle, " +
                "application/ld+json",
                HttpStatus.NOT_ACCEPTABLE);
    }

}
