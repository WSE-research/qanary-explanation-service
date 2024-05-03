package com.wse.qanaryexplanationservice.pojos;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public class QanaryComponent {

    public QanaryComponent() {

    }

    public QanaryComponent(String componentName, String componentMainType) {
        this.componentMainType = componentMainType;
        this.componentName = componentName;
    }

    @NotNull
    private String componentName;
    @NotNull
    private String componentMainType;

    public String getComponentMainType() {
        return componentMainType;
    }

    public String getComponentName() {
        return componentName;
    }

    public void setComponentMainType(String componentMainType) {
        this.componentMainType = componentMainType;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }
}
