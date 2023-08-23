package com.wse.qanaryexplanationservice.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.wse.qanaryexplanationservice.StringToJsonNode;
import com.wse.qanaryexplanationservice.repositories.AnnotationSparqlRepository;
import com.wse.qanaryexplanationservice.services.GetAnnotationsService;
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
public class AnnotationControllerTest {

    private final ControllerDataForTests controllerDataForTests = new ControllerDataForTests();
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private AnnotationSparqlRepository annotationSparqlRepository;
    @Mock
    private GetAnnotationsService getAnnotationsService;

    public void setup_givenResults_thenStatus200() throws IOException {
        StringToJsonNode stringToJsonNode = new StringToJsonNode();
        String jsonString = controllerDataForTests.getGivenResults();
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


}
