package com.wse.qanaryexplanationservice.pojos;

import com.wse.qanaryexplanationservice.pojos.AutomatedTests.automatedTestingObject.automatedTestingObject.TestDataObject;

import java.util.ArrayList;

public class GenerativeExplanationObject {

    public GenerativeExplanationObject() {
        this.exampleComponents = new ArrayList<>();
    }

    private TestDataObject testComponent;
    private ArrayList<TestDataObject> exampleComponents;
    private String prompt;
    private String generativeExplanation;

    public ArrayList<TestDataObject> getExampleComponents() {
        return exampleComponents;
    }

    public TestDataObject getTestComponent() {
        return testComponent;
    }

    public String getPrompt() {
        return prompt;
    }

    public String getGenerativeExplanation() {
        return generativeExplanation;
    }

    public void setExampleComponents(ArrayList<TestDataObject> exampleComponents) {
        this.exampleComponents = exampleComponents;
    }

    public void setTestComponent(TestDataObject testComponent) {
        this.testComponent = testComponent;
    }

    public void setGenerativeExplanation(String generativeExplanation) {
        this.generativeExplanation = generativeExplanation;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public void addExample(TestDataObject testDataObject) {
        this.exampleComponents.add(testDataObject);
    }
}
