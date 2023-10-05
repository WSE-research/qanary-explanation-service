package com.wse.qanaryexplanationservice.controller;

import com.wse.qanaryexplanationservice.pojos.QanaryRequestObject;
import com.wse.qanaryexplanationservice.repositories.AutomatedTestingRepository;
import com.wse.qanaryexplanationservice.services.AutomatedTestingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class AutomatedTestController {

    @Autowired
    private AutomatedTestingService automatedTestingService;
    @Autowired
    private AutomatedTestingRepository automatedTestingRepository;

    @PostMapping(value = "/automatedtest", consumes = {
            "application/json"
    })
    public ResponseEntity<?> explanationsTests(@RequestBody String requestBody) throws IOException {
        QanaryRequestObject qanaryRequestObject = new QanaryRequestObject(
                "What is the capital of Netherlands?",
                null,
                null,
                "NED-DBpediaSpotlight"
        );
        return new ResponseEntity<>(automatedTestingService.createDataset(), HttpStatus.OK);
    }

    @GetMapping("/dataset")
    public ResponseEntity<?> getDataset() {
        return new ResponseEntity<>("", HttpStatus.OK);
    }

}
