package com.wse.qanaryexplanationservice.helper.pojos;

import com.wse.qanaryexplanationservice.helper.enums.GptModel;

public class GPTRequest {

    private GptModel gptModel;
    private int shots;

    GPTRequest(GptModel gptModel, int shots) {
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

    @Override
    public String toString() {
        return "Shots: " + this.shots + "\n" +
                "GPT Model: " + this.gptModel.toString();
    }
}
