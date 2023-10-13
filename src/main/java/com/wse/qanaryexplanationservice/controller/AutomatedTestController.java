package com.wse.qanaryexplanationservice.controller;

import com.wse.qanaryexplanationservice.pojos.AutomatedTestRequestBody;
import com.wse.qanaryexplanationservice.pojos.automatedTestingObject.AnnotationType;
import com.wse.qanaryexplanationservice.repositories.AutomatedTestingRepository;
import com.wse.qanaryexplanationservice.services.AutomatedTestingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AutomatedTestController {

    @Autowired
    private AutomatedTestingService automatedTestingService;
    @Autowired
    private AutomatedTestingRepository automatedTestingRepository;

    /*
    @PostMapping(value = "/automatedtest", consumes = {
            "application/json"
    })
    public ResponseEntity<?> explanationsTests(@RequestBody String requestBody) throws Exception {
        return new ResponseEntity<>(automatedTestingService.selectTestingTriple(AnnotationType.annotationofinstance), HttpStatus.OK);
    }
    */

    @PostMapping(value = "/automatedtests", consumes = {
            "application/json"
    })
    public ResponseEntity<?> automatedExplanationTest(@RequestBody AutomatedTestRequestBody requestBody) throws Exception {
        String automatedTest = automatedTestingService.gptExplanation(requestBody);
        if (automatedTest == null)
            return new ResponseEntity<>("Fehler", HttpStatus.BAD_REQUEST);
        else
            return new ResponseEntity<>(automatedTest, HttpStatus.OK);
    }

    @PostMapping(value = "/explanationswithoutgptexplanation", consumes = {
            "application/json"
    })
    public ResponseEntity<?> getExplanationsWithoutGptExplanation(@RequestBody AutomatedTestRequestBody requestBody) throws Exception {
        String explanations = automatedTestingService.testWithoutGptExplanation(requestBody);

        return new ResponseEntity<>(explanations, HttpStatus.OK);
    }

    @GetMapping(value = "/getcomponents")
    public ResponseEntity<?> getComponent(@RequestBody String requestBody) {
        return new ResponseEntity<>(automatedTestingService.getcomps(AnnotationType.valueOf(requestBody)), HttpStatus.OK);
    }


    @GetMapping("/dataset")
    public ResponseEntity<?> getDataset() {
        return new ResponseEntity<>("", HttpStatus.OK);
    }

}
