package com.wse.webservice_for_annotationsRequest.controller;

import com.wse.webservice_for_annotationsRequest.pojos.ExplanationObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.wse.webservice_for_annotationsRequest.services.explanationService;
import java.io.IOException;

@RestController
public class ExplanationController {

    @Autowired
    private explanationService explanationService;

    /**
     *
     * @param graphID graphId to work with
     * @throws IOException
     */
    @GetMapping("/explanation")
    public ResponseEntity<String> explainComponent(@RequestParam String graphID) throws IOException {
        return new ResponseEntity<>(explanationService.explainComponent(graphID), HttpStatus.OK);
    }
}
