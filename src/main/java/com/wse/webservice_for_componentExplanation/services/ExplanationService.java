package com.wse.webservice_for_componentExplanation.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wse.webservice_for_componentExplanation.pojos.ComponentPojo;
import com.wse.webservice_for_componentExplanation.pojos.ExplanationObject;
import com.wse.webservice_for_componentExplanation.repositories.AnnotationSparqlRepository;
import com.wse.webservice_for_componentExplanation.repositories.ExplanationSparqlRepository;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.ModelCom;
import org.apache.jena.rdf.model.impl.SeqImpl;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.util.*;

import static com.complexible.stardog.plan.filter.functions.LeviathanFunctions.e;

@Service
public class ExplanationService {

    private final ObjectMapper objectMapper;
    Logger logger = LoggerFactory.getLogger(ExplanationService.class);
    @Autowired
    private ExplanationSparqlRepository explanationSparqlRepository;
    @Autowired
    private GetAnnotationsService getAnnotationsService;
    private static final String QUESTION_QUERY = "/queries/question_query.rq";

    private static final Map<String,String> headerFormatMap = new HashMap<>() {{
        put("application/rdf+xml","RDFXML");
        put("application/ld+json","JSONLD");
    }};

    public ExplanationService() {
        objectMapper = new ObjectMapper();
    }

    /**
     * Currently explains the DBpediaSpotlight-component since the query has the specific structure
     *
     * @param rawQuery specific Query which is being used fetching data from triplestore (in this case dbpedia sprql query used) -> defined in Controller
     * @param graphUri graphID to work with
     * @return textual explanation // TODO: change later, depending on needs
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
     * Computes an textual explanation for a specific component on a specific graphID
     *
     * @param graphUri     specific graphURI
     * @param componentUri specific componentURI
     * @param rawQuery     Used query to fetch needed information
     * @return representation as RDF Turtle
     * @throws IOException IOException
     */
    public String explainSpecificComponent(String graphUri, String componentUri, String rawQuery, String header) throws IOException {
        logger.info("Header: {}", header);
        ExplanationObject[] explanationObjects = computeExplanationObjects(graphUri, componentUri, rawQuery);
        String contentDe = convertToTextualExplanation(explanationObjects, "de", componentUri);
        String contentEn = convertToTextualExplanation(explanationObjects, "en", componentUri);

        //String resultExplanation = createRdfRepresentation(contentDe, contentEn, componentUri, header);
        String resultExplanation = convertToDesiredFormat(header,createRdfRepresentation(contentDe,contentEn,componentUri));

        return resultExplanation;
    }

    //Overloaded explainSpecificComponent-method for further use
    public Model explainSpecificComponent(String graphUri, String componentUri, String rawQuery) throws IOException {
        ExplanationObject[] explanationObjects = computeExplanationObjects(graphUri, componentUri, rawQuery);
        String contentDe = convertToTextualExplanation(explanationObjects, "de", componentUri);
        String contentEn = convertToTextualExplanation(explanationObjects, "en", componentUri);

        return createRdfRepresentation(contentDe,contentEn,componentUri);
    }

    public ExplanationObject[] computeExplanationObjects(String graphUri, String componentUri, String rawQuery) throws IOException {
        String queryToExecute = buildSparqlQuery(graphUri, componentUri, rawQuery);
        JsonNode explanationObjectsJsonNode = explanationSparqlRepository.executeSparqlQuery(queryToExecute);
        return convertToExplanationObjects(explanationObjectsJsonNode);
    }

