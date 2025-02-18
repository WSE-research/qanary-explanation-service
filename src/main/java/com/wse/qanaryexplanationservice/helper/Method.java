package com.wse.qanaryexplanationservice.helper;

import java.util.Objects;

public class Method {

    private String id;
    private boolean isLeaf;
    private String explanation;
    public Method(String id, boolean isLeaf) {
        this.id = id;
        this.isLeaf = isLeaf;
    }
    public Method(String id, boolean isLeaf, String explanation) {
        this.id = id;
        this.isLeaf = isLeaf;
        this.explanation = explanation;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isLeaf() {
        return isLeaf;
    }

    public void setLeaf(boolean leaf) {
        isLeaf = leaf;
    }

    @Override
    public boolean equals(Object o) {
        return this.getId().equals(((Method) o).getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());  // assuming getId() returns a unique identifier for each Method
    }

    @Override
    public String toString() {
        return "ID: " + getId() + ", isLeaf: " + isLeaf + ", explanation: " + explanation;
    }
}
