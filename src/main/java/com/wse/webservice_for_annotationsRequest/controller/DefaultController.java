package com.wse.webservice_for_annotationsRequest.controller;

import com.wse.webservice_for_annotationsRequest.pojos.ResultObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.wse.webservice_for_annotationsRequest.repositories.annotationSparqlRepository;
import com.wse.webservice_for_annotationsRequest.services.explanationService;
import java.io.IOException;

@RestController
public class DefaultController {

    @Autowired
    private explanationService explanationService;

    /** pass graphID as parameter not as json inside a body
     *
     * @param graphID - pass as parameter (e.g. ` curl http://localhost:8080/getannotations?graphID=urn:graph:04f7b7fe-046b-4569-aa7b-5259be59db54 `)
     * @return - body with List of annotations, OK-Http status
     * @throws IOException
     */
    @CrossOrigin
    @GetMapping("/getannotations")
    public ResponseEntity<ResultObject[]> getannotations(@RequestParam String graphID) throws IOException {

        return new ResponseEntity<>(annotationSparqlRepository.executeSparqlQuery(graphID), HttpStatus.OK);
    }

    @CrossOrigin
    @GetMapping("/getannotations")
    public ResponseEntity<ResultObject[]> getannotations(@RequestParam String graphID) throws IOException {

        return new ResponseEntity<>(explanationService.explainComponent(graphID), HttpStatus.OK);
    }

}
