package com.wse.qanaryexplanationservice.controller;

import com.wse.qanaryexplanationservice.pojos.AutomatedTests.automatedTestingObject.automatedTestingObject.AutomatedTestRequestBody;
import com.wse.qanaryexplanationservice.repositories.AutomatedTestingRepository;
import com.wse.qanaryexplanationservice.services.AutomatedTestingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AutomatedTestController {

    @Autowired
    private AutomatedTestingService automatedTestingService;
    @Autowired
    private AutomatedTestingRepository automatedTestingRepository;

    @PostMapping(value = "/automatedtests", consumes = {
            "application/json"
    })
    public ResponseEntity<?> automatedExplanationTest(@RequestBody AutomatedTestRequestBody requestBody) throws Exception {
        String automatedTest = automatedTestingService.createTestWorkflowWithOpenAiRequest(requestBody);
        try {
            return new ResponseEntity<>(automatedTest, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>("Invalid Annotation Type!", HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping(value = "/explanationswithoutgptexplanation", consumes = {
            "application/json"
    })
    public ResponseEntity<?> getExplanationsWithoutGptExplanation(@RequestBody AutomatedTestRequestBody requestBody) throws Exception {
        String automatedTest = automatedTestingService.createTestWorkflow(requestBody);
        try {
            return new ResponseEntity<>(automatedTest, HttpStatus.OK);
        } catch(IllegalArgumentException e) {
            return new ResponseEntity<>("Invalid Annotation Type!", HttpStatus.BAD_REQUEST);
        }

    }

}
