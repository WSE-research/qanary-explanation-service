package com.wse.qanaryexplanationservice.helper.dtos;

import com.wse.qanaryexplanationservice.helper.pojos.GenerativeExplanationRequest;

public class ComposedExplanationDTO {

    private GenerativeExplanationRequest generativeExplanationRequest;
    private String graphUri;

    public ComposedExplanationDTO() {

    }

    public GenerativeExplanationRequest getGenerativeExplanationRequest() {
        return generativeExplanationRequest;
    }

    public void setGenerativeExplanationRequest(GenerativeExplanationRequest generativeExplanationRequest) {
        this.generativeExplanationRequest = generativeExplanationRequest;
    }

    public String getGraphUri() {
        return graphUri;
    }

    public void setGraphUri(String graphUri) {
        this.graphUri = graphUri;
    }
}
