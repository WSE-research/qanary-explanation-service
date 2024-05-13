package com.wse.qanaryexplanationservice.helper.pojos.AutomatedTests.automatedTestingObject;

import java.util.Arrays;

public class AutomatedTestDTO {

    private TestDataObject testData;
    private TestDataObject[] exampleData;
    private String gptExplanation;
    private String prompt;

    public AutomatedTestDTO() {

    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public TestDataObject[] getExampleData() {
        return exampleData;
    }

    public void setExampleData(TestDataObject[] exampleData) {
        this.exampleData = exampleData;
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
        return "AutomatedTestDTO{" +
                "testData=" + testData +
                ", exampleData=" + Arrays.toString(exampleData) +
                ", gptExplanation='" + gptExplanation + '\'' +
                ", prompt='" + prompt + '\'' +
                '}';
    }
}
