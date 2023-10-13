package com.wse.qanaryexplanationservice.pojos;

public class Example {

    private String type;
    private boolean uniqueComponent;

    public Example() {

    }

    public boolean getUniqueComponent() {
        return uniqueComponent;
    }

    public void setUniqueComponent(boolean uniqueComponent) {
        this.uniqueComponent = uniqueComponent;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
