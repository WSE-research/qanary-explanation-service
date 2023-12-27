package com.wse.qanaryexplanationservice.controller;

import com.wse.qanaryexplanationservice.services.ExplanationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;

@Controller
public class ClientController {

    @Autowired
    private ExplanationService explanationService;

    //@CrossOrigin(origins = "http://localhost:3000") // check CORS Settings
    @CrossOrigin
    @GetMapping("/experiments")
    // TODO: Refactor to POSTMapping since we need to pass a body including the required types
    public ResponseEntity<String> getMultipleExperiments(/*@RequestBody String body  Placeholder, change to object including data from frontend*/) throws IOException {

        return new ResponseEntity<>(explanationService.getStringFromFile("./example.json"), HttpStatus.OK);
        //return new ResponseEntity<>(HttpStatus.OK);
    }

}
