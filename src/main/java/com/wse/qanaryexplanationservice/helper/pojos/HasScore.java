package com.wse.qanaryexplanationservice.helper.pojos;

public class HasScore {

    private int numberOfAnnotations;
    private int qualityAnnotations;
    private int qualityPrefix;

    public HasScore() {
    }

    public int getNumberOfAnnotations() {
        return numberOfAnnotations;
    }

    public void setNumberOfAnnotations(int numberOfAnnotations) {
        this.numberOfAnnotations = numberOfAnnotations;
    }

    public int getQualityAnnotations() {
        return qualityAnnotations;
    }

    public void setQualityAnnotations(int qualityAnnotations) {
        this.qualityAnnotations = qualityAnnotations;
    }

    public int getQualityPrefix() {
        return qualityPrefix;
    }

    public void setQualityPrefix(int qualityPrefix) {
        this.qualityPrefix = qualityPrefix;
    }
}
