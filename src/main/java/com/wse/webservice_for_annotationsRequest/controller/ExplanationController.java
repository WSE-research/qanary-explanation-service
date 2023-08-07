package com.wse.webservice_for_annotationsRequest.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.wse.webservice_for_annotationsRequest.services.explanationService;
import java.io.IOException;

@RestController
public class ExplanationController {

    @Autowired
    private explanationService explanationService;

    @GetMapping("/explanation")
    public void explainComponent(@RequestParam String graphID) throws IOException {
        explanationService.explainComponent(graphID);
    }
}
