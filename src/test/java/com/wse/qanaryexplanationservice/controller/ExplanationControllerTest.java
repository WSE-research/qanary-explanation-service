package com.wse.qanaryexplanationservice.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.wse.qanaryexplanationservice.StringToJsonNode;
import com.wse.qanaryexplanationservice.repositories.ExplanationSparqlRepository;
import com.wse.qanaryexplanationservice.services.ExplanationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
public class ExplanationControllerTest {

    private final ControllerDataForTests controllerDataForTests = new ControllerDataForTests();
    private final StringToJsonNode stringToJsonNode = new StringToJsonNode();
    private Logger logger = LoggerFactory.getLogger(ExplanationControllerTest.class);
    @MockBean
    private ExplanationSparqlRepository explanationSparqlRepository;

    @Nested
    class QueryBuilderTests {
        @Autowired
        private MockMvc mockMvc;
        @Mock
        private ExplanationService explanationService;

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


        @Nested
        class QaSystemExplanationTest {

            private final String testReturn = "randomString";
            @Autowired
            MockMvc mockMvc;
            @MockBean
            private ExplanationService explanationService;

            @BeforeEach
            void setup() throws Exception {
                Mockito.when(explanationService.explainQaSystem(any(), any(), any())).thenReturn(testReturn);
            }

            @Test
            void wrongAcceptHeaderLeadsToException() throws Exception {
                String notAcceptableHeader = "application/json";

                MvcResult result = mockMvc.perform(get("/explainqasystem")
                                .header("Accept", notAcceptableHeader)
                                .param("graphURI", "example"))
                        .andExpect(status().isNotAcceptable())
                        .andReturn();

                logger.info("Actual header: {}, Error-message: {}", notAcceptableHeader, result.getResponse().getContentAsString());
            }

            @Test
            void correctAcceptHeaderLeadsTo() throws Exception {
                String acceptableHeader = "application/rdf+xml";

                MvcResult result = mockMvc.perform(get("/explainqasystem")
                                .header("Accept", acceptableHeader)
                                .param("graphURI", "example"))
                        .andExpect(status().isOk())
                        .andReturn();

                assertEquals(result.getResponse().getContentAsString(), testReturn);

            }
        }
    }
}

