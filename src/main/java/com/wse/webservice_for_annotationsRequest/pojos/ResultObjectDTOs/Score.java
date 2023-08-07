package com.wse.webservice_for_annotationsRequest.pojos.ResultObjectDTOs;

public class Score {

    private double value;
    private String type;
    private String datatype;

    public Score() {}

    public String getType() {
        return type;
    }

    public String getDatatype() {
        return datatype;
    }

    public double getValue() {
        return value;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public void setDatatype(String datatype) {
        this.datatype = datatype;
    }
}
