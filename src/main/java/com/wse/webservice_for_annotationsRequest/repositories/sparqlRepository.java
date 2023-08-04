package com.wse.webservice_for_annotationsRequest.repositories;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wse.webservice_for_annotationsRequest.ParameterStringBuilder;
import com.wse.webservice_for_annotationsRequest.pojos.ResultObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import com.wse.webservice_for_annotationsRequest.services.getAnnotationsService;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

@Repository
public class sparqlRepository implements sparqlRepositoryIF {

    URL sparqlEndpoint = new URL("http://demos.swe.htwk-leipzig.de:40111/sparql");
    @Autowired
    private getAnnotationsService getannotationsservice;
    private ObjectMapper objectMapper;

    public sparqlRepository() throws MalformedURLException {
    }

    @Override
    public ResultObject[] executeSparqlQuery(String graphID) throws IOException {

        String query = getannotationsservice.createQuery(graphID);

        objectMapper = new ObjectMapper();

        HttpURLConnection connection = (HttpURLConnection) sparqlEndpoint.openConnection();
        connection.setRequestMethod("GET");

        // Set up query as a paramter for the request
        Map<String,String> parameters = new HashMap<>();
        parameters.put("query", query);

        connection.setDoOutput(true);
        DataOutputStream out = new DataOutputStream(connection.getOutputStream());
        out.writeBytes(ParameterStringBuilder.getParamsString(parameters));
        out.flush();
        out.close();

        // read the response and store it as a JsonNode
        InputStream responseStream = connection.getInputStream();

        // get the results-field from json and save it as jsonnode
        JsonNode sparqlResponse = objectMapper.readValue(responseStream, JsonNode.class).get("results");

        return getannotationsservice.somewhat(sparqlResponse);
    }

}
