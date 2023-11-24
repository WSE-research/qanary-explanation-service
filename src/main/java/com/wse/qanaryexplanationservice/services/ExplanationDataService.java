package com.wse.qanaryexplanationservice.services;

import com.wse.qanaryexplanationservice.pojos.AutomatedTests.automatedTestingObject.automatedTestingObject.AutomatedTest;
import com.wse.qanaryexplanationservice.pojos.AutomatedTests.automatedTestingObject.automatedTestingObject.TestDataObject;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;

@Service
public class ExplanationDataService {

    private final Logger logger = LoggerFactory.getLogger(ExplanationDataService.class);
    private final String AUTOMATED_TEST_INSERT_QUERY = "/queries/insertAutomatedTest.rq";
    private final String TEST_DATA_OBJECT_RQ = "/queries/testDataObject.rq";
    public ExplanationDataService() {}

    public void getDatasets() {

    }

    public void insertDataset(AutomatedTest automatedTest) throws IOException {
        logger.info("Inserting automated Test to triplestore");

        String query = buildInsertQuery(automatedTest);
        logger.info("Created Query: {}", query);
    }

    public String buildInsertQuery(AutomatedTest automatedTest) throws IOException {

        QuerySolutionMap bindingForInsertQuery = new QuerySolutionMap();
        bindingForInsertQuery.add("prompt", ResourceFactory.createPlainLiteral(automatedTest.getPrompt()));
        bindingForInsertQuery.add("gptExplanation", ResourceFactory.createPlainLiteral(automatedTest.getGptExplanation()));
        bindingForInsertQuery.add("testData", ResourceFactory.createPlainLiteral(setupTestdataObject(automatedTest.getTestData())));
        ArrayList<TestDataObject> exampleData = automatedTest.getExampleData();
       // bindingForInsertQuery.add("exampleData", ResourceFactory.createPlainLiteral(setupExampleData(automatedTest.getExampleData())));

        return QanaryTripleStoreConnector.readFileFromResourcesWithMap(AUTOMATED_TEST_INSERT_QUERY,bindingForInsertQuery);
    }

    public String setupTestdataObject(TestDataObject testDataObject) throws IOException {

        // Erstellen Sie ein neues RDF-Modell
        Model model = ModelFactory.createDefaultModel();
        model.setNsPrefix("aex", "http://www.semanticweb.org/dennisschiese/ontologies/2023/9/automatedExplanation#");

        // Properties
        Property annotationType = model.createProperty("http://www.semanticweb.org/dennisschiese/ontologies/2023/9/automatedExplanation#annotationType");
        Property usedComponent = model.createProperty("http://www.semanticweb.org/dennisschiese/ontologies/2023/9/automatedExplanation#usedComponent");
        Property usedComponentAsNum = model.createProperty("http://www.semanticweb.org/dennisschiese/ontologies/2023/9/automatedExplanation#usedComponentAsNum");
        Property dataset = model.createProperty("http://www.semanticweb.org/dennisschiese/ontologies/2023/9/automatedExplanation#dataset");
        Property graphId = model.createProperty("http://www.semanticweb.org/dennisschiese/ontologies/2023/9/automatedExplanation#graphId");
        Property explanation = model.createProperty("http://www.semanticweb.org/dennisschiese/ontologies/2023/9/automatedExplanation#explanation");
        Property questionId = model.createProperty("http://www.semanticweb.org/dennisschiese/ontologies/2023/9/automatedExplanation#questionId");
        Property question = model.createProperty("http://www.semanticweb.org/dennisschiese/ontologies/2023/9/automatedExplanation#question");

        // Erstellen Sie Ressourcen und Eigenschaften
        Resource resource = model.createResource();
        resource.addProperty(annotationType, ResourceFactory.createPlainLiteral(testDataObject.getAnnotationType().name()));
        resource.addProperty(usedComponent, ResourceFactory.createPlainLiteral(testDataObject.getUsedComponent()));
        resource.addProperty(usedComponentAsNum, ResourceFactory.createPlainLiteral(String.valueOf(testDataObject.getComponentNumber())));
        resource.addProperty(dataset, ResourceFactory.createPlainLiteral(testDataObject.getDataSet()));
        resource.addProperty(graphId, ResourceFactory.createPlainLiteral(testDataObject.getGraphID()));
        resource.addProperty(explanation, ResourceFactory.createPlainLiteral(testDataObject.getExplanation()));
        resource.addProperty(questionId, ResourceFactory.createPlainLiteral(testDataObject.getQuestionID()));
        resource.addProperty(question, ResourceFactory.createPlainLiteral(testDataObject.getQuestion()));

        // Ausgabe des RDF-Modells
        StringWriter stringWriter = new StringWriter();
        model.write(stringWriter, "Turtle");
        String rdfString = stringWriter.toString();
        logger.info("{}", rdfString);

        return rdfString.replace(".","");
    }

    public String setupExampleData(ArrayList<TestDataObject> examples) throws IOException {
        Model model = ModelFactory.createDefaultModel();
        model.setNsPrefix("rdfs", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");

        // Erzeuge eine Ressource für die Sequenz
        Resource sequence = model.createResource();

        for(int i = 0; i < examples.size(); i++) {
            addElementToSequence(model, sequence, i+1, setupTestdataObject(examples.get(i)));
        }

        StringWriter stringWriter = new StringWriter();
        model.write(stringWriter, "Turtle");
        String rdfString = stringWriter.toString();
        logger.info("{}", rdfString);

        return rdfString;
    }

    private static void addElementToSequence(Model model, Resource sequence, int index, String value) {
        Property rdfLi = model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#_" + index);
        Resource elementResource = model.createResource();
        model.add(sequence, rdfLi, value);
    }

}
