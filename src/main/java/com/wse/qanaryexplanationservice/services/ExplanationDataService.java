package com.wse.qanaryexplanationservice.services;

import com.wse.qanaryexplanationservice.helper.dtos.ExperimentSelectionDTO;
import com.wse.qanaryexplanationservice.helper.pojos.AutomatedTests.automatedTestingObject.AutomatedTest;
import com.wse.qanaryexplanationservice.helper.pojos.AutomatedTests.automatedTestingObject.TestDataObject;
import org.apache.jena.rdf.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import virtuoso.jena.driver.VirtModel;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ExplanationDataService {

    private final Logger logger = LoggerFactory.getLogger(ExplanationDataService.class);
    private final Model model;
    private final Property prompt;
    private final Property gptExplanation;
    private final Property testData;
    private final Property exampleData;
    private final Property rdfType;
    private final Property usedComponent;
    private final Property usedComponentAsNum;
    private final Property dataset;
    private final Property graphId;
    private final Property explanation;
    private final Property questionId;
    private final Property question;
    private final Property hasScore;
    private final Property numberOfAnnotations;
    private final Property qualityAnnotations;
    private final Property qualityPrefix;
    private final String QANARY_VOCAB = "http://www.wdaqua.eu/qa#";
    private final String RDFS = "http://www.w3.org/2000/01/rdf-schema#";
    private final String RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    private final String AEX_VOCAB = "http://www.semanticweb.org/dennisschiese/ontologies/2023/9/automatedExplanation#";
    @Value("${virtuoso.triplestore.endpoint}")
    private String VIRTUOSO_TRIPLESTORE_ENDPOINT;
    @Value("${virtuoso.triplestore.username}")
    private String VIRTUOSO_TRIPLESTORE_USERNAME;
    @Value("${virtuoso.triplestore.password}")
    private String VIRTUOSO_TRIPLESTORE_PASSWORD;

    public ExplanationDataService() {
        model = ModelFactory.createDefaultModel();

        // Init Prefixes
        model.setNsPrefix("aex", AEX_VOCAB);
        model.setNsPrefix("rdfs", RDFS);
        model.setNsPrefix("qa", QANARY_VOCAB);
        model.setNsPrefix("rdf", RDF);

        //Init Properties
        rdfType = model.createProperty(RDF + "type");
        usedComponent = model.createProperty(AEX_VOCAB + "usedComponent");
        usedComponentAsNum = model.createProperty(AEX_VOCAB + "usedComponentAsNum");
        dataset = model.createProperty(AEX_VOCAB + "dataset");
        graphId = model.createProperty(AEX_VOCAB + "graphId");
        explanation = model.createProperty(AEX_VOCAB + "explanation");
        questionId = model.createProperty(AEX_VOCAB + "questionId");
        question = model.createProperty(AEX_VOCAB + "question");
        prompt = model.createProperty(AEX_VOCAB + "prompt");
        gptExplanation = model.createProperty(AEX_VOCAB + "gptExplanation");
        testData = model.createProperty(AEX_VOCAB + "testData");
        exampleData = model.createProperty(AEX_VOCAB + "exampleData");
        hasScore = model.createProperty(AEX_VOCAB + "hasScore");
        numberOfAnnotations = model.createProperty(AEX_VOCAB + "numberOfAnnotations");
        qualityAnnotations = model.createProperty(AEX_VOCAB + "qualityAnnotations");
        qualityPrefix = model.createProperty(AEX_VOCAB + "qualityPrefix");
    }

    /**
     * Inserts a AutomatedTest object to the triplestore with a unique graphID
     *
     * @param automatedTest Object which will be parsed to a model and inserted to the triplestore
     */
    public void insertDataset(AutomatedTest automatedTest, boolean isGptCall) {

        String uuid = UUID.randomUUID().toString();
        List<Statement> statementList = createSpecificStatementList(automatedTest, uuid, isGptCall);
        model.add(statementList);
        VirtModel virtModel = VirtModel.openDatabaseModel("urn:aex:" + uuid, VIRTUOSO_TRIPLESTORE_ENDPOINT, VIRTUOSO_TRIPLESTORE_USERNAME, VIRTUOSO_TRIPLESTORE_PASSWORD);
        virtModel.add(model); // TODO: Auslagern des VirtModel Aufrufs
        virtModel.close();
        model.remove(statementList); //  Clear experiment specific Statements
    }

    /**
     * Creates statements (= triples)
     *
     * @param automatedTest Object including all information about the test(s)
     * @param uuid          graph identifier
     * @return List of Statements (= triples)
     */
    public List<Statement> createSpecificStatementList(AutomatedTest automatedTest, String uuid, boolean isGptCall) {
        List<Statement> statementList = new ArrayList<>();
        Resource experimentId = model.createResource(uuid);
        logger.info("Experiment ID: {}", experimentId);

        statementList.add(ResourceFactory.createStatement(experimentId, prompt, ResourceFactory.createPlainLiteral(automatedTest.getPrompt())));
        if (isGptCall)
            statementList.add(ResourceFactory.createStatement(experimentId, gptExplanation, ResourceFactory.createPlainLiteral(automatedTest.getGptExplanation())));
        statementList.add(ResourceFactory.createStatement(experimentId, testData, setUpTestObject(automatedTest.getTestData())));
        statementList.add(ResourceFactory.createStatement(experimentId, exampleData, setupExampleData(automatedTest.getExampleData())));

        return statementList;
    }

    /**
     * Creates a resource and adds all required triples to describe the resource (= subject)
     *
     * @param testDataObject Concrete subject (either the test or example)
     * @return Resource (= subject)
     */
    public Resource setUpTestObject(TestDataObject testDataObject) {

        Resource resource = model.createResource();

        // Add items to object
        resource.addProperty(rdfType, ResourceFactory.createProperty(QANARY_VOCAB + testDataObject.getAnnotationType().name()));
        resource.addProperty(usedComponent, ResourceFactory.createPlainLiteral(testDataObject.getUsedComponent().getPrefixedComponentName()));
        resource.addProperty(usedComponentAsNum, ResourceFactory.createPlainLiteral(String.valueOf(testDataObject.getComponentNumber())));
        resource.addProperty(dataset, ResourceFactory.createPlainLiteral(testDataObject.getDataSet()));
        resource.addProperty(graphId, ResourceFactory.createPlainLiteral(testDataObject.getGraphID()));
        resource.addProperty(explanation, ResourceFactory.createPlainLiteral(testDataObject.getExplanation()));
        resource.addProperty(questionId, ResourceFactory.createPlainLiteral(testDataObject.getQuestionID()));
        resource.addProperty(question, ResourceFactory.createPlainLiteral(testDataObject.getQuestion()));

        return resource;
    }

    /**
     * Method to create the list of example objects as a rdf-sequence
     *
     * @param examples List of examples of type TestDataObject
     */
    public Seq setupExampleData(ArrayList<TestDataObject> examples) {
        Seq seq = model.createSeq();
        for (int i = 0; i < examples.size(); i++) {
            seq.add(i + 1, setUpTestObject(examples.get(i)));
        }
        return seq;
    }


    // TODO: StringBuilder not the best solution here, probably refactor existing methods to use the model approach
    public String createSequenceForExperimentSelection(ExperimentSelectionDTO experimentSelectionDTO) {
        String[] list = experimentSelectionDTO.getAnnotationTypes();
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < experimentSelectionDTO.getShots(); i++) {
            stringBuilder.append("rdfs:_" + (i + 1) + "[ rdf:type qa:" + list[i] + "]");
            if (i + 1 != experimentSelectionDTO.getShots()) {
                stringBuilder.append(";");
            }
        }
        return stringBuilder.toString();
    }

}
