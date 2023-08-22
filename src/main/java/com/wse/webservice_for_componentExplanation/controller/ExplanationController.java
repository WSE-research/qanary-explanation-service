package com.wse.webservice_for_componentExplanation.controller;

import com.wse.webservice_for_componentExplanation.pojos.ExplanationObject;
import com.wse.webservice_for_componentExplanation.services.ExplanationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@ControllerAdvice
@RestController
public class ExplanationController {

    private static final String DBpediaSpotlight_SPARQL_QUERY = "/queries/explanation_sparql_query.rq";
    private static final String QBBirthdateWikidata_SPARQL_QUERY = "/queries/explanation_for_birthdate_wikidata.rq";
    private static final String GENERAL_EXPLANATION_SPARQL_QUERY = "/queries/general_explanation.rq";
    private final Logger logger = LoggerFactory.getLogger(ExplanationController.class);
    @Autowired
    private ExplanationService explanationService;

    /**
     * @param graphID graphId to work with
     */
    @CrossOrigin
    @GetMapping("/explanation")
    public ResponseEntity<ExplanationObject[]> explainComponentDBpediaSpotlight(@RequestParam String graphID) throws IOException {
        ExplanationObject[] explanationObjects = explanationService.explainComponent(graphID, DBpediaSpotlight_SPARQL_QUERY);
        if (explanationObjects != null)
            return new ResponseEntity<>(explanationObjects, HttpStatus.OK);
        else
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
    }

    @CrossOrigin
    @GetMapping("/explanationforqbbirthdatewikidata")
    public ResponseEntity<ExplanationObject[]> explainComponentQBBirthDataWikidata(@RequestParam String graphID) throws IOException {
        ExplanationObject[] explanationObjects = explanationService.explainComponent(graphID, QBBirthdateWikidata_SPARQL_QUERY);

        if (explanationObjects != null)
            return new ResponseEntity<>(explanationObjects, HttpStatus.OK);
        else
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
    }

    /**
     * @param graphURI     given graph URI
     * @param componentURI given component URI
     * @return String as RDF-Turtle
     * @throws IOException IOException
     */
    @CrossOrigin
    @GetMapping(value = "/explainspecificcomponent", produces = {
            "application/rdf+xml",
            "text/turtle",
            "application/ld+json"})
    public ResponseEntity<?> getRdfTurtle(@RequestParam String graphURI,
                                          @RequestParam String componentURI,
                                          @RequestHeader Map<String, String> headers,
                                          @RequestHeader(value = "accept", required = false) String acceptHeader) throws Exception {
        String result = this.explanationService.explainSpecificComponent(graphURI, componentURI, GENERAL_EXPLANATION_SPARQL_QUERY, acceptHeader);

        if (result != null)
            return new ResponseEntity<>(result, HttpStatus.OK);
        else
            return new ResponseEntity<>(null, HttpStatus.NOT_ACCEPTABLE);
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
        return new ResponseEntity<>("Accecpted header: " +
                "application/rdf+xml, " +
                "text/turtle, " +
                "application/ld+json",
                HttpStatus.NOT_ACCEPTABLE);
    }

}
