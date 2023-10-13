package com.wse.qanaryexplanationservice.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wse.qanaryexplanationservice.pojos.AutomatedTestRequestBody;
import com.wse.qanaryexplanationservice.pojos.Example;
import com.wse.qanaryexplanationservice.pojos.QanaryRequestObject;
import com.wse.qanaryexplanationservice.pojos.automatedTestingObject.AnnotationType;
import com.wse.qanaryexplanationservice.pojos.automatedTestingObject.AutomatedTest;
import com.wse.qanaryexplanationservice.pojos.automatedTestingObject.QanaryResponseObject;
import com.wse.qanaryexplanationservice.pojos.automatedTestingObject.TestDataObject;
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
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Random random = new Random();
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
    private Map<String, String[]> typeAndComponents = new HashMap<>() {{ // TODO: Replace placeholder
        // put(AnnotationType.annotationofinstance.name(), new String[]{"NED-DBpediaSpotlight", "DandelionNED",});
        put(AnnotationType.annotationofspotinstance.name(), new String[]{"MeaningCloud", "TextRazor", "NER-DBpediaSpotlight", "DandelionNER"});
        // put(AnnotationType.annotationofanswerjson.name(), new String[]{"urn:qanary:SparqlExecuter"});
        // put(AnnotationType.annotationofanswersparql.name(), new String[]{"Monolitic", "QB-SimpleRealNameOfSuperHero"});
        // put(AnnotationType.annotationofquestionlanguage.name(), new String[]{"LD-Shuyo"});
        // put(AnnotationType.annotationofquestiontranslation.name(), new String[]{"mno", "pqr"});
        // put(AnnotationType.annotationofrelation.name(), new String[]{"FalconRELcomponent-dbpedia"});
        // put(AnnotationType.annotationofclass.name(), new String[]{"DiambiguationClass"});
    }};

    // Dependency map for annotation types
    private Map<AnnotationType, AnnotationType[]> dependencyMapForAnnotationTypes = new TreeMap<>() {{
        put(AnnotationType.annotationofinstance, null);
        put(AnnotationType.annotationofrelation, null);
        put(AnnotationType.annotationofspotinstance, null);
        put(AnnotationType.annotationofquestiontranslation, null);
        put(AnnotationType.annotationofquestionlanguage, null);
        put(AnnotationType.annotationofclass, new AnnotationType[]{AnnotationType.annotationofquestionlanguage});
        put(AnnotationType.annotationofanswersparql, new AnnotationType[]{
                AnnotationType.annotationofclass,
                AnnotationType.annotationofinstance,
                AnnotationType.annotationofrelation,
                AnnotationType.annotationofspotinstance,
                AnnotationType.annotationofquestiontranslation
        });
        put(AnnotationType.annotationofanswerjson, new AnnotationType[]{AnnotationType.annotationofanswersparql});
    }};

    @Autowired
    private AutomatedTestingRepository automatedTestingRepository;
    private Logger logger = LoggerFactory.getLogger(AutomatedTestingService.class);
    @Autowired
    private ExplanationService explanationService;
    // stores the correct template for different x-shot approaches
    private Map<Integer, String> exampleCountAndTemplate = new HashMap<>() {{
        put(1, "/testtemplates/oneshot");
        put(2, "/testtemplates/twoshot");
        put(3, "/testtemplates/threeshot");
    }};

    // TODO: check if passed annotationType exists, otherwise reject request
    public boolean validatePassedType(String passedType) {
        return false;
    }

    public AnnotationType selectRandomAnnotationType() {
        AnnotationType[] list = AnnotationType.values();
        // return list[random.nextInt(list.length)];

        return AnnotationType.annotationofspotinstance;
        //TODO: insert components to map
        // return AnnotationType.annotationofinstance;
    }

    /**
     * TODO: Abstraction for return type // TODO: rename method
     * Selects a random question as well as a random component for a given annotation-type
     * All in all that will result in a triple containing the type,question,component
     */
    public TestDataObject selectTestingTriple(AnnotationType annotationType, AutomatedTest automatedTest, Example example) throws Exception { // TODO: maybe parallelization possible? Threads?

        TestDataObject data;
        AnnotationType annotationType_ = annotationType;

        if (annotationType_ == null)
            annotationType_ = selectRandomAnnotationType();

        //TODO: separate Testing and example objects!?

        String selectedComponent = selectComponent(annotationType_, automatedTest, example);
        int selectedComponentAsInt = Arrays.stream(this.typeAndComponents.get(annotationType_.name())).toList().indexOf(selectedComponent);

        Integer random = this.random.nextInt(394); // Number of question in dataset-1 // TODO: shift const value
        String question = getRandomQuestion(random);

        List<String> randomComponents = selectRandomComponents(getDependencies(annotationType_));
        randomComponents.add(selectedComponent);
        QanaryResponseObject response = executeQanaryPipeline(question, selectedComponent, randomComponents);

        String graphURI = response.getOutGraph();
        String questionID = response.getQuestion().replace("http://195.90.200.248:8090/question/stored-question__text_", "questionID:");

        String dataset = createDataset(selectedComponent, question, graphURI);
        if (dataset == null)
            return null;

        String explanation = getExplanation(graphURI, selectedComponent);

        // TODO: see todo below, additionally random picking
        // Integer selectedQuestionAsInt = this.qadoDatasetRepository ...
        // String selectedQuestion = this.qadoDatasetRepository.getDataset();   // TODO: How to work with that data since it's a huge dataset for parsing w/ JsonNode(s)
        // TODO: Caching? Maybe too large when it comes to thousands of datasets?
        return new TestDataObject(annotationType_, annotationType_.ordinal(), selectedComponent, question, explanation, dataset, graphURI, questionID, random, selectedComponentAsInt);
    }

    public ArrayList<AnnotationType> getDependencies(AnnotationType annotationType) {

        logger.info("AnnotationOfInstance: {}", annotationType);
        try {
            List<AnnotationType> list = Arrays.asList(dependencyMapForAnnotationTypes.get(annotationType));
            HashSet<AnnotationType> hashSet = new HashSet<>(list);

            for (AnnotationType annType : hashSet
            ) {
                hashSet.addAll(resolveRecursiveDependencies(annType));
            }
//            return hashSet.stream().toList();
            ArrayList<AnnotationType> resolvedDependenciesList = new ArrayList<>(hashSet.stream().toList());
            logger.info("Resolved dependencies for component {}: {}", annotationType, resolvedDependenciesList);
            return resolvedDependenciesList;
        } catch (NullPointerException e) {
            logger.info("No dependencies need to be resolved for type {}", annotationType);
            return new ArrayList<>() {{
                add(annotationType);
            }};
        }

    }

    public List<String> getcomps(AnnotationType annotationType) {
        return selectRandomComponents(getDependencies(annotationType));
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

    public List<String> selectRandomComponents(ArrayList<AnnotationType> list) {
        Collections.sort(list);
        List<String> componentList = new ArrayList<>();

        for (AnnotationType annType : list
        ) {
            logger.info("Current anntype: {}", annType);
            String[] componentsList = this.typeAndComponents.get(annType.name());
            Integer selectedComponentAsInt = random.nextInt(componentsList.length);
            logger.info("Random chosen component: {}", componentsList[selectedComponentAsInt]);
            componentList.add(componentsList[selectedComponentAsInt]);
        }

        logger.info("Random selected components: {}", componentList);
        return componentList;
    }

    /**
     * Creates the explanation and selects the english'
     *
     * @return
     */
    public String getExplanation(String graphURI, String componentURI) throws IOException, IndexOutOfBoundsException {

        Model explanationModel = explanationService.createModel(graphURI, "urn:qanary:" + componentURI);
        explanationModel.setNsPrefix("explanations", EXPLANATION_NAMESPACE);
        Property hasExplanationForCreatedDataProperty = explanationModel.createProperty(EXPLANATION_NAMESPACE, "hasExplanationForCreatedData");
        Statement statement = explanationModel.getRequiredProperty(ResourceFactory.createResource("urn:qanary:" + componentURI), hasExplanationForCreatedDataProperty, "en");
        logger.info("Statement: {}", statement.getString());

        return statement.getString();
    }

    /**
     * Should return a raw question from the QADO dataset
     *
     * @return
     */
    public String getRandomQuestion(Integer questionNumber) throws IOException {

        QuerySolutionMap querySolutionMap = new QuerySolutionMap();
        querySolutionMap.add("id", ResourceFactory.createTypedLiteral(questionNumber.toString(), XSDDatatype.XSDnonNegativeInteger));
        String query = QanaryTripleStoreConnector.readFileFromResourcesWithMap(QUESTION_QUERY, querySolutionMap);

        ResultSet resultSet = this.automatedTestingRepository.takeRandomQuestion(query);

        return resultSet.next().get("hasQuestion").asLiteral().getString();
    }

    /**
     * Should return a graphURI
     * TODO: Solve problem with String[] as param
     *
     * @return
     */
    public QanaryResponseObject executeQanaryPipeline(String question, String selectedComponent, List<String> randomComponents) throws IOException { // TODO: Respect component-dependency tree !!!
        logger.info("Component QPipeline: {}", selectedComponent);
        QanaryRequestObject qanaryRequestObject = new QanaryRequestObject(question, null, null, randomComponents);
        // executes a qanary pipeline and take the graphID from it + questionURI since the question can be fetched via <questionURI>/raw
        return automatedTestingRepository.executeQanaryPipeline(qanaryRequestObject);
    }

    /**
     * TODO: GER -> EN
     * TODO: Decide if Int or String is provided as componentURI
     * Führt die Qanary pipeline aus und fragt mit der graphID den SPARQL Endpunkt ab um das Datenset zu erhalten
     * Weiterhin wird das datenset angepasst (bspw. das Hinzufügen der Punkte am Ende)
     */
    public String createDataset(String componentURI, String question, String graphURI) throws Exception {

        ResultSet triples = fetchTriples(graphURI, componentURI);
        if (triples == null)
            return null;

        // TODO: triples must follow the pattern "<..> ... <...> ."
        // TODO: with prefixes included
        StringBuilder dataSet = new StringBuilder();
        while (triples.hasNext()) {
            QuerySolution querySolution = triples.next();
            dataSet.append(querySolution.getResource("s")).append(" ").append(querySolution.getResource("p")).append(" ").append(querySolution.get("o")).append(" .\n");
        }
        String dataSetAsString = dataSet.toString();
        logger.info("Dataset pre substituted: {}", dataSetAsString);

        for (Map.Entry<String, String> entry : prefixes.entrySet()) {
            dataSetAsString = dataSetAsString.replace(entry.getKey(), entry.getValue());
        }

        logger.info("Dataset after being substituted: {}", dataSetAsString);

        // TODO: Is there a solution for prefix resolving??? Otherwise map and replace...

        return dataSetAsString;

    }

    public ResultSet fetchTriples(String graphURI, String componentURI) throws Exception {
        QuerySolutionMap bindingsForQuery = new QuerySolutionMap();
        bindingsForQuery.add("graphURI", ResourceFactory.createResource(graphURI));
        bindingsForQuery.add("componentURI", ResourceFactory.createResource("urn:qanary:" + componentURI));
        String query = QanaryTripleStoreConnector.readFileFromResourcesWithMap(DATASET_QUERY, bindingsForQuery);

        ResultSet resultSet = automatedTestingRepository.executeSparqlQueryWithResultSet(query);
        if (!resultSet.hasNext())
            return null;
        else
            return resultSet;
    }

    public String selectRandomComponentFromGivenList() {
        return "";
    }

    public AutomatedTest setUpTest(AutomatedTestRequestBody requestBody) throws Exception {
        logger.info("Request Body properties: {}", requestBody.toString());
        AutomatedTest automatedTest = new AutomatedTest();

        // Firstly: creates the test data
        // Secondly: iterate through the provided example-data and creates examples with either given type or null (== random type will be selected)
        try {
            automatedTest.setTestData(selectTestingTriple(AnnotationType.valueOf(requestBody.getTestingType()), null, null));

            TestDataObject testDataObject;
            for (int i = 0; i < requestBody.getExamples().length; i++) {
                testDataObject = selectTestingTriple(AnnotationType.valueOf(requestBody.getExamples()[i].getType()), automatedTest, requestBody.getExamples()[i]);
                if (testDataObject == null)
                    return null;
                else
                    automatedTest.setExampleData(testDataObject);
            }
        } catch (Exception e) {
            logger.error("{}", e.getMessage());
            return null;
        }
        try {
            automatedTest.setPrompt(replacePromptPlaceholder(exampleCountAndTemplate.get(requestBody.getExamples().length), automatedTest));
        } catch (NullPointerException e) {
            return null;
        }

        return automatedTest;
    }

    public String selectComponent(AnnotationType annotationType_, AutomatedTest automatedTest, Example example) {

        String[] componentsList = this.typeAndComponents.get(annotationType_.name());

        if (example == null) {
            int selectedComponentAsInt = random.nextInt(componentsList.length);
            return componentsList[selectedComponentAsInt]; //TODO: selectedComponentList for component dependency tree, see executeQanaryPipeline method
        } else if (example.getUniqueComponent()) {
            ArrayList<String> usedComponentsInTest = fetchAllUsedComponents(automatedTest);
            ArrayList<String> componentList = new ArrayList<>(List.of(this.typeAndComponents.get(annotationType_.name())));
            String component;
            try {
                do {
                    int rnd = this.random.nextInt(componentList.size());
                    logger.info("Random no: {}", rnd);
                    logger.info("Liste: {}", componentList);
                    component = componentList.get(rnd);
                    componentList.remove(rnd);
                } while (usedComponentsInTest.contains(component));
            } catch (Exception e) {
                throw new RuntimeException("There is no other unique and unused component for type " + annotationType_.name());
            }
            return component;
        } else {
            // TODO: !!! Is example-level definition of uniqueness relevant? Handling could be a lot easier somewhere else (Errors and so on)
            int selectedComponentAsInt = random.nextInt(componentsList.length);
            return componentsList[selectedComponentAsInt]; //TODO: selectedComponentList for component dependency tree, see executeQanaryPipeline method
        }

        /**
         ArrayList<String> usedComponentsInTest = fetchAllUsedComponents(automatedTest);
         String[] componentList = this.typeAndComponents.get(annotationType_.name());
         for (int i = 0; i < componentList.length; i++) {
         if (usedComponentsInTest.contains(componentList[i]))
         continue;
         else
         return componentsList[i];
         }
         */
    }

    public ArrayList<String> fetchAllUsedComponents(AutomatedTest automatedTest) {
        ArrayList<String> list = new ArrayList<>();
        list.add(automatedTest.getTestData().getUsedComponent());
        ArrayList<TestDataObject> listExamples = automatedTest.getExampleData();

        for (TestDataObject item : listExamples) {
            list.add(item.getUsedComponent());
        }

        return list;

    }


    public String replacePromptPlaceholder(String emptyPromptPath, AutomatedTest automatedTest) throws IOException {

        // Replace test object
        TestDataObject obj = automatedTest.getTestData();
        String prompt = getStringFromFile(emptyPromptPath);
        prompt = prompt.replace("<TASK_RDF_DATA_TEST>", obj.getDataSet());

        // Replace examples
        int i = 1;
        for (TestDataObject exampleObject : automatedTest.getExampleData()
        ) {
            prompt = prompt.replace("<QUESTION_ID_EXAMPLE_" + i + ">", obj.getQuestionID()) // TODO: Question != questionID
                    .replace("<EXAMPLE_EXPLANATION_" + i + ">", obj.getExplanation())
                    .replace("<EXAMPLE_RDF_DATA_" + i + ">", obj.getDataSet());
            i += 1;
        }

        return prompt;
    }

    public String sendPrompt(String prompt, AutomatedTest automatedTest, int examples) throws IOException, URISyntaxException {
        return automatedTestingRepository.sendGptPrompt(prompt, examples);
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

        for (int i = 0; i < runs; i++) {
            AutomatedTest automatedTestObject = setUpTest(requestBody);

            if (automatedTestObject != null) {
                // send prompt to openai-chatgpt
                try {
                    String gptExplanation = sendPrompt(automatedTestObject.getPrompt(), automatedTestObject, examples);
                    automatedTestObject.setGptExplanation(gptExplanation);
                    jsonArray.put(new JSONObject(automatedTestObject));
                    logger.info("Current AutomatedObjectData: {}", jsonArray);
                } catch (IOException e) {
                    logger.error("Server side error, token max reached.");
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

        for (int i = 0; i < runs; i++) {
            AutomatedTest automatedTestObject = setUpTest(requestBody);
            if (automatedTestObject != null) {
                try {
                    jsonArray.put(new JSONObject(automatedTestObject));
                } catch (Exception e) {
                    logger.error("Error while processing gpt explanation, skipped.");
                }
            }
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
