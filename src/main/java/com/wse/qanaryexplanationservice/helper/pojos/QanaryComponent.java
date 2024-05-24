package com.wse.qanaryexplanationservice.helper.pojos;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public class QanaryComponent {

    @NotNull
    private String componentName;
    @NotNull
    private String componentMainType;

    public QanaryComponent() {

    }

    public QanaryComponent(String componentName) {
        this.componentName = componentName;
    }

    public QanaryComponent(String componentName, String componentMainType) {
        this.componentMainType = componentMainType;
        this.componentName = componentName;
    }

    public String getComponentMainType() {
        return componentMainType;
    }

    public void setComponentMainType(String componentMainType) {
        this.componentMainType = componentMainType;
    }

    public String getComponentName() {
        return componentName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }
}
