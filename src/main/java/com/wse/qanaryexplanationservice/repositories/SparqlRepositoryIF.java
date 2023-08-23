package com.wse.qanaryexplanationservice.repositories;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

public interface SparqlRepositoryIF {

    JsonNode executeSparqlQuery(String graphID) throws IOException;

    String fetchQuestion(String questionURI);

}
