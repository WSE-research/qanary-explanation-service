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
     *
     * @param graphID graphID to work with
     * @return textual explanation // TODO: change lates, depending on needs
     * @throws IOException
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

    /**
     *
     * @param explanationObjects list of ExplanationObjects to iterate through
     * @param question given raw question
     * @return modified list with entities set
     */
    public ExplanationObject[] createEntitiesFromQuestion(ExplanationObject[] explanationObjects, String question) {
        ExplanationObject[] tmp = explanationObjects;
        for (ExplanationObject obj: tmp
             ) {
            obj.setEntity(getEntity(obj,question));
        }
        return tmp;
    }

    /**
     *
     * @param obj Specific object for which the entity is to be found
     * @param question the raw question-string
     * @return the entity inside the given question
     */
    public String getEntity(ExplanationObject obj, String question) {
        String entity = question.substring(obj.getStart().getValue(), obj.getEnd().getValue());
        return entity;
    }

    /**
     *
     * @param firstObject takes the first object of the list to get the Question URI (any item in the list would work)
     * @return question as raw string
     */
    public String getQuestion(ExplanationObject firstObject) {
        return explanationSparqlRepository.fetchQuestion(firstObject.getSource().getValue().toString());
    }

    /**
     *
     * @param graphID given graphID
     * @return query with params set (graphURI)
     * @throws IOException
     */
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
        // As of implementation for several different components, the list could be sorted by component-name
        // Filter for component could happen in the sparql query
        String response = "Für die Komponente DBpediaSpotlightNED wurden folgende Annotationen mit folgenden Konfidenzen und DBpedia Quellen herausgefiltert: ";
        DecimalFormat df = new DecimalFormat("#.####");

        for (ExplanationObject obj: explanationObjects
             ) {
            response += ("\n " +
                    "Entität: '" + obj.getEntity() +
                    "' | Konfidenz: " + df.format(obj.getScore().getValue()*100) + " %" +
                    " | DBPedia URI: " + obj.getBody().getValue());
        }
        return response;
    }

}
