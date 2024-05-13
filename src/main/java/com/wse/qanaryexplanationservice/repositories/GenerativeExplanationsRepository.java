package com.wse.qanaryexplanationservice.repositories;

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

/**
 * Repository which provides additional request methods to create generative explanations
 */
@Repository
public class GenerativeExplanationsRepository {


    private Logger logger = LoggerFactory.getLogger(GenerativeExplanationsRepository.class);
    private final URL CHATGPT_ENDPOINT_4K = new URL("https://api.openai.com/v1/completions");
    private final URL CHATGPT_ENDPOINT_16K = new URL("https://api.openai.com/v1/chat/completions");
    private final int RESPONSE_TOKEN = 1000;
    @Value("${chatgpt.api.key}")
    private String chatGptApiKey;

    public GenerativeExplanationsRepository() throws MalformedURLException {
    }

    public String sendGptPrompt(String body, int tokens) throws Exception {

        boolean isTokenLessThan4k = tokens < (4096 - RESPONSE_TOKEN);
        HttpURLConnection con = isTokenLessThan4k ? (HttpURLConnection) CHATGPT_ENDPOINT_4K.openConnection() : (HttpURLConnection) CHATGPT_ENDPOINT_16K.openConnection();

        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        if (this.chatGptApiKey != null)
            con.setRequestProperty("Authorization", "Bearer " + chatGptApiKey);
        else
            throw new Exception("Missing API Key");

        JSONObject data = isTokenLessThan4k ? createRequestFor4kModel(body) : createRequestFor16kModel(body);

        logger.info("Json Request: {}", data);

        con.setDoOutput(true);
        con.getOutputStream().write(data.toString().getBytes());


        String output = new BufferedReader(new InputStreamReader(con.getInputStream())).lines()
                .reduce((a, b) -> a + b).get();


        return isTokenLessThan4k ?
                new JSONObject(output).getJSONArray("choices").getJSONObject(0).getString("text")
                :
                new JSONObject(output).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
    }

    public JSONObject createRequestFor4kModel(String body) {
        JSONObject data = new JSONObject();
        data.put("model", "gpt-3.5-turbo-instruct"); // TODO:
        data.put("prompt", body);
        data.put("max_tokens", RESPONSE_TOKEN);

        return data;
    }

    public JSONObject createRequestFor16kModel(String body) {
        JSONObject data = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        JSONObject arrayEntity = new JSONObject();
        arrayEntity.put("role", "user");
        arrayEntity.put("content", body);
        jsonArray.put(arrayEntity);
        data.put("model", "gpt-3.5-turbo-16k");
        data.put("messages", jsonArray);

        return data;
    }

}
