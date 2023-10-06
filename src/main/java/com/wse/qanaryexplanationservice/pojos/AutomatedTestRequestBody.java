package com.wse.qanaryexplanationservice.pojos;

public class AutomatedTestRequestBody {

    private String testingType;
    private int examples;

    public AutomatedTestRequestBody() {

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
                '}';
    }
}
