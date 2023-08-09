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

import com.wse.webservice_for_annotationsRequest.repositories.AnnotationSparqlRepository;

import java.io.IOException;

@Service
public class GetAnnotationsService {

    @Autowired
    AnnotationSparqlRepository annotationSparqlRepository;
    private static final String FILE_SPARQL_QUERY = "/queries/annotations_sparql_query.rq";
    private final ObjectMapper objectMapper;

    public GetAnnotationsService() {
        objectMapper = new ObjectMapper();
    }

    public String createQuery(String graphID) throws IOException {
        try {
            QuerySolutionMap bindingsForSparqlQuery = new QuerySolutionMap();
            bindingsForSparqlQuery.add("graphURI", ResourceFactory.createResource(graphID));

            return QanaryTripleStoreConnector.readFileFromResourcesWithMap(FILE_SPARQL_QUERY, bindingsForSparqlQuery);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * @param graphID graphID to operate with
     * @return Array of ResultObjects which will be redirected to the controller which returns it to the user
     */
    public ResultObject[] getAnnotations(String graphID) throws IOException {
        String query = createQuery(graphID);
        JsonNode resultObjectsJsonNode = annotationSparqlRepository.executeSparqlQuery(query);

        if (resultObjectsJsonNode != null)
            return mapResponseToObjectArray(resultObjectsJsonNode);
        else
            return null;
    }

    public ResultObject[] mapResponseToObjectArray(JsonNode sparqlResponse) {
        try {
            // Handle mapping for LocalDateTime
            objectMapper.registerModule(new JavaTimeModule());
            // select the bindings-field inside the Json(Node)
            ArrayNode resultsArraynode = (ArrayNode) sparqlResponse.get("bindings");
            return objectMapper.treeToValue(resultsArraynode, ResultObject[].class);
        } catch (Exception e) {
            System.out.println("Error" + e);
            return null;
        }
    }

}
