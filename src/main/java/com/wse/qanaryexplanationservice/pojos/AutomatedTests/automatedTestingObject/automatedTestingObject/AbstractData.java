package com.wse.qanaryexplanationservice.pojos.AutomatedTests.automatedTestingObject.automatedTestingObject;

public abstract class AbstractData {

    private AnnotationType annotationType;
    private Integer annotationTypeAsInt;
    private String usedComponent;
    private String question;
    private String explanation;
    private String dataSet;
    private String graphID;
    private String questionID;
    private Integer questionNumber;
    private Integer componentNumber;
    private String randomComponents;


    public AbstractData() {
    }

    public AbstractData(
            AnnotationType annotationType,
            Integer annotationTypeAsInt,
            String usedComponent,
            String question,
            String explanation,
            String dataSet,
            String graphID,
            String questionID,
            Integer questionNumber,
            Integer componentNumber,
            String randomComponents
    ) {
        this.annotationType = annotationType;
        this.annotationTypeAsInt = annotationTypeAsInt;
        this.usedComponent = usedComponent;
        this.explanation = explanation;
        this.dataSet = dataSet;
        this.question = question;
        this.graphID = graphID;
        this.questionID = questionID;
        this.questionNumber = questionNumber;
        this.componentNumber = componentNumber;
        this.randomComponents = randomComponents;
    }

    public String getRandomComponents() {
        return randomComponents;
    }

    public void setRandomComponents(String randomComponents) {
        this.randomComponents = randomComponents;
    }

    public Integer getAnnotationTypeAsInt() {
        return annotationTypeAsInt;
    }

    public void setAnnotationTypeAsInt(Integer annotationTypeAsInt) {
        this.annotationTypeAsInt = annotationTypeAsInt;
    }

    public Integer getComponentNumber() {
        return componentNumber;
    }

    public void setComponentNumber(Integer componentNumber) {
        this.componentNumber = componentNumber;
    }

    public Integer getQuestionNumber() {
        return questionNumber;
    }

    public void setQuestionNumber(Integer questionNumber) {
        this.questionNumber = questionNumber;
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

