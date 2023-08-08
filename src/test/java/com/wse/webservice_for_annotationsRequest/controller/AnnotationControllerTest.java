package com.wse.webservice_for_annotationsRequest.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jsonldjava.utils.Obj;
import com.wse.webservice_for_annotationsRequest.StringToJsonNode;
import com.wse.webservice_for_annotationsRequest.repositories.AnnotationSparqlRepository;
import com.wse.webservice_for_annotationsRequest.services.GetAnnotationsService;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.event.annotation.BeforeTestMethod;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class AnnotationControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private AnnotationSparqlRepository annotationSparqlRepository;
    @Mock
    private GetAnnotationsService getAnnotationsService;
    private ObjectMapper objectMapper = new ObjectMapper();
    private StringToJsonNode stringToJsonNode;

    public void setup_givenResults_thenStatus200() throws IOException {
        stringToJsonNode = new StringToJsonNode();
        JsonNode toBeTested = stringToJsonNode.convertStingToJsonNode(jsonString);
        Mockito.when(annotationSparqlRepository.executeSparqlQuery(any())).thenReturn(toBeTested);
    }

    @Test
    public void givenResults_thenStatus200() throws Exception {
        setup_givenResults_thenStatus200();
        mockMvc.perform(get("/getannotations")
                        .param("graphID", "anyGraphID"))
                .andExpect(status().isOk());
    }

    @Test
    public void missingParameter_thenError() throws Exception {
        mockMvc.perform(get("/getannotations"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void noResults_thenStatus400() throws Exception {
        setup_noResults_thenStatus400();
        mockMvc.perform(get("/getannotations")
                        .param("graphID", "anyGraphID"))
                .andExpect(status().isBadRequest());
    }

    public void setup_noResults_thenStatus400() throws IOException {
        Mockito.when(annotationSparqlRepository.executeSparqlQuery(any())).thenReturn(null);
    }


    private final String jsonString = "{\"bindings\":[{\"annotationId\":{\"type\":\"uri\",\"value\":\"tag:stardog:api:0.5264017467650085\"},\"type\":{\"type\":\"uri\",\"value\":\"http://www.wdaqua.eu/qa#AnnotationOfInstance\"},\"body\":{\"type\":\"uri\",\"value\":\"http://dbpedia.org/resource/String_theory\"},\"target\":{\"type\":\"bnode\",\"value\":\"b0\"},\"createdBy\":{\"type\":\"uri\",\"value\":\"urn:qanary:NED-DBpediaSpotlight\"},\"createdAt\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#dateTime\",\"type\":\"typed-literal\",\"value\":\"2023-08-08T09:05:31.387Z\"}}]}";

}
