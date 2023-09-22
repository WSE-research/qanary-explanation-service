package com.wse.qanaryexplanationservice.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.wse.qanaryexplanationservice.StringToJsonNode;
import com.wse.qanaryexplanationservice.repositories.AnnotationSparqlRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
public class AnnotationControllerTest {

    private final ControllerDataForTests controllerDataForTests = new ControllerDataForTests();
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private AnnotationSparqlRepository annotationSparqlRepository;

    public void setup_givenResults_thenStatus200() throws IOException {
        StringToJsonNode stringToJsonNode = new StringToJsonNode();
        String jsonString = controllerDataForTests.getGivenResults();
        JsonNode toBeTested = stringToJsonNode.convertStingToJsonNode(jsonString);
        Mockito.when(annotationSparqlRepository.executeSparqlQuery(any())).thenReturn(toBeTested);
    }

    @Test
    public void givenResults_thenStatus200() throws Exception {
        setup_givenResults_thenStatus200();
        mockMvc.perform(get("/annotations/" + "anyGraphURI"))
                .andExpect(status().isOk());
    }

    @Test
    public void missingPathVariable_thenError() throws Exception {
        mockMvc.perform(get("/annotations"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void noResults_thenStatus400() throws Exception {
        setup_noResults_thenStatus400();
        mockMvc.perform(get("/annotations/" + "anyGraphURI"))
                .andExpect(status().isBadRequest());
    }

    public void setup_noResults_thenStatus400() throws IOException {
        Mockito.when(annotationSparqlRepository.executeSparqlQuery(any())).thenReturn(null);
    }


}
