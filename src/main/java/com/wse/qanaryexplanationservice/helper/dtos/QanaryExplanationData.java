package com.wse.qanaryexplanationservice.helper.dtos;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;
import java.util.Map;

public class QanaryExplanationData {

    private String graph;
    private String questionId;
    private String component;
    private String serverHost;
    private Map<String,String> explanations;

    public QanaryExplanationData() {

    }

    public Map<String,String> getExplanations() {
        return explanations;
    }

    public void setExplanations(Map<String, String> explanations) {
        this.explanations = explanations;
    }

    public String getComponent() {
        return component;
    }

    public String getGraph() {
        return graph;
    }

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public void setGraph(String graph) {
        this.graph = graph;
    }

    public String getServerHost() {
        return serverHost;
    }

    public void setServerHost(String serverHost) {
        this.serverHost = serverHost;
    }

}
