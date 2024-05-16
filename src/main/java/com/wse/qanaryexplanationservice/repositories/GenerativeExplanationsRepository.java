package com.wse.qanaryexplanationservice.repositories;

import com.wse.qanaryexplanationservice.helper.GptModel;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Repository which provides additional request methods to create generative explanations
 */
@Repository
public class GenerativeExplanationsRepository {


    private final Logger logger = LoggerFactory.getLogger(GenerativeExplanationsRepository.class);
    private final URL COMPLETIONS_ENDPOINT = new URL("https://api.openai.com/v1/completions");
    private final URL CHAT_COMPLETIONS_ENDPOINT = new URL("https://api.openai.com/v1/chat/completions");
    private final int RESPONSE_TOKEN = 1000;
    @Value("${chatgpt.api.key}")
    private String chatGptApiKey;
    private final Map<GptModel, URL> GPT_MODEL_ENDPOINT = new HashMap<>() {{
        put(GptModel.GPT_3_5, COMPLETIONS_ENDPOINT);
        put(GptModel.GPT_3_5_16K, CHAT_COMPLETIONS_ENDPOINT);
        put(GptModel.GPT_4, CHAT_COMPLETIONS_ENDPOINT);
        put(GptModel.GPT_4_O, null);
    }};

    private final Map<GptModel, String> GPT_CONCRETE_MODEL = new HashMap<>() {{
        put(GptModel.GPT_3_5, "gpt-3.5-turbo-instruct");
        put(GptModel.GPT_3_5_16K, "gpt-3.5-turbo-16k");
        put(GptModel.GPT_4, "gpt-4-0613");
        put(GptModel.GPT_4_O, "");
    }};

    public GenerativeExplanationsRepository() throws MalformedURLException {
    }

    public String sendGptPrompt(String body, int tokens, GptModel gptModel) throws Exception {

        gptModel = selectGptModelBasedOnTokens(gptModel, tokens);

        HttpURLConnection con = (HttpURLConnection) GPT_MODEL_ENDPOINT.get(gptModel).openConnection();

        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        if (this.chatGptApiKey != null)
            con.setRequestProperty("Authorization", "Bearer " + chatGptApiKey);
        else
            throw new Exception("Missing ChatGPT/OpenAI API Key");

        JSONObject data = (GPT_MODEL_ENDPOINT.get(gptModel) == COMPLETIONS_ENDPOINT) ?
                createRequestForCompletions(body, gptModel) : createRequestForChatCompletions(body,gptModel);

        logger.info("Json Request: {}", data);

        con.setDoOutput(true);
        con.getOutputStream().write(data.toString().getBytes());


        String output = new BufferedReader(new InputStreamReader(con.getInputStream())).lines()
                .reduce((a, b) -> a + b).get();

        return GPT_MODEL_ENDPOINT.get(gptModel) == COMPLETIONS_ENDPOINT ? // gptModel ist GPT_3_5 and therefore the 4K token model
                new JSONObject(output).getJSONArray("choices").getJSONObject(0).getString("text")
                :
                new JSONObject(output).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
    }

    public GptModel selectGptModelBasedOnTokens(GptModel gptModel, int tokens) {
        return (gptModel.equals(GptModel.GPT_3_5) && tokens > (4096 - RESPONSE_TOKEN)) ? GptModel.GPT_3_5_16K : gptModel;
    }

    public JSONObject createRequestForCompletions(String body, GptModel gptModel) {
        JSONObject data = new JSONObject();
        data.put("model", GPT_CONCRETE_MODEL.get(gptModel));
        data.put("prompt", body);
        data.put("max_tokens", RESPONSE_TOKEN);

        return data;
    }

    public JSONObject createRequestForChatCompletions(String body, GptModel gptModel) {
        JSONObject data = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        JSONObject arrayEntity = new JSONObject();
        arrayEntity.put("role", "user");
        arrayEntity.put("content", body);
        jsonArray.put(arrayEntity);
        data.put("model", GPT_CONCRETE_MODEL.get(gptModel));
        data.put("messages", jsonArray);

        return data;
    }

}
