package com.wse.webservice_for_annotationsRequest.pojos.ResultObjectDTOs;

public abstract class ResultObjectDTOsAbstract {

    private String type;
    private String value;

    public void setType(String type) {
        this.type = type;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "ResultObjectDTOsAbstract{" +
                "type='" + type + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
