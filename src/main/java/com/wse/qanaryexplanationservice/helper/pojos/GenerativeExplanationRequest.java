package com.wse.qanaryexplanationservice.helper.pojos;

import java.util.ArrayList;

public class GenerativeExplanationRequest {

    private ArrayList<QanaryComponent> qanaryComponents;
    private int shots;
    private String gptModel;
    public GenerativeExplanationRequest() {

    }

    public ArrayList<QanaryComponent> getQanaryComponents() {
        return qanaryComponents;
    }

    public void setQanaryComponents(ArrayList<QanaryComponent> qanaryComponents) {
        this.qanaryComponents = qanaryComponents;
    }

    public int getShots() {
        return shots;
    }

    public void setShots(int shots) {
        this.shots = shots;
    }

    public String getGptModel() {
        return gptModel;
    }

    public void setGptModel(String gptModel) {
        this.gptModel = gptModel;
    }

    public String getComponentListAsRequestList() {
        return "";
    }
}
