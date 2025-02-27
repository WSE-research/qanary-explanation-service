package com.wse.qanaryexplanationservice.helper.pojos;

import com.wse.qanaryexplanationservice.helper.enums.GptModel;

import java.util.ArrayList;

public class GenerativeExplanationRequest {

    private ArrayList<QanaryComponent> qanaryComponents;
    private int shots;
    private GptModel gptModel;

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

    public GptModel getGptModel() {
        return gptModel;
    }

    public void setGptModel(GptModel gptModel) {
        this.gptModel = gptModel;
    }

    public String getComponentListAsRequestList() {
        return "";
    }
}
