package com.wse.qanaryexplanationservice.pojos;

import com.wse.qanaryexplanationservice.pojos.ResultObjectDTOs.*;

public class ResultObject {

    private AnnotationId annotationId;
    private CreatedAt createdAt;
    private CreatedBy createdBy;
    private Body body;
    private Type type;

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

    public CreatedAt getCreatedAt() {
        return createdAt;
    }

    public CreatedBy getCreatedBy() {
        return createdBy;
    }

    public void setAnnotationId(AnnotationId annotationId) {
        this.annotationId = annotationId;
    }

    public void setCreatedAt(CreatedAt createdAt) {
        this.createdAt = createdAt;
    }

    public void setCreatedBy(CreatedBy createdBy) {
        this.createdBy = createdBy;
    }
}
