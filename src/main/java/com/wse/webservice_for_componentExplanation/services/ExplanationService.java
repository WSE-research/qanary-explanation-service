package com.wse.webservice_for_componentExplanation.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wse.webservice_for_componentExplanation.pojos.ExplanationObject;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.ResourceFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.wse.webservice_for_componentExplanation.repositories.ExplanationSparqlRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Arrays;

@Service
public class ExplanationService {

    // private static final String FILE_SPARQL_QUERY = "/queries/explanation_for_dbpediaSpotlight_sparql_query.rq";
    private final ObjectMapper objectMapper;
    @Autowired
    private ExplanationSparqlRepository explanationSparqlRepository;

    public ExplanationService() {
        objectMapper = new ObjectMapper();
    }

    /**
     * @param graphID graphID to work with
     * @return textual explanation // TODO: change later, depending on needs
     */
    public ExplanationObject[] explainComponentDBpediaSpotlight(String graphID, String rawQuery) throws IOException {

        ExplanationObject[] explanationObjects = getExplanationObjects(graphID, rawQuery);
        String question;

        if (explanationObjects != null && explanationObjects.length > 0) {
            question = getQuestion(explanationObjects[0]); // question uri is saved in every single Object, just take the first one
            return createEntitiesFromQuestion(explanationObjects, question);
        } else
            return null;

    }

    public String explainQueryBuilder(String graphID, String rawQuery) throws IOException {
        ExplanationObject[] explanationObjects = getExplanationObjects(graphID, rawQuery);

        // Restriction to QueryBuilder
        String qb = "QB";

        // filter Explanationobjects for objects with annotations made by query builder
        explanationObjects = Arrays.stream(explanationObjects).filter(x -> x.getCreatedBy().getValue().contains(qb)).toArray(ExplanationObject[]::new);

        // create the explanation
        // adds the sparql-queries if there are any to add, else return null
        if (explanationObjects.length > 0) {
            StringBuilder explanation = new StringBuilder("The component created the following SPARQL queries: '");
            for (ExplanationObject object : explanationObjects
            ) {
                explanation.append(object.getBody().getValue()).append("'\n");
            }
            return explanation.toString();
        } else
            return null;
    }

    public ExplanationObject[] getExplanationObjects(String graphID, String rawQuery) throws IOException {
        // Get annotation properties with explanation_for_dbpediaSpotlight_sparql_query.rq query
        String query = buildSparqlQuery(graphID, rawQuery);
        JsonNode explanationObjectsJsonNode = explanationSparqlRepository.executeSparqlQuery(query); // already selected results-fields

        return convertToExplanationObjects(explanationObjectsJsonNode);
    }

    /**
     * @param explanationObjects list of ExplanationObjects to iterate through
     * @param question           given raw question
     * @return modified list with entities set
     */
    public ExplanationObject[] createEntitiesFromQuestion(ExplanationObject[] explanationObjects, String question) {
        for (ExplanationObject obj : explanationObjects
        ) {
            obj.setEntity(getEntity(obj, question));
        }
        return explanationObjects;
    }

    /**
     * @param obj      Specific object for which the entity is to be found
     * @param question the raw question-string
     * @return the entity inside the given question
     */
    public String getEntity(ExplanationObject obj, String question) {
        return question.substring(obj.getStart().getValue(), obj.getEnd().getValue());
    }

    /**
     * @param firstObject takes the first object of the list to get the Question URI (any item in the list would work)
     * @return question as raw string
     */
    public String getQuestion(ExplanationObject firstObject) {
        return explanationSparqlRepository.fetchQuestion(firstObject.getSource().getValue());
    }

    /**
     * @param graphID given graphID
     * @return query with params set (graphURI)
     */
    public String buildSparqlQuery(String graphID, String rawQuery) throws IOException {
        QuerySolutionMap bindingsForSparqlQuery = new QuerySolutionMap();
        bindingsForSparqlQuery.add("graphURI", ResourceFactory.createResource(graphID));

        return QanaryTripleStoreConnector.readFileFromResourcesWithMap(rawQuery, bindingsForSparqlQuery);
    }

    /**
     * converts a JsonNode into an ArrayNode which contains the objects properties as a Array and converts there into an Array of ExplanationObject objects
     *
     * @param explanationObjectsJsonNode JSON Node with explanationObject properties
     * @return Array of ExplanationObject objects
     * @throws JsonProcessingException
     */
    public ExplanationObject[] convertToExplanationObjects(JsonNode explanationObjectsJsonNode) throws JsonProcessingException {
        try {
            // Handle mapping for LocalDateTime
            objectMapper.registerModule(new JavaTimeModule());
            // select the bindings-field inside the Json(Node)
            ArrayNode resultsArraynode = (ArrayNode) explanationObjectsJsonNode.get("bindings");
            ExplanationObject[] explanationObjects = objectMapper.treeToValue(resultsArraynode, ExplanationObject[].class);
            return explanationObjects;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * not needed now since that happens at client level
     *
     * @param explanationObjects objects which will be shown
     * @return textual representation as string
     */
    public String convertToTextualExplanation(ExplanationObject[] explanationObjects) {
        // As of implementation for several different components, the list could be sorted by component-name
        // Filter for component could happen in the sparql query
        StringBuilder response = new StringBuilder("There are following information regarding the entity, its confidence and the dbpedia URI for the given graphID on the DBpedia-Spotlight-NED component:  ");
        DecimalFormat df = new DecimalFormat("#.####");

        for (ExplanationObject obj : explanationObjects
        ) {
            response.append("\n " + "Entity: '").append(obj.getEntity()).append("' | Confidence: ").append(df.format(obj.getScore().getValue() * 100)).append(" %").append(" | DBPedia URI: ").append(obj.getBody().getValue());
        }
        return response.toString();
    }

}
