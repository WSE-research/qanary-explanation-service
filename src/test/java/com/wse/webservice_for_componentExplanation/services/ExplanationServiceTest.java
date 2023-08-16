package com.wse.webservice_for_componentExplanation.services;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wse.webservice_for_componentExplanation.controller.ExplanationController;
import com.wse.webservice_for_componentExplanation.pojos.ExplanationObject;
import com.wse.webservice_for_componentExplanation.repositories.ExplanationSparqlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;


import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static reactor.core.publisher.Mono.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class ExplanationServiceTest {
    @Autowired
    private ExplanationService explanationService;
    private static final String graphID = "testID123-.urn://21äö";

    /**
     * Assertion-Tests on converted Data
     */

    private static final String QUERY = "/queries/explanation_for_query_builder.rq";
    @Nested
    public class ConversionTests {

        ExplanationObject[] explanationObjects;
        ServiceDataForTests serviceDataForTests;

        @BeforeEach
        void setup() throws JsonProcessingException {
            serviceDataForTests = new ServiceDataForTests();
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readValue(this.serviceDataForTests.getJsonForExplanationObjects(), JsonNode.class);

            explanationObjects = explanationService.convertToExplanationObjects(jsonNode);
        }

        @Test
        void convertJsonNodeToExplanationObjectsTest() {

            assertAll("Correct conversion",
                    () -> assertEquals(3, explanationObjects.length),
                    () -> assertEquals("http://dbpedia.org/resource/String_theory", explanationObjects[0].getBody().getValue()),
                    () -> assertEquals("http://dbpedia.org/resource/Real_number", explanationObjects[1].getBody().getValue()),
                    () -> assertEquals("http://dbpedia.org/resource/Batman", explanationObjects[2].getBody().getValue())
            );
        }

        // That's the test for createEntitiesFromQuestion and that method is equal to the return value of explainComponent
        @Test
        void createEntitiesFromQuestionTest() {
            String question = "What is the real name of Batman?";
            ExplanationObject[] explanationObjectsWithEntities = explanationService.createEntitiesFromQuestion(explanationObjects, question);

            assertAll("Correct entities",
                    () -> assertEquals(3, explanationObjectsWithEntities.length),
                    () -> assertEquals("What", explanationObjectsWithEntities[0].getEntity()),
                    () -> assertEquals("real", explanationObjectsWithEntities[1].getEntity()),
                    () -> assertEquals("Batman", explanationObjectsWithEntities[2].getEntity())
            );
        }
    }

    /*
    @Nested
    class ExplanationQueryBuilderTests {
        ServiceDataForTests serviceDataForTests;
        @MockBean
        ExplanationSparqlRepository explanationSparqlRepository;

        @BeforeEach
        void setup() throws IOException {
            serviceDataForTests = new ServiceDataForTests();
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readValue(this.serviceDataForTests.getJsonForExplanationObjects(), JsonNode.class);
            Mockito.when(explanationSparqlRepository.executeSparqlQuery(any())).thenReturn(jsonNode);
        }

        @Test
        public void explainQueryBuilderTest() throws IOException {
            String result = explanationService.explainQueryBuilder("1",QUERY);

            System.out.println(result);
        }
    }
    */

    @Test
    public void convertNullToExplanationObjects() throws JsonProcessingException {
        JsonNode jsonNode = null;

        ExplanationObject[] explanationObjects = explanationService.convertToExplanationObjects(jsonNode);

        assertNull(explanationObjects);
    }


}
