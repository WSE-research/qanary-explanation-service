package com.wse.webservice_for_annotationsRequest.controller;

import com.wse.webservice_for_annotationsRequest.pojos.ResultObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.wse.webservice_for_annotationsRequest.repositories.sparqlRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
public class DefaultController {

    @Autowired
    private sparqlRepository sparqlRepository;

    /** pass graphID as parameter not as json inside a body
     *
     * @param graphID - pass as parameter (e.g. ` curl http://localhost:8080/getannotations?graphID=urn:graph:04f7b7fe-046b-4569-aa7b-5259be59db54 `)
     * @return - body with List of annotations, OK-Http status
     * @throws IOException
     */
     @CrossOrigin
    @GetMapping("/getannotations")
    public ResponseEntity<ResultObject[]> getannotations(@RequestParam String graphID) throws IOException {

        return new ResponseEntity<>(sparqlRepository.executeSparqlQuery(graphID), HttpStatus.OK);
    }

}
