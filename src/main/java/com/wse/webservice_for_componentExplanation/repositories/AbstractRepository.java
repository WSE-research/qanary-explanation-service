package com.wse.webservice_for_componentExplanation.repositories;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wse.webservice_for_componentExplanation.ParameterStringBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Repository
public abstract class AbstractRepository implements SparqlRepositoryIF {

    protected URL sparqlEndpoint;
    protected ObjectMapper objectMapper;
    protected WebClient webClient;

    protected Environment environment;

    @Autowired
    protected AbstractRepository(Environment environment) throws MalformedURLException {
        this.environment = environment;
        this.sparqlEndpoint = new URL(Objects.requireNonNull(this.environment.getProperty("sparqlEndpoint")));
    }

    protected void setWebClient(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * @param sparqlQuery From service returned query which already contains all relevant paramters
     * @return The Requests-Response-Body as JsonNode
     */
    @Override
    public JsonNode executeSparqlQuery(String sparqlQuery) throws IOException {

        HttpURLConnection connection = (HttpURLConnection) sparqlEndpoint.openConnection();
        connection.setRequestMethod("GET");

        // Set up query as a parameter for the request
        Map<String, String> parameters = new HashMap<>();
        parameters.put("query", sparqlQuery);

        connection.setDoOutput(true);

        DataOutputStream out = new DataOutputStream(connection.getOutputStream());
        out.writeBytes(ParameterStringBuilder.getParamsString(parameters));
        out.flush();
        out.close();


        // read the response and store it as a JsonNode
        InputStream responseStream = getInputStream(connection);

        // get the results-field from json and save it as jsonNode

        return objectMapper.readValue(responseStream, JsonNode.class).get("results");
    }

    protected InputStream getInputStream(HttpURLConnection connection) throws IOException {
        return connection.getInputStream();
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
