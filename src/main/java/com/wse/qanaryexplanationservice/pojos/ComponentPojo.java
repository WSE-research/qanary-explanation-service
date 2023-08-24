package com.wse.qanaryexplanationservice.pojos;

import com.wse.qanaryexplanationservice.pojos.ResultObjectDTOs.Component;

public class ComponentPojo {

    private Component component;

    public ComponentPojo() {
    }

    public ComponentPojo(Component component) {
        this.component = component;
    }

    public Component getComponent() {
        return component;
    }

    public void setComponent(Component component) {
        this.component = component;
    }
}
