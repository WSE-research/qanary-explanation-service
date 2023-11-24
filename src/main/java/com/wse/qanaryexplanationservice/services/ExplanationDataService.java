package com.wse.qanaryexplanationservice.services;

import com.wse.qanaryexplanationservice.pojos.AutomatedTests.automatedTestingObject.automatedTestingObject.AutomatedTest;
import com.wse.qanaryexplanationservice.pojos.AutomatedTests.automatedTestingObject.automatedTestingObject.TestDataObject;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.ResourceFactory;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
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


        return QanaryTripleStoreConnector.readFileFromResourcesWithMap(AUTOMATED_TEST_INSERT_QUERY,bindingForInsertQuery);
    }

    public String setupTestdataObject(TestDataObject testDataObject) throws IOException {
        QuerySolutionMap querySolutionMap = new QuerySolutionMap();

        querySolutionMap.add("annotationType",ResourceFactory.createPlainLiteral(testDataObject.getAnnotationType().name()));
        querySolutionMap.add("usedComponent",ResourceFactory.createPlainLiteral(testDataObject.getUsedComponent()));
        querySolutionMap.add("usedComponentAsNum",ResourceFactory.createPlainLiteral(String.valueOf(testDataObject.getComponentNumber())));
        querySolutionMap.add("dataset",ResourceFactory.createPlainLiteral(testDataObject.getDataSet()));
        querySolutionMap.add("graphId",ResourceFactory.createPlainLiteral(testDataObject.getGraphID()));
        querySolutionMap.add("explanation",ResourceFactory.createPlainLiteral(testDataObject.getExplanation()));
        querySolutionMap.add("questionId",ResourceFactory.createPlainLiteral(testDataObject.getQuestionID()));
        querySolutionMap.add("question",ResourceFactory.createPlainLiteral(testDataObject.getQuestion()));

        return QanaryTripleStoreConnector.readFileFromResourcesWithMap(TEST_DATA_OBJECT_RQ, querySolutionMap);
    }

    public String setupExampleData(ArrayList<TestDataObject> examples) {


        return "";
    }

}
