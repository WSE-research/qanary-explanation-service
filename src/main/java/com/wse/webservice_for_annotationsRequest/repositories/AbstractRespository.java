package com.wse.webservice_for_annotationsRequest.repositories;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wse.webservice_for_annotationsRequest.ParameterStringBuilder;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class AbstractRespository implements sparqlRepositoryIF{

    protected URL sparqlEndpoint;
    protected ObjectMapper objectMapper;
    protected WebClient webClient;

    @Override
    public JsonNode executeSparqlQuery(String sparqlQuery) throws IOException {

        HttpURLConnection connection = (HttpURLConnection) sparqlEndpoint.openConnection();
        connection.setRequestMethod("GET");

        // Set up query as a paramter for the request
        Map<String,String> parameters = new HashMap<>();
        parameters.put("query", sparqlQuery);

        connection.setDoOutput(true);
        DataOutputStream out = new DataOutputStream(connection.getOutputStream());
        out.writeBytes(ParameterStringBuilder.getParamsString(parameters));
        out.flush();
        out.close();

        // read the response and store it as a JsonNode
        InputStream responseStream = connection.getInputStream();

        // get the results-field from json and save it as jsonnode
        JsonNode sparqlResponse = objectMapper.readValue(responseStream, JsonNode.class).get("results");
        return sparqlResponse;
    }

    @Override
    public String fetchQuestion(String questionURI) {
        return webClient
                .get()
                .uri(questionURI + "/raw") // raw question-string
                .accept(MediaType.TEXT_PLAIN)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}
