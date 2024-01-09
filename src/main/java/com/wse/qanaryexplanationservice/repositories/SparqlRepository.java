package com.wse.qanaryexplanationservice.repositories;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wse.qanaryexplanationservice.services.ParameterStringBuilder;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdfconnection.RDFConnection;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Repository;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@Repository
public class SparqlRepository {


    private final ObjectMapper objectMapper = new ObjectMapper();
    @Value("${sparql.endpoint}") // TODO: Set up!
    private URL sparqlEndpoint;
    private Environment environment;

    public SparqlRepository() {

    }

    public void setSparqlEndpoint(String sparqlEndpoint) throws MalformedURLException {
        this.sparqlEndpoint = new URL(sparqlEndpoint);
    }

    public ResultSet executeSparqlQueryWithResultSet(String query) {
        RDFConnection rdfConnection = RDFConnection.connect(sparqlEndpoint.toString());
        QueryExecution queryExecution = rdfConnection.query(query);
        return queryExecution.execSelect();
    }

    public JsonNode executeSparqlQueryWithJsonNode(String query) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) sparqlEndpoint.openConnection();
        connection.setRequestMethod("GET");

        // Set up query as a parameter for the request
        Map<String, String> parameters = new HashMap<>();
        parameters.put("query", query);

        connection.setDoOutput(true);

        DataOutputStream out = new DataOutputStream(connection.getOutputStream());
        out.writeBytes(ParameterStringBuilder.getParamsString(parameters));
        out.flush();
        out.close();

        // read the response and store it as a JsonNode
        InputStream responseStream = connection.getInputStream();

        return objectMapper.readValue(responseStream, JsonNode.class);
    }

    public JSONObject executeSparqlQueryWithJsonObject(String query) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) sparqlEndpoint.openConnection();
        connection.setRequestMethod("GET");

        // Set up query as a parameter for the request
        Map<String, String> parameters = new HashMap<>();
        parameters.put("query", query);

        connection.setDoOutput(true);

        DataOutputStream out = new DataOutputStream(connection.getOutputStream());
        out.writeBytes(ParameterStringBuilder.getParamsString(parameters));
        out.flush();
        out.close();

        // read the response and store it as a JsonNode
        InputStream responseStream = connection.getInputStream();

        return objectMapper.readValue(responseStream, JSONObject.class);
    }

}
