package com.wse.qanaryexplanationservice.helper.pojos;

import com.wse.qanaryexplanationservice.helper.GptModel;

public class GPTRequest {

    private boolean doGenerative;
    private GptModel gptModel;
    private int shots;
    GPTRequest(boolean doGenerative, GptModel gptModel, int shots) {
        this.doGenerative = doGenerative;
        this.shots = shots;
        this.gptModel = gptModel;
    }

    public GptModel getGptModel() {
        return gptModel;
    }

    public void setGptModel(GptModel gptModel) {
        this.gptModel = gptModel;
    }

    public int getShots() {
        return shots;
    }

    public void setShots(int shots) {
        this.shots = shots;
    }

    public boolean isDoGenerative() {
        return doGenerative;
    }

    public void setDoGenerative(boolean doGenerative) {
        this.doGenerative = doGenerative;
    }

    @Override
    public String toString() {
        return "Do Generative: " + this.doGenerative + "\n" +
                "Shots: " + this.shots + "\n" +
                "GPT Model: " + this.gptModel.toString();
    }
}
