package com.wse.webservice_for_annotationsRequest.pojos;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wse.webservice_for_annotationsRequest.pojos.ResultObjectDTOs.*;

public class ExplanationObject {

    @JsonProperty("annotationId")
    private AnnotationId annotationId;
    private Source source;
    private Start start;
    private End end;
    private Body body;
    private Type type;
    private ErstelltVon erstelltVon;
    private ErstelltAm erstelltAm;

    // default constructor
    public ExplanationObject() {

    }

    public void setAnnotationID(AnnotationId annotationId) {
        this.annotationId = annotationId;
    }

    public void setBody(Body body) {
        this.body = body;
    }

    public void setErstelltAm(ErstelltAm erstelltAm) {
        this.erstelltAm = erstelltAm;
    }

    public void setErstelltVon(ErstelltVon erstelltVon) {
        this.erstelltVon = erstelltVon;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public AnnotationId getAnnotationID() {
        return annotationId;
    }

    public Body getBody() {
        return body;
    }

    public ErstelltAm getErstelltAm() {
        return erstelltAm;
    }

    public ErstelltVon getErstelltVon() {
        return erstelltVon;
    }

    public Type getType() {
        return type;
    }

    public void setEnd(End end) {
        this.end = end;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public void setStart(Start start) {
        this.start = start;
    }

    public End getEnd() {
        return end;
    }

    public Source getSource() {
        return source;
    }

    public Start getStart() {
        return start;
    }
}
