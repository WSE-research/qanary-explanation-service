package com.wse.webservice_for_annotationsRequest.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wse.webservice_for_annotationsRequest.pojos.ResultObject;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.ResourceFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.wse.webservice_for_annotationsRequest.repositories.annotationSparqlRepository;

import java.io.IOException;

@Service
public class getAnnotationsService {

    @Autowired annotationSparqlRepository annotationSparqlRepository;
    private static final String FILE_SPARQL_QUERY = "/annotations_sparql_query.rq";
    private ObjectMapper objectMapper;
    public getAnnotationsService() {
        objectMapper = new ObjectMapper();
    }

    public String createQuery(String graphID) throws IOException {

        QuerySolutionMap bindingsForSparqlQuery = new QuerySolutionMap();
        bindingsForSparqlQuery.add("graphURI", ResourceFactory.createResource(graphID));
        String query = QanaryTripleStoreConnector.readFileFromResourcesWithMap(FILE_SPARQL_QUERY, bindingsForSparqlQuery);

        return query;
    }

    /**
     *
     * @param response JSON-Node of SPARQL-QUERY response
     * @return List of ResultObjects which will be rediredted to the controller which returns it to the user
     */
    public ResultObject[] getAnnotations(String graphID) throws IOException {

        String query = createQuery(graphID);
        JsonNode resultObjectsJsonNode = annotationSparqlRepository.executeSparqlQuery(query);

        ResultObject[] list = mapResponseToObjectArray(resultObjectsJsonNode);

        return list;
    }

    public ResultObject[] mapResponseToObjectArray(JsonNode sparqlResponse) throws IOException {
        // Handle mapping for LocalDateTime
        objectMapper.registerModule(new JavaTimeModule());
        // select the bindings-field inside the Json(Node)
        ArrayNode resultsArraynode = (ArrayNode) sparqlResponse.get("bindings");

        return objectMapper.treeToValue(resultsArraynode, ResultObject[].class);
    }






}
