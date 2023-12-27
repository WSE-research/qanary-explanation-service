package com.wse.qanaryexplanationservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wse.qanaryexplanationservice.pojos.AutomatedTests.automatedTestingObject.automatedTestingObject.AutomatedTest;
import com.wse.qanaryexplanationservice.pojos.ExperimentSelectionDTO;
import com.wse.qanaryexplanationservice.services.ClientService;
import com.wse.qanaryexplanationservice.services.ExplanationDataService;
import com.wse.qanaryexplanationservice.services.ExplanationService;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.IOException;

@Controller
public class ClientController {

    @Autowired
    private ExplanationService explanationService;
    @Autowired
    private ExplanationDataService explanationDataService;
    @Autowired
    private ClientService clientService;

    private final Logger logger = LoggerFactory.getLogger(ClientController.class);

    //@CrossOrigin(origins = "http://localhost:3000") // check CORS Settings
    @CrossOrigin
    @PostMapping("/experiments/explanations")
    // TODO: Refactor to POSTMapping since we need to pass a body including the required types
    public ResponseEntity<String> getMultipleExperiments(@RequestBody ExperimentSelectionDTO experimentSelectionDTO) throws IOException {

        return new ResponseEntity<>(clientService.getExperimentExplanations(experimentSelectionDTO),HttpStatus.OK);
    }

    @PostMapping("/insertjson")
    public ResponseEntity<String> insertAutomatedTestJson(@RequestBody String automatedTest) throws Exception {
        JSONObject jsonObject = new JSONObject(automatedTest);
        clientService.insertJSONs(jsonObject);
        return new ResponseEntity("Successful", HttpStatus.OK);
    }

    // TODO: Refactor GET from ln 38 to combine these methods / API-calls with optional path parameter
    @PostMapping("/experiments")
    public ResponseEntity<String> test(@RequestBody ExperimentSelectionDTO experimentSelectionDTO) {
        //explanationDataService.selectAllExperiments(experimentSelectionDTO);
        return new ResponseEntity("", HttpStatus.OK);
    }

}
