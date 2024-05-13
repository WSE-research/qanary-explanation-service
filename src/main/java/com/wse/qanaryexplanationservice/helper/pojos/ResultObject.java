package com.wse.qanaryexplanationservice.helper.pojos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.wse.qanaryexplanationservice.helper.pojos.ResultObjectDTOs.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ResultObject {

    private AnnotationId annotationId;
    private CreatedAt createdAt;
    private CreatedBy createdBy;
    private Body body;
    private Type type;

    public ResultObject() {

    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Body getBody() {
        return body;
    }

    public void setBody(Body body) {
        this.body = body;
    }

    public AnnotationId getAnnotationId() {
        return annotationId;
    }

    public void setAnnotationId(AnnotationId annotationId) {
        this.annotationId = annotationId;
    }

    public CreatedAt getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(CreatedAt createdAt) {
        this.createdAt = createdAt;
    }

    public CreatedBy getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(CreatedBy createdBy) {
        this.createdBy = createdBy;
    }
}
