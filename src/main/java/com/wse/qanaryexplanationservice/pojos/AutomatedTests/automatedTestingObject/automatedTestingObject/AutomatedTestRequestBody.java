package com.wse.qanaryexplanationservice.pojos.AutomatedTests.automatedTestingObject.automatedTestingObject;

import java.util.Arrays;

public class AutomatedTestRequestBody {

    private String testingType;
    private Example[] examples;
    private int runs;

    public AutomatedTestRequestBody() {

    }

    public AutomatedTestRequestBody(String testingType) {

    }

    public int getRuns() {
        return runs;
    }

    public String getTestingType() {
        return testingType;
    }

    public Example[] getExamples() {
        return examples;
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

    public void setExamples(Example[] examples) {
        this.examples = examples;
    }

    public void setRuns(int runs) {
        this.runs = runs;
    }

    public void setTestingType(String testingType) {
        this.testingType = testingType;
    }
}
