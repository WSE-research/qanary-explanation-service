package com.wse.qanaryexplanationservice.pojos.automatedTestingObject;

import java.util.ArrayList;

public class AutomatedTest {

    private TestData testData;
    private ArrayList<ExampleData> exampleData;
    private String gptExplanation;


    public AutomatedTest() {
        this.exampleData = new ArrayList<>();
    }

    public ArrayList<ExampleData> getExampleData() {
        return exampleData;
    }

    public void setExampleData(ExampleData exampleData) {
        this.exampleData.add(exampleData);
    }

    public TestData getTestData() {
        return testData;
    }

    public void setTestData(TestData testData) {
        this.testData = testData;
    }
}
