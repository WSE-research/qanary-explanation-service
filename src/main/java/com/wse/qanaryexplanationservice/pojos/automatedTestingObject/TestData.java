package com.wse.qanaryexplanationservice.pojos.automatedTestingObject;

public class TestData extends AbstractData {

    public TestData(AnnotationType annotationType, String usedComponent, String question, String explanation, String dataSet, String graphID) {
        super(annotationType, usedComponent, question, explanation, dataSet, graphID);
    }


    public TestData() {
        super();
    }
}
