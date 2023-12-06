package com.wse.qanaryexplanationservice.services;

import com.wse.qanaryexplanationservice.pojos.AutomatedTests.automatedTestingObject.automatedTestingObject.AnnotationType;
import com.wse.qanaryexplanationservice.pojos.AutomatedTests.automatedTestingObject.automatedTestingObject.AutomatedTest;
import com.wse.qanaryexplanationservice.pojos.AutomatedTests.automatedTestingObject.automatedTestingObject.Example;
import com.wse.qanaryexplanationservice.pojos.AutomatedTests.automatedTestingObject.automatedTestingObject.TestDataObject;
import com.wse.qanaryexplanationservice.repositories.AutomatedTestingRepository;
import org.apache.jena.query.ResultSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class AutomatedTestingServiceTest {


    private static ServiceDataForTests serviceDataForTests = new ServiceDataForTests();
    private final Logger logger = LoggerFactory.getLogger(AutomatedTestingService.class);
    private final Map<String, String[]> typeAndComponents;
    @Autowired
    private AutomatedTestingService automatedTestingService;

    @Autowired
    public AutomatedTestingServiceTest(Environment environment) {
        typeAndComponents = new HashMap<>() {{
            put(AnnotationType.AnnotationOfInstance.name(), environment.getProperty("qanary.components.annotationofinstance", String[].class));
            put(AnnotationType.AnnotationOfSpotInstance.name(), environment.getProperty("qanary.components.annotationofspotinstance", String[].class));
            put(AnnotationType.AnnotationOfAnswerJSON.name(), environment.getProperty("qanary.components.annotationofanswerjson", String[].class));
            put(AnnotationType.AnnotationOfAnswerSPARQL.name(), environment.getProperty("qanary.components.annotationofanswersparql", String[].class));
            put(AnnotationType.AnnotationOfQuestionLanguage.name(), environment.getProperty("qanary.components.annotationofquestionlanguage", String[].class));
            // put(AnnotationType.annotationofquestiontranslation.name(), environment.getProperty("annotationofquestiontranslation", String[].class));
            put(AnnotationType.AnnotationOfRelation.name(), environment.getProperty("qanary.components.annotationofrelation", String[].class));
        }};
    }

    @Nested
    class SelectComponentTests {

        private final Random random = new Random();
        private Example example;
        private AutomatedTest automatedTest;

        @ParameterizedTest
        @EnumSource(AnnotationType.class)
        public void selectComponentExampleNullTest(AnnotationType annotationType) {
            String component = automatedTestingService.selectComponent(annotationType, automatedTest, null);
            logger.info("Selected component: {}", component);
            Assertions.assertTrue(Arrays.stream(typeAndComponents.get(annotationType.name())).toList().contains(component));
            Assertions.assertNotNull(component);
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
            AnnotationType annotationType = AnnotationType.AnnotationOfRelation;

            // Set up Automated Test without any existing examples
            automatedTest = new AutomatedTest();
            automatedTest.setTestData(new TestDataObject(null, null, "DandelionNED", null, null, null, null, null, null, null, null));
            example = new Example(annotationType.name(), true);

            String component = automatedTestingService.selectComponent(annotationType, automatedTest, example);
            Assertions.assertTrue(Arrays.stream(typeAndComponents.get(annotationType.name())).toList().contains(component));
        }

        @Test
        public void selectComponentUniqueAndExisting() {
            AnnotationType annotationType = AnnotationType.AnnotationOfQuestionLanguage;
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

        @Test
        public void createDatasetResultSetIsNullTest() throws Exception {
            setupTest(null);

            Exception exception = Assertions.assertThrows(Exception.class, () -> {
                automatedTestingService.createDataset("componentURI", "graphURI", "anyAnnotationType");
            });

        }
    }


}
