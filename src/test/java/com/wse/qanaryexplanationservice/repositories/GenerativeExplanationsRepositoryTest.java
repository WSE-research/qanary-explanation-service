package com.wse.qanaryexplanationservice.repositories;

import com.wse.qanaryexplanationservice.helper.GptModel;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class GenerativeExplanationsRepositoryTest {

    private final GenerativeExplanationsRepository generativeExplanationsRepository = new GenerativeExplanationsRepository();

    public GenerativeExplanationsRepositoryTest() throws MalformedURLException {
    }

    @Test
    public void selectGptModelBasedOnTokensGpt_3_5_4K() {
        GptModel gptModel = GptModel.GPT_3_5;
        int tokens = 2890;
        GptModel resultModel = generativeExplanationsRepository.selectGptModelBasedOnTokens(gptModel, tokens);
        assertEquals(gptModel, resultModel);
    }

    @Test
    public void selectGptModelBasedOnTokensGpt_3_5_16K() {
        GptModel gptModel = GptModel.GPT_3_5;
        int tokens = 3204;
        GptModel resultModel = generativeExplanationsRepository.selectGptModelBasedOnTokens(gptModel, tokens);
        assertNotEquals(gptModel, resultModel);
    }

    @Test
    public void selectGptModelBasedOnTokensGpt_3_5EdgeCase() {
        GptModel gptModel = GptModel.GPT_3_5;
        int tokens = 3096;
        GptModel resultModel = generativeExplanationsRepository.selectGptModelBasedOnTokens(gptModel, tokens);
        assertEquals(gptModel, resultModel);
    }

}
