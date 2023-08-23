package com.wse.qanaryexplanationservice.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wse.qanaryexplanationservice.pojos.ComponentPojo;
import com.wse.qanaryexplanationservice.pojos.ExplanationObject;
import com.wse.qanaryexplanationservice.repositories.ExplanationSparqlRepository;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Service
public class ExplanationService {

    private static final String QUESTION_QUERY = "/queries/question_query.rq";
    private static final Map<String, String> headerFormatMap = new HashMap<>() {{
        put("application/rdf+xml", "RDFXML");
        put("application/ld+json", "JSONLD");
    }};
    final String EXPLANATION_NAMESPACE = "urn:qanary:explanations";
    private final ObjectMapper objectMapper;
    Logger logger = LoggerFactory.getLogger(ExplanationService.class);
    @Autowired
    private ExplanationSparqlRepository explanationSparqlRepository;
    @Autowired
    private GetAnnotationsService getAnnotationsService;

    public ExplanationService() {
        objectMapper = new ObjectMapper();
    }

    /**
     * Currently explains the DBpediaSpotlight-component since the query has the specific structure
     *
     * @param rawQuery specific Query which is being used fetching data from triplestore (in this case dbpedia sprql query used) -> defined in Controller
     * @param graphUri graphID to work with
     * @return textual explanation
     */

    public ExplanationObject[] explainComponent(String graphUri, String rawQuery) throws IOException {

        ExplanationObject[] explanationObjects = computeExplanationObjects(graphUri, null, rawQuery);

        if (explanationObjects != null && explanationObjects.length > 0) {
            if (explanationObjects[0].getSource() != null) {
                return createEntitiesFromQuestion(explanationObjects, getQuestion(explanationObjects[0]));
            } else
                return explanationObjects;
        } else
            return null;
    }

    /**
     * Computes a textual explanation for a specific component on a specific graphID
     *
     * @param graphUri     specific graphURI
     * @param componentUri specific componentURI
     * @param rawQuery     Used query to fetch needed information
     * @return representation as RDF Turtle
     * @throws IOException IOException
     */
    public String explainSpecificComponent(String graphUri, String componentUri, String rawQuery, String header) throws Exception {
        logger.info("Header: {}", header);
        ExplanationObject[] explanationObjects = computeExplanationObjects(graphUri, componentUri, rawQuery);
        String contentDe = convertToTextualExplanation(explanationObjects, "de", componentUri);
        String contentEn = convertToTextualExplanation(explanationObjects, "en", componentUri);

        return convertToDesiredFormat(header, createRdfRepresentation(contentDe, contentEn, componentUri));
    }

