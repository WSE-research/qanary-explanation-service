package com.wse.qanaryexplanationservice.pojos;

public class ExplanationItem {

    public ExplanationItem(String templatebased, String prompt, String generative) {
            this.generative = generative;
            this.prompt = prompt;
            this.templatebased = templatebased;
        }
        private String templatebased;
        private String prompt;
        private String generative;

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public void setGenerative(String generative) {
        this.generative = generative;
    }

    public void setTemplatebased(String templatebased) {
        this.templatebased = templatebased;
    }

    public String getPrompt() {
        return prompt;
    }

    public String getGenerative() {
        return generative;
    }

    public String getTemplatebased() {
        return templatebased;
    }
}
