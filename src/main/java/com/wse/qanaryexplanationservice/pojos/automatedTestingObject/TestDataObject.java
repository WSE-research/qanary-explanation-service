package com.wse.qanaryexplanationservice.pojos.automatedTestingObject;

public class TestDataObject extends AbstractData {

    public TestDataObject(AnnotationType annotationType, Integer annotationTypeAsInt, String usedComponent, String question, String explanation, String dataSet, String graphID, String questionID, Integer questionNumber, Integer componentNumber) {
        super(annotationType, annotationTypeAsInt, usedComponent, question, explanation, dataSet, graphID, questionID, questionNumber, componentNumber);
    }
}
