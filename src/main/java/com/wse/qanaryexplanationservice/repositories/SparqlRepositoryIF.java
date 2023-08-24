package com.wse.qanaryexplanationservice.repositories;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

public interface SparqlRepositoryIF {

    JsonNode executeSparqlQuery(String graphURI) throws IOException;

    String fetchQuestion(String questionURI);

}
