package com.wse.qanaryexplanationservice.pojos.automatedTestingObject;

public abstract class AbstractData {

    private AnnotationType annotationType;
    private String usedComponent;
    private String question;
    private String explanation;
    private String dataSet;
    private String graphID;

    public String getDataSet() {
        return dataSet;
    }

    public String getGraphID() {
        return graphID;
    }

    public void setDataSet(String dataSet) {
        this.dataSet = dataSet;
    }

    public void setGraphID(String graphID) {
        this.graphID = graphID;
    }
    public AnnotationType getAnnotationType() {
        return annotationType;
    }

    public String getExplanation() {
        return explanation;
    }

    public String getUsedComponent() {
        return usedComponent;
    }

    public String getQuestion() {
        return question;
    }

    public void setAnnotationType(AnnotationType annotationType) {
        this.annotationType = annotationType;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public void setUsedComponent(String usedComponent) {
        this.usedComponent = usedComponent;
    }
}

