package com.wse.qanaryexplanationservice.helper.pojos.ResultObjectDTOs;

import java.time.LocalDateTime;

public class CreatedAt {

    private LocalDateTime value;
    private String type;
    private String datatype;

    public String getDatatype() {
        return datatype;
    }

    public void setDatatype(String datatype) {
        this.datatype = datatype;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDateTime getValue() {
        return value;
    }

    public void setValue(LocalDateTime value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "CreatedAt{" + getValue() + "---" +
                "datatype='" + datatype + '\'' +
                '}';
    }
}
