package com.wse.qanaryexplanationservice.helper.pojos;

import com.wse.qanaryexplanationservice.helper.pojos.AutomatedTests.automatedTestingObject.TestDataObject;

import java.util.ArrayList;

public class GenerativeExplanationObject {

    private TestDataObject testComponent;
    private ArrayList<TestDataObject> exampleComponents;
    private String prompt;
    private String generativeExplanation;

    public GenerativeExplanationObject() {
        this.exampleComponents = new ArrayList<>();
    }

    public ArrayList<TestDataObject> getExampleComponents() {
        return exampleComponents;
    }

    public void setExampleComponents(ArrayList<TestDataObject> exampleComponents) {
        this.exampleComponents = exampleComponents;
    }

    public TestDataObject getTestComponent() {
        return testComponent;
    }

    public void setTestComponent(TestDataObject testComponent) {
        this.testComponent = testComponent;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getGenerativeExplanation() {
        return generativeExplanation;
    }

    public void setGenerativeExplanation(String generativeExplanation) {
        this.generativeExplanation = generativeExplanation;
    }

    public void addExample(TestDataObject testDataObject) {
        this.exampleComponents.add(testDataObject);
    }
}
