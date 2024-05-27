package com.wse.qanaryexplanationservice.helper.pojos.AutomatedTests.automatedTestingObject;

import com.wse.qanaryexplanationservice.helper.AnnotationType;
import com.wse.qanaryexplanationservice.helper.pojos.QanaryComponent;

public class TestDataObject extends AbstractData {

    public TestDataObject(AnnotationType annotationType, Integer annotationTypeAsInt, QanaryComponent usedComponent, String question, String explanation, String dataSet, String graphID, String questionID, Integer questionNumber, Integer componentNumber, String randomComponents) {
        super(annotationType, annotationTypeAsInt, usedComponent, question, explanation, dataSet, graphID, questionID, questionNumber, componentNumber, randomComponents);
    }

    public TestDataObject() {
    }
}
