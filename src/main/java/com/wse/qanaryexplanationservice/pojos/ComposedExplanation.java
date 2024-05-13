package com.wse.qanaryexplanationservice.pojos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.wse.qanaryexplanationservice.pojos.ExplanationItem;

public class ComposedExplanation {



    public ComposedExplanation() {

    }

    Map<String,ExplanationItem> explanationItems = new HashMap<>();

    public Map<String, ExplanationItem> getExplanationItems() {
        return explanationItems;
    }

    public void setExplanationItems(Map<String, ExplanationItem> explanationItems) {
        this.explanationItems = explanationItems;
    }

    public void addExplanationItem(String component, String rulebased, String prompt, String generative) {
        this.explanationItems.put(component, new ExplanationItem(rulebased,prompt,generative));
    }
}