    //Overloaded explainSpecificComponent-method for further use
    public Model explainSpecificComponent(String graphUri, String componentUri, String rawQuery) throws IOException {
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

    public ExplanationObject[] explainComponentDBpediaSpotlight(String graphID, String rawQuery) throws IOException {
        ExplanationObject[] explanationObjects = getExplanationObjects(graphID, rawQuery);
        String question;
        if (explanationObjects != null && explanationObjects.length > 0) {
            question = getQuestion(explanationObjects[0]); // question uri is saved in every single Object, just take the first one
            return createEntitiesFromQuestion(explanationObjects, question);
        } else
            return null;
    }

    /**
     * @param contentDe    Textual representation of the explanation in german
     * @param contentEn    Textual representation of the explanation in english
     * @param componentURI component URI
     * @return String formatted in either RDFXML, JSONLD or Turtle, depending on Accept-Header
     */
    public Model createRdfRepresentation(String contentDe, String contentEn, String componentURI) throws IOException {

        Model model = ModelFactory.createDefaultModel();

        // set Prefixes
        model.setNsPrefix("rdfs", RDFS.getURI());
        model.setNsPrefix("explanation", EXPLANATION_NAMESPACE);

        // Literals for triples with LanguageKey
        Literal contentDeLiteral = model.createLiteral(contentDe, "de");
        Literal contentEnLiteral = model.createLiteral(contentEn, "en");

        // Create property 'hasExplanationForCreatedDataProperty'
        Property hasExplanationForCreatedDataProperty = model.createProperty(EXPLANATION_NAMESPACE, "hasExplanationForCreatedData");
        Property rdfsSubPropertyOf = model.createProperty(RDFS.getURI(), "subPropertyOf");
        Property hasExplanation = model.createProperty(EXPLANATION_NAMESPACE, "hasExplanation");

        // creates Resource, in this case the componentURI
        Resource componentUriResource = model.createResource(componentURI);

        // add triples to the model
        model.add(hasExplanationForCreatedDataProperty, rdfsSubPropertyOf, hasExplanation);
        model.add(model.createStatement(componentUriResource, hasExplanationForCreatedDataProperty, contentDeLiteral));
        model.add(model.createStatement(componentUriResource, hasExplanationForCreatedDataProperty, contentEnLiteral));

        return model;
    }

    /**
     * Converts model to desired format (RDFXML, Turtle, JSONLD) depending on header
     *
     * @param header Accept-header
     * @param model  Model which contains Statements
     * @return String in desired output format
     */
    public String convertToDesiredFormat(String header, Model model) {
        StringWriter writer = new StringWriter();

        model.write(writer, headerFormatMap.getOrDefault(header, "TURTLE"));
        return writer.toString();
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
        String query = buildSparqlQuery(graphID, null, rawQuery);
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

    public String buildSparqlQuery(String graphID, String componentUri, String rawQuery) throws IOException {

        QuerySolutionMap bindingsForSparqlQuery = new QuerySolutionMap();
        bindingsForSparqlQuery.add("graphURI", ResourceFactory.createResource(graphID));
        if (componentUri != null)    // Extension for compatibility w/ explanation for specific component
            bindingsForSparqlQuery.add("componentURI", ResourceFactory.createResource(componentUri));

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
            logger.info("ArrayNode: {}", resultsArraynode);
            return objectMapper.treeToValue(resultsArraynode, ExplanationObject[].class);
        } catch (Exception e) {
            return null;
        }
    }


    /**
     * @param explanationObjects Objects gathered from previous JsonNode, contains all information
     * @param lang               desired language, hard coded translation and used attributes from the objects
     * @param componentURI       needed for string
     * @return textual representation for the objects
     */
    public String convertToTextualExplanation(ExplanationObject[] explanationObjects, String lang, String componentURI) {
        DecimalFormat df = new DecimalFormat("#.####");
        StringBuilder textualRepresentation = null;
        switch (lang) {
            case "de" -> {
                textualRepresentation = new StringBuilder("Die Komponente " + componentURI + " hat folgende Ergebnisse berechnet und dem Graphen hinzugefügt: ");
                for (ExplanationObject obj : explanationObjects
                ) {
                    textualRepresentation.append(" Zeitpunkt: '").append(obj.getCreatedAt().getValue().toString()).append("' | Konfidenz: ").append(df.format(obj.getScore().getValue() * 100)).append(" %").append(" | Inhalt: ").append(obj.getBody().getValue());
                }
            }
            case "en" -> {
                textualRepresentation = new StringBuilder("The component " + componentURI + " has added the following properties to the graph: ");
                for (ExplanationObject obj : explanationObjects
                ) {
                    textualRepresentation.append(" Time: '").append(obj.getCreatedAt().getValue().toString()).append("' | Confidence: ").append(df.format(obj.getScore().getValue() * 100)).append(" %").append(" | Content: ").append(obj.getBody().getValue());
                }
            }
            default -> {
            }
        }
        return textualRepresentation.toString().replaceAll("\n", " ").replaceAll("\\\\", "a");
    }

    /**
     * Explains a qa-system in the following steps:
     * 1. find out which components were involved
     * 2. create an explanation for every involved component
     * 3. create a rdf model which describes that
     *
     * @param graphId the only paramter given for a qa-system
     */
    public String explainQaSystem(String graphId, String specificComponentQuery, String header) throws Exception {

        ComponentPojo[] components = getAnnotationsService.getUsedComponents(graphId);
        Map<String, Model> models = new HashMap<>();
        for (ComponentPojo component : components
        ) {
            models.put(component.getComponent().getValue(),
                    explainSpecificComponent(graphId, component.getComponent().getValue(), specificComponentQuery));
        }

        String questionURI = fetchQuestionUri(graphId);

        Model systemExplanationModel = createSystemModel(models, components, questionURI, graphId);

        return convertToDesiredFormat(header, systemExplanationModel);
    }

    public String fetchQuestionUri(String graphId) throws Exception {
        String query = buildSparqlQuery(graphId, null, QUESTION_QUERY);

        JsonNode jsonNode = explanationSparqlRepository.executeSparqlQuery(query);

        try {
            String question = (jsonNode.get("bindings").get(0).get("source").get("value").asText());
            logger.info("QuestionURI = {}", question);
            return question;
        } catch (Exception e) {
            throw new Exception("Couldn't fetch the question!", e);
        }

    }

    public Model createSystemModel(Map<String, Model> models, ComponentPojo[] components, String question, String graphId) {

        Model systemExplanationModel = ModelFactory.createDefaultModel();

        // PREFIXES
        String wasProcessedInGraphString = "urn:qanary:wasProcessedInGraph";
        String wasProcessedByString = "urn:qanary:wasProcessedBy";
        // Set namespaces
        systemExplanationModel.setNsPrefix("rdfs", RDFS.getURI());
        systemExplanationModel.setNsPrefix("rdf", RDF.getURI());
        systemExplanationModel.setNsPrefix("explanation", EXPLANATION_NAMESPACE);
        // Set properties
        Property wasProcessedInGraph = systemExplanationModel.createProperty(wasProcessedInGraphString);
        Property wasProcessedBy = systemExplanationModel.createProperty(wasProcessedByString);
        // Set resources
        Resource questionResource = systemExplanationModel.createResource(question);
        Resource graphResource = systemExplanationModel.createResource(graphId);
        Resource sequence = systemExplanationModel.createResource(); // equals the outer sequence(s)
        // questionResource is the reference resource
        Property rdfType = systemExplanationModel.createProperty(RDF.getURI() + "type");
        questionResource.addProperty(wasProcessedInGraph, graphResource);
        questionResource.addProperty(wasProcessedBy, sequence);
        sequence.addProperty(RDF.type, RDF.Seq);

        // Iterates over Models with componentURI as key
        // for every model an inner sequence is created and the statements from the model are transformed to reified statements (to save them as a "resource" in a Sequence)
        for (int i = 0; i < components.length; i++) {
            int j = 1;
            Model model = models.get(components[i].getComponent().getValue()); // get the model for the component at position "i" in component list // remember: models is Map with componentUri as key

            // creating inner Sequence for the reified statements
            Resource innerSequence = systemExplanationModel.createResource();
            // adding the inner Sequence as a property to the outer sequence / the resource questionResource
            innerSequence.addProperty(rdfType, RDF.Seq);
            sequence.addProperty(RDF.li(i + 1), innerSequence);
            Iterator<Statement> itr = model.listStatements();
            // iterate over the statements in the model which contains any statements for the current component
            while (itr.hasNext()) {
                ReifiedStatement reifiedStatement = systemExplanationModel.createReifiedStatement(itr.next());
                innerSequence.addProperty(RDF.li(j), reifiedStatement);
                j++;
            }
        }
        // Logging the created model as TURTLE-String
        StringWriter stringWriter = new StringWriter();
        systemExplanationModel.write(stringWriter, "TURTLE");
        logger.info("Created Turtle: {}", stringWriter);

        return systemExplanationModel;
    }

}
