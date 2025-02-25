package com.wse.qanaryexplanationservice.helper.pojos;

import java.util.List;

public class MethodItem {

    String method;
    String caller;
    String callerName;
    String methodName;
    List<Variable> outputVariables;
    List<Variable> inputVariables;
    String annotatedAt;
    String annotatedBy;
    String explanation;

    public MethodItem(String caller, String callerName, String methodName, List<Variable> inputVariables, List<Variable> outputVariables, String annotatedAt, String annotatedBy) {
        this.caller = caller;
        this.callerName = callerName == null ? caller : callerName;
        this.methodName = methodName;
        this.inputVariables = inputVariables;
        this.outputVariables = outputVariables;
        this.annotatedAt = annotatedAt;
        this.annotatedBy = annotatedBy;
    }

    public List<Variable> getInputVariables() {
        return inputVariables;
    }

    public List<Variable> getOutputVariables() {
        return outputVariables;
    }

    public void setInputVariables(List<Variable> inputVariables) {
        this.inputVariables = inputVariables;
    }

    public void setOutputVariables(List<Variable> outputVariables) {
        this.outputVariables = outputVariables;
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
                "Output data: " + this.outputVariables + "\n" +
                "Input data: " + this.inputVariables + "\n";
    }
}
