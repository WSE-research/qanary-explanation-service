package com.wse.qanaryexplanationservice.pojos.automatedTestingObject;

import com.wse.qanaryexplanationservice.pojos.Example;

public class AutomatedTestRequestBody {

    private String testingType;
    private Example[] examples;
    private int runs;

    public AutomatedTestRequestBody() {

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
        return testingType + listToString();
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
