package com.wse.qanaryexplanationservice.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wse.qanaryexplanationservice.pojos.AutomatedTests.automatedTestingObject.automatedTestingObject.AutomatedTest;
import com.wse.qanaryexplanationservice.pojos.AutomatedTests.automatedTestingObject.automatedTestingObject.AutomatedTestDTO;
import com.wse.qanaryexplanationservice.pojos.ExperimentSelectionDTO;
import com.wse.qanaryexplanationservice.repositories.ClientRepository;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.ResourceFactory;
import org.checkerframework.checker.units.qual.A;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Service
public class ClientService {

    @Autowired
    private ExplanationDataService explanationDataService;
    @Autowired
    private ExplanationService explanationService;
    @Autowired
    private ClientRepository clientRepository;
    private final Logger logger = LoggerFactory.getLogger(ClientService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String SELECT_ALL_EXPERIMENTS_QUERY = "/queries/selectAllExperiments.rq";

    public JSONArray extractExperiments(JSONObject jsonObject) {
        return jsonObject.getJSONArray("explanations");
    }

    public void insertJSONs(JSONObject jsonObject) throws Exception {
        JSONArray experiments = extractExperiments(jsonObject);
        experiments.forEach(experiment -> {
            try {
                AutomatedTestDTO automatedTestDto = objectMapper.readValue(experiment.toString(), AutomatedTestDTO.class);
                AutomatedTest automatedTest = convertToAutomatedTest(automatedTestDto);
                logger.info("{}",automatedTest.toString());
                explanationDataService.insertDataset(automatedTest);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public AutomatedTest convertToAutomatedTest(AutomatedTestDTO automatedTestDTO) {
        AutomatedTest automatedTest = new AutomatedTest();
        automatedTest.setTestData(automatedTestDTO.getTestData());
        automatedTest.setPrompt(automatedTestDTO.getPrompt());
        automatedTest.setGptExplanation(automatedTestDTO.getGptExplanation());
        automatedTest.setExampleDataArrayList(new ArrayList<>(List.of(automatedTestDTO.getExampleData())));

        return automatedTest;
    }

    public String getExperimentExplanations(ExperimentSelectionDTO experimentSelectionDTO) throws IOException {
        String sequenceToBeInserted = explanationDataService.createSequenceForExperimentSelection(experimentSelectionDTO);
        String query = explanationService.getStringFromFile(SELECT_ALL_EXPERIMENTS_QUERY);
        query = query.replace("?sequence",sequenceToBeInserted);
        clientRepository.setSparqlEndpoint(new URL("http://localhost:8890/sparql"));
        JsonNode jsonNode = clientRepository.executeSparqlQuery(query);

        return jsonNode.asText();
    }

}
