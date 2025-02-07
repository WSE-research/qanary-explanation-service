package com.wse.qanaryexplanationservice.services;

import com.wse.qanaryexplanationservice.helper.ExplanationHelper;
import com.wse.qanaryexplanationservice.helper.pojos.QanaryComponent;
import com.wse.qanaryexplanationservice.repositories.QanaryRepository;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.junit.jupiter.api.Assertions;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class TemplateExplanationsServiceTest {
    private static final String EXPLANATION_NAMESPACE = "urn:qanary:explanations";
    private final ServiceDataForTests serviceDataForTests = new ServiceDataForTests();
    protected ClassLoader classLoader = this.getClass().getClassLoader();
    Logger logger = LoggerFactory.getLogger(TemplateExplanationsServiceTest.class);
    @Autowired
    private TemplateExplanationsService templateExplanationsService;

    @Nested
    class ExplanationAsRdfTurtle {

        private final QanaryComponent component = new QanaryComponent("urn:qanary:QB-SimpleRealNameOfSuperHero");
        LanguageContentProvider languageContentProvider;
        Model model;
        String sparqlQuery;
        String queryPrefixes = "PREFIX explanation: <" + EXPLANATION_NAMESPACE + ">";

        @BeforeEach
        void setup() {
            languageContentProvider = new LanguageContentProvider("Dieser Content ist auf Deutsch", "And this one is english");
            model = ModelFactory.createDefaultModel();
            sparqlQuery = queryPrefixes + " SELECT ?subject ?object WHERE { ?subject explanation:hasExplanationForCreatedData ?object }";
        }

        @Test
        void createRdfRepresentationTest() {
            String result = templateExplanationsService.convertToDesiredFormat(null, templateExplanationsService.createModelForSpecificComponent(languageContentProvider.getContentDe(), languageContentProvider.getContentEn(), component));

            assertAll("String contains content elements as well as componentURI",
                    () -> assertTrue(result.contains(languageContentProvider.getContentDe())),
                    () -> assertTrue(result.contains(languageContentProvider.getContentEn())),
                    () -> assertTrue(result.contains(component.getComponentName()))
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
            String resultEmptyHeader = templateExplanationsService.convertToDesiredFormat(null, templateExplanationsService.createModelForSpecificComponent(languageContentProvider.getContentDe(), languageContentProvider.getContentEn(), component));
            String resultTurtleHeader = templateExplanationsService.convertToDesiredFormat("text/turtle", templateExplanationsService.createModelForSpecificComponent(languageContentProvider.getContentDe(), languageContentProvider.getContentEn(), component));
            String resultRDFXMLHeader = templateExplanationsService.convertToDesiredFormat("application/rdf+xml", templateExplanationsService.createModelForSpecificComponent(languageContentProvider.getContentDe(), languageContentProvider.getContentEn(), component));
            String resultJSONLDHeader = templateExplanationsService.convertToDesiredFormat("application/ld+json", templateExplanationsService.createModelForSpecificComponent(languageContentProvider.getContentDe(), languageContentProvider.getContentEn(), component));

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
        private TemplateExplanationsService templateExplanationsService;
        @MockBean
        private QanaryRepository qanaryRepository;

        @BeforeEach
        public void setup() {
            when(qanaryRepository.getQuestionFromQuestionId(any())).thenReturn("test");
        }

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

            Map<String, String> comparedMap = ExplanationHelper.convertRdfNodeToStringValue(toBeConvertedMap);

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


            assertAll("Testing correct replacement for templates",
                    () -> {
                        String computedTemplate = templateExplanationsService.replaceProperties(convertedMap, ExplanationHelper.getStringFromFile(annotationTypeExplanationTemplate.get(type) + "de" + "_list_item"));
                        String expectedOutcomeFilePath = "expected_list_explanations/" + type + "/de_list_item";
                        File file = new File(Objects.requireNonNull(classLoader.getResource(expectedOutcomeFilePath)).getFile());
                        String expectedOutcome = new String(Files.readAllBytes(file.toPath()));
                        assertEquals(expectedOutcome, computedTemplate);
                    },
                    () -> {
                        String computedTemplate = templateExplanationsService.replaceProperties(convertedMap, ExplanationHelper.getStringFromFile(annotationTypeExplanationTemplate.get(type) + "en" + "_list_item"));
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

            List<String> computedExplanations = templateExplanationsService.addingExplanations(type, "de", resultSet);

            // Should contain the (if existing) prefix (is an empty element) and one explanation
            assertEquals(2, computedExplanations.size());
            assertNotEquals("", computedExplanations.get(1));
            for (String expl : computedExplanations
            ) {
                assertFalse(expl.contains("$"));
            }

        }

        @Test
        public void pipelineInputExplanationTest() throws IOException {
            String question = "When was Konrad Zuse born?";
            String explanation = templateExplanationsService.getPipelineInputExplanation(question);
            File file = new File(Objects.requireNonNull(classLoader.getResource("expected_explanations/pipeline_input")).getFile());
            String expectedOutcome = new String(Files.readAllBytes(file.toPath()));
            assertEquals(expectedOutcome, explanation);
        }

        @Test
        public void pipelineOutputExplanationTest() throws IOException {
            QuerySolutionMap querySolutionMap1 = new QuerySolutionMap();
            querySolutionMap1.add("component", ResourceFactory.createResource("DBpedia"));
            QuerySolutionMap querySolutionMap2 = new QuerySolutionMap();
            querySolutionMap2.add("component", ResourceFactory.createResource("DBpedia2"));
            List<QuerySolutionMap> querySolutionMapList = new ArrayList<>() {{
                add(querySolutionMap1);
                add(querySolutionMap2);
            }};
            ResultSet results = serviceDataForTests.createResultSet(querySolutionMapList);
            String graph = "test-graph";
            String explanation = templateExplanationsService.getPipelineOutputExplanation(results, graph);
            File file = new File(Objects.requireNonNull(classLoader.getResource("expected_explanations/pipeline_output")).getFile());
            String expectedOutcome = new String(Files.readAllBytes(file.toPath()));
            // assertEquals(expectedOutcome, explanation);
        }

    }

    @Nested
    class ComposedExplanation {

        private final String EXAMPLE_INPUT_EXPLANATION = "A";
        private final String EXAMPLE_OUTPUT_EXPLANATION = "B";
        private final String EXAMPLE_COMPONENT = "exampleComponent";

        @Test
        public void composeInputAndOutputExplanationsPipelineTest() throws IOException {
            File file = new File(Objects.requireNonNull(classLoader.getResource("expected_explanations/input_output_pipeline")).getFile());
            String expectedOutcome = new String(Files.readAllBytes(file.toPath()));
            String explanation = templateExplanationsService.composeInputAndOutputExplanations(
                    EXAMPLE_INPUT_EXPLANATION,
                    EXAMPLE_OUTPUT_EXPLANATION,
                    null
            );

            assertEquals(expectedOutcome, explanation);
        }

        @Test
        public void composeInputAndOutputExplanationsComponentTest() throws IOException {
            File file = new File(Objects.requireNonNull(classLoader.getResource("expected_explanations/input_output_component")).getFile());
            String expectedOutcome = new String(Files.readAllBytes(file.toPath()));
            String explanation = templateExplanationsService.composeInputAndOutputExplanations(
                    EXAMPLE_INPUT_EXPLANATION,
                    EXAMPLE_OUTPUT_EXPLANATION,
                    EXAMPLE_COMPONENT
            );

            assertEquals(expectedOutcome, explanation);
        }

        @Nested
        class composeExplanationTests {
            QanaryComponent qanaryComponent;
            private List<String> explanations;

            @BeforeEach
            public void setup() {
                this.qanaryComponent = new QanaryComponent("component");
                this.explanations = new ArrayList<>();
                explanations.add("prefix");
                explanations.add("explanation1");
                explanations.add("explanation2");
                explanations.add("explanation3");
            }

            @Test
            public void composeExplanationsTestGerman() {
                String expectedDe = "Die Komponente component hat 3 Annotation(en) zum Graph hinzugefügt: 1. explanation1 2. explanation2 3. explanation3";
                String computedDe = templateExplanationsService.composeExplanations(qanaryComponent,"de",explanations,"");
                Assertions.assertEquals(expectedDe,computedDe);

            }
            @Test
            public void composeExplanationsTestEnglish() {
                String expectedEn = "The component component has added 3 annotation(s) to the graph: 1. explanation1 2. explanation2 3. explanation3";
                String computedEn = templateExplanationsService.composeExplanations(qanaryComponent, "en", explanations, "");
                Assertions.assertEquals(expectedEn,computedEn);
            }
        }

    }

}
