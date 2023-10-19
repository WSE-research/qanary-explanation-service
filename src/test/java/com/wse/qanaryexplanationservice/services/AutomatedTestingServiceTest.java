package com.wse.qanaryexplanationservice.services;

import com.wse.qanaryexplanationservice.pojos.Example;
import com.wse.qanaryexplanationservice.pojos.automatedTestingObject.AnnotationType;
import com.wse.qanaryexplanationservice.pojos.automatedTestingObject.AutomatedTest;
import com.wse.qanaryexplanationservice.pojos.automatedTestingObject.TestDataObject;
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
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class AutomatedTestingServiceTest {

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

    @Test
    public void createDatasetTest() {

    }

    @Test
    public void selectComponentTest() {
        // Happy path: 1) example null 2) not uniqueComponent


        // Sad path: 1) uniqueComponent and not already existing 2) uniqueComponent and already existing (super sad)
    }

    @Nested
    class selectComponentTests {

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
}
