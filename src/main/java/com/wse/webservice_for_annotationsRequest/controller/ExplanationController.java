package com.wse.webservice_for_annotationsRequest.controller;

import jakarta.validation.groups.Default;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.wse.webservice_for_annotationsRequest.services.explanationService;
import com.wse.webservice_for_annotationsRequest.repositories.annotationSparqlRepository;
import com.wse.webservice_for_annotationsRequest.repositories.explanationSparqlRepository;
import java.io.IOException;

@RestController
public class ExplanationController {

    @Autowired
    private explanationService explanationService;
    @Autowired
    private explanationSparqlRepository explanationSparqlRepository;

    @GetMapping("/explanation")
    public void explainComponent(@RequestParam String graphID) throws IOException {
        explanationSparqlRepository.explainComponent(graphID);
    }
}
