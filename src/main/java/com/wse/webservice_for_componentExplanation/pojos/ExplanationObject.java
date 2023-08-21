package com.wse.webservice_for_componentExplanation.pojos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wse.webservice_for_componentExplanation.pojos.ResultObjectDTOs.*;

public class ExplanationObject {

    // Idea: Set standard properties and additional properties as ignorable, anyway, for specific purposes they can still be accessed
    // like if an Output sparql query varies within different components

    @JsonProperty("annotationId")
    @JsonIgnoreProperties
    private AnnotationId annotationId;
    @JsonIgnoreProperties
    private Source source;
    @JsonIgnoreProperties
    private Start start;
    @JsonIgnoreProperties
    private End end;
    private Body body;
    @JsonIgnoreProperties
    private Type type;
    private CreatedBy createdBy;
    private CreatedAt createdAt;
    private Score score;
    @JsonIgnoreProperties
    private String entity;

    // default constructor
    public ExplanationObject() {

    }

    public Score getScore() {
        return score;
    }

    public void setScore(Score score) {
        this.score = score;
    }

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public void setAnnotationID(AnnotationId annotationId) {
        this.annotationId = annotationId;
    }

    public void setBody(Body body) {
        this.body = body;
    }

    public void setCreatedAt(CreatedAt createdAt) {
        this.createdAt = createdAt;
    }

    public void setCreatedBy(CreatedBy createdBy) {
        this.createdBy = createdBy;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public AnnotationId getAnnotationID() {
        return annotationId;
    }

    public Body getBody() {
        return body;
    }

    public CreatedAt getCreatedAt() {
        return createdAt;
    }

    public CreatedBy getCreatedBy() {
        return createdBy;
    }

    public Type getType() {
        return type;
    }

    public void setEnd(End end) {
        this.end = end;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public void setStart(Start start) {
        this.start = start;
    }

    public End getEnd() {
        return end;
    }

    public Source getSource() {
        return source;
    }

    public Start getStart() {
        return start;
    }

    @Override
    public String toString() {
        return "ExplanationObject{" +
                "annotationId=" + annotationId +
                ", source=" + source +
                ", start=" + start +
                ", end=" + end +
                ", body=" + body +
                ", type=" + type +
                ", createdBy=" + createdBy +
                ", createdAt=" + createdAt +
                ", score=" + score.getValue() +
                ", entity=" + entity +
                '}';
    }
}
