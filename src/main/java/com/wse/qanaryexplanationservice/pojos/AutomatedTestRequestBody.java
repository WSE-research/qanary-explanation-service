package com.wse.qanaryexplanationservice.pojos;

public class AutomatedTestRequestBody {

    private String testingType;
    private int examples;
    private int runs;

    public AutomatedTestRequestBody() {

    }

    public int getRuns() {
        return runs;
    }

    public String getTestingType() {
        return testingType;
    }

    public int getExamples() {
        return examples;
    }

    public void incrementExamples() {
        this.examples += 1;
    }

    @Override
    public String toString() {
        return "AutomatedTestRequestBody{" +
                "testingType='" + testingType + '\'' +
                ", examples=" + examples +
                ", runs=" + runs +
                '}';
    }
}
