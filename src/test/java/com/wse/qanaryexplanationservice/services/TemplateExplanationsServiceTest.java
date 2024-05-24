package com.wse.qanaryexplanationservice.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wse.qanaryexplanationservice.controller.ControllerDataForTests;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class TemplateExplanationsServiceTest {
    private static final String EXPLANATION_NAMESPACE = "urn:qanary:explanations";
    private final ServiceDataForTests serviceDataForTests = new ServiceDataForTests();
    Logger logger = LoggerFactory.getLogger(TemplateExplanationsServiceTest.class);

    @Nested
    class ExplanationAsRdfTurtle {

        static final String componentURI = "urn:qanary:QB-SimpleRealNameOfSuperHero";
        LanguageContentProvider languageContentProvider;
        Model model;
        String sparqlQuery;
        String queryPrefixes = "PREFIX explanation: <" + EXPLANATION_NAMESPACE + ">";
        @Autowired
        TemplateExplanationsService tmplExpService;

        @BeforeEach
        void setup() {
            languageContentProvider = new LanguageContentProvider("Dieser Content ist auf Deutsch", "And this one is english");
            model = ModelFactory.createDefaultModel();
            sparqlQuery = queryPrefixes + " SELECT ?subject ?object WHERE { ?subject explanation:hasExplanationForCreatedData ?object }";
        }

        @Test
        void createRdfRepresentationTest() {
            String result = tmplExpService.convertToDesiredFormat(null, tmplExpService.createModelForSpecificComponent(languageContentProvider.getContentDe(), languageContentProvider.getContentEn(), componentURI));

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
        public void compareRepresentationModels() {
            // Create Strings with different format (plain == turtle, turtle, rdfxml,jsonld)
            String resultEmptyHeader = tmplExpService.convertToDesiredFormat(null, tmplExpService.createModelForSpecificComponent(languageContentProvider.getContentDe(), languageContentProvider.getContentEn(), componentURI));
            String resultTurtleHeader = tmplExpService.convertToDesiredFormat("text/turtle", tmplExpService.createModelForSpecificComponent(languageContentProvider.getContentDe(), languageContentProvider.getContentEn(), componentURI));
            String resultRDFXMLHeader = tmplExpService.convertToDesiredFormat("application/rdf+xml", tmplExpService.createModelForSpecificComponent(languageContentProvider.getContentDe(), languageContentProvider.getContentEn(), componentURI));
            String resultJSONLDHeader = tmplExpService.convertToDesiredFormat("application/ld+json", tmplExpService.createModelForSpecificComponent(languageContentProvider.getContentDe(), languageContentProvider.getContentEn(), componentURI));

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

    }

    @Nested
    class QaSystemExplanationTest {

        final String graphID = "http://exampleQuestionURI.a/question";
        final String questionURI = "http://question-example.com/123/32a";
        ResultSet emptyResultSet = serviceDataForTests.createResultSet(serviceDataForTests.getEmptyQuerySolutionMapList());
        ControllerDataForTests controllerDataForTests;
        ObjectMapper objectMapper = new ObjectMapper();
        @Autowired
        TemplateExplanationsService tmplExpService;
        List<String> components;
        Map<String, Model> models;

        @BeforeEach
        void setup() {
            controllerDataForTests = new ControllerDataForTests();
            //when(explanationSparqlRepository.executeSparqlQueryWithResultSet(any())).thenReturn(emptyResultSet);
        }

        // Testing the createSystemModel-method
        @Test
        void createSystemModelTest() {
            models = controllerDataForTests.getQaSystemExplanationMap();
            components = controllerDataForTests.getComponents();
            // Get the expected model from the test data
            Model expectedModel = controllerDataForTests.getExpectedModelForQaSystemExplanation();
            // call method to create model from Models and components
            Model computedModel = tmplExpService.createSystemModel(models, components, questionURI, graphID);

            assertTrue(expectedModel.isIsomorphicWith(computedModel));
        }

    }

    @Nested
    class ComponentExplanationTests {

        private static final Map<String, String> annotationTypeExplanationTemplate = new HashMap<>() {{
            put("annotationofspotinstance", "/explanations/annotation_of_spot_instance/");
            put("annotationofinstance", "/explanations/annotation_of_instance/");
            put("annotationofanswersparql", "/explanations/annotation_of_answer_sparql/");
            put("annotationofrelation", "/explanations/annotation_of_relation/");
            put("annotationofanswerjson", "/explanations/annotation_of_answer_json/");
            put("annotationofquestiontranslation", "/explanations/annotation_of_question_translation/");
            put("annotationofquestionlanguage", "/explanations/annotation_of_question_language/");
        }};
        private final ResultSet resultSet = serviceDataForTests.createResultSet(serviceDataForTests.getQuerySolutionMapList());
        @SpyBean
        private TemplateExplanationsService tmplExpService;

        @Test
        public void createTextualExplanationTest() {

        }

        /*
        Converts a given Map<String,RDFNode> to a Map<String, String>
         */
        @Test
        public void convertRdfNodeToStringValue() {
            Map<String, RDFNode> toBeConvertedMap = serviceDataForTests.getMapWithRdfNodeValues();
            Map<String, String> comparingMap = serviceDataForTests.getConvertedMapWithStringValues();

            Map<String, String> comparedMap = tmplExpService.convertRdfNodeToStringValue(toBeConvertedMap);

            assertEquals(comparingMap, comparedMap);
        }

        /*
        Given a set of (key, value) the result should be the template without any more placeholders,
        TODO: For further annotation types this test can be easily extended by adding values to the map within the serviceDataForTests as well as
        TODO: adding a test-case with the corresponding template
         */
        @ParameterizedTest
        @ValueSource(
                strings = {
                        "annotationofinstance",
                        "annotationofspotinstance",
                        "annotationofanswersparql",
                        "annotationofanswerjson",
                        "annotationofrelation",
                        "annotationofquestionlanguage",
                        "annotationofquestiontranslation"
                })
        public void replacePropertiesTest(String type) {

            Map<String, String> convertedMap = serviceDataForTests.getConvertedMapWithStringValues();
            ClassLoader classLoader = this.getClass().getClassLoader();

            assertAll("Testing correct replacement for templates",
                    () -> {
                        String computedTemplate = tmplExpService.replaceProperties(convertedMap, tmplExpService.getStringFromFile(annotationTypeExplanationTemplate.get(type) + "de" + "_list_item"));
                        String expectedOutcomeFilePath = "expected_list_explanations/" + type + "/de_list_item";
                        File file = new File(Objects.requireNonNull(classLoader.getResource(expectedOutcomeFilePath)).getFile());
                        String expectedOutcome = new String(Files.readAllBytes(file.toPath()));
                        assertEquals(expectedOutcome, computedTemplate);
                    },
                    () -> {
                        String computedTemplate = tmplExpService.replaceProperties(convertedMap, tmplExpService.getStringFromFile(annotationTypeExplanationTemplate.get(type) + "en" + "_list_item"));
                        String expectedOutcomeFilePath = "expected_list_explanations/" + type + "/en_list_item";
                        File file = new File(Objects.requireNonNull(classLoader.getResource(expectedOutcomeFilePath)).getFile());
                        String expectedOutcome = new String(Files.readAllBytes(file.toPath()));
                        assertEquals(expectedOutcome, computedTemplate);
                    }
            );
        }

        // Paramterized ? // Create .ttl-files parse them into a model, set RDFConnection, execute w/ repository
        // just several maps with different values -> increased testability for other tests
        @ParameterizedTest
        @ValueSource(
                strings = {
                        "annotationofinstance",
                        "annotationofspotinstance",
                        "annotationofanswersparql",
                        "annotationofanswerjson",
                        "annotationofrelation",
                        "annotationofquestionlanguage"
                })
        public void addingExplanationsTest(String type) throws IOException {

            List<String> computedExplanations = tmplExpService.addingExplanations(type, "de", resultSet);

            // Should contain the (if existing) prefix (is an empty element) and one explanation
            assertEquals(2, computedExplanations.size());
            assertNotEquals("", computedExplanations.get(1));
            for (String expl : computedExplanations
            ) {
                assertFalse(expl.contains("$"));
            }

        }

        /*
        @Test
        public void createSpecificExplanationTest() throws IOException {
            //when(explanationSparqlRepository.executeSparqlQueryWithResultSet(any())).thenReturn(resultSet);

            List<String> explanations = tmplExpService.createSpecificExplanation("annotationofinstance", "graphURI", "de", "componentuRI");

            //verify(explanationSparqlRepository, times(1)).executeSparqlQueryWithResultSet(any());
            verify(tmplExpService, times(1)).buildSparqlQuery(any(), any(), any());
            verify(tmplExpService, times(1)).addingExplanations(any(), any(), any());

            assertNotNull(explanations);
        }
         */

    }

}