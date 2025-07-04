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
    String docstring;
    String sourceCode;

    public MethodItem(String caller, String callerName, String methodName, List<Variable> inputVariables, List<Variable> outputVariables, String annotatedAt, String annotatedBy, String docstring, String sourceCode) {
        this.caller = caller;
        this.callerName = callerName == null ? caller : callerName;
        this.methodName = methodName;
        this.inputVariables = inputVariables;
        this.outputVariables = outputVariables;
        this.annotatedAt = annotatedAt;
        this.annotatedBy = annotatedBy;
        this.docstring = docstring;
        this.sourceCode = sourceCode;
    }

    public String getDocstringRepresentation() {
        if (this.docstring != null)
            return "Docstring: " + docstring + "\n";
        else
            return "";
    }

    public String getDocstring() {
        return docstring;
    }

    public void setDocstring(String docstring) {
        this.docstring = docstring;
    }

    public String getSourceCodeRepresentation() {
        if (this.sourceCode != null)
            return "Source code: " + sourceCode + "\n";
        else
            return "";
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public void setSourceCode(String sourceCode) {
        this.sourceCode = sourceCode;
    }

    public List<Variable> getInputVariables() {
        return inputVariables;
    }

    public void setInputVariables(List<Variable> inputVariables) {
        this.inputVariables = inputVariables;
    }

    public List<Variable> getOutputVariables() {
        return outputVariables;
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
