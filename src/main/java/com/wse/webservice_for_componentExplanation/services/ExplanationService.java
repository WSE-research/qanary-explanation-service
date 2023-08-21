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
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.util.*;

@Service
public class ExplanationService {

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

        // if no header is provided, return as text/turtle
        if (header == null) {
            model.write(writer, "TURTLE");
            return writer.toString();
        }

        switch (header) {
            case "application/rdf+xml": {
                model.write(writer, "RDFXML");
                return writer.toString();
            }
            case "text/turtle": {
                model.write(writer, "TURTLE");
                return writer.toString();
            }
            case "application/ld+json": {
                model.write(writer, "JSONLD");
                return writer.toString();
            }
            default: {
                logger.warn("Not supported Type in Accept Header");
                return null;
            }
        }
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
    public void explainQaSystem(String graphId, String specificComponentQuery) throws IOException {
        // Get involved components with request of any made annotations
        // returns us ExplanationObject[], with all required properties to craft explanation

        /*
        // Step 1: get involved components
        ExplanationObject[] explanationObjects = getAnnotationsService.getAnnotations(graphId);

        // Step 2: create models on these

        // convert Array to List for further processing
        List<ExplanationObject> explanationObjectList = Arrays.asList(explanationObjects);
        logger.info("Map: {}",explanationObjectList.toString());

        // group explanationObjects by componentURI as Map
        // Key represents the componentURI, Value the List of ExplanationObject(s) // TODO: Convert in an Resource, IRI, URI?
        Map<String, List<ExplanationObject>> groupedMap = new HashMap<>();
        explanationObjectList.forEach(item -> {
            if(groupedMap.containsKey(item.getCreatedBy().getValue())) {
                groupedMap.get(item.getCreatedBy().getValue()).add(item);
            } else {
                List<ExplanationObject> explanationObjectsSpecific = new ArrayList<>();
                groupedMap.put(item.getCreatedBy().getValue(),explanationObjectsSpecific);
            }
        });

        // process the components and their content to a explanation and create Models of that
        List<Model> models = getModelsFromMap(groupedMap);
        */

        // TODO: sort out the unnecessary triples or: dont repeat them (done automatically by Parser?? Like its checking s,p,o isn't it?)

        // TODO: create final Model which can then be returned or post-processed to the desired format (RDFXML,Turtle,JSONLD)

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
        logger.info("Models: {}", models1);

        // create new model

        Model systemExplanationModel = ModelFactory.createDefaultModel();
        systemExplanationModel = createSystemModel(models1, components);



    }

    public Model createSystemModel(Map<String, Model> models, ComponentPojo[] components) throws IOException {

        Model systemExplanationModel = ModelFactory.createDefaultModel();

        /*
        // Create sequences
        ArrayList<Seq> sequences = new ArrayList<>();
        for (ComponentPojo component: components
             ) {
            Seq sequence = systemExplanationModel.createSeq(component.getComponent().getValue());
            Model componentModel = models.get(component.getComponent().getValue());
            Iterator<Statement> statementsItr = componentModel.listStatements();
            // add items from the spec. model to it
            // get correct model
            // add all triples here
            while(statementsItr.hasNext()) {
                Statement statement = statementsItr.next();
           //     logger.info("Statement: {}", statement);
                sequence.add(statement);
            }
         //   logger.info("Sequence: {}", sequence);
            sequences.add(sequence);
        }
        */

        // PREFIXES TODO: Refactor to the top
        final String URN_NAMESPACE = "urn";
        final String QANARY_NAMESPACE = "qanary";
        final String EXPLANATION_NAMESPACE = "urn:qanary:explanations";
        final String RDFS_NAMESPACE = "http://www.w3.org/2000/01/rdf-schema#";

        // Set namespaces
        systemExplanationModel.setNsPrefix("rdfs", RDFS_NAMESPACE);
        systemExplanationModel.setNsPrefix("rdf", RDFS_NAMESPACE);
        systemExplanationModel.setNsPrefix("urn", URN_NAMESPACE);
        systemExplanationModel.setNsPrefix("qanary", QANARY_NAMESPACE);
        systemExplanationModel.setNsPrefix("explanation", EXPLANATION_NAMESPACE);

        // Set properties
        Property wasProcessedInGraph = systemExplanationModel.createProperty(URN_NAMESPACE+QANARY_NAMESPACE, "wasProcessedInGraph");
        Property wasProcessedBy = systemExplanationModel.createProperty(URN_NAMESPACE+QANARY_NAMESPACE,"wasProcessedBy");

        // Set resources
        Resource questionResource = systemExplanationModel.createResource("TODO_QuestionURI");
        Resource graphResource = systemExplanationModel.createResource("TODO_GraphURI");

        // add Statement
        systemExplanationModel.add(systemExplanationModel.createStatement(questionResource,wasProcessedInGraph,graphResource));
        systemExplanationModel.add(questionResource, RDF.type, RDF.Seq);

        for (int i = 0; i <= components.length; i++) {
            Model model = models.get(components[i].getComponent().getValue());
            Iterator<Statement> itr = model.listStatements();

            while(itr.hasNext()) {
                questionResource.addProperty(
                        RDF.li(i),
                        systemExplanationModel.createReifiedStatement(components[i].getComponent().getValue(), itr.next())
                        );
            }
        }

        // Add items to the sequence
       // questionResource.addProperty(RDF.li(1),/*hier das RDF NODE*/reifiedStatement);





        FileWriter fileWriter = new FileWriter("output.rdf");
        systemExplanationModel.write(fileWriter,"Turtle");

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
