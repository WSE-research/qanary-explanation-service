package com.wse.qanaryexplanationservice.services;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.ModelType;
import com.wse.qanaryexplanationservice.helper.GptModel;
import com.wse.qanaryexplanationservice.helper.pojos.AutomatedTests.QanaryRequestPojos.QanaryResponseObject;
import com.wse.qanaryexplanationservice.helper.AnnotationType;
import com.wse.qanaryexplanationservice.helper.pojos.AutomatedTests.automatedTestingObject.TestDataObject;
import com.wse.qanaryexplanationservice.helper.pojos.GenerativeExplanationObject;
import com.wse.qanaryexplanationservice.helper.pojos.InputQueryExample;
import com.wse.qanaryexplanationservice.helper.pojos.QanaryComponent;
import com.wse.qanaryexplanationservice.repositories.GenerativeExplanationsRepository;
import org.apache.jena.query.QuerySolution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * This service is used to create [...]
 */
@Service
public class GenerativeExplanationsService {


    private final EncodingRegistry encodingRegistry = Encodings.newLazyEncodingRegistry();
    private final Logger logger = LoggerFactory.getLogger(GenerativeExplanationsService.class);

    private final Random random = new Random();
    @Autowired
    private GenerativeExplanations generativeExplanations;
    @Autowired
    private GenerativeExplanationsRepository generativeExplanationsRepository;
    @Value("${questionId.replacement}")
    private String questionIdReplacement;

    public GenerativeExplanationsService() {

    }

    /**
     * Method to produce a generative created explanation with x shots with the testing component @param component
     *
     * @param component Test component
     * @param shots     Number of shots used within the prompt
     * @param graphUri  GraphUri for the test component
     * @return
     */
    public GenerativeExplanationObject createGenerativeExplanation(QanaryComponent component, int shots, String graphUri) throws Exception {
        GenerativeExplanationObject generativeExplanationObject = new GenerativeExplanationObject();
        TestDataObject testObject = new TestDataObject(
                AnnotationType.valueOf(component.getComponentMainType()),
                AnnotationType.valueOf(component.getComponentMainType()).ordinal(),
                component.getComponentName(),
                null,
                generativeExplanations.getExplanation(graphUri, component.getComponentName()),
                generativeExplanations.createDataset(component.getComponentName(), graphUri, component.getComponentMainType()),
                graphUri,
                null, null, null, null
        );
        logger.warn("Dataset for test object: {}", testObject.getDataSet());
        generativeExplanationObject.setTestComponent(testObject);
        for (int i = 0; i < shots; i++) {
            TestDataObject example = computeSingleTestObject(null);
            while (example == null)
                example = computeSingleTestObject(null);
            generativeExplanationObject.addExample(example);
        }
        return generativeExplanationObject;
    }

    public TestDataObject computeSingleTestObject(QanaryComponent qanaryComponent) {

        try {

            QanaryComponent component = qanaryComponent == null ? createRandomQanaryComponent() : qanaryComponent;

            logger.info("Random qanary component: {}", component);

            // Selection Question
            logger.info("Selecting question");
            Integer randomQuestionID = random.nextInt(generativeExplanations.QADO_DATASET_QUESTION_COUNT);
            String question = generativeExplanations.getRandomQuestion(randomQuestionID);

            // Resolve dependencies and select random components
            logger.info("Resolve dependencies and select components");
            ArrayList<AnnotationType> annotationTypes = new ArrayList<>(Arrays.asList(generativeExplanations.getDependencyList(component.getComponentMainType())));
            logger.info("Annotation List: {}", annotationTypes);
            List<String> componentListForQanaryPipeline = generativeExplanations.selectRandomComponents(annotationTypes);
            componentListForQanaryPipeline.add(component.getComponentName()); // Separation of concerns, add this to the selectRandomComps method
            logger.info("List of comps: {}", componentListForQanaryPipeline);

            // Execute Qanary pipeline and store graphURI + questionID
            logger.info("Execute Qanary pipeline");
            QanaryResponseObject qanaryResponse = generativeExplanations.executeQanaryPipeline(question, componentListForQanaryPipeline);
            String graphURI = qanaryResponse.getOutGraph();
            String questionID = replaceQuestionId(qanaryResponse.getQuestion());

            // Create dataset
            logger.info("Create dataset");
            String dataset = generativeExplanations.createDataset(component.getComponentName(), graphURI, component.getComponentMainType());
            // Create Explanation for selected component
            logger.info("Create explanation");
            String explanation = generativeExplanations.getExplanation(graphURI, component.getComponentName());
            return new TestDataObject(
                    AnnotationType.valueOf(component.getComponentMainType()),
                    AnnotationType.valueOf(component.getComponentMainType()).ordinal(),
                    component.getComponentName(),
                    question,
                    explanation,
                    dataset,
                    graphURI,
                    questionID,
                    randomQuestionID,
                    null,
                    componentListForQanaryPipeline.toString());
        } catch (Exception e) {
            logger.error("{}", e.getMessage());
            return null;
        }
    }

