package com.wse.qanaryexplanationservice.pojos;

import java.util.ArrayList;

public class GenerativeExplanationRequest {

    public GenerativeExplanationRequest() {

    }

    private ArrayList<QanaryComponent> qanaryComponents;
    private int shots;
    private String gptModel;

    public ArrayList<QanaryComponent> getQanaryComponents() {
        return qanaryComponents;
    }

    public int getShots() {
        return shots;
    }

    public String getGptModel() {
        return gptModel;
    }

    public void setGptModel(String gptModel) {
        this.gptModel = gptModel;
    }

    public void setQanaryComponents(ArrayList<QanaryComponent> qanaryComponents) {
        this.qanaryComponents = qanaryComponents;
    }

    public void setShots(int shots) {
        this.shots = shots;
    }

    public String getComponentListAsRequestList() {
        return "";
    }
}
