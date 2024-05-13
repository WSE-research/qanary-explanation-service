package com.wse.qanaryexplanationservice.controller;

import com.wse.qanaryexplanationservice.dtos.ComposedExplanationDTO;
import com.wse.qanaryexplanationservice.services.ExplanationService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@ControllerAdvice
public class ExplanationController {

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
            description = """
                    This endpoint currently offers two sort of requests:\s
                     1. Explanation for a QA-system by providing a graphURI and\s
                     2. Explanation for a component within a QA-process by providing the graphURI
                     as well as the URI for the component
                     Note: You must at least provide a graphURI to use this endpoint"""
    )
    public ResponseEntity<?> getExplanations(
            @PathVariable String graphURI,
            @PathVariable(required = false) String componentURI,
            @RequestHeader(value = "accept", required = false) String acceptHeader) throws Exception {
        if (componentURI == null) {
            String result = explanationService.explainQaSystem(graphURI, acceptHeader);
            if (result != null)
                return new ResponseEntity<>(result, HttpStatus.OK);
            else
                return new ResponseEntity<>(null, HttpStatus.NOT_ACCEPTABLE);
        } else {
            String result = this.explanationService.explainSpecificComponent(graphURI, componentURI, acceptHeader);
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
    public ResponseEntity<?> getInputExplanation(
            @PathVariable String graphURI,
            @PathVariable(required = false) String componentURI) throws IOException {
        return new ResponseEntity<>(this.explanationService.createInputExplanation(graphURI,componentURI), HttpStatus.OK);
    }

    @CrossOrigin
    @PostMapping(value = {"/inputdatagenerative"}, produces = {
            "application/rdf+xml",
            "text/turtle",
            "application/ld+json",
            "*/*"})
    public ResponseEntity<?> getInputExplanationGenerative(@RequestBody ComposedExplanationDTO composedExplanationDTO) throws Exception {
        return new ResponseEntity<>(this.explanationService.composedExplanationForInputData(composedExplanationDTO), HttpStatus.OK);
    }

    @PostMapping(value = {"/composedexplanations"})
    public ResponseEntity<?> getComposedExplanation(@RequestBody ComposedExplanationDTO composedExplanationDTO) {
        return new ResponseEntity<>(explanationService.composedExplanationsForQaProcess(composedExplanationDTO), HttpStatus.OK);
    }

}


/*
EXAMPLE REQUEST for /composedexplanations
{
    "graphUri": "",
    "generativeExplanationRequest": {
        "shots": 1,
        "gptModel": "gpt3.5",
        "qanaryComponents": [
            {
                "componentName": null,
                "componentMainType": "AnnotationOfInstance"
            }
        ]
    }
}

 */