package com.wse.qanaryexplanationservice.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wse.qanaryexplanationservice.pojos.ResultObject;
import com.wse.qanaryexplanationservice.repositories.AnnotationSparqlRepository;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class AnnotationsService {

    private static final String FILE_SPARQL_QUERY = "/queries/annotations_sparql_query.rq";
    private static final String COMPONENTS_SPARQL_QUERY = "/queries/components_sparql_query.rq";
    private final ObjectMapper objectMapper;
    private final Logger logger = LoggerFactory.getLogger(AnnotationsService.class);
    @Autowired
    AnnotationSparqlRepository annotationSparqlRepository;

    public AnnotationsService() {
        objectMapper = new ObjectMapper();
    }


    public String createQuery(String usedQuery, String graphURI) {
        try {
            QuerySolutionMap bindingsForSparqlQuery = new QuerySolutionMap();
            bindingsForSparqlQuery.add("graphURI", ResourceFactory.createResource(graphURI));

            return QanaryTripleStoreConnector.readFileFromResourcesWithMap(usedQuery, bindingsForSparqlQuery);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * @param graphURI graphURI to operate with
     * @return Array of ResultObjects which will be redirected to the controller which returns it to the user
     */
    public ResultObject[] getAnnotations(String graphURI) throws IOException {
        String query = createQuery(FILE_SPARQL_QUERY, graphURI);
        JsonNode resultObjectsJsonNode = annotationSparqlRepository.executeSparqlQuery(query);
        if (resultObjectsJsonNode != null)
            return mapResponseToObjectArray(resultObjectsJsonNode);
        else
            return null;
    }

    public List<String> getUsedComponents(String graphID) {
        String query = createQuery(COMPONENTS_SPARQL_QUERY, graphID);
        ResultSet resultSet = annotationSparqlRepository.executeSparqlQueryWithResultSet(query);
        List<String> components = new ArrayList<>();
        while (resultSet.hasNext()) {
            components.add(resultSet.next().get("component").toString());
        }
        return components;
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
