package com.wse.webservice_for_annotationsRequest.repositories;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.List;

public interface sparqlRepositoryIF<T> {

    public T[] executeSparqlQuery(String graphID) throws IOException;

}
