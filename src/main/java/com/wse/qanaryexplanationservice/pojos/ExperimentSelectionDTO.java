package com.wse.qanaryexplanationservice.pojos;

public class ExperimentSelectionDTO {

    public ExperimentSelectionDTO() {}

    private String[] annotationTypes;
    private int shots;

    public int getShots() {
        return shots;
    }
    public String[] getAnnotationTypes() {
        return annotationTypes;
    }
    public void setAnnotationTypes(String[] annotationTypes) {
        this.annotationTypes = annotationTypes;
    }
    public void setShots(int shots) {
        this.shots = shots;
    }
}