package com.wse.webservice_for_annotationsRequest.pojos;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wse.webservice_for_annotationsRequest.pojos.ResultObjectDTOs.*;

/**
 * Represents an entry from the sparql-response === an annotation
 */
public class ResultObject implements Comparable{

    @JsonProperty("annotationId")
    private AnnotationId annotationId;
    private Type type;
    private Body body;
    private Target target;
    private ErstelltVon erstelltVon;
    private ErstelltAm erstelltAm;

    // default constructor
    public ResultObject() {

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

    public void setTarget(Target target) {
        this.target = target;
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

    public Target getTarget() {
        return target;
    }

    public Type getType() {
        return type;
    }

    @Override
    public int compareTo(Object o) {
        ResultObject resultObject = (ResultObject) o;
        return getErstelltAm().getValue().compareTo(resultObject.erstelltAm.getValue());
    }
}
