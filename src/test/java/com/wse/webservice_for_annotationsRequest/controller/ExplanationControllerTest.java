package com.wse.webservice_for_annotationsRequest.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wse.webservice_for_annotationsRequest.StringToJsonNode;
import com.wse.webservice_for_annotationsRequest.pojos.ExplanationObject;
import com.wse.webservice_for_annotationsRequest.pojos.ResultObject;
import com.wse.webservice_for_annotationsRequest.repositories.AnnotationSparqlRepository;
import com.wse.webservice_for_annotationsRequest.repositories.ExplanationSparqlRepository;
import com.wse.webservice_for_annotationsRequest.services.ExplanationService;
import com.wse.webservice_for_annotationsRequest.services.GetAnnotationsService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class ExplanationControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private ExplanationSparqlRepository explanationSparqlRepository;
    @Mock
    private ExplanationService explanationService;
    private ObjectMapper objectMapper = new ObjectMapper();
    private StringToJsonNode stringToJsonNode;
    private String jsonString = "{\"bindings\":[{\"annotationId\":{\"type\":\"uri\",\"value\":\"tag:stardog:api:0.8751403921456865\"},\"type\":{\"type\":\"uri\",\"value\":\"http://www.wdaqua.eu/qa#AnnotationOfInstance\"},\"body\":{\"type\":\"uri\",\"value\":\"http://dbpedia.org/resource/String_theory\"},\"source\":{\"type\":\"uri\",\"value\":\"http://demos.swe.htwk-leipzig.de:40111/question/stored-question__text_fd913cc4-4088-4eaf-a922-9fbe998096cd\"},\"start\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#integer\",\"type\":\"typed-literal\",\"value\":\"0\"},\"end\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#integer\",\"type\":\"typed-literal\",\"value\":\"4\"},\"createdBy\":{\"type\":\"uri\",\"value\":\"urn:qanary:NED-DBpediaSpotlight\"},\"createdAt\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#dateTime\",\"type\":\"typed-literal\",\"value\":\"2023-08-08T08:28:38.029Z\"},\"score\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#decimal\",\"type\":\"typed-literal\",\"value\":\"0.9347568085631697\"}}," +
            "{\"annotationId\":{\"type\":\"uri\",\"value\":\"tag:stardog:api:0.6958628947427131\"},\"type\":{\"type\":\"uri\",\"value\":\"http://www.wdaqua.eu/qa#AnnotationOfInstance\"},\"body\":{\"type\":\"uri\",\"value\":\"http://dbpedia.org/resource/Real_number\"},\"source\":{\"type\":\"uri\",\"value\":\"http://demos.swe.htwk-leipzig.de:40111/question/stored-question__text_fd913cc4-4088-4eaf-a922-9fbe998096cd\"},\"start\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#integer\",\"type\":\"typed-literal\",\"value\":\"12\"},\"end\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#integer\",\"type\":\"typed-literal\",\"value\":\"16\"},\"createdBy\":{\"type\":\"uri\",\"value\":\"urn:qanary:NED-DBpediaSpotlight\"},\"createdAt\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#dateTime\",\"type\":\"typed-literal\",\"value\":\"2023-08-08T08:28:38.238Z\"},\"score\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#decimal\",\"type\":\"typed-literal\",\"value\":\"0.977747974809564\"}}," +
            "{\"annotationId\":{\"type\":\"uri\",\"value\":\"tag:stardog:api:0.7064216296025844\"},\"type\":{\"type\":\"uri\",\"value\":\"http://www.wdaqua.eu/qa#AnnotationOfInstance\"},\"body\":{\"type\":\"uri\",\"value\":\"http://dbpedia.org/resource/Superman\"},\"source\":{\"type\":\"uri\",\"value\":\"http://demos.swe.htwk-leipzig.de:40111/question/stored-question__text_fd913cc4-4088-4eaf-a922-9fbe998096cd\"},\"start\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#integer\",\"type\":\"typed-literal\",\"value\":\"25\"},\"end\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#integer\",\"type\":\"typed-literal\",\"value\":\"33\"},\"createdBy\":{\"type\":\"uri\",\"value\":\"urn:qanary:NED-DBpediaSpotlight\"},\"createdAt\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#dateTime\",\"type\":\"typed-literal\",\"value\":\"2023-08-08T08:28:38.443Z\"},\"score\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#decimal\",\"type\":\"typed-literal\",\"value\":\"0.999238163684283\"}}]}";
    private String givenQuestion = "What is the real name of Superman?";

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
        stringToJsonNode = new StringToJsonNode();
        JsonNode toBeTested = stringToJsonNode.convertStingToJsonNode(jsonString);
        Mockito.when(explanationSparqlRepository.executeSparqlQuery(any())).thenReturn(toBeTested);
        Mockito.when(explanationSparqlRepository.fetchQuestion(any())).thenReturn(givenQuestion);
    }

    @Test
    public void givenResults_thenStatus200() throws Exception {
        setup_givenExplanations_thenStatus200();
        mockMvc.perform(get("/explanation")
                        .param("graphID", "anyGraphID"))
                .andExpect(status().isOk());
    }


    @Test
    public void calculateCorrectEntities() throws Exception {
        setup_givenExplanations_thenStatus200();
        MvcResult result = mockMvc.perform(get("/explanation")
                        .param("graphID", "anyGraphID"))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpServletResponse response = result.getResponse();
        ExplanationObject[] explanationObjects = convertToExplanationObjects(objectMapper.readValue(response.getContentAsString(), JsonNode.class));

        assertAll("Correct Entities",
                () -> assertEquals(3, explanationObjects.length),
                () -> assertEquals(explanationObjects[0].getEntity(), "What"),
                () -> assertEquals(explanationObjects[1].getEntity(), "real"),
                () -> assertEquals(explanationObjects[2].getEntity(), "Superman")
        );
    }


    public ExplanationObject[] convertToExplanationObjects(JsonNode explanationObjectsJsonNode) throws JsonProcessingException {
        try {
            // Handle mapping for LocalDateTime
            objectMapper.registerModule(new JavaTimeModule());
            // select the bindings-field inside the Json(Node)
            ArrayNode resultsArraynode = (ArrayNode) explanationObjectsJsonNode;

            return objectMapper.treeToValue(resultsArraynode, ExplanationObject[].class);
        } catch (Exception e) {
            return null;
        }
    }

}
