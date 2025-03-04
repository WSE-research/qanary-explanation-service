package com.wse.qanaryexplanationservice.helper.pojos;

import org.json.JSONObject;

import java.util.Objects;

public class Method {

    private String id;
    private String methodName;
    private boolean isLeaf;
    private String explanation;
    private String docstring;
    private String sourceCode;
    private String prompt;

    public Method(String id, boolean isLeaf) {
        this.id = id;
        this.isLeaf = isLeaf;
    }

    public Method(String id, boolean isLeaf, String explanation) {
        this.id = id;
        this.isLeaf = isLeaf;
        this.explanation = explanation;
    }

    public Method(String id, String methodName, String explanation, String docstring, String sourceCode) {
        this.id = id;
        this.methodName = methodName;
        this.explanation = explanation;
        this.docstring = docstring;
        this.sourceCode = sourceCode;
    }

    public Method(String id, String methodName, String explanation, String docstring, String sourceCode, String prompt) {
        this.id = id;
        this.methodName = methodName;
        this.explanation = explanation;
        this.docstring = docstring;
        this.sourceCode = sourceCode;
        this.prompt = prompt;
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

    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", getId());
        jsonObject.put("methodName", methodName);
        jsonObject.put("docstring", docstring == null ? "Not used" : docstring);
        jsonObject.put("sourceCode", sourceCode == null ? "Not used" : sourceCode);
        jsonObject.put("prompt", prompt == null ? "Not used" : prompt);
        jsonObject.put("explanation", getExplanation());
        return jsonObject;
    }
}