    public String replaceQuestionId(String replaceableString) {
        return replaceableString.replace(questionIdReplacement + "/question/stored-question__text_", "questionID:");
    }

    public QanaryComponent createRandomQanaryComponent() {
        AnnotationType type = AnnotationType.values()[random.nextInt(AnnotationType.values().length)];
        String component = selectRandomComponentWithAnnotationType(type.name());
        return new QanaryComponent(component, type.name());
    }

    public String selectRandomComponentWithAnnotationType(String annotationType) {
        String[] components = generativeExplanations.typeAndComponents.get(annotationType);
        return components[random.nextInt(components.length)];
    }

    public String createPrompt(int shots, GenerativeExplanationObject generativeExplanationObject) throws IOException {
        String prompt = getStringFromFile(
                generativeExplanations.getPromptTemplate(shots)
        );

        logger.info("Shots {} and Object {}", shots, generativeExplanationObject.getExampleComponents().get(0).getExplanation());

        prompt = prompt.replace("<TASK_RDF_DATA_TEST>", generativeExplanationObject.getTestComponent().getDataSet());

        ArrayList<TestDataObject> testDataObjects = generativeExplanationObject.getExampleComponents();
        int i = 1;
        for (TestDataObject example : testDataObjects) {
            prompt = prompt.replace("<QUESTION_ID_EXAMPLE_" + i + ">", example.getQuestionID()) // TODO: Question != questionID
                    .replace("<EXAMPLE_EXPLANATION_" + i + ">", example.getExplanation())
                    .replace("<EXAMPLE_RDF_DATA_" + i + ">", example.getDataSet());
            i += 1;
        }

        return prompt;

    }

    public String getStringFromFile(String path) throws IOException {
        File file = new ClassPathResource(path).getFile();
        return new String(Files.readAllBytes(file.toPath()));
    }

    public String sendPrompt(String prompt, GptModel gptModel) throws Exception {
        Encoding encoding = encodingRegistry.getEncodingForModel(ModelType.GPT_3_5_TURBO); // TODO: Move to applications.properties
        int tokens = encoding.countTokens(prompt);
        logger.info("Calculated Token: {}", tokens);
        return generativeExplanationsRepository.sendGptPrompt(prompt, tokens, gptModel);
    }

    /**
     * Replaces the prompt depending on the passed number of shots, in case of shots > 0, than pre-defined examples are used.
     *
     * @param component
     * @param shots     Determines the selected prompt
     * @return
     * @throws Exception
     */
    public String createGenerativeExplanationInputDataForOneComponent(String component, String body, int shots, GptModel gptModel) throws Exception {
        String prompt = getStringFromFile(generativeExplanations.getPromptTemplateInputData(shots));

        prompt = prompt.replace("${QUERY}", body).replace("${COMPONENT}", component);
        if (shots > 0) {
            InputQueryExample inputQueryExample = GenerativeExplanations.INPUT_QUERIES_AND_EXAMPLE.get(random.nextInt(GenerativeExplanations.INPUT_QUERIES_AND_EXAMPLE.size()));
            prompt = prompt.replace("${ZEROSHOT_QUERY}", inputQueryExample.getQuery()).replace("${ZEROSHOT_EXPLANATION", inputQueryExample.getExplanations()); // select random pre-defined statements
            if (shots > 1) {
                InputQueryExample inputQueryExample2 = GenerativeExplanations.INPUT_QUERIES_AND_EXAMPLE.get(random.nextInt(GenerativeExplanations.INPUT_QUERIES_AND_EXAMPLE.size()));
                prompt = prompt.replace("${ONESHOT_QUERY}", inputQueryExample2.getQuery()).replace("${ONE_EXPLANATION", inputQueryExample2.getExplanations()); // select random pre-defined statements
            }
        }
        return sendPrompt(prompt, gptModel);
    }


}
