package com.wse.webservice_for_componentExplanation.controller;

import com.wse.webservice_for_componentExplanation.pojos.ExplanationObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.wse.webservice_for_componentExplanation.services.ExplanationService;

import java.io.IOException;

@RestController
public class ExplanationController {

    private static final String DBpediaSpotlight_SPARQL_QUERY = "/queries/explanation_sparql_query.rq";
    private static final String QBBirthdateWikidata_SPARQL_QUERY = "/queries/temp.rq";

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

        if(explanationObjects != null)
            return new ResponseEntity<>(explanationObjects, HttpStatus.OK);
        else
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
    }

}
