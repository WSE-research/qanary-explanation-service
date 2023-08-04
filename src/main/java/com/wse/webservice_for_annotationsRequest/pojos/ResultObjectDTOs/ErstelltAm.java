package com.wse.webservice_for_annotationsRequest.pojos.ResultObjectDTOs;

import java.time.LocalDateTime;
import java.util.Date;

public class ErstelltAm {

    private LocalDateTime value;
    private String type;
    private String datatype;


    public void setDatatype(String datatype) {
        this.datatype = datatype;
    }

    public String getDatatype() {
        return datatype;
    }

    public String getType() {
        return type;
    }

    public LocalDateTime getValue() {
        return value;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setValue(LocalDateTime value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "ErstelltAm{" + getValue() + "---" +
                "datatype='" + datatype + '\'' +
                '}';
    }
}
