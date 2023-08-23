package com.wse.qanaryexplanationservice.pojos.ResultObjectDTOs;

public class End {

    private String datatype;
    private int value;
    private String type;

    public int getValue() {
        return value;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public String getDatatype() {
        return datatype;
    }

    public void setDatatype(String datatype) {
        this.datatype = datatype;
    }

    public End() {
    }
}
