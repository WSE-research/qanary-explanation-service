package com.wse.webservice_for_annotationsRequest.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wse.webservice_for_annotationsRequest.pojos.ResultObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;


@ExtendWith(SpringExtension.class)
@SpringBootTest
public class GetAnnotationServiceTest {
    @Autowired
    private GetAnnotationsService getAnnotationsService;

    @Test
    public void mapResponseToResultObjectsTest() {
        JsonNode jsonNode = null;

        Assertions.assertNull(getAnnotationsService.mapResponseToObjectArray(jsonNode));
    }

    @Nested
    public class ConversionTests {

        ResultObject[] resultObjects;
        ServiceDataForTests serviceDataForTests;

        @BeforeEach
        void setup() throws IOException {
            serviceDataForTests = new ServiceDataForTests();
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readValue(this.serviceDataForTests.getJsonForResultObjects(), JsonNode.class);

            resultObjects = getAnnotationsService.mapResponseToObjectArray(jsonNode);
        }

        @Test
        public void convertJsonNodeToExplanationObjectsTest() {

            assertAll("Correct conversion",
                    () -> assertEquals(4, resultObjects.length),
                    () -> assertEquals("http://dbpedia.org/resource/String_theory", resultObjects[0].getBody().getValue()),
                    () -> assertEquals("http://dbpedia.org/resource/Real_number", resultObjects[1].getBody().getValue()),
                    () -> assertEquals("http://dbpedia.org/resource/Batman", resultObjects[2].getBody().getValue())
            );
        }
    }

}
