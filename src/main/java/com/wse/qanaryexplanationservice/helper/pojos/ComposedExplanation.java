package com.wse.qanaryexplanationservice.helper.pojos;

import java.util.HashMap;
import java.util.Map;

public class ComposedExplanation {


    Map<String, ExplanationItem> explanationItems = new HashMap<>();

    public ComposedExplanation() {

    }

    public Map<String, ExplanationItem> getExplanationItems() {
        return explanationItems;
    }

    public void setExplanationItems(Map<String, ExplanationItem> explanationItems) {
        this.explanationItems = explanationItems;
    }

    public void addExplanationItem(String component, String rulebased, String prompt, String generative, String dataset) {
        this.explanationItems.put(component, new ExplanationItem(rulebased, prompt, generative, dataset));
    }
}