    /**
     * @param contentDe    Textual representation of the explanation in german
     * @param contentEn    Textual representation of the explanation in english
     * @param componentURI component URI
     * @return String formatted as RDF-Turtle
     */
    public Model createRdfRepresentation(String contentDe, String contentEn, String componentURI) throws IOException {

        final String EXPLANATION_NAMESPACE = "urn:qanary:explanations";
        final String RDFS_NAMESPACE = "http://www.w3.org/2000/01/rdf-schema#";

        Model model = ModelFactory.createDefaultModel();

        // set Prefixes
        model.setNsPrefix("rdfs", RDFS_NAMESPACE);
        model.setNsPrefix("explanation", EXPLANATION_NAMESPACE);

        // Literals for triples with LanguageKey
        Literal contentDeLiteral = model.createLiteral(contentDe, "de");
        Literal contentEnLiteral = model.createLiteral(contentEn, "en");

        // Create property 'hasExplanationForCreatedDataProperty'
        Property hasExplanationForCreatedDataProperty = model.createProperty(EXPLANATION_NAMESPACE, "hasExplanationForCreatedData");
        Property rdfsSubPropertyOf = model.createProperty(RDFS_NAMESPACE, "subPropertyOf");
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
     * Converts model to desired format (RDFXML, Turtle,
     *
     * @param header accept header
     * @param model  Model whicht contains created triples
     * @return String in desired output format
     */
    public String convertToDesiredFormat(String header, Model model) {
        StringWriter writer = new StringWriter();

        model.write(writer, headerFormatMap.getOrDefault(header, "TURTLE"));
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
        if (componentUri != null)    // Extension for compatibility w/ explanation for specific component
            bindingsForSparqlQuery.add("componentURI", ResourceFactory.createResource(componentUri));

        return QanaryTripleStoreConnector.readFileFromResourcesWithMap(rawQuery, bindingsForSparqlQuery);
    }

    /**
     * Convert JsonNode from the SPARQL-Query execution to a Array of ExplanationObject-objects
     * @param explanationObjectsJsonNode
     * @return
     * @throws JsonProcessingException
     */
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
     * @param explanationObjects Objects gathered from previous JsonNode, contains all information
     * @param lang               desired language, hard coded translation and used attributes from the objects
     * @param componentURI       needed for string
     * @return textual representation for the objects
     */
    public String convertToTextualExplanation(ExplanationObject[] explanationObjects, String lang, String componentURI) {
        DecimalFormat df = new DecimalFormat("#.####");
        StringBuilder textualRepresentation = null;
        switch (lang) {
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
            default:
                break;
        }
        return textualRepresentation.toString().replaceAll("\n", " ").replaceAll("\\\\", "a");
    }

    /**
     * Explains a qa-system in the following steps:
     * 1. find out which components were involved
     * 2. create a explanation for every involved component
     * 3. create a rdf model which describes that
     * @param graphId the only paramter given for a qa-system
     */
    public String explainQaSystem(String graphId, String specificComponentQuery, String header) throws Exception {

        // TODO: Different approach:
        // - fetching components,
        // - execute coded methods for component+graph request to get their output throughout the process
        // - create content / explanation for specific component
        // - depending on the 2nd: create model for system or reformat the given rdf/xml/turtle -> overload existing function

        ComponentPojo[] components = getAnnotationsService.getUsedComponents(graphId);
        Map<String,Model> models1 = new HashMap<>();
        for (ComponentPojo component: components
             ) {
            models1.put(component.getComponent().getValue(),
                    explainSpecificComponent(graphId,component.getComponent().getValue(),specificComponentQuery));
        }

        String questionURI = fetchQuestionUri(graphId);

        Model systemExplanationModel = ModelFactory.createDefaultModel();
        systemExplanationModel = createSystemModel(models1, components, questionURI, graphId);

        return convertToDesiredFormat(header, systemExplanationModel);
    }

    public String fetchQuestionUri(String graphId) throws Exception {
        String query = buildSparqlQuery(graphId, null, QUESTION_QUERY);

        JsonNode jsonNode = explanationSparqlRepository.executeSparqlQuery(query);

        if(jsonNode == null)
            throw new Exception();
        else {
            String question = (jsonNode.get("bindings").get(0).get("source").get("value").asText());
            logger.info("QuestionURI = {}", question);
            return question;
        }

    }

    public Model createSystemModel(Map<String, Model> models, ComponentPojo[] components, String question, String graphId) throws IOException {

        Model systemExplanationModel = ModelFactory.createDefaultModel();

        // PREFIXES TODO: Refactor to the top
        final String EXPLANATION_NAMESPACE = "urn:qanary:explanations";
        final String wasProcessedInGraphString = "urn:qanary:wasProcessedInGraph";
        final String wasProcessedByString = "urn:qanary:wasProcessedBy";
        final String questionUri = (String) question;


        // Set namespaces // TODO: Not working correctly until now
        systemExplanationModel.setNsPrefix("rdfs", RDFS.getURI());
        systemExplanationModel.setNsPrefix("rdf", RDF.getURI());
        systemExplanationModel.setNsPrefix("explanation", EXPLANATION_NAMESPACE);

        // Set properties
        Property wasProcessedInGraph = systemExplanationModel.createProperty(wasProcessedInGraphString);
        Property wasProcessedBy = systemExplanationModel.createProperty(wasProcessedByString);

        // Set resources
        Resource questionResource = systemExplanationModel.createResource(questionUri);
        Resource graphResource = systemExplanationModel.createResource(graphId);
        Resource sequence = systemExplanationModel.createResource();

        // questionResource is the reference resource
        Property rdfType = systemExplanationModel.createProperty(RDF.getURI() + "type");
      //  questionResource.addProperty(rdfType, RDF.Seq);
        questionResource.addProperty(wasProcessedInGraph, graphResource);
        questionResource.addProperty(wasProcessedBy, sequence);
        sequence.addProperty(RDF.type, RDF.Seq);

        for(int i = 0; i < components.length; i++) {
            int j = i;
            Model model = models.get(components[i].getComponent().getValue()); // get the model for the component at position "i" in component list // remember: models is Map with componentUri as key
            Iterator<Statement> itr = model.listStatements();
            // creating inner Sequence for the reified statements
            Resource innerSequence = systemExplanationModel.createResource();
            innerSequence.addProperty(rdfType, RDF.Seq);    // doesn't need to be a Sequence, order is not relevant here (?)
            // adding the inner Sequence as a property to the outer sequence / the resource questionResource
            sequence.addProperty(RDF.li(i+1),innerSequence);
            // iterate over the statements in the model which contains any triples for the current component
            while(itr.hasNext()) {
                ReifiedStatement reifiedStatement = systemExplanationModel.createReifiedStatement(itr.next());
                innerSequence.addProperty(RDF.li(j), reifiedStatement);
                j++;
            }
        }

        StringWriter stringWriter = new StringWriter();
        systemExplanationModel.write(stringWriter,"TURTLE");
        logger.info("Created Turtle: {}", stringWriter);

        return systemExplanationModel;
    }


    /**
     *  Create models (Models contain triples::Statement)
     *  One model represents all explanation for one component, furthermore every explanation is one triple (we have de+eng, means 2 triples for one
     *  semantic equal explanation)
     * @param groupedMap Contains key-value pairs with the key representing the componentURI for the value which represents ExplanationObjects which
     *                   rely to the (in the key) specified component
     * @return List of models::Model
     */
    public List<Model> getModelsFromMap(Map<String,List<ExplanationObject>> groupedMap) {
        List<Model> models = new ArrayList<>();
        groupedMap.forEach((k,v) -> { // creates models and inside creating the german as well as the english explanation
            try {
                models.add(createRdfRepresentation(convertToTextualExplanation(v.toArray(new ExplanationObject[0]),"de",k), convertToTextualExplanation(v.toArray(new ExplanationObject[0]),"en",k),k));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        return models;
    }

}
