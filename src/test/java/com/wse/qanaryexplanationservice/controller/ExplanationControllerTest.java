package com.wse.qanaryexplanationservice.controller;

import com.wse.qanaryexplanationservice.services.ExplanationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
public class ExplanationControllerTest {

    @Nested
    class QueryBuilderTests {

        @Nested
        class QaSystemExplanationTest {

            private final String testReturn = "randomString";
            @Autowired
            MockMvc mockMvc;
            @MockBean
            private ExplanationService explanationService;

            @BeforeEach
            void setup() throws Exception {
                Mockito.when(explanationService.explainQaSystem(any(), any())).thenReturn(testReturn);
            }

            @Test
            public void givenGraphURICallsSystemExplanation() throws Exception {

                MvcResult mvcResult = mockMvc.perform(get("/explanations/{graphURI}", "wewqeewrwe"))
                        .andReturn();

                assertEquals(200, mvcResult.getResponse().getStatus());

                // check if SystemExplanation method was called once
                verify(explanationService, times(1)).explainQaSystem(any(),any());
            }

            @Test
            public void notFoundWhenNoPathVariables() throws Exception {
                mockMvc.perform(get("/explanations")).andExpect(status().isNotFound());
            }

        }
    }
}

