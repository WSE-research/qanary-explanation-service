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

    @Override
    public String toString() {
        return "AutomatedTestRequestBody{" +
                "testingType='" + testingType + '\'' +
                ", examples=" + examples +
                ", runs=" + runs +
                '}';
    }
}
