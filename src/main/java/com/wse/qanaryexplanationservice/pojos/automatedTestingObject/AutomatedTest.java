package com.wse.qanaryexplanationservice.pojos.automatedTestingObject;

import java.util.ArrayList;

public class AutomatedTest {

    private TestDataObject testData;
    private ArrayList<TestDataObject> exampleData;
    private String gptExplanation;
    private String prompt;

    public AutomatedTest() {
        this.exampleData = new ArrayList<>();
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public ArrayList<TestDataObject> getExampleData() {
        return exampleData;
    }

    public void setExampleData(TestDataObject exampleData) {
        this.exampleData.add(exampleData);
    }

    public TestDataObject getTestData() {
        return testData;
    }

    public void setTestData(TestDataObject testData) {
        this.testData = testData;
    }

    public String getGptExplanation() {
        return gptExplanation;
    }

    public void setGptExplanation(String gptExplanation) {
        this.gptExplanation = gptExplanation;
    }

    @Override
    public String toString() {
        return "AutomatedTest{" +
                "testData=" + testData +
                ", exampleData=" + exampleData +
                ", gptExplanation='" + gptExplanation + '\'' +
                ", prompt='" + prompt + '\'' +
                '}';
    }
}
