package com.wse.qanaryexplanationservice.helper.pojos;

import com.wse.qanaryexplanationservice.helper.GptModel;

public class GPTRequest {

    GPTRequest(boolean doGenerative, GptModel gptModel, int shots) {
        this.doGenerative = doGenerative;
        this.shots = shots;
        this.gptModel = gptModel;
    }

    private boolean doGenerative;
    private GptModel gptModel;
    private int shots;

    public GptModel getGptModel() {
        return gptModel;
    }

    public int getShots() {
        return shots;
    }

    public boolean isDoGenerative() {
        return doGenerative;
    }

    public void setGptModel(GptModel gptModel) {
        this.gptModel = gptModel;
    }

    public void setDoGenerative(boolean doGenerative) {
        this.doGenerative = doGenerative;
    }

    public void setShots(int shots) {
        this.shots = shots;
    }
}
