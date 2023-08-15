package com.wse.webservice_for_componentExplanation.services;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wse.webservice_for_componentExplanation.pojos.ExplanationObject;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.event.annotation.BeforeTestMethod;
import org.springframework.test.context.junit.jupiter.SpringExtension;


import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class ExplanationServiceTest {
    private static final String graphID = "testID123-.urn://21äö";
    private static final String EXPLANATION_NAMESPACE = "urn:qanary:explanations";

    /**
     * Assertion-Tests on converted Data
     */
    @Nested
    public class ConversionTests {

        @Autowired
        ExplanationService explanationService;
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

    @Nested
    class ExplanationAsRdfTurtle {

        static final String componentURI = "testComponentURI";
        LanguageContentProvider languageContentProvider;
        Model model;
        String sparqlQuery;
        String queryPrefixes = "PREFIX explanation: <" + EXPLANATION_NAMESPACE + ">";
        ExplanationObject[] explanationObjects;
        ObjectMapper objectMapper = new ObjectMapper();
        @Autowired
        ExplanationService explanationService;
        ExplanationService explanationServiceMock;

        @BeforeEach
        void setup() {
            languageContentProvider = new LanguageContentProvider("Dieser Content ist auf Deutsch", "And this one is english");
            model = ModelFactory.createDefaultModel();
            sparqlQuery = queryPrefixes + " SELECT ?subject ?object WHERE { ?subject explanation:hasExplanationForCreatedData ?object }";
        }

        /**
         * - INPUT: Content in languages german and english, componentURI
         */
        @Test
        void createRdfRepresentationTest() {
            String result = explanationService.createRdfRepresentation(languageContentProvider.getContentDe(), languageContentProvider.getContentEn(), componentURI);

            assertAll("String contains content elements as well as componentURI",
                    () -> assertTrue(result.contains(languageContentProvider.getContentDe())),
                    () -> assertTrue(result.contains(languageContentProvider.getContentEn())),
                    () -> assertTrue(result.contains(componentURI))
            );

            model.read(new java.io.StringReader(result), null, "Turtle");
            Query query = QueryFactory.create(sparqlQuery);

            try(QueryExecution queryExecution = QueryExecutionFactory.create(query, model)) {
                ResultSet results = queryExecution.execSelect();
                while(results.hasNext()) {
                    QuerySolution temp = results.next();
                    assertTrue(temp.get("object").isLiteral());
                    assertTrue(temp.get("subject").isResource());
                    if(results.getRowNumber() % 2 == 0) {
                        Literal englishContent = temp.get("object").asLiteral();
                        assertEquals("en", englishContent.getLanguage());
                        assertEquals(languageContentProvider.getContentEn(), englishContent.getString());
                    }
                    else {
                        Literal germanContent = temp.get("object").asLiteral();
                        assertEquals("de", germanContent.getLanguage());
                        assertEquals(languageContentProvider.getContentDe(), germanContent.getString());
                    }
                }
            }
        }
        @BeforeEach
        void setupExplainSpecificComponentTest() throws IOException {
            ServiceDataForTests serviceDataForTests = new ServiceDataForTests();
            JsonNode jsonNode = objectMapper.readValue(serviceDataForTests.getJsonForExplanationObjects(), JsonNode.class);
            explanationObjects = explanationService.convertToExplanationObjects(jsonNode);
            explanationServiceMock = mock(ExplanationService.class);
            Mockito.when(explanationServiceMock.computeExplanationObjects(any(),any(),any())).thenReturn(explanationObjects);
        }

        /**
         * Not working now, result becomes null
         *      - explanationServiceMock.explainSepcificComponent call returns null since it`s a mock
         *      - explanationService.explainsepcifiacComponent wouldn't respect the defined return value in setup-method
         * @throws IOException
         */
        @Test
        void explainSpecificComponentTest() throws IOException {
            String result = explanationServiceMock.explainSpecificComponent("",componentURI,"");
            assertNotNull(result);
        }
    }

    /*
        @Test
        public void convertNullToExplanationObjects() throws JsonProcessingException {
            JsonNode jsonNode = null;

            ExplanationObject[] explanationObjects = explanationService.convertToExplanationObjects(jsonNode);

            assertNull(explanationObjects);
        }
        */


}
