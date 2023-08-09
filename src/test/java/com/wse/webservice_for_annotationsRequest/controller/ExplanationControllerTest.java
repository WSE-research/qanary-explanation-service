package com.wse.webservice_for_annotationsRequest.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wse.webservice_for_annotationsRequest.StringToJsonNode;
import com.wse.webservice_for_annotationsRequest.repositories.ExplanationSparqlRepository;
import com.wse.webservice_for_annotationsRequest.services.ExplanationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
public class ExplanationControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private ExplanationSparqlRepository explanationSparqlRepository;
    @Mock
    private ExplanationService explanationService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ControllerDataForTests controllerDataForTests = new ControllerDataForTests();

    @Test
    public void missingParameter_thenError() throws Exception {
        mockMvc.perform(get("/explanation"))
                .andExpect(status().isBadRequest());
    }

    public void setup_noResults_thenStatus400() throws IOException {
        Mockito.when(explanationSparqlRepository.executeSparqlQuery(any())).thenReturn(null);
    }

    @Test
    public void noExplanations_thenStatus400() throws Exception {
        setup_noResults_thenStatus400();
        mockMvc.perform(get("/explanation")
                        .param("graphID", "anyGraphID"))
                .andExpect(status().isBadRequest());
    }

    public void setup_givenExplanations_thenStatus200() throws IOException {
        StringToJsonNode stringToJsonNode = new StringToJsonNode();
        String jsonString = controllerDataForTests.getGivenExplanations();
        JsonNode toBeTested = stringToJsonNode.convertStingToJsonNode(jsonString);
        Mockito.when(explanationSparqlRepository.executeSparqlQuery(any())).thenReturn(toBeTested);
        String givenQuestion = "What is the real name of Superman?";
        Mockito.when(explanationSparqlRepository.fetchQuestion(any())).thenReturn(givenQuestion);
    }

    @Test
    public void givenResults_thenStatus200() throws Exception {
        setup_givenExplanations_thenStatus200();
        mockMvc.perform(get("/explanation")
                        .param("graphID", "anyGraphID"))
                .andExpect(status().isOk());
    }


}
