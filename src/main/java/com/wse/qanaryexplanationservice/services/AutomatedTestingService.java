package com.wse.qanaryexplanationservice.services;

import com.complexible.common.base.DateTime;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.ModelType;
import com.wse.qanaryexplanationservice.pojos.AutomatedTests.QanaryObjects.QanaryRequestObject;
import com.wse.qanaryexplanationservice.pojos.AutomatedTests.QanaryObjects.QanaryResponseObject;
import com.wse.qanaryexplanationservice.pojos.AutomatedTests.automatedTestingObject.automatedTestingObject.*;
import com.wse.qanaryexplanationservice.repositories.AutomatedTestingRepository;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

@Service
public class AutomatedTestingService {

    private final EncodingRegistry encodingRegistry = Encodings.newLazyEncodingRegistry();
    // Prefixes for ResultSets

    private final Logger logger = LoggerFactory.getLogger(AutomatedTestingService.class);
    // stores the correct template for different x-shot approaches
    private final Map<Integer, String> exampleCountAndTemplate = new HashMap<>() {{
        put(1, "/testtemplates/oneshot");
        put(2, "/testtemplates/twoshot");
        put(3, "/testtemplates/threeshot");
    }};
    private final Random random;
    @Autowired
    private GenerativeExplanations generativeExplanations;
    @Autowired
    private ExplanationDataService explanationDataService;
    @Value("${explanations.dataset.limit}")
    private int EXPLANATIONS_DATASET_LIMIT;
    @Autowired
    private GenerativeExplanationsService generativeExplanationsService;

    // CONSTRUCTOR(s)
    public AutomatedTestingService(Environment environment) {
        this.random = new Random();
    }

    public String selectComponent(AnnotationType annotationType, AutomatedTest automatedTest, Example example) {

        String[] componentsList = generativeExplanations.typeAndComponents.get(annotationType.name());

        // Case if example is null, e.g. when the testing data is calculated
        if (example == null || !example.getUniqueComponent()) {
            int selectedComponentAsInt = random.nextInt(componentsList.length);
            return componentsList[selectedComponentAsInt];
        }
        // Case if component should be unique in the whole test-case
        else {
            ArrayList<String> usedComponentsInTest = fetchUsedComponents(automatedTest);
            ArrayList<String> componentList = new ArrayList<>(List.of(generativeExplanations.typeAndComponents.get(annotationType.name())));
            String component;
            try {
                do {
                    int rnd = this.random.nextInt(componentList.size());
                    component = componentList.get(rnd);
                    componentList.remove(rnd);  // Remove visited item -> list.size()-1 -> prevent infinite loop
                } while (usedComponentsInTest.contains(component));
            } catch (Exception e) {
                throw new RuntimeException("There is no other unique and unused component for type " + annotationType.name());
            }
            return component;
        }
    }

    public ArrayList<String> fetchUsedComponents(AutomatedTest automatedTest) {
        ArrayList<String> list = new ArrayList<>();
        list.add(automatedTest.getTestData().getUsedComponent()); // Adds test-data component
        ArrayList<TestDataObject> listExamples = automatedTest.getExampleData();

        for (TestDataObject item : listExamples) { // Adds every currently known component to the list
            list.add(item.getUsedComponent());
        }

        return list;
    }


    public String replacePromptPlaceholder(String emptyPromptPath, AutomatedTest automatedTest) throws IOException {

        // Replace test object
        TestDataObject testData = automatedTest.getTestData();
        String prompt = getStringFromFile(emptyPromptPath);
        prompt = prompt.replace("<TASK_RDF_DATA_TEST>", testData.getDataSet());

        // Replace examples
        int i = 1;
        for (TestDataObject exampleObject : automatedTest.getExampleData()
        ) {
            prompt = prompt.replace("<QUESTION_ID_EXAMPLE_" + i + ">", exampleObject.getQuestionID()) // TODO: Question != questionID
                    .replace("<EXAMPLE_EXPLANATION_" + i + ">", exampleObject.getExplanation())
                    .replace("<EXAMPLE_RDF_DATA_" + i + ">", exampleObject.getDataSet());
            i += 1;
        }
        return prompt;
    }

    public String getStringFromFile(String path) throws IOException {
        File file = new ClassPathResource(path).getFile();
        return new String(Files.readAllBytes(file.toPath()));
    }

    public String computeFileName(AutomatedTestRequestBody requestBody) {
        StringBuilder fileName = new StringBuilder();
        fileName.append(requestBody.getRuns())
                .append("_runs_")
                .append(requestBody.getTestingType())
                .append("_").append(requestBody.getExamples().length)
                .append("shot").append(requestBody.listToString())
                .append("_").append(DateTime.now())
                .append(".json");
        logger.info("Filename: {}", fileName);

        return fileName.toString();
    }

