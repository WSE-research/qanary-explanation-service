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

    private static final String DBpediaSpotlight_SPARQL_QUERY = "/queries/explanation_for_dbpediaSpotlight_sparql_query.rq";
    private static final String QueryBuilder_SPARQL_QUERY = "/queries/explanation_for_query_builder.rq";

    @Autowired
    private ExplanationService explanationService;

    /**
     * @param graphID graphId to work with
     */
    @CrossOrigin
    @GetMapping("/explanation")
    public ResponseEntity<ExplanationObject[]> explainComponentDBpediaSpotlight(@RequestParam String graphID) throws IOException {
        ExplanationObject[] explanationObjects = explanationService.explainComponentDBpediaSpotlight(graphID, DBpediaSpotlight_SPARQL_QUERY);
        if (explanationObjects != null)
            return new ResponseEntity<>(explanationObjects, HttpStatus.OK);
        else
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
    }

    /**
     * Provides an explanation of query builder and returns the created sparql queries
     *
     * @param graphID Given graphID
     * @return textual explanation if there are any annotations made by any query builder
     * @throws IOException
     */
    @CrossOrigin
    @GetMapping("/explanationforquerybuilder")
    public ResponseEntity<String> explainQueryBuilder(@RequestParam String graphID) throws IOException {
        String explanation = explanationService.explainQueryBuilder(graphID, QueryBuilder_SPARQL_QUERY);

        if (explanation != null)
            return new ResponseEntity<>(explanation, HttpStatus.OK);
        else
            return new ResponseEntity<>("There are no created sparql queries", HttpStatus.BAD_REQUEST);
    }

}
