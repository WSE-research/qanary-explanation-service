package com.wse.qanaryexplanationservice.controller;

import com.wse.qanaryexplanationservice.services.AutomatedTestingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AutomatedTestController {

    @Autowired
    private AutomatedTestingService automatedTestingService;

    @PostMapping(value = "/automatedtest", consumes = {
            "application/json"
    })
    public ResponseEntity<?> explanationsTests(@RequestBody String requestBody) {
        return null;
    }

}
