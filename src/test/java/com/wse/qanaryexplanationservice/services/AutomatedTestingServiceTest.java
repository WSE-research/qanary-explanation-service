package com.wse.qanaryexplanationservice.services;

import com.complexible.stardog.plan.filter.functions.numeric.E;
import com.wse.qanaryexplanationservice.pojos.AutomatedTests.QanaryObjects.QanaryResponseObject;
import com.wse.qanaryexplanationservice.pojos.AutomatedTests.automatedTestingObject.automatedTestingObject.*;
import com.wse.qanaryexplanationservice.repositories.AutomatedTestingRepository;
import com.wse.qanaryexplanationservice.repositories.ExplanationSparqlRepository;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class AutomatedTestingServiceTest {

    private static ServiceDataForTests serviceDataForTests = new ServiceDataForTests();
    private final Map<String, String[]> typeAndComponents = new HashMap<>() {{ // TODO: Replace placeholder
        put(AnnotationType.annotationofinstance.name(), new String[]{"NED-DBpediaSpotlight", "DandelionNED", "OntoTextNED", "MeaningCloudNed", "TagmeNED"});
        put(AnnotationType.annotationofspotinstance.name(), new String[]{"TagmeNER", "TextRazor", "NER-DBpediaSpotlight", "DandelionNER"});
        put(AnnotationType.annotationofanswerjson.name(), new String[]{"QAnswerQueryBuilderAndExecutor", "SparqlExecuter"});
        put(AnnotationType.annotationofanswersparql.name(), new String[]{"SINA", "PlatypusQueryBuilder", "QAnswerQueryBuilderAndExecutor"});
        put(AnnotationType.annotationofquestionlanguage.name(), new String[]{"LD-Shuyo"});
        // put(AnnotationType.annotationofquestiontranslation.name(), new String[]{"mno", "pqr"});
        put(AnnotationType.annotationofrelation.name(), new String[]{"FalconRELcomponent-dbpedia"});
    }};
    private final Logger logger = LoggerFactory.getLogger(AutomatedTestingService.class);
    @Autowired
    private AutomatedTestingService automatedTestingService;


    @Nested
    class SelectComponentTests {

        private final Random random = new Random();
        private Example example;
        private AutomatedTest automatedTest;

        @ParameterizedTest
        @EnumSource(AnnotationType.class)
        public void selectComponentExampleNullTest(AnnotationType annotationType) {
            example = null;

            String component = automatedTestingService.selectComponent(annotationType, automatedTest, example);
            logger.info("Selected component: {}", component);
            Assertions.assertTrue(Arrays.stream(typeAndComponents.get(annotationType.name())).toList().contains(component));
        }

        @ParameterizedTest
        @EnumSource(AnnotationType.class)
        public void selectComponentNotUniqueTest(AnnotationType annotationType) {
            example = new Example(annotationType.name(), false);

            String component = automatedTestingService.selectComponent(annotationType, automatedTest, example);
            logger.info("Selected component: {}", component);
            Assertions.assertTrue(Arrays.stream(typeAndComponents.get(annotationType.name())).toList().contains(component));
        }

        @Test
        public void selectComponentUniqueAndNotExisting() {
            AnnotationType annotationType = AnnotationType.annotationofrelation;

            // Set up Automated Test without any existing examples
            automatedTest = new AutomatedTest();
            automatedTest.setTestData(new TestDataObject(null, null, "DandelionNED", null, null, null, null, null, null, null, null));
            example = new Example(annotationType.name(), true);

            String component = automatedTestingService.selectComponent(annotationType, automatedTest, example);
            Assertions.assertTrue(Arrays.stream(typeAndComponents.get(annotationType.name())).toList().contains(component));
        }

        @Test
        public void selectComponentUniqueAndExisting() {
            AnnotationType annotationType = AnnotationType.annotationofquestionlanguage;
            automatedTest = new AutomatedTest();
            automatedTest.setTestData(new TestDataObject(null, null, "LD-Shuyo", null, null, null, null, null, null, null, null));
            example = new Example(annotationType.name(), true);

            Exception exception = Assertions.assertThrows(RuntimeException.class, () -> {
                automatedTestingService.selectComponent(annotationType, automatedTest, example);
            });
        }
    }

    @Nested
    class CreateDatasetTests {

        /*
         * Happy path: ResultSet not null
         * Sad path: ResultSet is null
         */

        @MockBean
        private AutomatedTestingRepository automatedTestingRepository;


        // Setup fetchTriples handling
        private void setupTest(ResultSet resultSet) {
            when(automatedTestingRepository.executeSparqlQueryWithResultSet(any())).thenReturn(resultSet);
        }

        /*
        @Test
        public void createDatasetResultSetNotNullTest() throws Exception {
            ResultSet resultSet = serviceDataForTests.createResultSet(serviceDataForTests.getQuerySolutionMapList());
            setupTest(resultSet);

            String resultDataset = automatedTestingService.createDataset("componentURI", "graphURI");
            Assertions.assertFalse(resultDataset.isEmpty());
        }
        */

        @Test
        public void createDatasetResultSetIsNullTest() throws Exception {
            setupTest(null);

            Exception exception = Assertions.assertThrows(RuntimeException.class, () -> {
                automatedTestingService.createDataset("componentURI", "graphURI");
            });

            //Assertions.assertEquals("ResultSet is null", exception.getMessage());
        }
    }

    @Nested
    class GetDependenciesTests {

        /*
         * Happy path: No dependencies
         * Sad path: Dependencies
         * Result: List of Dependencies without (!) the origin AnnotationType (else the method would lead to a infinite loop)
         */

        @ParameterizedTest
        @EnumSource(AnnotationType.class)
        public void getDependenciesTest(AnnotationType annotationType) {
            ArrayList<AnnotationType> listOfDependencies = automatedTestingService.fetchDependencies(annotationType);
            if (listOfDependencies != null)
                Collections.sort(listOfDependencies);

            switch (annotationType) {
                case annotationofinstance:
                case annotationofspotinstance:
                case annotationofrelation:
                case annotationofquestionlanguage: {
                    Assertions.assertNull(listOfDependencies);
                    break;
                }
                case annotationofanswersparql: {
                    ArrayList<AnnotationType> arrayList = new ArrayList<>() {{
                        add(AnnotationType.annotationofinstance);
                        add(AnnotationType.annotationofspotinstance);
                        add(AnnotationType.annotationofrelation);
                    }};
                    Assertions.assertEquals(arrayList, listOfDependencies);
                    break;
                }
                case annotationofanswerjson: {
                    ArrayList<AnnotationType> arrayList = new ArrayList<>() {{
                        add(AnnotationType.annotationofinstance);
                        add(AnnotationType.annotationofspotinstance);
                        add(AnnotationType.annotationofrelation);
                        add(AnnotationType.annotationofanswersparql);
                    }};
                    Assertions.assertEquals(arrayList, listOfDependencies);
                    break;
                }
                default: {
                    throw new RuntimeException("getDependenciesTest did not succeed");
                }
            }
        }

    }

    @Nested
    @RunWith(MockitoJUnitRunner.class)
    class InsertAutomatedTestsTest {

        @Mock
        private AutomatedTestingRepository automatedTestingRepository;
        @Mock
        private ExplanationSparqlRepository explanationSparqlRepository;
        @Mock
        private ExplanationService explanationService;

        private AutomatedTestRequestBody automatedTestRequestBody = new AutomatedTestRequestBody();
        private ArrayList<Example> examples = new ArrayList<>() {{
            new Example("annotationofinstance", true);
        }};
        private ServiceDataForTests serviceDataForTests = new ServiceDataForTests();
        private List<QuerySolutionMap> liste = new ArrayList<>() {{
            new QuerySolutionMap().add("hasQuestion", ResourceFactory.createPlainLiteral("Example_Question"));
        }};

        @BeforeEach
        public void setup() throws IOException {
            ResultSet resultSet = serviceDataForTests.createResultSet(liste);
            QanaryResponseObject qanaryResponseObject = new QanaryResponseObject();
            qanaryResponseObject.setEndpoint("endpoint"); qanaryResponseObject.setInGraph("ingraph"); qanaryResponseObject.setOutGraph(qanaryResponseObject.getInGraph()); qanaryResponseObject.setQuestion("Example_question");
            when(automatedTestingRepository.takeRandomQuestion(any())).thenReturn(resultSet);
            when(automatedTestingRepository.executeQanaryPipeline(any())).thenReturn(qanaryResponseObject);
            when(automatedTestingRepository.executeSparqlQueryWithResultSet(any())).thenReturn(resultSet);
            when(explanationService.createModel(any(), any())).thenReturn(ModelFactory.createDefaultModel());
        }

        @Test
        public void insertQueryCorrect() throws Exception {
            automatedTestRequestBody.setTestingType("annotationofinstance");
            automatedTestRequestBody.setRuns(1);
            automatedTestRequestBody.setExamples(examples.toArray(Example[]::new));

            automatedTestingService.createTestWorkflow(automatedTestRequestBody);
        }


    }

}
