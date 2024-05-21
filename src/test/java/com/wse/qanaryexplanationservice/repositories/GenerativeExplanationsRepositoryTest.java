package com.wse.qanaryexplanationservice.repositories;

import com.wse.qanaryexplanationservice.QanaryExplanationServiceApplication;
import com.wse.qanaryexplanationservice.helper.GptModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;

import java.net.MalformedURLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class GenerativeExplanationsRepositoryTest {

    private GenerativeExplanationsRepository generativeExplanationsRepository = new GenerativeExplanationsRepository();

    public GenerativeExplanationsRepositoryTest() throws MalformedURLException {
    }

    @Test
    public void selectGptModelBasedOnTokensGpt_3_5_4K() {
        GptModel gptModel = GptModel.GPT_3_5;
        int tokens = 2890;
        GptModel resultModel = generativeExplanationsRepository.selectGptModelBasedOnTokens(gptModel,tokens);
        assertEquals(gptModel, resultModel);
    }

    @Test
    public void selectGptModelBasedOnTokensGpt_3_5_16K() {
        GptModel gptModel = GptModel.GPT_3_5;
        int tokens = 3204;
        GptModel resultModel = generativeExplanationsRepository.selectGptModelBasedOnTokens(gptModel,tokens);
        assertNotEquals(gptModel, resultModel);
    }

    @Test
    public void selectGptModelBasedOnTokensGpt_3_5EdgeCase() {
        GptModel gptModel = GptModel.GPT_3_5;
        int tokens = 3096;
        GptModel resultModel = generativeExplanationsRepository.selectGptModelBasedOnTokens(gptModel,tokens);
        assertEquals(gptModel, resultModel);
    }

}
