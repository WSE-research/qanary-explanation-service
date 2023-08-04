package com.wse.webservice_for_annotationsRequest.repositories;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wse.webservice_for_annotationsRequest.ParameterStringBuilder;
import com.wse.webservice_for_annotationsRequest.pojos.ExplanationObject;
import com.wse.webservice_for_annotationsRequest.pojos.ResultObject;
import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryQuestionTextual;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import com.wse.webservice_for_annotationsRequest.services.explanationService;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@Repository
public class explanationSparqlRepository implements sparqlRepositoryIF{

    private ObjectMapper objectMapper;
    private static final URL sparqlEndpoint = new URL("http://demos.swe.htwk-leipzig.de:40111/sparql");

    public explanationSparqlRepository() {
        objectMapper = new ObjectMapper();
    }

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

        QanaryQuestion<String> qanaryQuestion = QanaryMessage.

        // read the response and store it as a JsonNode
        InputStream responseStream = connection.getInputStream();

        // get the results-field from json and save it as jsonnode
        JsonNode sparqlResponse = objectMapper.readValue(responseStream, JsonNode.class).get("results");

        return sparqlResponse;
    }
}
