package com.wse.qanaryexplanationservice.repositories;

import com.wse.qanaryexplanationservice.exceptions.GenerativeExplanationException;
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
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Repository which provides additional request methods to create generative explanations
 */
@Repository
public class GenerativeExplanationsRepository {

    private final Logger logger = LoggerFactory.getLogger(GenerativeExplanationsRepository.class);
    private final URL COMPLETIONS_ENDPOINT = URI.create("https://api.openai.com/v1/completions").toURL();
    private final URL CHAT_COMPLETIONS_ENDPOINT = URI.create("https://api.openai.com/v1/chat/completions").toURL();
    private final int RESPONSE_TOKEN = 1000;
    
    @Value("${chatgpt.api.key}")
    private String chatGptApiKey;

    private final Map<GptModel, ModelConfig> MODEL_CONFIGS = new HashMap<>() {{
        put(GptModel.GPT_3_5, new ModelConfig(COMPLETIONS_ENDPOINT, "gpt-3.5-turbo-instruct"));
        put(GptModel.GPT_3_5_16K, new ModelConfig(CHAT_COMPLETIONS_ENDPOINT, "gpt-3.5-turbo-16k"));
        put(GptModel.GPT_4, new ModelConfig(CHAT_COMPLETIONS_ENDPOINT, "gpt-4-0613"));
        put(GptModel.GPT_4_O, new ModelConfig(null, ""));
    }};

    private record ModelConfig(URL endpoint, String modelName) {}

    public GenerativeExplanationsRepository() throws MalformedURLException {
    }

    public String sendGptPrompt(String body, int tokens, GptModel gptModel) throws Exception {
        gptModel = selectGptModelBasedOnTokens(gptModel, tokens);
        ModelConfig config = MODEL_CONFIGS.get(gptModel);
        
        HttpURLConnection con = (HttpURLConnection) config.endpoint().openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        
        if (chatGptApiKey == null) {
            throw new GenerativeExplanationException("Missing ChatGPT/OpenAI API Key");
        }
        con.setRequestProperty("Authorization", "Bearer " + chatGptApiKey);

        JSONObject data = createRequest(body, gptModel);
        logger.debug("Json Request: {}", data);

        con.setDoOutput(true);
        try (var out = con.getOutputStream()) {
            out.write(data.toString().getBytes());
        }

        try (var reader = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
            String output = reader.lines()
                .reduce((a, b) -> a + b)
                .orElseThrow(() -> new GenerativeExplanationException("Empty response from GPT API"));
            return parseResponse(output, config.endpoint() == COMPLETIONS_ENDPOINT);
        }
    }

    private String parseResponse(String output, boolean isCompletionsEndpoint) {
        JSONObject response = new JSONObject(output);
        JSONObject choice = response.getJSONArray("choices").getJSONObject(0);
        return isCompletionsEndpoint ? 
            choice.getString("text") : 
            choice.getJSONObject("message").getString("content");
    }

    public GptModel selectGptModelBasedOnTokens(GptModel gptModel, int tokens) {
        return (gptModel.equals(GptModel.GPT_3_5) && tokens > (4096 - RESPONSE_TOKEN)) ? 
            GptModel.GPT_3_5_16K : gptModel;
    }

    private JSONObject createRequest(String body, GptModel gptModel) {
        ModelConfig config = MODEL_CONFIGS.get(gptModel);
        JSONObject data = new JSONObject();
        data.put("model", config.modelName());

        if (config.endpoint() == COMPLETIONS_ENDPOINT) {
            data.put("prompt", body);
            data.put("max_tokens", RESPONSE_TOKEN);
        } else {
            JSONArray messages = new JSONArray()
                .put(new JSONObject()
                    .put("role", "user")
                    .put("content", body));
            data.put("messages", messages);
        }

        return data;
    }
}
