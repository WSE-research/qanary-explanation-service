package com.wse.webservice_for_annotationsRequest.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wse.webservice_for_annotationsRequest.pojos.ExplanationObject;
import com.wse.webservice_for_annotationsRequest.pojos.ResultObject;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.ResourceFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.wse.webservice_for_annotationsRequest.repositories.explanationSparqlRepository;

import java.io.IOException;

public class explanationService {

    private static final String FILE_SPARQL_QUERY = "/explanation_sparql_query.rq";
    private ObjectMapper objectMapper;
    @Autowired
    private explanationSparqlRepository explanationSparqlRepository;
    public explanationService() {
        objectMapper = new ObjectMapper();
    }

    /**
     * - filter items and return textual message
     * @param annotations :: ResultObject
     * @throws IOException
     */
    public String filterResults(ResultObject[] annotations) throws IOException {


    }

    /**
     * - get specific component-entries (how many different component may occur in that List depends on how many annotations fulfill the SPAQRL Query =>
     * set up the sparql-Query? !!!
     * @param graphID
     */
    public String explainComponent(String graphID) throws IOException {

        String query = buildSparqlQuery(graphID);
        JsonNode explanationObjectsJsonNode = explanationSparqlRepository.executeSparqlQuery(query); // already selected results-fields

        String question = getQuestion(explanationObjectsJsonNode);
        ExplanationObject[] explanationObjects = convertToExplanationObjects(explanationObjectsJsonNode);

        if(explanationObjects.length > 0)
            return convertToTextualExplanation(explanationObjects);
        else
            return "Es gibt leider keine Annotationen!";

    }

    public String getQuestion(JsonNode response) {
        return "";
    }

    public String buildSparqlQuery(String graphID) throws IOException {
        QuerySolutionMap bindingsForSparqlQuery = new QuerySolutionMap();
        bindingsForSparqlQuery.add("graphURI", ResourceFactory.createResource(graphID));

        String query = QanaryTripleStoreConnector.readFileFromResourcesWithMap(FILE_SPARQL_QUERY, bindingsForSparqlQuery);

        return query;
    }

    public ExplanationObject[] convertToExplanationObjects(JsonNode explanationObjectsJsonNode) throws JsonProcessingException {
        // Handle mapping for LocalDateTime
        objectMapper.registerModule(new JavaTimeModule());
        // select the bindings-field inside the Json(Node)
        ArrayNode resultsArraynode = (ArrayNode) explanationObjectsJsonNode.get("bindings");

        return objectMapper.treeToValue(resultsArraynode, ExplanationObject[].class);
    }

    public String convertToTextualExplanation(ExplanationObject[] explanationObjects) {
        return "";
    }

}
