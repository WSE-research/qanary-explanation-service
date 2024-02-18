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

    private final static String DATASET_QUERY = "/queries/evaluation_dataset_query.rq";
    private final static String QUESTION_QUERY = "/queries/random_question_query.rq";
    private static final String EXPLANATION_NAMESPACE = "urn:qanary:explanations#";
    private final static int QADO_DATASET_QUESTION_COUNT = 394;
    private final EncodingRegistry encodingRegistry = Encodings.newLazyEncodingRegistry();
    // Prefixes for ResultSets
    private final Map<String, String> prefixes = new HashMap<>() {{
        put("http://www.w3.org/ns/openannotation/core/", "oa:");
        put("http://localhost:8080/question/stored-question__text_", "questionID:");
        put("http://www.wdaqua.eu/qa#", "qa:");
        put("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "rdf:");
        put("http://www.w3.org/2000/01/rdf-schema#", "rdfs:");
        put("http://www.w3.org/2002/07/owl#", "owl:");
        put("^^http://www.w3.org/2001/XMLSchema#integer", "");
        put("^^http://www.w3.org/2001/XMLSchema#dateTime", "");
        put("^^http://www.w3.org/2001/XMLSchema#decimal", "");
        put("^^http://www.w3.org/2001/XMLSchema#float", "");
    }};
    // Dependency map for annotation types, used for setting up the Qanary pipeline, so that all relevant annotation are made
    // Important: Old approach was to resolve deeper dependencies recursively, however, yet they are hard coded in this map due to the little amount of different types
    private final Map<AnnotationType, AnnotationType[]> dependencyMapForAnnotationTypes = new TreeMap<>() {{
        put(AnnotationType.AnnotationOfInstance, new AnnotationType[]{});
        put(AnnotationType.AnnotationOfRelation, new AnnotationType[]{
                AnnotationType.AnnotationOfQuestionLanguage
        });
        put(AnnotationType.AnnotationOfSpotInstance, new AnnotationType[]{});
        //put(AnnotationType.annotationofquestiontranslation, null);
        put(AnnotationType.AnnotationOfQuestionLanguage, new AnnotationType[]{});
        put(AnnotationType.AnnotationOfAnswerSPARQL, new AnnotationType[]{
                AnnotationType.AnnotationOfInstance,
                AnnotationType.AnnotationOfRelation,
                AnnotationType.AnnotationOfSpotInstance,
                AnnotationType.AnnotationOfQuestionLanguage
        });
        put(AnnotationType.AnnotationOfAnswerJSON, new AnnotationType[]{
                AnnotationType.AnnotationOfAnswerSPARQL,
                AnnotationType.AnnotationOfInstance,
                AnnotationType.AnnotationOfRelation,
                AnnotationType.AnnotationOfSpotInstance,
                AnnotationType.AnnotationOfQuestionLanguage
        });
    }};
    private final Logger logger = LoggerFactory.getLogger(AutomatedTestingService.class);
    // stores the correct template for different x-shot approaches
    private final Map<Integer, String> exampleCountAndTemplate = new HashMap<>() {{
        put(1, "/testtemplates/oneshot");
        put(2, "/testtemplates/twoshot");
        put(3, "/testtemplates/threeshot");
    }};
    private final Random random;
    // All available annotation types and the components which create them
    private final Map<String, String[]> typeAndComponents = new HashMap<>();
    @Autowired
    private ExplanationDataService explanationDataService;
    @Value("${explanations.dataset.limit}")
    private int EXPLANATIONS_DATASET_LIMIT;
    @Autowired
    private AutomatedTestingRepository automatedTestingRepository;
    @Autowired
    private ExplanationService explanationService;

    // CONSTRUCTOR(s)
    public AutomatedTestingService(Environment environment) {
        this.random = new Random();
        for (AnnotationType annType : AnnotationType.values()
        ) {
            typeAndComponents.put(annType.name(), environment.getProperty("qanary.components." + annType.name().toLowerCase(), String[].class));
        }
    }

    /**
     * @param list List of annotation-types - in the usual workflow this comes from the dependency resolver
     * @return List of components in the order of their annotation type (and therefore their dependencies)
     */
    public List<String> selectRandomComponents(ArrayList<AnnotationType> list) {
        // Is null when no dependencies were resolved and therefore only one component shall be executed
        if (list == null)
            return new ArrayList<>();

        Collections.sort(list); // sorts them by the enum definition, which equals the dependency tree (the last is the target-component)
        List<String> componentList = new ArrayList<>();

        for (AnnotationType annType : list
        ) {
            String[] componentsList = this.typeAndComponents.get(annType.name());
            int selectedComponentAsInt = random.nextInt(componentsList.length);
            componentList.add(componentsList[selectedComponentAsInt]);
        }
        return componentList;
    }

    /**
     * Creates the explanation and returns the english one
     *
     * @return The explanation for given graphURI and componentURI
     */
    public String getExplanation(String graphURI, String componentURI) throws IOException, IndexOutOfBoundsException {

        Model explanationModel = explanationService.createModel(graphURI, "urn:qanary:" + componentURI);
        explanationModel.setNsPrefix("explanations", EXPLANATION_NAMESPACE);
        Property hasExplanationForCreatedDataProperty = explanationModel.createProperty(EXPLANATION_NAMESPACE, "hasExplanationForCreatedData");
        Statement statement = explanationModel.getRequiredProperty(ResourceFactory.createResource("urn:qanary:" + componentURI), hasExplanationForCreatedDataProperty, "en");

        return statement.getString();
    }

    /**
     * Executes a SPARQL query on the triplestore to fetch a question from the (existing!) QADO-dataset
     *
     * @param questionNumber The number of the question
     * @return a random question as plain String
     */
    public String getRandomQuestion(Integer questionNumber) throws IOException {

        QuerySolutionMap querySolutionMap = new QuerySolutionMap();
        querySolutionMap.add("id", ResourceFactory.createTypedLiteral(questionNumber.toString(), XSDDatatype.XSDnonNegativeInteger));

        try {
            String query = QanaryTripleStoreConnector.readFileFromResourcesWithMap(QUESTION_QUERY, querySolutionMap);
            ResultSet resultSet = automatedTestingRepository.takeRandomQuestion(query);
            return resultSet.next().get("hasQuestion").asLiteral().getString();
        } catch (IOException e) {
            String errorMessage = "Error while fetching a random question";
            logger.error("Error: {}", errorMessage);
            throw new IOException(errorMessage);
        }

    }

    /**
     * Calls the corresponding repository to execute the Qanary pipeline with the given components and question.
     *
     * @param randomComponents Holds the components which will be executed in the correct order (respects dependencies)
     * @return QanaryResponseObject involving the questionID as well as the graphURI
     */
    public QanaryResponseObject executeQanaryPipeline(String question, List<String> randomComponents) throws Exception {
        QanaryRequestObject qanaryRequestObject = new QanaryRequestObject(question, null, null, randomComponents);

        try {
            return automatedTestingRepository.executeQanaryPipeline(qanaryRequestObject);
        } catch (WebClientResponseException e) {
            String errorMessage = "Error while executing Qanary pipeline, Error message: " + e.getMessage();
            logger.error(errorMessage);
            throw new Exception(e);
        }
    }

    /**
     * Transforms ResultSet QuerySolutions to triple-"sentence" representation by appending s, p, o and an "."
     *
     * @return Dataset as String
     */
    public String createDataset(String componentURI, String graphURI, String annotationType) throws Exception {

        try {
            ResultSet triples = fetchTriples(graphURI, componentURI, annotationType);
            StringBuilder dataSet = new StringBuilder();
            while (triples.hasNext()) {
                QuerySolution querySolution = triples.next();
                dataSet.append(querySolution.getResource("s")).append(" ").append(querySolution.getResource("p")).append(" ").append(querySolution.get("o")).append(" .\n");
            }
            String dataSetAsString = dataSet.toString();

            // Replace prefixes
            for (Map.Entry<String, String> entry : prefixes.entrySet()) {
                dataSetAsString = dataSetAsString.replace(entry.getKey(), entry.getValue());
            }
            return dataSetAsString;
        } catch (Exception e) {
            logger.error(String.valueOf(e));
            throw new Exception(e);
        }
    }

    /**
     * Fetches triples for specific graph + component
     *
     * @return Triples as ResultSet
     * @throws Exception If a component hasn't made any annotations to the graph the query will result in an empty ResultSet
     */
    public ResultSet fetchTriples(String graphURI, String componentURI, String annotationType) throws Exception {
        QuerySolutionMap bindingsForQuery = new QuerySolutionMap();
        bindingsForQuery.add("graphURI", ResourceFactory.createResource(graphURI));
        bindingsForQuery.add("componentURI", ResourceFactory.createResource("urn:qanary:" + componentURI));
        bindingsForQuery.add("annotatedBy", ResourceFactory.createResource("urn:qanary:" + componentURI));
        try {
            String query = QanaryTripleStoreConnector.readFileFromResourcesWithMap(DATASET_QUERY, bindingsForQuery);
            query = query.replace("?annotationType", "qa:" + annotationType);
            ResultSet resultSet = automatedTestingRepository.executeSparqlQueryWithResultSet(query);

            if (!resultSet.hasNext())
                throw new RuntimeException("ResultSet is null");
            else
                return resultSet;
        } catch (IOException e) {
            logger.error("Error while fetching triples: {}", e.getMessage());
            throw new Exception(e);
        }
    }

    public ResultSet fetchTriplesWithComponentSelectQueries(String graphURI, String componentURI, String annotationType) throws Exception {
        QuerySolutionMap bindingsForSelectQuery = new QuerySolutionMap();
        bindingsForSelectQuery.add("graph", ResourceFactory.createResource(graphURI));
        bindingsForSelectQuery.add("annotatedBy", ResourceFactory.createResource("urn:qanary:" + componentURI));

        try {
            String query = QanaryTripleStoreConnector.readFileFromResourcesWithMap("/queries/select_all_" + annotationType + ".rq", bindingsForSelectQuery);
            ResultSet resultSet = automatedTestingRepository.executeSparqlQueryWithResultSet(query);
            if (!resultSet.hasNext())
                throw new RuntimeException("Error while fetching Dataset, ResultSet is null");
            else
                return resultSet;
        } catch (IOException e) {
            logger.error("Error while fetching triples: {}", e.getMessage());
            throw new Exception(e);
        }
    }

    public String selectComponent(AnnotationType annotationType, AutomatedTest automatedTest, Example example) {

        String[] componentsList = this.typeAndComponents.get(annotationType.name());

        // Case if example is null, e.g. when the testing data is calculated
        if (example == null || !example.getUniqueComponent()) {
            int selectedComponentAsInt = random.nextInt(componentsList.length);
            return componentsList[selectedComponentAsInt];
        }
        // Case if component should be unique in the whole test-case
        else {
            ArrayList<String> usedComponentsInTest = fetchUsedComponents(automatedTest);
            ArrayList<String> componentList = new ArrayList<>(List.of(this.typeAndComponents.get(annotationType.name())));
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

    public String sendPrompt(String prompt) throws Exception {
        Encoding encoding = encodingRegistry.getEncodingForModel(ModelType.GPT_3_5_TURBO); // TODO: Move to applications.properties
        int tokens = encoding.countTokens(prompt);
        logger.info("Calculated Token: {}", tokens);
        return automatedTestingRepository.sendGptPrompt(prompt, tokens);
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
            String selectedComponent = typeAndComponents.get(givenAnnotationType.name())[selectedComponentAsInt];

            // Selection Question
            logger.info("Selecting question");
            Integer randomQuestionID = random.nextInt(QADO_DATASET_QUESTION_COUNT);
            String question = getRandomQuestion(randomQuestionID);

            // Resolve dependencies and select random components
            logger.info("Resolve dependencies and select components");
            ArrayList<AnnotationType> annotationTypes = new ArrayList<>(Arrays.asList(dependencyMapForAnnotationTypes.get(givenAnnotationType)));
            List<String> componentListForQanaryPipeline = selectRandomComponents(annotationTypes);
            componentListForQanaryPipeline.add(selectedComponent); // Seperation of concerns, add this to the selectRandomComps method

            // Execute Qanary pipeline and store graphURI + questionID
            logger.info("Execute Qanary pipeline");
            QanaryResponseObject qanaryResponse = executeQanaryPipeline(question, componentListForQanaryPipeline);
            String graphURI = qanaryResponse.getOutGraph();
            String questionID = qanaryResponse.getQuestion().replace("http://localhost:8080/question/stored-question__text_", "questionID:");

            // Create dataset
            logger.info("Create dataset");
            String dataset = createDataset(selectedComponent, graphURI, givenAnnotationType.name());

            // Create Explanation for selected component
            logger.info("Create explanation");
            String explanation = getExplanation(graphURI, selectedComponent);
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
                    String gptExplanation = sendPrompt(test.getPrompt()); // Send prompt to OpenAI-API
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
        return random.nextInt(typeAndComponents.get(annotationType.name()).length);
    }


}

// TODO: don't retry already used combinations







