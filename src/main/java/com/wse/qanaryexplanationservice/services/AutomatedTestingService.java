package com.wse.qanaryexplanationservice.services;

import com.complexible.common.base.DateTime;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.ModelType;
import com.wse.qanaryexplanationservice.pojos.Example;
import com.wse.qanaryexplanationservice.pojos.automatedTestingObject.*;
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
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtuosoUpdateFactory;
import virtuoso.jena.driver.VirtuosoUpdateRequest;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.*;

@Service
public class AutomatedTestingService {

    private final static String DATASET_QUERY = "/queries/evaluation_dataset_query.rq";
    private final static String QUESTION_QUERY = "/queries/random_question_query.rq";
    private static final String EXPLANATION_NAMESPACE = "urn:qanary:explanations#";
    private final static int QADO_DATASET_QUESTION_COUNT = 394;
    private final EncodingRegistry encodingRegistry = Encodings.newLazyEncodingRegistry();
    private final Random random = new Random();
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
    }};
    // All available annotation types and the components creating these
    private final Map<String, String[]> typeAndComponents = new HashMap<>() {{ // TODO: Replace placeholder
        put(AnnotationType.annotationofinstance.name(), new String[]{"NED-DBpediaSpotlight", "DandelionNED", "OntoTextNED", "MeaningCloudNed", "TagmeNED"});
        put(AnnotationType.annotationofspotinstance.name(), new String[]{"TagmeNER", "TextRazor", "NER-DBpediaSpotlight", "DandelionNER"});
        put(AnnotationType.annotationofanswerjson.name(), new String[]{"QAnswerQueryBuilderAndExecutor", "SparqlExecuter"});
        put(AnnotationType.annotationofanswersparql.name(), new String[]{"SINA", "PlatypusQueryBuilder", "QAnswerQueryBuilderAndExecutor"});
        put(AnnotationType.annotationofquestionlanguage.name(), new String[]{"LD-Shuyo"});
        // put(AnnotationType.annotationofquestiontranslation.name(), new String[]{"mno", "pqr"});
        put(AnnotationType.annotationofrelation.name(), new String[]{"FalconRELcomponent-dbpedia", "DiambiguationProperty"});
    }};
    /*
     * Dependency map for annotation types
     * Used for setting up the Qanary pipeline, so that all relevant annotation are made
     */
    private final Map<AnnotationType, AnnotationType[]> dependencyMapForAnnotationTypes = new TreeMap<>() {{
        put(AnnotationType.annotationofinstance, new AnnotationType[]{});
        put(AnnotationType.annotationofrelation, new AnnotationType[]{
                AnnotationType.annotationofquestionlanguage
        });
        put(AnnotationType.annotationofspotinstance, new AnnotationType[]{});
        //put(AnnotationType.annotationofquestiontranslation, null);
        put(AnnotationType.annotationofquestionlanguage, new AnnotationType[]{});
        put(AnnotationType.annotationofanswersparql, new AnnotationType[]{
                AnnotationType.annotationofinstance,
                AnnotationType.annotationofrelation,
                AnnotationType.annotationofspotinstance,
                AnnotationType.annotationofquestionlanguage
        });
        put(AnnotationType.annotationofanswerjson, new AnnotationType[]{
                AnnotationType.annotationofanswersparql,
                AnnotationType.annotationofinstance,
                AnnotationType.annotationofrelation,
                AnnotationType.annotationofspotinstance,
                AnnotationType.annotationofquestionlanguage
        });
    }};
    private final Logger logger = LoggerFactory.getLogger(AutomatedTestingService.class);
    // stores the correct template for different x-shot approaches
    private final Map<Integer, String> exampleCountAndTemplate = new HashMap<>() {{
        put(1, "/testtemplates/oneshot");
        put(2, "/testtemplates/twoshot");
        put(3, "/testtemplates/threeshot");
    }};
    private final String INSERT_NEW_GRAPH = "/queries/insertAutomatedTest.rq";
    @Autowired
    private AutomatedTestingRepository automatedTestingRepository;
    @Autowired
    private ExplanationService explanationService;

    /**
     * TODO: Comment-out when whole map is used
     * Selects an annotation type from the map
     *
     * @return AnnotationType
     */
    public AnnotationType selectRandomAnnotationType() {
        AnnotationType[] list = AnnotationType.values();
        return list[random.nextInt(list.length)];
    }

    /**
     * TODO: Abstraction for return type // TODO: rename method
     * Selects a random question as well as a random component for a given annotation-type
     * All in all that will result in a triple containing the type,question,component
     */
    public TestDataObject createTestDataObject(AnnotationType annotationType, AutomatedTest automatedTest, Example example) throws Exception { // TODO: maybe parallelization possible? Threads?

        AnnotationType annotationType_ = annotationType;

        // Select a random annotationtype if not specified
        if (annotationType_ == null)
            annotationType_ = selectRandomAnnotationType();

        // Select random question from the QADO dataset
        Integer random = this.random.nextInt(QADO_DATASET_QUESTION_COUNT);
        String question = getRandomQuestion(random);

        // Select random components for Qanary pipeline execution
        String selectedComponent = selectComponent(annotationType_, automatedTest, example);
        int selectedComponentAsInt = Arrays.stream(this.typeAndComponents.get(annotationType_.name())).toList().indexOf(selectedComponent);
        List<String> randomComponents = selectRandomComponents(fetchDependencies(annotationType_));
        randomComponents.add(selectedComponent);

        QanaryResponseObject response = executeQanaryPipeline(question, randomComponents);
        logger.info("GraphID from pipeline execution: {}", response.getOutGraph());

        String graphURI = response.getOutGraph();
        String questionID = response.getQuestion().replace("http://localhost:8080/question/stored-question__text_", "questionID:");

        logger.info("Checkpoint 1");

        String dataset = createDataset(selectedComponent, graphURI);

        logger.info("Checkpoint 2");

        String explanation = getExplanation(graphURI, selectedComponent);

        logger.info("Checkpoint 3");

        return new TestDataObject(annotationType_, annotationType_.ordinal(), selectedComponent, question, explanation, dataset, graphURI, questionID, random, selectedComponentAsInt, randomComponents.toString());
    }

    public ArrayList<AnnotationType> fetchDependencies(AnnotationType annotationType) {

        logger.info("Get dependencies for {}", annotationType);
        try {
            List<AnnotationType> list = Arrays.asList(dependencyMapForAnnotationTypes.get(annotationType));
            HashSet<AnnotationType> hashSet = new HashSet<>(list);
            for (AnnotationType annType : hashSet
            ) {
                hashSet.addAll(resolveRecursiveDependencies(annType));
            }
            return new ArrayList<>(hashSet.stream().toList());
        } catch (NullPointerException e) {
            logger.info("No dependencies need to be resolved for type {}", annotationType);
            return null;
        }

    }

    public ArrayList<AnnotationType> resolveRecursiveDependencies(AnnotationType annType) { // TODO: Check it !!!

        AnnotationType[] list = dependencyMapForAnnotationTypes.get(annType);
        ArrayList<AnnotationType> list_ = new ArrayList<>(Arrays.asList(list));

        if (list == null) {
            list_.add(annType);
            return list_;
        }

        for (AnnotationType annoType : list
        ) {
            if (dependencyMapForAnnotationTypes.get(annType) != null) {
                list_.addAll(resolveRecursiveDependencies(annoType));
            }
        }
        return list_;
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
            ResultSet resultSet = this.automatedTestingRepository.takeRandomQuestion(query);
            return resultSet.next().get("hasQuestion").asLiteral().getString();
        } catch (IOException e) {
            String errorMessage = "Error while fetching a random question";
            logger.error("{}", errorMessage);
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
    public String createDataset(String componentURI, String graphURI) throws Exception {

        try {
            ResultSet triples = fetchTriples(graphURI, componentURI);

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
            logger.error(e.getMessage());
            throw new Exception(e);
        }
    }

    /**
     * Fetches triples for specific graph + component
     *
     * @return Triples as ResultSet
     * @throws Exception If a component hasn't made any annotations to the graph the query will result in an empty ResultSet
     */
    public ResultSet fetchTriples(String graphURI, String componentURI) throws Exception {
        QuerySolutionMap bindingsForQuery = new QuerySolutionMap();
        bindingsForQuery.add("graphURI", ResourceFactory.createResource(graphURI));
        bindingsForQuery.add("componentURI", ResourceFactory.createResource("urn:qanary:" + componentURI));
        try {
            String query = QanaryTripleStoreConnector.readFileFromResourcesWithMap(DATASET_QUERY, bindingsForQuery);

            ResultSet resultSet = automatedTestingRepository.executeSparqlQueryWithResultSet(query);

            if (!resultSet.hasNext())
                throw new RuntimeException("ResultSet is null");
            else
                return resultSet;
        } catch (IOException e) {
            logger.error("Error while fetching triples: {}", e);
            throw new Exception(e);
        }
    }

    /**
     * Sets up a complete Test-Case by setting the values for the AutomatedTest-Object
     *
     * @return AutomatedTest as a complete test-case
     * @throws Exception If ResultSet for one example is null or failures in replacePlaceholder method
     */
    public AutomatedTest executeTest(AutomatedTestRequestBody requestBody, int currentRun) throws Exception {

        AutomatedTest automatedTest = new AutomatedTest();
        try {
            // Compute Test data
            do {
                automatedTest.setTestData(createTestDataObject(AnnotationType.valueOf(requestBody.getTestingType()), null, null));
            } while (automatedTest.getTestData() != null);

            // Compute example data
            while (automatedTest.getExampleData().size() < requestBody.getExamples().length) {
                logger.info("+-+-+-+-+-+-+-+-+-+-+-+-+-+- {}.run, computing example {} out of {}", currentRun, automatedTest.getExampleData().size() + 1, requestBody.getExamples().length);
                int currentValue = automatedTest.getExampleData().size();
                TestDataObject testDataObject = null;
                do {
                    testDataObject = createTestDataObject(
                            AnnotationType.valueOf(requestBody.getExamples()[currentValue].getType()), // if null, random annotation type will be calculated
                            automatedTest, // pass the current object for further use/comparison
                            requestBody.getExamples()[currentValue] // the current Example-Object inheriting Type and uniqueness properties
                    );
                    if (testDataObject != null)
                        automatedTest.setExampleData(testDataObject); // If successful, add example to the automatedTest
                } while (testDataObject == null);
            }
        } catch (RuntimeException e) {
            logger.error("Error: {}, Runtime Exception", e.getMessage());
            return null;
        }
        try {
            automatedTest.setPrompt(replacePromptPlaceholder(exampleCountAndTemplate.get(requestBody.getExamples().length), automatedTest));
        } catch (NullPointerException e) {
            logger.error("Error: {}, Nullpointer Exception", e.getMessage());
            return null;
        }

        return automatedTest;
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

    public String sendPrompt(String prompt) throws IOException, URISyntaxException {
        Encoding encoding = encodingRegistry.getEncodingForModel(ModelType.GPT_3_5_TURBO);
        int tokens = encoding.countTokens(prompt);
        logger.info("Calculated Token: {}", tokens);
        return automatedTestingRepository.sendGptPrompt(prompt, tokens);
    }

    public String getStringFromFile(String path) throws IOException {
        File file = new ClassPathResource(path).getFile();
        return new String(Files.readAllBytes(file.toPath()));
    }

    /**
     * Method for whole process
     */
    public String executeTestsWithGptExplanation(AutomatedTestRequestBody requestBody) throws Exception {

        JSONObject jsonObject = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        AutomatedTest test = null;

        while (jsonArray.length() < requestBody.getRuns()) {
            logger.info("++++++++++++++ {}. run === {}. test is being executed", jsonArray.length() + 1, jsonArray.length() + 1);
            try {
                test = executeTest(requestBody, jsonArray.length() + 1); // null if not successful
            } catch (IOException e) {
                logger.info("Error while executing pipeline: {}", e.getMessage());
            }
            if (test != null) {
                try {
                    String gptExplanation = sendPrompt(test.getPrompt()); // Send prompt to OpenAI-API
                    test.setGptExplanation(gptExplanation); // Add the response-explanation
                    jsonArray.put(new JSONObject(test));
                } catch (IOException e) {   // Catch IOException from OpenAI-Call
                    logger.error("{}", e.getMessage());
                    jsonObject.put("explanations", jsonArray);
                    writeObjectToFile(computeFileName(requestBody), jsonObject);
                }
            }
        }

        jsonObject.put("explanations", jsonArray);
        writeObjectToFile(computeFileName(requestBody), jsonObject);

        return jsonObject.toString();
    }

    public String executeTestsWithoutGptExplanation(AutomatedTestRequestBody requestBody) throws Exception {

        // insertExplanationToTriplestore(null, requestBody);

        JSONObject jsonObject = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        AutomatedTest test = null;

        while (jsonArray.length() < requestBody.getRuns()) {
            logger.info("-------- {}. run --------", jsonArray.length() + 1);
            try {
                test = executeTest(requestBody, jsonArray.length() + 1); // null if not successful
            } catch (IOException e) { // 500 error from Qanary pipeline
                logger.info("Error while executing pipeline: {}", e.getMessage());
            }
            if (test != null)
                jsonArray.put(new JSONObject(test)); // Add test to Json-Array
            else
                logger.info("Skipped run due to null-ResultSet");
        }

        jsonObject.put("explanations", jsonArray); // Add filled-Json Array to Json-Object
        writeObjectToFile(computeFileName(requestBody), jsonObject);

        return jsonObject.toString(); // Return JSON-String
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

    // TODO: Work in progress

    public void insertExplanationToTriplestore(String graphId, AutomatedTestRequestBody automatedTestRequestBody) throws IOException {
        String graph = automatedTestRequestBody.toString();
        QuerySolutionMap bindingsForInsertQuery = new QuerySolutionMap();
        bindingsForInsertQuery.add("graph", ResourceFactory.createResource(graph));
        bindingsForInsertQuery.add("testType", ResourceFactory.createStringLiteral(automatedTestRequestBody.getTestingType()));
        bindingsForInsertQuery.add("examples", ResourceFactory.createTypedLiteral(automatedTestRequestBody.getExamples().length));

        String query = QanaryTripleStoreConnector.readFileFromResourcesWithMap(INSERT_NEW_GRAPH, bindingsForInsertQuery);
        VirtuosoUpdateRequest vur = VirtuosoUpdateFactory.create(query, new VirtGraph("jdbc:virtuoso://192.168.178.37:1111", "dba", "dba"));
        vur.exec();
    }

    ////////// RECREATE AUTOMATED SERVICE WORKFLOW
    /*
        1. Auswahl Komponente
        2. Auswahl zufällige Frage
        3. Berechnen der Dependencies
        4. Auswahl der Komponenten
        5. Ausführen der Qanary pipeline
            5.1 Auswahl der GraphURI
        6. Auswahl Dataset
        7. Erklärung für Test berechnen
     */

    public TestDataObject computeSingleTestObject(AnnotationType givenAnnotationType) {

        TestDataObject testDataObject = null;

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
            String questionID = qanaryResponse.getQuestion();

            // Create dataset
            logger.info("Create dataset");
            String dataset = createDataset(selectedComponent, graphURI);

            // Create Explanation for selected component
            logger.info("Create explanation");
            String explanation = getExplanation(graphURI, selectedComponent);
            return new TestDataObject(givenAnnotationType, givenAnnotationType.ordinal(), selectedComponent, question, explanation, dataset, graphURI, questionID, randomQuestionID, selectedComponentAsInt, componentListForQanaryPipeline.toString());
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
                TestDataObject testDataObject = null;
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

    public String createTestWorkflow(AutomatedTestRequestBody requestBody) throws Exception {

        JSONObject jsonObject = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        AutomatedTest test = null;

        while (jsonArray.length() < requestBody.getRuns()) {
            test = createTest(requestBody); // null if not successful
            if (test != null) {
                jsonArray.put(new JSONObject(test)); // Add test to Json-Array
                logger.info("------------------------- {} --------------------", test);
            } else
                logger.info("Skipped run due to null-ResultSet");
        }

        jsonObject.put("explanations", jsonArray); // Add filled-Json Array to Json-Object
        writeObjectToFile(computeFileName(requestBody), jsonObject);

        return jsonObject.toString(); // Return JSON-String
    }


    public Integer selectComponentAsInt(AnnotationType annotationType) {
        return random.nextInt(typeAndComponents.get(annotationType.name()).length);
    }


}

// TODO: don't retry already used combinations







