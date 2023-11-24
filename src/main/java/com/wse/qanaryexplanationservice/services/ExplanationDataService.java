package com.wse.qanaryexplanationservice.services;

import com.wse.qanaryexplanationservice.pojos.AutomatedTests.automatedTestingObject.automatedTestingObject.AutomatedTest;
import com.wse.qanaryexplanationservice.pojos.AutomatedTests.automatedTestingObject.automatedTestingObject.TestDataObject;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import virtuoso.jena.driver.VirtModel;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

@Service
public class ExplanationDataService {

    private final Logger logger = LoggerFactory.getLogger(ExplanationDataService.class);
    private final String AUTOMATED_TEST_INSERT_QUERY = "/queries/insertAutomatedTest.rq";
    private final String TEST_DATA_OBJECT_RQ = "/queries/testDataObject.rq";
    private Model model = ModelFactory.createDefaultModel();
    Property annotationType = model.createProperty("http://www.semanticweb.org/dennisschiese/ontologies/2023/9/automatedExplanation#annotationType");
    Property usedComponent = model.createProperty("http://www.semanticweb.org/dennisschiese/ontologies/2023/9/automatedExplanation#usedComponent");
    Property usedComponentAsNum = model.createProperty("http://www.semanticweb.org/dennisschiese/ontologies/2023/9/automatedExplanation#usedComponentAsNum");
    Property dataset = model.createProperty("http://www.semanticweb.org/dennisschiese/ontologies/2023/9/automatedExplanation#dataset");
    Property graphId = model.createProperty("http://www.semanticweb.org/dennisschiese/ontologies/2023/9/automatedExplanation#graphId");
    Property explanation = model.createProperty("http://www.semanticweb.org/dennisschiese/ontologies/2023/9/automatedExplanation#explanation");
    Property questionId = model.createProperty("http://www.semanticweb.org/dennisschiese/ontologies/2023/9/automatedExplanation#questionId");
    Property question = model.createProperty("http://www.semanticweb.org/dennisschiese/ontologies/2023/9/automatedExplanation#question");
    public ExplanationDataService() {}

    public void getDatasets() {

    }

    public void insertDataset(AutomatedTest automatedTest) throws IOException {
        buildInsertQuery(automatedTest);

        VirtModel virtModel = VirtModel.openDatabaseModel("urn:bulkload:general", "jdbc:virtuoso://localhost:1111", "dba", "dba");
        virtModel.add(model);
        virtModel.close();
        model.removeAll();
    }

    public void buildInsertQuery(AutomatedTest automatedTest) throws IOException {

        Model model = createInsertModel(automatedTest);

        StringWriter stringWriter = new StringWriter();
        model.write(stringWriter, "Turtle");
        logger.info("Created Query: {}", stringWriter);
    }

    // TODO: Hinterlege Model irgendwo und importieren
    public Model createInsertModel(AutomatedTest automatedTest) throws IOException {
        model.setNsPrefix("aex", "http://www.semanticweb.org/dennisschiese/ontologies/2023/9/automatedExplanation#");
        model.setNsPrefix("rdfs", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");

        Resource experimentId = model.createResource(UUID.randomUUID().toString());

        Property prompt = model.createProperty("http://www.semanticweb.org/dennisschiese/ontologies/2023/9/automatedExplanation#prompt");
        Property gptExplanation = model.createProperty("http://www.semanticweb.org/dennisschiese/ontologies/2023/9/automatedExplanation#gptExplanation");
        Property testData = model.createProperty("http://www.semanticweb.org/dennisschiese/ontologies/2023/9/automatedExplanation#testData");
        Property exampleData = model.createProperty("http://www.semanticweb.org/dennisschiese/ontologies/2023/9/automatedExplanation#exampleData");

        experimentId.addProperty(prompt, ResourceFactory.createPlainLiteral("REPLACEPROMPT"));
        experimentId.addProperty(gptExplanation, ResourceFactory.createPlainLiteral("REPLACEEXPLANATION"));
        experimentId.addProperty(testData,setUpTestObject(automatedTest.getTestData()));
        experimentId.addProperty(exampleData, setupExampleData(automatedTest.getExampleData()));

        return model;
    }

    public Resource setUpTestObject(TestDataObject testDataObject) throws IOException {

        Resource resource = model.createResource();

        // Erstellen Sie Ressourcen und Eigenschaften
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

    public RDFNode setupExampleData(ArrayList<TestDataObject> examples) throws IOException {

        Seq seq = model.createSeq();
        for(int i = 0; i < examples.size(); i++) {
            seq.add(i+1, setUpTestObject(examples.get(i)));
        }

        return seq;
    }

    public static String removeFirstTwoLines(String input) {
        // Split the input string into lines
        String[] lines = input.split("\n");

        // Check if there are at least two lines
        if (lines.length >= 2) {
            // Join the lines starting from the third line
            return String.join("\n", Arrays.copyOfRange(lines, 2, lines.length));
        } else {
            // Return an empty string if there are fewer than two lines
            return "";
        }
    }

}
