package com.wse.qanaryexplanationservice.services;

import com.wse.qanaryexplanationservice.pojos.AutomatedTests.automatedTestingObject.automatedTestingObject.AutomatedTest;
import com.wse.qanaryexplanationservice.pojos.AutomatedTests.automatedTestingObject.automatedTestingObject.TestDataObject;
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
    private final Property annotationType;
    private final Property usedComponent;
    private final Property usedComponentAsNum;
    private final Property dataset;
    private final Property graphId;
    private final Property explanation;
    private final Property questionId;
    private final Property question;

    @Value("${virtuoso.triplestore.endpoint}")
    private String VIRTUOSO_TRIPLESTORE_ENDPOINT;
    @Value("${virtuoso.triplestore.username}")
    private String VIRTUOSO_TRIPLESTORE_USERNAME;
    @Value("${virtuoso.triplestore.password}")
    private String VIRTUOSO_TRIPLESTORE_PASSWORD;


    public ExplanationDataService() {
        model = ModelFactory.createDefaultModel();

        //Init Properties
        annotationType = model.createProperty("http://www.semanticweb.org/dennisschiese/ontologies/2023/9/automatedExplanation#annotationType");
        usedComponent = model.createProperty("http://www.semanticweb.org/dennisschiese/ontologies/2023/9/automatedExplanation#usedComponent");
        usedComponentAsNum = model.createProperty("http://www.semanticweb.org/dennisschiese/ontologies/2023/9/automatedExplanation#usedComponentAsNum");
        dataset = model.createProperty("http://www.semanticweb.org/dennisschiese/ontologies/2023/9/automatedExplanation#dataset");
        graphId = model.createProperty("http://www.semanticweb.org/dennisschiese/ontologies/2023/9/automatedExplanation#graphId");
        explanation = model.createProperty("http://www.semanticweb.org/dennisschiese/ontologies/2023/9/automatedExplanation#explanation");
        questionId = model.createProperty("http://www.semanticweb.org/dennisschiese/ontologies/2023/9/automatedExplanation#questionId");
        question = model.createProperty("http://www.semanticweb.org/dennisschiese/ontologies/2023/9/automatedExplanation#question");
        model.setNsPrefix("aex", "http://www.semanticweb.org/dennisschiese/ontologies/2023/9/automatedExplanation#");
        model.setNsPrefix("rdfs", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        prompt = model.createProperty("http://www.semanticweb.org/dennisschiese/ontologies/2023/9/automatedExplanation#prompt");
        gptExplanation = model.createProperty("http://www.semanticweb.org/dennisschiese/ontologies/2023/9/automatedExplanation#gptExplanation");
        testData = model.createProperty("http://www.semanticweb.org/dennisschiese/ontologies/2023/9/automatedExplanation#testData");
        exampleData = model.createProperty("http://www.semanticweb.org/dennisschiese/ontologies/2023/9/automatedExplanation#exampleData");
    }

    public void insertDataset(AutomatedTest automatedTest) {

        String uuid = UUID.randomUUID().toString();
        List<Statement> statementList = createSpecificStatementList(automatedTest, uuid);
        model.add(statementList);
        // TODO: Move connection details (e.g. application.properties / ...)
        VirtModel virtModel = VirtModel.openDatabaseModel("urn:aex:" + uuid, VIRTUOSO_TRIPLESTORE_ENDPOINT, VIRTUOSO_TRIPLESTORE_USERNAME, VIRTUOSO_TRIPLESTORE_PASSWORD);
        virtModel.add(model);
        virtModel.close();

        //  Clear experiment specific Statements
        model.remove(statementList);
    }

    public List<Statement> createSpecificStatementList(AutomatedTest automatedTest, String uuid) {
        List<Statement> statementList = new ArrayList<>();
        Resource experimentId = model.createResource(uuid);
        logger.info("Experiment ID: {}", experimentId);

        statementList.add(ResourceFactory.createStatement(experimentId, prompt, ResourceFactory.createPlainLiteral(automatedTest.getPrompt())));
        statementList.add(ResourceFactory.createStatement(experimentId, gptExplanation, ResourceFactory.createPlainLiteral(automatedTest.getGptExplanation())));
        statementList.add(ResourceFactory.createStatement(experimentId, testData, setUpTestObject(automatedTest.getTestData())));
        statementList.add(ResourceFactory.createStatement(experimentId, exampleData, setupExampleData(automatedTest.getExampleData())));

        return statementList;
    }

    public Resource setUpTestObject(TestDataObject testDataObject) {

        Resource resource = model.createResource();

        // Add items to object
        resource.addProperty(annotationType, ResourceFactory.createPlainLiteral(testDataObject.getAnnotationType().name()));
        resource.addProperty(usedComponent, ResourceFactory.createPlainLiteral(testDataObject.getUsedComponent()));
        resource.addProperty(usedComponentAsNum, ResourceFactory.createPlainLiteral(String.valueOf(testDataObject.getComponentNumber())));
        resource.addProperty(dataset, ResourceFactory.createPlainLiteral(testDataObject.getDataSet()));
        resource.addProperty(graphId, ResourceFactory.createPlainLiteral(testDataObject.getGraphID()));
        resource.addProperty(explanation, ResourceFactory.createPlainLiteral(testDataObject.getExplanation()));
        resource.addProperty(questionId, ResourceFactory.createPlainLiteral(testDataObject.getQuestionID()));
        resource.addProperty(question, ResourceFactory.createPlainLiteral(testDataObject.getQuestion()));

        return resource;
    }

    public Seq setupExampleData(ArrayList<TestDataObject> examples) {
        Seq seq = model.createSeq();
        for (int i = 0; i < examples.size(); i++) {
            seq.add(i + 1, setUpTestObject(examples.get(i)));
        }
        return seq;
    }

}
