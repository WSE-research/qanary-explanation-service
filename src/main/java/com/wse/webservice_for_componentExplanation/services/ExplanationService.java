package com.wse.webservice_for_componentExplanation.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wse.webservice_for_componentExplanation.pojos.ExplanationObject;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.LiteralImpl;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import com.wse.webservice_for_componentExplanation.repositories.ExplanationSparqlRepository;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.text.DecimalFormat;

@Service
public class ExplanationService {

    private final ObjectMapper objectMapper;
    @Autowired
    private ExplanationSparqlRepository explanationSparqlRepository;

    private static final String EXPLANATION_NAMESPACE = "urn:qanary:explanations";
    private static final String RDFS_NAMESPACE = "http://www.w3.org/2000/01/rdf-schema#";

    public ExplanationService() {
        objectMapper = new ObjectMapper();
    }

    /**
     * Currently explains the DBpediaSpotlight-component since the query has the specific structure
     * @param rawQuery specific Query which is being used fetching data from triplestore (in this case dbpedia sprql query used) -> defined in Controller
     * @param graphID graphID to work with
     * @return textual explanation // TODO: change later, depending on needs
     */
    public ExplanationObject[] explainComponent(String graphUri, String rawQuery) throws IOException {

        ExplanationObject[] explanationObjects = computeExplanationObjects(graphUri, null, rawQuery);

        if (explanationObjects != null && explanationObjects.length > 0) {
            if(explanationObjects[0].getSource() != null) {
                return createEntitiesFromQuestion(explanationObjects, getQuestion(explanationObjects[0]));
            }
            else
                return explanationObjects;
        } else
            return null;
    }

    /**
     * Computes an textual explanation for a specific component on a specific graphID
     * @param graphUri specific graphURI
     * @param componentUri specific componentURI
     * @param rawQuery Used query to fetch needed information
     * @return representation as RDF Turtle
     * @throws IOException IOException
     */
    public String explainSpecificComponent(String graphUri, String componentUri, String rawQuery) throws IOException {

        ExplanationObject[] explanationObjects = computeExplanationObjects(graphUri, componentUri, rawQuery);
        String contentDe = convertToTextualExplanation(explanationObjects, "de", componentUri);
        String contentEn = convertToTextualExplanation(explanationObjects, "en", componentUri);
        return createRdfRepresentation(contentDe, contentEn, componentUri);
    }

    public ExplanationObject[] computeExplanationObjects(String graphUri, String componentUri, String rawQuery) throws IOException {
        String queryToExecute = buildSparqlQuery(graphUri, componentUri, rawQuery);
        JsonNode explanationObjectsJsonNode = explanationSparqlRepository.executeSparqlQuery(queryToExecute);
        return convertToExplanationObjects(explanationObjectsJsonNode);
    }


    /**
     *
     * @param contentDe Textual representation of the explanation in german
     * @param contentEn Textual representation of the explanation in english
     * @param componentURI component URI
     * @return String formatted as RDF-Turtle
     */
    public String createRdfRepresentation(String contentDe, String contentEn, String componentURI) {

        Model model = ModelFactory.createDefaultModel();

        // set Prefixes
        model.setNsPrefix("rdfs", RDFS_NAMESPACE);
        model.setNsPrefix("explanation", EXPLANATION_NAMESPACE);

        // Literals for triples with LanguageKey
        Literal contentDeLiteral = model.createLiteral(contentDe,"de");
        Literal contentEnLiteral = model.createLiteral(contentEn, "en");

        // Create property 'hasExplanationForCreatedDataProperty'
        Property hasExplanationForCreatedDataProperty = model.createProperty(EXPLANATION_NAMESPACE, "hasExplanationForCreatedData");
        Property rdfsSubPropertyOf = model.createProperty(RDFS_NAMESPACE, "subPropertyOf");
        Property hasExplanation = model.createProperty(EXPLANATION_NAMESPACE, "hasExplanation");

        // creates Resource, in this case the componentURI
        Resource componentUriResource = model.createResource(componentURI);

        // add triples to the model
        model.add(hasExplanationForCreatedDataProperty, rdfsSubPropertyOf, hasExplanation);
        model.add(model.createStatement(componentUriResource,hasExplanationForCreatedDataProperty, contentDeLiteral));
        model.add(model.createStatement(componentUriResource,hasExplanationForCreatedDataProperty, contentEnLiteral));

        // Write Model as Turtle to a String
        StringWriter writer = new StringWriter();
        model.write(writer, "Turtle");

        return writer.toString();
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
    public String buildSparqlQuery(String graphID, String componentUri, String rawQuery) throws IOException {
        QuerySolutionMap bindingsForSparqlQuery = new QuerySolutionMap();
        bindingsForSparqlQuery.add("graphURI", ResourceFactory.createResource(graphID));
        if(componentUri != null)    // Extension for compatibility w/ explanation for specific component
            bindingsForSparqlQuery.add("componentURI", ResourceFactory.createResource(componentUri));

        return QanaryTripleStoreConnector.readFileFromResourcesWithMap(rawQuery, bindingsForSparqlQuery);
    }

    public ExplanationObject[] convertToExplanationObjects(JsonNode explanationObjectsJsonNode) throws JsonProcessingException {
        try {
            // Handle mapping for LocalDateTime
            objectMapper.registerModule(new JavaTimeModule());
            // select the bindings-field inside the Json(Node)
            ArrayNode resultsArraynode = (ArrayNode) explanationObjectsJsonNode.get("bindings");

            return objectMapper.treeToValue(resultsArraynode, ExplanationObject[].class);
        } catch (Exception e) {
            return null;
        }
    }


    /**
     *
     * @param explanationObjects Objects gathered from previous JsonNode, contains all information
     * @param lang desired language, hard coded translation and used attributes from the objects
     * @param componentURI needed for string
     * @return textual representation for the objects
     */
    public String convertToTextualExplanation(ExplanationObject[] explanationObjects, String lang, String componentURI) {
        DecimalFormat df = new DecimalFormat("#.####");
        StringBuilder textualRepresentation = null;
        switch(lang) {
            case "de": {
                textualRepresentation = new StringBuilder("Die Komponente " + componentURI + " hat folgende Ergebnisse berechnet und dem Graphen hinzugefügt: ");
                for (ExplanationObject obj : explanationObjects
                ) {
                    textualRepresentation.append(" Zeitpunkt: '").append(obj.getCreatedAt().getValue().toString()).append("' | Konfidenz: ").append(df.format(obj.getScore().getValue() * 100)).append(" %").append(" | Inhalt: ").append(obj.getBody().getValue());
                }
                break;
            }
            case "en": {
                textualRepresentation = new StringBuilder("The component " + componentURI + " has added the following properties to the graph: ");
                for (ExplanationObject obj : explanationObjects
                ) {
                    textualRepresentation.append(" Time: '").append(obj.getCreatedAt().getValue().toString()).append("' | Confidence: ").append(df.format(obj.getScore().getValue() * 100)).append(" %").append(" | Content: ").append(obj.getBody().getValue());
                }
                break;
            }
            default: break;
        }

        return textualRepresentation.toString();
    }

}
