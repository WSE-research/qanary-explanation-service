package com.wse.qanaryexplanationservice.services;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wse.qanaryexplanationservice.controller.ControllerDataForTests;
import com.wse.qanaryexplanationservice.pojos.ComponentPojo;
import com.wse.qanaryexplanationservice.pojos.ExplanationObject;
import com.wse.qanaryexplanationservice.repositories.ExplanationSparqlRepository;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class ExplanationServiceTest {
    private static final String EXPLANATION_NAMESPACE = "urn:qanary:explanations";

    private static final String QUERY = "/queries/explanation_for_query_builder.rq";
    protected final Logger logger = LoggerFactory.getLogger(ExplanationService.class);

    @Nested
    public class ConversionTests {

        private final static Logger logger = LoggerFactory.getLogger(ExplanationServiceTest.class);
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


        static final String componentURI = "urn:qanary:QB-SimpleRealNameOfSuperHero";
        LanguageContentProvider languageContentProvider;
        Model model;
        String sparqlQuery;
        String queryPrefixes = "PREFIX explanation: <" + EXPLANATION_NAMESPACE + ">";
        ExplanationObject[] explanationObjects;
        ObjectMapper objectMapper = new ObjectMapper();
        @Autowired
        ExplanationService explanationService;
        ExplanationService explanationServiceMock;
        Logger logger = LoggerFactory.getLogger(ExplanationAsRdfTurtle.class);

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
        void createRdfRepresentationTest() throws Exception {
            String result = explanationService.convertToDesiredFormat(null, explanationService.createRdfRepresentation(languageContentProvider.getContentDe(), languageContentProvider.getContentEn(), componentURI));

            assertAll("String contains content elements as well as componentURI",
                    () -> assertTrue(result.contains(languageContentProvider.getContentDe())),
                    () -> assertTrue(result.contains(languageContentProvider.getContentEn())),
                    () -> assertTrue(result.contains(componentURI))
            );

            model.read(new java.io.StringReader(result), null, "Turtle");
            Query query = QueryFactory.create(sparqlQuery);

            try (QueryExecution queryExecution = QueryExecutionFactory.create(query, model)) {
                ResultSet results = queryExecution.execSelect();
                while (results.hasNext()) {
                    QuerySolution temp = results.next();
                    assertTrue(temp.get("object").isLiteral());
                    assertTrue(temp.get("subject").isResource());
                    if (results.getRowNumber() % 2 == 0) {
                        Literal englishContent = temp.get("object").asLiteral();
                        assertEquals("en", englishContent.getLanguage());
                        assertEquals(languageContentProvider.getContentEn(), englishContent.getString());
                    } else {
                        Literal germanContent = temp.get("object").asLiteral();
                        assertEquals("de", germanContent.getLanguage());
                        assertEquals(languageContentProvider.getContentDe(), germanContent.getString());
                    }
                }
            }
        }

        @Test
        public void compareRepresentationModels() throws Exception {
            String resultEmptyHeader = explanationService.convertToDesiredFormat(null, explanationService.createRdfRepresentation(languageContentProvider.getContentDe(), languageContentProvider.getContentEn(), componentURI));
            String resultTurtleHeader = explanationService.convertToDesiredFormat("text/turtle", explanationService.createRdfRepresentation(languageContentProvider.getContentDe(), languageContentProvider.getContentEn(), componentURI));
            String resultRDFXMLHeader = explanationService.convertToDesiredFormat("application/rdf+xml", explanationService.createRdfRepresentation(languageContentProvider.getContentDe(), languageContentProvider.getContentEn(), componentURI));
            String resultJSONLDHeader = explanationService.convertToDesiredFormat("application/ld+json", explanationService.createRdfRepresentation(languageContentProvider.getContentDe(), languageContentProvider.getContentEn(), componentURI));

            assertEquals(resultEmptyHeader, resultTurtleHeader);
            assertAll("Check different result-string",
                    () -> assertEquals(resultTurtleHeader, resultEmptyHeader),
                    () -> assertNotEquals(resultJSONLDHeader, resultRDFXMLHeader),
                    () -> assertNotEquals(resultJSONLDHeader, resultEmptyHeader),
                    () -> assertNotEquals(resultRDFXMLHeader, resultEmptyHeader)
            );

            // create und compare models
            Model modelResultEmptyHeader = ModelFactory.createDefaultModel();
            StringReader in = new StringReader(resultEmptyHeader);
            modelResultEmptyHeader.read(in, null, "TURTLE");
            logger.info("Created model from resultEmptyHeader: {}", modelResultEmptyHeader);

            Model modelResultRDFXMLHeader = ModelFactory.createDefaultModel();
            in = new StringReader(resultRDFXMLHeader);
            modelResultRDFXMLHeader.read(in, null, "RDFXML");
            logger.info("Created model from resultRDFXMLHeader: {}", modelResultRDFXMLHeader);

            Model modelResultJSONLDHeader = ModelFactory.createDefaultModel();
            in = new StringReader(resultJSONLDHeader);
            modelResultJSONLDHeader.read(in, null, "JSONLD");
            logger.info("Created model from modelResultJSONLDHeaeder: {}", modelResultJSONLDHeader);

            assertAll("Comparing model structure",
                    () -> assertTrue(modelResultEmptyHeader.isIsomorphicWith(modelResultRDFXMLHeader)),
                    () -> assertTrue(modelResultEmptyHeader.isIsomorphicWith(modelResultJSONLDHeader)),
                    () -> assertTrue(modelResultRDFXMLHeader.isIsomorphicWith(modelResultJSONLDHeader))
            );
        }

        @BeforeEach
        void setupExplainSpecificComponentTest() throws IOException {
            ServiceDataForTests serviceDataForTests = new ServiceDataForTests();
            JsonNode jsonNode = objectMapper.readValue(serviceDataForTests.getJsonForExplanationObjects(), JsonNode.class);
            explanationObjects = explanationService.convertToExplanationObjects(jsonNode);
            explanationServiceMock = mock(ExplanationService.class);
            Mockito.when(explanationServiceMock.computeExplanationObjects(any(), any(), any())).thenReturn(explanationObjects);
        }
    }

    @Nested
    class QaSystemExplanationTest {

        final String graphID = "exampleGraphID";
        final String questionURI = "http://question-example.com/123/32a";
        @MockBean
        ExplanationSparqlRepository explanationSparqlRepository;
        JsonNode jsonNode;
        ControllerDataForTests controllerDataForTests;
        ObjectMapper objectMapper = new ObjectMapper();
        @Autowired
        ExplanationService explanationService;
        ComponentPojo[] components;
        Map<String, Model> models;

        QaSystemExplanationTest() throws FileNotFoundException {
        }

        @BeforeEach
        void setup() throws IOException {
            controllerDataForTests = new ControllerDataForTests();
            jsonNode = objectMapper.readTree(controllerDataForTests.getGivenResults());
            when(explanationSparqlRepository.executeSparqlQuery(anyString())).thenReturn(jsonNode);
        }

        // Testing if a wrong JsonNode leads to an error
        @Test
        void fetchQuestionUriFailingTest() throws Exception {
            Throwable exception = assertThrows(Exception.class, () -> explanationService.fetchQuestionUri(graphID));
            assertEquals("Couldn't fetch the question!", exception.getMessage());
        }

        void setupCreateSystemModelTest() throws FileNotFoundException {
            models = controllerDataForTests.getQaSystemExplanationMap();
            components = controllerDataForTests.getComponents();
        }

        /*
        // Testing the createSystemModel-method with several assertions
        @Test
        void createSystemModelTest() throws FileNotFoundException {
            setupCreateSystemModelTest();
            Model expectedModel = controllerDataForTests.getExpectedModelForQaSystemExplanation();
            Model computedModel = explanationService.createSystemModel(models, components, questionURI, graphID);

            assertTrue(expectedModel.isIsomorphicWith(computedModel));
        }
        */
    }

}