    public void writeObjectToFile(String filename, JSONObject jsonObject) throws IOException {
        if (!jsonObject.isEmpty()) {
            FileWriter fileWriter = new FileWriter("createdFiles/" + filename);
            fileWriter.write(jsonObject.toString());
            fileWriter.flush();
            fileWriter.close();
        }
    }

    public TestDataObject computeSingleTestObject(AnnotationType givenAnnotationType) {

        try {
            // Select component
            logger.info("Selecting component");
            Integer selectedComponentAsInt = selectComponentAsInt(givenAnnotationType);
            String selectedComponent = generativeExplanations.typeAndComponents.get(givenAnnotationType.name())[selectedComponentAsInt];

            // Selection Question
            logger.info("Selecting question");
            Integer randomQuestionID = random.nextInt(generativeExplanations.QADO_DATASET_QUESTION_COUNT);
            String question = generativeExplanations.getRandomQuestion(randomQuestionID);

            // Resolve dependencies and select random components
            logger.info("Resolve dependencies and select components");
            ArrayList<AnnotationType> annotationTypes = new ArrayList<>(Arrays.asList(generativeExplanations.dependencyMapForAnnotationTypes.get(givenAnnotationType)));
            List<String> componentListForQanaryPipeline = generativeExplanations.selectRandomComponents(annotationTypes);
            componentListForQanaryPipeline.add(selectedComponent); // Seperation of concerns, add this to the selectRandomComps method

            // Execute Qanary pipeline and store graphURI + questionID
            logger.info("Execute Qanary pipeline");
            QanaryResponseObject qanaryResponse = generativeExplanations.executeQanaryPipeline(question, componentListForQanaryPipeline);
            String graphURI = qanaryResponse.getOutGraph();
            String questionID = qanaryResponse.getQuestion().replace("http://localhost:8080/question/stored-question__text_", "questionID:");

            // Create dataset
            logger.info("Create dataset");
            String dataset = generativeExplanations.createDataset(selectedComponent, graphURI, givenAnnotationType.name());

            // Create Explanation for selected component
            logger.info("Create explanation");
            String explanation = generativeExplanations.getExplanation(graphURI, selectedComponent);
            return new TestDataObject(
                    givenAnnotationType,
                    givenAnnotationType.ordinal(),
                    selectedComponent,
                    question,
                    explanation,
                    dataset,
                    graphURI,
                    questionID,
                    randomQuestionID,
                    selectedComponentAsInt,
                    componentListForQanaryPipeline.toString());
        } catch (Exception e) {
            logger.error("{}", e.getMessage());
            return null;
        }
    }

    public AutomatedTest createTest(AutomatedTestRequestBody requestBody) {

        AutomatedTest automatedTest = new AutomatedTest();

        try {
            // Compute Test data
            do {
                automatedTest.setTestData(computeSingleTestObject(AnnotationType.valueOf(requestBody.getTestingType())));
            } while (automatedTest.getTestData() == null);
            // Compute example data
            while (automatedTest.getExampleData().size() < requestBody.getExamples().length) {
                int currentValue = automatedTest.getExampleData().size();
                TestDataObject testDataObject;
                do {
                    testDataObject = computeSingleTestObject(
                            AnnotationType.valueOf(requestBody.getExamples()[currentValue].getType()) // if null, random annotation type will be calculated
                    );
                    if (testDataObject != null)
                        automatedTest.setExampleData(testDataObject); // If successful, add example to the automatedTest
                } while (testDataObject == null);
            }
            automatedTest.setPrompt(replacePromptPlaceholder(exampleCountAndTemplate.get(requestBody.getExamples().length), automatedTest));
        } catch (RuntimeException e) {
            logger.error("{}", e.getMessage());
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return automatedTest;
    }

    public String createTestWorkflow(AutomatedTestRequestBody requestBody, boolean doGptCall) throws Exception {

        JSONObject jsonObject = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        AutomatedTest test;

        while (jsonArray.length() < requestBody.getRuns()) {
            logger.info("CURRENT RUN: {}", jsonArray.length()+1);
            test = createTest(requestBody); // null if not successful
            if (test != null) {
                if (doGptCall) {
                    String gptExplanation = generativeExplanationsService.sendPrompt(test.getPrompt()); // Send prompt to OpenAI-API
                    test.setGptExplanation(gptExplanation); // Add the response-explanation
                }
                JSONObject finishedTest = new JSONObject(test);
                jsonArray.put(finishedTest); // Add test to Json-Array
                explanationDataService.insertDataset(test);
            } else
                logger.info("Skipped run due to null-ResultSet");
        }

        jsonObject.put("explanations", jsonArray); // Add filled JSON Array to Json-Object
        writeObjectToFile(computeFileName(requestBody), jsonObject);

        return jsonObject.toString(); // Return as JSON-String
    }

    public Integer selectComponentAsInt(AnnotationType annotationType) {
        return random.nextInt(generativeExplanations.typeAndComponents.get(annotationType.name()).length);
    }

}

// TODO: don't retry already used combinations







