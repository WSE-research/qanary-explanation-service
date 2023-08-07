package com.wse.webservice_for_annotationsRequest.repositories;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wse.webservice_for_annotationsRequest.ParameterStringBuilder;
import com.wse.webservice_for_annotationsRequest.pojos.ExplanationObject;
import com.wse.webservice_for_annotationsRequest.pojos.ResultObject;
import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryQuestionTextual;
import org.apache.jena.base.Sys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;
import com.wse.webservice_for_annotationsRequest.services.explanationService;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@Repository
public class explanationSparqlRepository extends AbstractRespository{

    public explanationSparqlRepository() throws MalformedURLException {
        this.sparqlEndpoint = new URL("http://demos.swe.htwk-leipzig.de:40111/sparql"); //could be passed as paramter as well
        objectMapper = new ObjectMapper();
        webClient = WebClient.create();
    }

}
