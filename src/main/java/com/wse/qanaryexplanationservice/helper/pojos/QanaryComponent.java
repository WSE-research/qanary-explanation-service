package com.wse.qanaryexplanationservice.helper.pojos;

import jakarta.validation.constraints.NotNull;

public class QanaryComponent {

    @NotNull
    private String componentName;
    private String prefixedComponentName;
    private String componentMainType;

    public QanaryComponent() {

    }

    public QanaryComponent(String componentName) {
        setComponentNames(componentName);
    }

    public QanaryComponent(String componentName, String componentMainType) {
        this.componentMainType = componentMainType;
        this.componentName = componentName;
    }

    private void setComponentNames(String componentName) {
        if (componentName.contains("urn:qanary:")) {
            this.componentName = componentName.replace("urn:qanary:", "");
            this.prefixedComponentName = componentName;
        } else {
            this.componentName = componentName;
            this.prefixedComponentName = "urn:qanary:" + componentName;
        }
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

    public String getPrefixedComponentName() {
        return prefixedComponentName;
    }
}
