package com.wse.webservice_for_componentExplanation.pojos;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wse.webservice_for_componentExplanation.pojos.ResultObjectDTOs.*;

/**
 * Represents an entry from the sparql-response === an annotation
 */
public class ResultObject implements Comparable {

    @JsonProperty("annotationId")
    private AnnotationId annotationId;
    private Type type;
    private Body body;
    private Target target;
    private CreatedBy createdBy;
    private CreatedAt createdAt;

    // default constructor
    public ResultObject() {

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

    public void setTarget(Target target) {
        this.target = target;
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

    public Target getTarget() {
        return target;
    }

    public Type getType() {
        return type;
    }

    @Override
    public int compareTo(Object o) {
        ResultObject resultObject = (ResultObject) o;
        return getCreatedAt().getValue().compareTo(resultObject.createdAt.getValue());
    }
}
