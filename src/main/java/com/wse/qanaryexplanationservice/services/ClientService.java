package com.wse.qanaryexplanationservice.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wse.qanaryexplanationservice.pojos.AutomatedTests.automatedTestingObject.automatedTestingObject.AutomatedTest;
import com.wse.qanaryexplanationservice.pojos.AutomatedTests.automatedTestingObject.automatedTestingObject.AutomatedTestDTO;
import com.wse.qanaryexplanationservice.pojos.ExperimentSelectionDTO;
import com.wse.qanaryexplanationservice.repositories.SparqlRepository;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Service
public class ClientService {

    private final Logger logger = LoggerFactory.getLogger(ClientService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String SELECT_ALL_EXPERIMENTS_QUERY = "/queries/selectAllExperiments.rq";
    @Autowired
    private ExplanationDataService explanationDataService;
    @Autowired
    private SparqlRepository sparqlRepository;

    public JSONArray extractExperiments(JSONObject jsonObject) {
        return jsonObject.getJSONArray("explanations");
    }

    /**
     * Takes a JSON including several experiments and inserts every experiment to the underlying triplestore
     *
     * @param jsonObject
     * @throws Exception
     */ // TODO: Refactor: Create a Class which contains explanations and a array of AutomatedTestDTOs, maybe with a inherited conversion for AutomatedTest
    public void insertJson(JSONObject jsonObject) throws RuntimeException {
        JSONArray experiments = extractExperiments(jsonObject);
        experiments.forEach(experiment -> {
            try {
                AutomatedTestDTO automatedTestDto = objectMapper.readValue(experiment.toString(), AutomatedTestDTO.class);
                AutomatedTest automatedTest = convertToAutomatedTest(automatedTestDto);
                logger.info("{}", automatedTest.toString());
                explanationDataService.insertDataset(automatedTest);
            } catch (JsonProcessingException e) {
                logger.error("{}", e.toString());
                throw new RuntimeException(e);
            }
        });
    }

    private AutomatedTest convertToAutomatedTest(AutomatedTestDTO automatedTestDTO) {
        AutomatedTest automatedTest = new AutomatedTest();
        automatedTest.setTestData(automatedTestDTO.getTestData());
        automatedTest.setPrompt(automatedTestDTO.getPrompt());
        automatedTest.setGptExplanation(automatedTestDTO.getGptExplanation());
        automatedTest.setExampleDataArrayList(new ArrayList<>(List.of(automatedTestDTO.getExampleData())));

        return automatedTest;
    }

    public String getExperimentExplanations(ExperimentSelectionDTO experimentSelectionDTO) throws IOException {
        String sequenceToBeInserted = explanationDataService.createSequenceForExperimentSelection(experimentSelectionDTO);
        String query = new String(Files.readAllBytes(new ClassPathResource(SELECT_ALL_EXPERIMENTS_QUERY).getFile().toPath()));
        query = query.replace("?testType", "qa:" + experimentSelectionDTO.getTestType());
        query = query.replace("?sequence", sequenceToBeInserted).replace("?testType", "qa:" + experimentSelectionDTO.getTestType());
        logger.info("{}", query);
        sparqlRepository.setSparqlEndpoint("http://localhost:8890/sparql");
        ResultSet resultSet = sparqlRepository.executeSparqlQueryWithResultSet(query);
        JSONObject jsonObject = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        while (resultSet.hasNext()) {
            QuerySolution querySolution = resultSet.next();
            JSONObject temp = new JSONObject();
            temp.put("explanation", querySolution.get("explanation"));
            temp.put("gptExplanation", querySolution.get("gptExplanation"));
            jsonArray.put(temp);
        }
        jsonObject.put("explanations", jsonArray);

        return jsonObject.toString();
    }

    public String getExperiments(ExperimentSelectionDTO experimentSelectionDTO) {
        return null;
    }

}
