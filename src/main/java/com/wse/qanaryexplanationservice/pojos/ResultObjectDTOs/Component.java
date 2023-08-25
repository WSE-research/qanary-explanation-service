package com.wse.qanaryexplanationservice.pojos.ResultObjectDTOs;

public class Component extends ExplanationPojosDtosAbstract {

    public Component() {

    }

    // for testing purposes
    public Component(String type, String value) {
        this.setType(type);
        this.setValue(value);
    }
}
