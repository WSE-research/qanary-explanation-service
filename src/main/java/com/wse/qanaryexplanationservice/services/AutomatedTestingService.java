package com.wse.qanaryexplanationservice.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wse.qanaryexplanationservice.pojos.QanaryRequestObject;
import com.wse.qanaryexplanationservice.pojos.automatedTestingObject.AnnotationType;
import com.wse.qanaryexplanationservice.pojos.automatedTestingObject.AutomatedTest;
import com.wse.qanaryexplanationservice.pojos.automatedTestingObject.TestData;
import com.wse.qanaryexplanationservice.repositories.AutomatedTestingRepository;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class AutomatedTestingService {

    private final static String DATASET_QUERY = "/queries/evaluation_dataset_query.rq";
    private static final String EXPLANATION_NAMESPACE = "urn:qanary:explanations#";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Random random = new Random();
    private Map<String, String[]> typeAndComponents = new HashMap<>() {{
        put(AnnotationType.annotationofinstance.name(), new String[]{"NED-DBpediaSpotlight", "DandelionNED"});
        put(AnnotationType.annotationofspotinstance.name(), new String[]{});
        put(AnnotationType.annotationofanswerjson.name(), new String[]{});
        put(AnnotationType.annotationofanswersparql.name(), new String[]{});
        put(AnnotationType.annotationofquestionlanguage.name(), new String[]{});
        put(AnnotationType.annotationofquestiontranslation.name(), new String[]{});
        put(AnnotationType.annotationofrelation.name(), new String[]{});
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

    public void automatedTest(String requestBody) throws JsonProcessingException {

        // Create new Object for test
        AutomatedTest automatedTest = new AutomatedTest();

        // Create Object as output-template where properties are within this process // TODO:
        JsonNode bodyAsJsonNode = objectMapper.readTree(requestBody);

        // Set testData
        TestData testData = automatedTest.getTestData();
        testData.setAnnotationType(AnnotationType.valueOf(bodyAsJsonNode.get("annotationType").asText()));
        automatedTest.setTestData(testData);

        /*
         * REQUEST BODY CONTENT:
         * annotation type (to be tested)
         * amount of examples (=x-shot-approach)
         * (type of examples, not now)
         */

        // select correct template
        String gptTemplate = exampleCountAndTemplate.get(bodyAsJsonNode.get("exampleAmount"));

    }

    /**
     * Selects a random question as well as a random component for a given annotation-type
     * All in all that will result in a triple containing the type,question,component
     */
    public String selectTestingTriple(String annotationType) throws Exception { // TODO: maybe parallelization possible? Threads?

        // TODO: save the index or the concrete component? For triples a number might be better
        String[] componentsList = this.typeAndComponents.get(annotationType);
        Integer selectedComponentAsInt = random.nextInt(componentsList.length);
        String selectedComponent = componentsList[random.nextInt(componentsList.length)];

        // TODO: selectQuestion
        // TODO: save the concrete question instead of the explicit question ID
        String question = getRandomQuestion();

        // execute Qanary pipeline
        String graphURI = executeQanaryPipeline(question, selectedComponent);

        String dataset = createDataset(selectedComponent, question);

        String explanation = getExplanation(graphURI, selectedComponent);

        TestData testData = new TestData(
                AnnotationType.valueOf(annotationType), selectedComponent, question, explanation, dataset, graphURI
        );

        logger.info(testData.toString());
        // TODO: see todo below, additionally random picking
        // Integer selectedQuestionAsInt = this.qadoDatasetRepository ...
        // String selectedQuestion = this.qadoDatasetRepository.getDataset(); // TODO: How to work with that data since it's a huge dataset for parsing w/ JsonNode(s)

        return testData.toString();
    }

    /**
     * Creates the explanation and selects the english'
     *
     * @return
     */
    public String getExplanation(String graphURI, String componentURI) throws Exception {

        Model explanationModel = explanationService.createModel(graphURI, "urn:qanary:" + componentURI);
        explanationModel.setNsPrefix("explanations", EXPLANATION_NAMESPACE);
        Property hasExplanationForCreatedDataProperty = explanationModel.createProperty(EXPLANATION_NAMESPACE, "hasExplanationForCreatedData");
        Statement statement = explanationModel.getRequiredProperty(ResourceFactory.createResource("urn:qanary:" + componentURI), ResourceFactory.createProperty("explanations:hasExplanationForCreatedData"), "en");
        logger.info("Statement: {}", statement.getString());

        return statement.getString();
    }

    /**
     * Should return a raw question from the QADO dataset
     *
     * @return
     */
    public String getRandomQuestion() {
        return "What is the real name of Batman?";
    }

    /**
     * Should return a graphURI
     * TODO: Solve problem with String[] as param
     *
     * @return
     */
    public String executeQanaryPipeline(String question, String selectedComponent) throws IOException {
        logger.info("Component QPipeline: {}", selectedComponent);
        QanaryRequestObject qanaryRequestObject = new QanaryRequestObject(question, null, null, selectedComponent);
        // executes a qanary pipeline and take the graphID from it + questionURI since the question can be fetched via <questionURI>/raw
        return automatedTestingRepository.executeQanaryPipeline(qanaryRequestObject).get("outGraph").asText();
    }

    /**
     * TODO: GER -> EN
     * TODO: Decide if Int or String is provided as componentURI
     * Führt die Qanary pipeline aus und fragt mit der graphID den SPARQL Endpunkt ab um das Datenset zu erhalten
     * Weiterhin wird das datenset angepasst (bspw. das Hinzufügen der Punkte am Ende)
     */
    public String createDataset(String componentURI, String question) throws IOException {

        QanaryRequestObject qanaryRequestObject = new QanaryRequestObject(question, null, null, componentURI);
        // executes a qanary pipeline and take the graphID from it + questionURI since the question can be fetched via <questionURI>/raw
        String graphURI = automatedTestingRepository.executeQanaryPipeline(qanaryRequestObject).get("outGraph").asText();

        ResultSet triples = fetchTriples(graphURI, componentURI);

        // TODO: triples must follow the pattern "<..> ... <...> ."
        // TODO: with prefixes included
        StringBuilder dataSet = new StringBuilder();
        while (triples.hasNext()) {
            QuerySolution querySolution = triples.next();
            dataSet.append(querySolution.getResource("s")).append(" ").append(querySolution.getResource("p")).append(" ").append(querySolution.get("o")).append(" .\n");
        }
        // TODO: Is there a solution for prefix resolving??? Otherwise map and replace...

        return dataSet.toString();

    }

    public ResultSet fetchTriples(String graphURI, String componentURI) throws IOException {
        QuerySolutionMap bindingsForQuery = new QuerySolutionMap();
        bindingsForQuery.add("graphURI", ResourceFactory.createResource(graphURI));
        bindingsForQuery.add("componentURI", ResourceFactory.createResource("urn:qanary:" + componentURI));
        String query = QanaryTripleStoreConnector.readFileFromResourcesWithMap(DATASET_QUERY, bindingsForQuery);

        return automatedTestingRepository.executeSparqlQueryWithResultSet(query);
    }


}
