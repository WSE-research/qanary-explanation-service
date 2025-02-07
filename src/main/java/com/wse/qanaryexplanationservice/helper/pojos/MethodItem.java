package com.wse.qanaryexplanationservice.helper.pojos;

public class MethodItem {

    String caller;
    String callerName;
    String methodName;
    String outputType;
    String outputValue;
    String inputTypes;
    String inputValues;
    String annotatedAt;
    String annotatedBy;

    public MethodItem(String caller, String callerName, String methodName, String outputType, String outputValue, String inputTypes, String inputValues, String annotatedAt, String annotatedBy) {
        this.caller = caller;
        this.callerName = callerName;
        this.methodName = methodName;
        this.outputType = outputType;
        this.outputValue = outputValue;
        this.inputTypes = inputTypes;
        this.inputValues = inputValues;
        this.annotatedAt = annotatedAt;
        this.annotatedBy = annotatedBy;
    }

    public String getCallerName() {
        return callerName;
    }

    public void setCallerName(String callerName) {
        this.callerName = callerName;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getCaller() {
        return caller;
    }

    public String getInputTypes() {
        return inputTypes;
    }

    public String getInputValues() {
        return inputValues;
    }

    public String getOutputType() {
        return outputType;
    }

    public String getOutputValue() {
        return outputValue;
    }

    public String getAnnotatedAt() {
        return annotatedAt;
    }

    public String getAnnotatedBy() {
        return annotatedBy;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public void setCaller(String caller) {
        this.caller = caller;
    }

    public void setOutputType(String outputType) {
        this.outputType = outputType;
    }

    public void setInputTypes(String inputTypes) {
        this.inputTypes = inputTypes;
    }

    public void setInputValues(String inputValues) {
        this.inputValues = inputValues;
    }

    public void setOutputValue(String outputValue) {
        this.outputValue = outputValue;
    }

    public void setAnnotatedAt(String annotatedAt) {
        this.annotatedAt = annotatedAt;
    }

    public void setAnnotatedBy(String annotatedBy) {
        this.annotatedBy = annotatedBy;
    }
}
