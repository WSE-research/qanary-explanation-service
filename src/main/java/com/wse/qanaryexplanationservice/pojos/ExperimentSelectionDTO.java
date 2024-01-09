package com.wse.qanaryexplanationservice.pojos;

public class ExperimentSelectionDTO {

    private String testType;
    private String[] annotationTypes;
    private int shots;

    public ExperimentSelectionDTO() {
    }

    public String getTestType() {
        return testType;
    }

    public void setTestType(String testType) {
        this.testType = testType;
    }

    public int getShots() {
        return shots;
    }

    public void setShots(int shots) {
        this.shots = shots;
    }

    public String[] getAnnotationTypes() {
        return annotationTypes;
    }

    public void setAnnotationTypes(String[] annotationTypes) {
        this.annotationTypes = annotationTypes;
    }
}