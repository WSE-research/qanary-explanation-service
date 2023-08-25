package com.wse.qanaryexplanationservice.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wse.qanaryexplanationservice.pojos.ExplanationObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;


@ExtendWith(SpringExtension.class)
@SpringBootTest
public class GetAnnotationServiceTest {
    @Autowired
    private AnnotationsService annotationsService;

    @Test
    public void mapResponseToExplanationObjectsTest() {
        JsonNode jsonNode = null;

        Assertions.assertNull(annotationsService.mapResponseToObjectArray(jsonNode));
    }

    @Nested
    public class ConversionTests {

        ExplanationObject[] explanationObjects;
        ServiceDataForTests serviceDataForTests;

        @BeforeEach
        void setup() throws IOException {
            serviceDataForTests = new ServiceDataForTests();
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readValue(this.serviceDataForTests.getJsonForResultObjects(), JsonNode.class);
            explanationObjects = annotationsService.mapResponseToObjectArray(jsonNode);
        }

        @Test
        public void convertJsonNodeToExplanationObjectsTest() {

            assertAll("Correct conversion",
                    () -> assertEquals(4, explanationObjects.length),
                    () -> assertEquals("http://dbpedia.org/resource/String_theory", explanationObjects[0].getBody().getValue()),
                    () -> assertEquals("http://dbpedia.org/resource/Real_number", explanationObjects[1].getBody().getValue()),
                    () -> assertEquals("http://dbpedia.org/resource/Batman", explanationObjects[2].getBody().getValue())
            );
        }
    }

}
