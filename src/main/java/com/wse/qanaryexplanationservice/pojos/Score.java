package com.wse.qanaryexplanationservice.pojos;

// TODO: Rename to clarify its for updating a graph with a score
public class Score {

    private HasScore hasScore;
    private String graphId;

    public Score() {}

    public HasScore getHasScore() {
        return hasScore;
    }

    public void setHasScore(HasScore hasScore) {
        this.hasScore = hasScore;
    }

    public String getGraphId() {
        return graphId;
    }

    public void setGraphId(String graphId) {
        this.graphId = graphId;
    }
}
