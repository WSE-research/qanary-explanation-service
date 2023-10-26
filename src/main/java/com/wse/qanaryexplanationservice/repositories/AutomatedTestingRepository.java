package com.wse.qanaryexplanationservice.repositories;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wse.qanaryexplanationservice.pojos.automatedTestingObject.QanaryRequestObject;
import com.wse.qanaryexplanationservice.pojos.automatedTestingObject.QanaryResponseObject;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdfconnection.RDFConnection;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Repository;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

@Repository
public class AutomatedTestingRepository extends AbstractRepository {

    private final URL QANARY_ENDPOINT;
    private final URL CHATGPT_ENDPOINT = new URL("https://api.openai.com/v1/completions"); // TODO:
    private final String chatGptApiKey = "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"; // TODO: put in applications.settings
    private final int RESPONSE_TOKEN = 500;

    private Logger logger = LoggerFactory.getLogger(AutomatedTestingRepository.class);

    public AutomatedTestingRepository(Environment environment) throws MalformedURLException {
        super(environment);
        QANARY_ENDPOINT = new URL("http://195.90.200.248:8090/startquestionansweringwithtextquestion");
        this.objectMapper = new ObjectMapper();
        this.webClient = WebClient.create();
    }

    public QanaryResponseObject executeQanaryPipeline(QanaryRequestObject qanaryRequestObject) throws IOException {

        logger.info("Execute Qanary Pipeline");

        MultiValueMap<String, String> multiValueMap = new LinkedMultiValueMap();
        multiValueMap.add("question", qanaryRequestObject.getQuestion());

        for (String component : qanaryRequestObject.getComponentlist()
        ) {
            multiValueMap.add("componentlist[]", component);
        }

        QanaryResponseObject responseObject = webClient.post().uri(uriBuilder -> uriBuilder
                        .scheme("http").host("195.90.200.248").port(8090).path("/startquestionansweringwithtextquestion")
                        .queryParams(multiValueMap)
                        .build())
                .retrieve().
                bodyToMono(QanaryResponseObject.class).
                block();

        logger.info("Response Object: {}", responseObject);

        return responseObject;
    }

    public ResultSet takeRandomQuestion(String query) {
        RDFConnection rdfConnection1 = RDFConnection.connect("http://localhost:8890/sparql");
        QueryExecution queryExecution = rdfConnection1.query(query);
        return queryExecution.execSelect();

    }

    // Variable as object
    public String sendGptPrompt(String body, int tokens) throws URISyntaxException, IOException {

        HttpURLConnection con = (HttpURLConnection) CHATGPT_ENDPOINT.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Authorization", "Bearer " + chatGptApiKey);

        boolean isTokenLessThan4k = tokens < (4096 - RESPONSE_TOKEN);
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