package com.wse.qanaryexplanationservice.dtos;

import com.wse.qanaryexplanationservice.pojos.GenerativeExplanationRequest;
import com.wse.qanaryexplanationservice.pojos.QanaryComponent;

import java.util.List;

public class ComposedExplanationDTO {

    public ComposedExplanationDTO() {

    }

    private GenerativeExplanationRequest generativeExplanationRequest;
    private String graphUri;


    public GenerativeExplanationRequest getGenerativeExplanationRequest() {
        return generativeExplanationRequest;
    }

    public void setGenerativeExplanationRequest(GenerativeExplanationRequest generativeExplanationRequest) {
        this.generativeExplanationRequest = generativeExplanationRequest;
    }

    public void setGraphUri(String graphUri) {
        this.graphUri = graphUri;
    }

    public String getGraphUri() {
        return graphUri;
    }
}
