package com.wse.qanaryexplanationservice.helper.pojos;

public class ExplanationItem {

    private String templatebased;
    private String prompt;
    private String generative;
    private String dataset;
    public ExplanationItem(String templatebased, String prompt, String generative, String dataset) {
        this.generative = generative;
        this.prompt = prompt;
        this.templatebased = templatebased;
        this.dataset = dataset;
    }

    public String getDataset() {
        return dataset;
    }

    public void setDataset(String dataset) {
        this.dataset = dataset;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getGenerative() {
        return generative;
    }

    public void setGenerative(String generative) {
        this.generative = generative;
    }

    public String getTemplatebased() {
        return templatebased;
    }

    public void setTemplatebased(String templatebased) {
        this.templatebased = templatebased;
    }
}
