package com.wse.qanaryexplanationservice.pojos.automatedTestingObject;

public class QanaryResponseObject {

    private String endpoint;
    private String inGraph;
    private String outGraph;
    private String question;
    public QanaryResponseObject() {

    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getInGraph() {
        return inGraph;
    }

    public void setInGraph(String inGraph) {
        this.inGraph = inGraph;
    }

    public String getOutGraph() {
        return outGraph;
    }

    public void setOutGraph(String outGraph) {
        this.outGraph = outGraph;
    }
}
