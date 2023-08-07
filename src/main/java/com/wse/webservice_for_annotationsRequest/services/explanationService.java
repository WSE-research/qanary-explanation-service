package com.wse.webservice_for_annotationsRequest.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wse.webservice_for_annotationsRequest.pojos.ExplanationObject;
import com.wse.webservice_for_annotationsRequest.pojos.ResultObject;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import org.apache.jena.base.Sys;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.ResourceFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.wse.webservice_for_annotationsRequest.repositories.explanationSparqlRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.DecimalFormat;

@Service
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

        return null;
    }

    /**
     * - get specific component-entries (how many different component may occur in that List depends on how many annotations fulfill the SPAQRL Query =>
     * set up the sparql-Query? !!!
     * @param graphID
     */
    public String explainComponent(String graphID) throws IOException {

        String query = buildSparqlQuery(graphID);
        JsonNode explanationObjectsJsonNode = explanationSparqlRepository.executeSparqlQuery(query); // already selected results-fields

        ExplanationObject[] explanationObjects = convertToExplanationObjects(explanationObjectsJsonNode);
        String question;

        if(explanationObjects.length > 0) {
            question = getQuestion(explanationObjects[0]); // question uri is saved in every single Object, just take the first one
            createEntitiesFromQuestion(explanationObjects, question);
            return convertToTextualExplanation(explanationObjects);

        }
        else
            return "Es gibt leider keine Annotationen!";

    }

    public ExplanationObject[] createEntitiesFromQuestion(ExplanationObject[] explanationObjects, String question) {
        ExplanationObject[] tmp = explanationObjects;
        for (ExplanationObject obj: tmp
             ) {
            obj.setEntity(getEntity(obj,question));
        }
        return tmp;
    }

    public String getEntity(ExplanationObject obj, String question) {
        String entity = question.substring(obj.getStart().getValue(), obj.getEnd().getValue());
        return entity;
    }

    //Probably edit, that isn't really smart working with a random object (??)
    public String getQuestion(ExplanationObject firstObject) {
        return explanationSparqlRepository.fetchQuestion(firstObject.getSource().getValue().toString());
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
        String response = "Für die Komponente DBedia-Show wurden folgende Annotationen mit folgenden Konfidenzen herausgefiltert: ";
        DecimalFormat df = new DecimalFormat("#.####");

        for (ExplanationObject obj: explanationObjects
             ) {
            response += ("\n " +
                    "Entität: " + obj.getEntity() +
                    " | Konfidenz: " + df.format(obj.getScore().getValue()*100) + "%" +
                    " | DBPedia URI: " + obj.getBody().getValue()) + " %";
        }
        System.out.println(response);
        return response;
    }

}
