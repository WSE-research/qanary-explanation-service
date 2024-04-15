package com.wse.qanaryexplanationservice.pojos.AutomatedTests.ComponentSet;

public class ComponentSet {

    private Component testingComponent;

    private Component[] exampleComponents;

    public Component getTestingComponent() {
        return testingComponent;
    }

    public Component[] getExampleComponents() {
        return exampleComponents;
    }

    public void setExampleComponents(Component[] exampleComponents) {
        this.exampleComponents = exampleComponents;
    }

    public void setTestingComponent(Component testingComponent) {
        this.testingComponent = testingComponent;
    }
}
