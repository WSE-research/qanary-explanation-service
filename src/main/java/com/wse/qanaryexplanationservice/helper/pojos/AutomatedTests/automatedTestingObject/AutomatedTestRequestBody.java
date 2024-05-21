package com.wse.qanaryexplanationservice.helper.pojos.AutomatedTests.automatedTestingObject;

import com.wse.qanaryexplanationservice.helper.GptModel;

import java.util.Arrays;

public class AutomatedTestRequestBody {

    private String testingType;
    private Example[] examples;
    GptModel gptModel;
    private int runs;

    public AutomatedTestRequestBody() {

    }

    public AutomatedTestRequestBody(String testingType) {

    }

    public GptModel getGptModel() {
        return gptModel;
    }

    public void setGptModel(GptModel gptModel) {
        this.gptModel = gptModel;
    }

    public int getRuns() {
        return runs;
    }

    public void setRuns(int runs) {
        this.runs = runs;
    }

    public String getTestingType() {
        return testingType;
    }

    public void setTestingType(String testingType) {
        this.testingType = testingType;
    }

    public Example[] getExamples() {
        return examples;
    }

    public void setExamples(Example[] examples) {
        this.examples = examples;
    }

    @Override
    public String toString() {
        return "AutomatedTestRequestBody{" +
                "testingType='" + testingType + '\'' +
                ", examples=" + Arrays.toString(examples) +
                ", runs=" + runs +
                '}';
    }

    public String listToString() {
        String temp = "";
        for (Example example : examples
        ) {
            temp += "_" + example.getType();
        }
        return temp;
    }
}
