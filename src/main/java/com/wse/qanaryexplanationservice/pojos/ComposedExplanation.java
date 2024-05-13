package com.wse.qanaryexplanationservice.pojos;

import java.util.ArrayList;
import com.wse.qanaryexplanationservice.pojos.ExplanationItem;

public class ComposedExplanation {



    public ComposedExplanation() {

    }

    ArrayList<ExplanationItem> explanationItems = new ArrayList<>();

    public ArrayList<ExplanationItem> getExplanationItems() {
        return explanationItems;
    }

    public void setExplanationItems(ArrayList<ExplanationItem> explanationItems) {
        this.explanationItems = explanationItems;
    }

    public void addExplanationItem(String rulebased, String prompt, String generative) {
        this.explanationItems.add(new ExplanationItem(rulebased,prompt,generative));
    }
}
