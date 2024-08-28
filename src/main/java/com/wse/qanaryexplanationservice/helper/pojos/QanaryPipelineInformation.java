package com.wse.qanaryexplanationservice.helper.pojos;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class QanaryPipelineInformation {

    @JsonProperty("question")
    private String question;
    @JsonProperty("questionId")
    private String questionId;
    @JsonProperty("graph")
    private String graph;
    @JsonProperty("components")
    private List<String> components;

    public String getQuestionId() {
        return questionId;
    }

    public String getGraph() {
        return graph;
    }

    public List<String> getComponents() {
        return components;
    }

    public String getQuestion() {
        return question;
    }

    public void setGraph(String graph) {
        this.graph = graph;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public void setComponents(List<String> components) {
        this.components = components;
    }

    public void setQuestion(String question) {
        this.question = question;
    }
}
