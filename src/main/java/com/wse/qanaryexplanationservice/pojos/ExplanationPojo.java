package com.wse.qanaryexplanationservice.pojos;

import com.wse.qanaryexplanationservice.pojos.ResultObjectDTOs.Body;
import com.wse.qanaryexplanationservice.pojos.ResultObjectDTOs.Component;
import com.wse.qanaryexplanationservice.pojos.ResultObjectDTOs.CreatedAt;
import com.wse.qanaryexplanationservice.pojos.ResultObjectDTOs.CreatedBy;

public class ExplanationPojo {

    private Component component;
    private CreatedAt createdAt;
    private CreatedBy createdBy;
    private Body body;

    public Body getBody() {
        return body;
    }

    public Component getComponent() {
        return component;
    }

    public CreatedAt getCreatedAt() {
        return createdAt;
    }

    public CreatedBy getCreatedBy() {
        return createdBy;
    }

    public void setBody(Body body) {
        this.body = body;
    }

    public void setComponent(Component component) {
        this.component = component;
    }

    public void setCreatedAt(CreatedAt createdAt) {
        this.createdAt = createdAt;
    }

    public void setCreatedBy(CreatedBy createdBy) {
        this.createdBy = createdBy;
    }
}
