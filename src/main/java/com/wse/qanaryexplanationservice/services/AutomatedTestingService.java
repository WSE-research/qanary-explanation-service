package com.wse.qanaryexplanationservice.services;

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
        put("http://195.90.200.248:8090/question/stored-question__text_", "questionID:");
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
        // put(AnnotationType.annotationofanswerjson.name(), new String[]{"SINA", "PlatypusQueryBuilder"});
        put(AnnotationType.annotationofanswersparql.name(), new String[]{"SINA", "PlatypusQueryBuilder", "Monolitic"});
        // put(AnnotationType.annotationofquestionlanguage.name(), new String[]{"LD-Shuyo"});
        // put(AnnotationType.annotationofquestiontranslation.name(), new String[]{"mno", "pqr"});
        put(AnnotationType.annotationofrelation.name(), new String[]{"FalconRELcomponent-dbpedia"});
    }};

    /*
     * Dependency map for annotation types
     * Used for setting up the Qanary pipeline, so that all relevant annotation are made
     */
    private final Map<AnnotationType, AnnotationType[]> dependencyMapForAnnotationTypes = new TreeMap<>() {{
        put(AnnotationType.annotationofinstance, null);
        put(AnnotationType.annotationofrelation, null);
        put(AnnotationType.annotationofspotinstance, null);
        //put(AnnotationType.annotationofquestiontranslation, null);
        put(AnnotationType.annotationofquestionlanguage, null);
        put(AnnotationType.annotationofanswersparql, new AnnotationType[]{
                AnnotationType.annotationofinstance,
                AnnotationType.annotationofrelation,
                AnnotationType.annotationofspotinstance,
                //  AnnotationType.annotationofquestiontranslation
        });
        put(AnnotationType.annotationofanswerjson, new AnnotationType[]{AnnotationType.annotationofanswersparql});
    }};
    private final Logger logger = LoggerFactory.getLogger(AutomatedTestingService.class);

    // stores the correct template for different x-shot approaches
    private final Map<Integer, String> exampleCountAndTemplate = new HashMap<>() {{
        put(1, "/testtemplates/oneshot");
        put(2, "/testtemplates/twoshot");
        put(3, "/testtemplates/threeshot");
    }};
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

        // return AnnotationType.annotationofspotinstance;
    }

    /**
     * TODO: Abstraction for return type // TODO: rename method
     * Selects a random question as well as a random component for a given annotation-type
     * All in all that will result in a triple containing the type,question,component
     */
    public TestDataObject selectTestingTriple(AnnotationType annotationType, AutomatedTest automatedTest, Example example) throws Exception { // TODO: maybe parallelization possible? Threads?

        AnnotationType annotationType_ = annotationType;

        if (annotationType_ == null)
            annotationType_ = selectRandomAnnotationType();

        //TODO: separate Testing and example objects!?

        String selectedComponent = selectComponent(annotationType_, automatedTest, example);
        int selectedComponentAsInt = Arrays.stream(this.typeAndComponents.get(annotationType_.name())).toList().indexOf(selectedComponent);

        Integer random = this.random.nextInt(QADO_DATASET_QUESTION_COUNT);
        String question = getRandomQuestion(random);

        List<String> randomComponents = selectRandomComponents(getDependencies(annotationType_));
        randomComponents.add(selectedComponent);
        QanaryResponseObject response = executeQanaryPipeline(question, randomComponents);

        String graphURI = response.getOutGraph();
        String questionID = response.getQuestion().replace("http://195.90.200.248:8090/question/stored-question__text_", "questionID:");

        String dataset = createDataset(selectedComponent, graphURI);

        String explanation = getExplanation(graphURI, selectedComponent);

        // TODO: see todo below, additionally random picking
        // Integer selectedQuestionAsInt = this.qadoDatasetRepository ...
        // String selectedQuestion = this.qadoDatasetRepository.getDataset();   // TODO: How to work with that data since it's a huge dataset for parsing w/ JsonNode(s)
        // TODO: Caching? Maybe too large when it comes to thousands of datasets?
        return new TestDataObject(annotationType_, annotationType_.ordinal(), selectedComponent, question, explanation, dataset, graphURI, questionID, random, selectedComponentAsInt, randomComponents.toString());
    }

    public ArrayList<AnnotationType> getDependencies(AnnotationType annotationType) {

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
            return new ArrayList<>() {{
                add(annotationType);
            }};
        }

    }

    public ArrayList<AnnotationType> resolveRecursiveDependencies(AnnotationType annType) { // TODO: Check it !!!

        try {
            AnnotationType[] list = dependencyMapForAnnotationTypes.get(annType);
            ArrayList<AnnotationType> list_ = new ArrayList<>(Arrays.asList(list));

            for (AnnotationType annoType : list
            ) {
                if (dependencyMapForAnnotationTypes.get(annType) != null) {
                    list_.addAll(resolveRecursiveDependencies(annoType));
                }
            }
            return list_;
        } catch (NullPointerException e) {
            return new ArrayList<>() {{
                add(annType);
            }};
        }

    }

    /**
     * @param list List of annotation-types - in the usual workflow this comes from the dependency resolver
     * @return List of components in the order of their annotation type (and therefore their dependencies)
     */
    public List<String> selectRandomComponents(ArrayList<AnnotationType> list) {
        Collections.sort(list);
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
        String query = QanaryTripleStoreConnector.readFileFromResourcesWithMap(QUESTION_QUERY, querySolutionMap);

        ResultSet resultSet = this.automatedTestingRepository.takeRandomQuestion(query);

        return resultSet.next().get("hasQuestion").asLiteral().getString();
    }

    /**
     * Calls the corresponding repository to execute the Qanary pipeline with the given components and question.
     *
     * @param randomComponents Holds the components which will be executed in the correct order (respects dependencies)
     * @return QanaryResponseObject involving the questionID as well as the graphURI
     */
    public QanaryResponseObject executeQanaryPipeline(String question, List<String> randomComponents) throws IOException { // TODO: Respect component-dependency tree !!!
        QanaryRequestObject qanaryRequestObject = new QanaryRequestObject(question, null, null, randomComponents);
        // executes a qanary pipeline and take the graphID from it + questionURI since the question can be fetched via <questionURI>/raw
        return automatedTestingRepository.executeQanaryPipeline(qanaryRequestObject);
    }

    /**
     * Transforms ResultSet QuerySolutions to triple-"sentence" representation by appending s, p, o and an "."
     *
     * @return Dataset as String
     */
    public String createDataset(String componentURI, String graphURI) throws Exception {

        ResultSet triples = fetchTriples(graphURI, componentURI);

        // TODO: triples must follow the pattern "<..> ... <...> ."
        // TODO: with prefixes included
        StringBuilder dataSet = new StringBuilder();
        while (triples.hasNext()) {
            QuerySolution querySolution = triples.next();
            dataSet.append(querySolution.getResource("s")).append(" ").append(querySolution.getResource("p")).append(" ").append(querySolution.get("o")).append(" .\n");
        }
        String dataSetAsString = dataSet.toString();

        // Replaces prefixes
        for (Map.Entry<String, String> entry : prefixes.entrySet()) {
            dataSetAsString = dataSetAsString.replace(entry.getKey(), entry.getValue());
        }

        return dataSetAsString;
    }

    /**
     * Fetches triples for specific graph + component
     *
     * @return Triples as ResultSet
     * @throws Exception If a component hasn't made any annotations to the graph the query will result in a empty ResultSet
     */
    public ResultSet fetchTriples(String graphURI, String componentURI) throws Exception {
        QuerySolutionMap bindingsForQuery = new QuerySolutionMap();
        bindingsForQuery.add("graphURI", ResourceFactory.createResource(graphURI));
        bindingsForQuery.add("componentURI", ResourceFactory.createResource("urn:qanary:" + componentURI));
        String query = QanaryTripleStoreConnector.readFileFromResourcesWithMap(DATASET_QUERY, bindingsForQuery);

        ResultSet resultSet = automatedTestingRepository.executeSparqlQueryWithResultSet(query);

        if (!resultSet.hasNext())
            throw new RuntimeException("ResultSet is null");
        else
            return resultSet;
    }

    /**
     * Sets up a complete Test-Case by setting the values for the AutomatedTest-Object
     *
     * @return AutomatedTest as a complete test-case
     * @throws Exception If ResultSet for one example is null or failures in replacePlaceholder method
     */
    public AutomatedTest setUpTest(AutomatedTestRequestBody requestBody) throws Exception {

        AutomatedTest automatedTest = new AutomatedTest();
        try {
            automatedTest.setTestData(selectTestingTriple(AnnotationType.valueOf(requestBody.getTestingType()), null, null));
            for (int i = 0; i < requestBody.getExamples().length; i++) {
                TestDataObject testDataObject = selectTestingTriple(
                        AnnotationType.valueOf(requestBody.getExamples()[i].getType()), // if null, random annotation type will be calculated
                        automatedTest, // pass the current object for further use/comparison
                        requestBody.getExamples()[i] // the current Example-Object inheriting Type and uniqueness properties
                );
                if (testDataObject == null) // becomes null if no ResultSet is available, then throw away this test-case // TODO: Do better by sorting out
                    return null;
                else {
                    automatedTest.setExampleData(testDataObject); // If successful, add example to the automatedTest
                }
            }
        } catch (RuntimeException e) {
            logger.error("Error: {}", e.getMessage());
            return null;
        }
        try {
            automatedTest.setPrompt(replacePromptPlaceholder(exampleCountAndTemplate.get(requestBody.getExamples().length), automatedTest));
        } catch (NullPointerException e) {
            logger.error("Error: {}", e.getMessage());
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
        else if (example.getUniqueComponent()) {
            ArrayList<String> usedComponentsInTest = fetchAllUsedComponents(automatedTest);
            ArrayList<String> componentList = new ArrayList<>(List.of(this.typeAndComponents.get(annotationType.name())));
            String component;
            try {
                do {
                    int rnd = this.random.nextInt(componentList.size());
                    component = componentList.get(rnd);
                    componentList.remove(rnd);
                } while (usedComponentsInTest.contains(component));
            } catch (Exception e) {
                throw new RuntimeException("There is no other unique and unused component for type " + annotationType.name());
            }
            return component;
        }

        throw new RuntimeException("Failure in selectComponent method");
    }

    public ArrayList<String> fetchAllUsedComponents(AutomatedTest automatedTest) {
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

    public String sendPrompt(String prompt, AutomatedTest automatedTest) throws IOException, URISyntaxException {
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
    public String gptExplanation(AutomatedTestRequestBody requestBody) throws Exception {

        int runs = requestBody.getRuns();
        int examples = requestBody.getExamples().length;
        JSONArray jsonArray = new JSONArray();
        JSONObject jsonObject = new JSONObject();
        AutomatedTest automatedTestObject;

        for (int i = 0; i < runs; i++) {
            logger.info("Run number {}", i);
            try {
                automatedTestObject = setUpTest(requestBody);
            } catch (IOException e) {
                automatedTestObject = null;
                //wait(1000);
            }
            if (automatedTestObject != null) {
                // send prompt to openai-chatgpt
                try {
                    String gptExplanation = sendPrompt(automatedTestObject.getPrompt(), automatedTestObject);
                    automatedTestObject.setGptExplanation(gptExplanation);
                    jsonArray.put(new JSONObject(automatedTestObject));
                } catch (IOException e) {
                    logger.error("{}", e.getMessage());
                    jsonObject.put("explanations", jsonArray);
                    writeObjectToFile(jsonObject);
                }
            }
        }

        jsonObject.put("explanations", jsonArray);
        writeObjectToFile(jsonObject);

        return jsonObject.toString();
    }

    public String testWithoutGptExplanation(AutomatedTestRequestBody requestBody) throws Exception {
        int runs = requestBody.getRuns();
        JSONArray jsonArray = new JSONArray();
        JSONObject jsonObject = new JSONObject();
        AutomatedTest automatedTestObject;
        for (int i = 0; i < runs; i++) {
            logger.info("Run number {}", i);
            try {
                automatedTestObject = setUpTest(requestBody);
            } catch (IOException e) { // 500 error from Qanary pipeline
                automatedTestObject = null;
                //wait(1000);
            }
            if (automatedTestObject != null) {
                try {
                    jsonArray.put(new JSONObject(automatedTestObject));
                    // Token calculation
                    Encoding encoding = encodingRegistry.getEncodingForModel(ModelType.GPT_3_5_TURBO);
                    int tokens = encoding.countTokens(automatedTestObject.getPrompt());
                    logger.info("Calculated Token: {}", tokens);
                    //
                    logger.info("Completed run number {}", i + 1);
                } catch (Exception e) {
                    logger.error("Error while processing gpt explanation, skipped.");
                }
            } else
                logger.info("Skipped run {} due to null-ResultSet", i + 1);
        }
        jsonObject.put("explanations", jsonArray);
        writeObjectToFile(jsonObject);

        return jsonObject.toString();
    }

    public void writeObjectToFile(JSONObject jsonObject) throws IOException {
        FileWriter fileWriter = new FileWriter("output.json");
        fileWriter.write(jsonObject.toString());
        fileWriter.flush();
        fileWriter.close();
    }

}
