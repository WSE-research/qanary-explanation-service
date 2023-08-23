package com.wse.qanaryexplanationservice.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wse.qanaryexplanationservice.pojos.ComponentPojo;
import com.wse.qanaryexplanationservice.pojos.ExplanationObject;
import com.wse.qanaryexplanationservice.repositories.AnnotationSparqlRepository;
import com.wse.qanaryexplanationservice.pojos.ResultObject;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.wse.qanaryexplanationservice.repositories.AnnotationSparqlRepository;
import java.io.IOException;

@Service
public class GetAnnotationsService {

    @Autowired
    AnnotationSparqlRepository annotationSparqlRepository;
    private static final String FILE_SPARQL_QUERY = "/queries/annotations_sparql_query.rq";
    private static final String COMPONENTS_SPARQL_QUERY = "/queries/components_sparql_query.rq";
    private final ObjectMapper objectMapper;
    private final Logger logger = LoggerFactory.getLogger(GetAnnotationsService.class);

    public GetAnnotationsService() {
        objectMapper = new ObjectMapper();
    }

    public String createQuery(String usedQuery, String graphID) {
        try {
            QuerySolutionMap bindingsForSparqlQuery = new QuerySolutionMap();
            bindingsForSparqlQuery.add("graphURI", ResourceFactory.createResource(graphID));

            return QanaryTripleStoreConnector.readFileFromResourcesWithMap(usedQuery, bindingsForSparqlQuery);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * @param graphID graphID to operate with
     * @return Array of ResultObjects which will be redirected to the controller which returns it to the user
     */
    public ExplanationObject[] getAnnotations(String graphID) throws IOException {
        String query = createQuery(FILE_SPARQL_QUERY, graphID);
        logger.info("Created query {}", query);
        JsonNode resultObjectsJsonNode = annotationSparqlRepository.executeSparqlQuery(query);
        logger.info("Jsonnode: {}", resultObjectsJsonNode);
        if (resultObjectsJsonNode != null)
            return mapResponseToObjectArray(resultObjectsJsonNode);
        else
            return null;
    }

    public ComponentPojo[] getUsedComponents(String graphID) throws IOException {
        String query = createQuery(COMPONENTS_SPARQL_QUERY, graphID);
        logger.info("Query: {}", query);
        JsonNode jsonNode = annotationSparqlRepository.executeSparqlQuery(query);
        logger.info("JsonNode: {}", jsonNode);
        ArrayNode resultsArrayNode = (ArrayNode) jsonNode.get("bindings");
        return objectMapper.treeToValue(resultsArrayNode, ComponentPojo[].class);
    }

    public ExplanationObject[] mapResponseToObjectArray(JsonNode sparqlResponse) {
        try {
            // Handle mapping for LocalDateTime
            objectMapper.registerModule(new JavaTimeModule());
            // select the bindings-field inside the Json(Node)
            ArrayNode resultsArraynode = (ArrayNode) sparqlResponse.get("bindings");
            return objectMapper.treeToValue(resultsArraynode, ExplanationObject[].class);
        } catch (Exception e) {
            System.out.println("Error" + e);
            return null;
        }
    }

}
