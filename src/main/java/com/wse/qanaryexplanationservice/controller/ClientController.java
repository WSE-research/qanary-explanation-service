package com.wse.qanaryexplanationservice.controller;

import com.wse.qanaryexplanationservice.helper.dtos.ExperimentSelectionDTO;
import com.wse.qanaryexplanationservice.helper.pojos.Score;
import com.wse.qanaryexplanationservice.services.ClientService;
import com.wse.qanaryexplanationservice.services.ExplanationDataService;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.IOException;

@Controller
public class ClientController {

    private final Logger logger = LoggerFactory.getLogger(ClientController.class);
    @Autowired
    private ClientService clientService;
    @Autowired
    private ExplanationDataService explanationDataService;

    // TODO: set CrossOrigin later

    /**
     * Endpoint to return all explanations for the experiments which are described by the passed JSON-String
     *
     * @param experimentSelectionDTO Class to mirror passed JSON
     * @return Experiments explanations
     * @throws IOException while reading file
     */
    @CrossOrigin(origins = {"http://localhost:3000"})
    @PostMapping("/experiments/explanations")
    public ResponseEntity<String> getExperimentExplanations(@RequestBody ExperimentSelectionDTO experimentSelectionDTO) throws IOException {
        return new ResponseEntity<>(clientService.getExperimentExplanations(experimentSelectionDTO), HttpStatus.OK);
    }

    /**
     * Endpoint to pass a JSON-String following the AutomatedTest-Structure and insert it to the underlying triplestore
     *
     * @param automatedTest JSON-String following the AutomatedTest-Structure
     * @return ResponseEntity with "Successful"-message
     */
    @PostMapping(value = "/insertjson", consumes = "application/json")
    public ResponseEntity<String> insertAutomatedTestJson(@RequestBody String automatedTest) {
        try {
            JSONObject jsonObject = new JSONObject(automatedTest);
            clientService.insertJson(jsonObject);
            return new ResponseEntity<>("Successful", HttpStatus.OK);
        } catch (RuntimeException e) {
            logger.error("{}", e.toString());
            return new ResponseEntity<>(null, HttpStatusCode.valueOf(500));
        }
    }

    /**
     * Endpoint to return all experiments including all data
     *
     * @param experimentSelectionDTO Class to mirror passed JSON
     * @return JSON-String with all experiments including all data
     */
    @PostMapping("/experiments")
    public ResponseEntity<String> test(@RequestBody ExperimentSelectionDTO experimentSelectionDTO) {
        return new ResponseEntity<>(clientService.getExperiments(experimentSelectionDTO), HttpStatus.OK);
    }

    @CrossOrigin
    @PostMapping("/updatedataset")
    public ResponseEntity<String> updateDatasetWithScore(@RequestBody Score score) {
        return new ResponseEntity<>(explanationDataService.updateDataset(score), HttpStatus.OK);
    }

}
