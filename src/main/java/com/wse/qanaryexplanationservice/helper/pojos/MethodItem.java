package com.wse.qanaryexplanationservice.helper.pojos;

public class MethodItem {

    String method;
    String caller;
    String callerName;
    String methodName;
    String outputType;
    String outputValue;
    String inputTypes;
    String inputValues;
    String annotatedAt;
    String annotatedBy;
    String explanation;

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

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
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

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getCaller() {
        return caller;
    }

    public void setCaller(String caller) {
        this.caller = caller;
    }

    public String getInputTypes() {
        return inputTypes;
    }

    public void setInputTypes(String inputTypes) {
        this.inputTypes = inputTypes;
    }

    public String getInputValues() {
        return inputValues;
    }

    public void setInputValues(String inputValues) {
        this.inputValues = inputValues;
    }

    public String getOutputType() {
        return outputType;
    }

    public void setOutputType(String outputType) {
        this.outputType = outputType;
    }

    public String getOutputValue() {
        return outputValue;
    }

    public void setOutputValue(String outputValue) {
        this.outputValue = outputValue;
    }

    public String getAnnotatedAt() {
        return annotatedAt;
    }

    public void setAnnotatedAt(String annotatedAt) {
        this.annotatedAt = annotatedAt;
    }

    public String getAnnotatedBy() {
        return annotatedBy;
    }

    public void setAnnotatedBy(String annotatedBy) {
        this.annotatedBy = annotatedBy;
    }

    @Override
    public String toString() {
        return "Method name: " + this.methodName + "\n" +
                "Method ID: " + this.method + "\n" +
                "Caller: " + this.callerName + "(" + this.caller + ")\n" +
                "Annotated at: " + this.annotatedAt + "\n" +
                "Output data value: " + this.outputValue + "\n" +
                "Output type: " + this.outputType + "\n" +
                "Input data values: " + this.inputValues + "\n" +
                "Input data types: " + this.inputTypes;
    }
}
