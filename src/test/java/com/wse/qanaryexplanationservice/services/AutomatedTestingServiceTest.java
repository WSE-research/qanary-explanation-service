package com.wse.qanaryexplanationservice.services;

import com.wse.qanaryexplanationservice.helper.AnnotationType;
import com.wse.qanaryexplanationservice.helper.pojos.AutomatedTests.automatedTestingObject.AutomatedTest;
import com.wse.qanaryexplanationservice.helper.pojos.AutomatedTests.automatedTestingObject.Example;
import com.wse.qanaryexplanationservice.helper.pojos.AutomatedTests.automatedTestingObject.TestDataObject;
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
import org.springframework.core.env.Environment;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class AutomatedTestingServiceTest {

    private final Logger logger = LoggerFactory.getLogger(AutomatedTestingService.class);
    private final Map<String, String[]> typeAndComponents = new HashMap<>();
    @Autowired
    private AutomatedTestingService automatedTestingService;
    @Autowired
    private GenerativeExplanations generativeExplanations;

    @Autowired
    public AutomatedTestingServiceTest(Environment environment) {
        for (AnnotationType annType : AnnotationType.values()
        ) {
            typeAndComponents.put(annType.name(), environment.getProperty("qanary.components." + annType.name().toLowerCase(), String[].class));
        }
    }

    @Nested
    class SelectComponentTests {

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

            Assertions.assertThrows(RuntimeException.class, () -> automatedTestingService.selectComponent(annotationType, automatedTest, example));
        }
    }

    @Nested
    class CreateDatasetTests {

        /*
         * Happy path: ResultSet not null
         * Sad path: ResultSet is null
         */

        /*
        @Test
        public void createDatasetResultSetIsNullTest() {
            Assertions.assertThrows(Exception.class, () -> generativeExplanations.createDataset("componentURI", "graphURI", "anyAnnotationType"));
        }
         */
    }


}
