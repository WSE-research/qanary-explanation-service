package com.wse.qanaryexplanationservice.controller;

import com.wse.qanaryexplanationservice.services.ExplanationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
public class ExplanationControllerTest {

    private final String testRdfDataPath = "testRdfData.rdf";
    @Autowired
    MockMvc mockMvc;
    String testRdfData;
    @MockBean
    private ExplanationService explanationService;
    private ClassLoader classLoader = this.getClass().getClassLoader();

    @BeforeEach
    public void setup() throws IOException {
        File file = new File(Objects.requireNonNull(classLoader.getResource(testRdfDataPath)).getFile());
        testRdfData = new String(Files.readAllBytes(file.toPath()));
    }

    @Test
    public void notFoundWhenNoPathVariables() throws Exception {
        mockMvc.perform(get("/explanations")).andExpect(status().isNotFound());
    }

    @Test
    public void explanationsForComponentResultNotNull() throws Exception {
        when(explanationService.getTemplateComponentExplanation(any(), any(), any())).thenReturn(testRdfData);

        MvcResult mvcResult = mockMvc.perform(get("/explanations/graphURI/componentURI"))
                .andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus());
        assertEquals(testRdfData, mvcResult.getResponse().getContentAsString());
        verify(explanationService, times(1)).getTemplateComponentExplanation(any(), any(), any());
    }

    @Test
    public void explanationsForSystemResultNotNull() throws Exception {
        when(explanationService.getQaSystemExplanation(any(), any())).thenReturn(testRdfData);

        MvcResult mvcResult = mockMvc.perform(get("/explanations/graphURI"))
                .andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus());
        assertEquals(testRdfData, mvcResult.getResponse().getContentAsString());
        verify(explanationService, times(1)).getQaSystemExplanation(any(), any());
    }

    @Test
    public void explanationsForComponentResultIsNull() throws Exception {
        when(explanationService.getTemplateComponentExplanation(any(), any(), any())).thenReturn(null);

        MvcResult mvcResult = mockMvc.perform(get("/explanations/graphURI/componentURI"))
                .andReturn();

        assertEquals(400, mvcResult.getResponse().getStatus());
        assertEquals("", mvcResult.getResponse().getContentAsString());
    }

    @Test
    public void explanationsForSystemResultIsNull() throws Exception {
        when(explanationService.getQaSystemExplanation(any(), any())).thenReturn(null);

        MvcResult mvcResult = mockMvc.perform(get("/explanations/graphURI"))
                .andReturn();

        assertEquals(406, mvcResult.getResponse().getStatus());
        assertEquals("", mvcResult.getResponse().getContentAsString());
    }

}


