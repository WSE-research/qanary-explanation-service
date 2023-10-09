package com.wse.qanaryexplanationservice.pojos.automatedTestingObject;

public abstract class AbstractData {

    private AnnotationType annotationType;
    private String usedComponent;
    private String question;
    private String explanation;
    private String dataSet;
    private String graphID;
    private String questionID;

    public AbstractData() {
    }

    public AbstractData(
            AnnotationType annotationType,
            String usedComponent,
            String question,
            String explanation,
            String dataSet,
            String graphID,
            String questionID
    ) {
        this.annotationType = annotationType;
        this.usedComponent = usedComponent;
        this.explanation = explanation;
        this.dataSet = dataSet;
        this.question = question;
        this.graphID = graphID;
        this.questionID = questionID;
    }

    public String getQuestionID() {
        return questionID;
    }

    public void setQuestionID(String questionID) {
        this.questionID = questionID;
    }

    public String getDataSet() {
        return dataSet;
    }

    public void setDataSet(String dataSet) {
        this.dataSet = dataSet;
    }

    public String getGraphID() {
        return graphID;
    }

    public void setGraphID(String graphID) {
        this.graphID = graphID;
    }

    public AnnotationType getAnnotationType() {
        return annotationType;
    }

    public void setAnnotationType(AnnotationType annotationType) {
        this.annotationType = annotationType;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getUsedComponent() {
        return usedComponent;
    }

    public void setUsedComponent(String usedComponent) {
        this.usedComponent = usedComponent;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    @Override
    public String toString() {
        return "AbstractData{" +
                "annotationType=" + annotationType +
                ", usedComponent='" + usedComponent + '\'' +
                ", question='" + question + '\'' +
                ", explanation='" + explanation + '\'' +
                ", dataSet='" + dataSet + '\'' +
                ", graphID='" + graphID + '\'' +
                '}';
    }
}

