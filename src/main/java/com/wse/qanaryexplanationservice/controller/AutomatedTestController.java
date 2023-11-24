package com.wse.qanaryexplanationservice.controller;

import com.wse.qanaryexplanationservice.pojos.AutomatedTests.automatedTestingObject.automatedTestingObject.AutomatedTestRequestBody;
import com.wse.qanaryexplanationservice.repositories.AutomatedTestingRepository;
import com.wse.qanaryexplanationservice.services.AutomatedTestingService;
import jakarta.json.JsonArray;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;

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
        if (automatedTest == null)
            return new ResponseEntity<>("Fehler", HttpStatus.BAD_REQUEST);
        else
            return new ResponseEntity<>(automatedTest, HttpStatus.OK);
    }

    @PostMapping(value = "/explanationswithoutgptexplanation", consumes = {
            "application/json"
    })
    public ResponseEntity<?> getExplanationsWithoutGptExplanation(@RequestBody AutomatedTestRequestBody requestBody) throws Exception {
        String explanations = automatedTestingService.createTestWorkflow(requestBody);

        return new ResponseEntity<>(explanations, HttpStatus.OK);
    }

    @CrossOrigin
    @PostMapping(value = "/plainexplanations", consumes = "application/json")
    public ResponseEntity<?> getPlainExplanations(@RequestBody String[] array) {
        try {
            return new ResponseEntity<>(automatedTestingService.getPlainExplanations(array), HttpStatus.OK);
        } catch(Exception e)
        {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_ACCEPTABLE);
        }
    }

}
