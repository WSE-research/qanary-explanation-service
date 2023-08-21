package com.wse.webservice_for_componentExplanation.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wse.webservice_for_componentExplanation.StringToJsonNode;
import com.wse.webservice_for_componentExplanation.repositories.ExplanationSparqlRepository;
import com.wse.webservice_for_componentExplanation.services.ExplanationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;


import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
public class ExplanationControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ControllerDataForTests controllerDataForTests = new ControllerDataForTests();
    private final StringToJsonNode stringToJsonNode = new StringToJsonNode();
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private ExplanationSparqlRepository explanationSparqlRepository;
    @Mock
    private ExplanationService explanationService;

    @BeforeEach
    public void generalSetup() {

    }

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

    @Nested
    class QueryBuilderTests {
        public void setupExplainQueryBuilderTest(boolean withQBValues) throws IOException {
            String jsonString;
            if (withQBValues)
                jsonString = controllerDataForTests.getGivenExplanations();
            else
                jsonString = controllerDataForTests.getGivenExplanationsWithoutQbValues();
            JsonNode toBeTested = stringToJsonNode.convertStingToJsonNode(jsonString);
            Mockito.when(explanationSparqlRepository.executeSparqlQuery(any())).thenReturn(toBeTested);
            Mockito.when(explanationService.buildSparqlQuery(any(), any(), any())).thenReturn("example Sparql-query");
        }

        @Test
        public void explainQueryBuilderTest() throws Exception {
            setupExplainQueryBuilderTest(true);
            String qbResourceInControllerDataForTests = "http://dbpedia.org/resource/String_theory";
            String expectedResult = "The component created the following SPARQL queries: '" + qbResourceInControllerDataForTests + "'\n";
            MvcResult result = mockMvc.perform(get("/explanationforquerybuilder")
                            .param("graphID", "example"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andReturn();

            String resultBody = result.getResponse().getContentAsString();
            assertEquals(expectedResult, resultBody);
        }

        @Test
        public void explainQueryBuilderWithoutQueryBuilderComponentsTest() throws Exception {
            setupExplainQueryBuilderTest(false);
            String failedResponse = "There are no created sparql queries";
            MvcResult result = mockMvc.perform(get("/explanationforquerybuilder")
                            .param("graphID", "example"))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andReturn();

            assertEquals(failedResponse, result.getResponse().getContentAsString());
        }
    }

}
