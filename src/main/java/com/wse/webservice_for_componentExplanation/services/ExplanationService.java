package com.wse.webservice_for_componentExplanation.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wse.webservice_for_componentExplanation.pojos.ExplanationObject;
import com.wse.webservice_for_componentExplanation.repositories.ExplanationSparqlRepository;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.util.Arrays;

@Service
public class ExplanationService {

    private final ObjectMapper objectMapper;
    Logger logger = LoggerFactory.getLogger(ExplanationService.class);
    @Autowired
    private ExplanationSparqlRepository explanationSparqlRepository;

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

        String resultExplanation = createRdfRepresentation(contentDe, contentEn, componentUri, header);

        return resultExplanation;
    }

    public ExplanationObject[] computeExplanationObjects(String graphUri, String componentUri, String rawQuery) throws IOException {
        String queryToExecute = buildSparqlQuery(graphUri, componentUri, rawQuery);
        JsonNode explanationObjectsJsonNode = explanationSparqlRepository.executeSparqlQuery(queryToExecute);
        return convertToExplanationObjects(explanationObjectsJsonNode);
    }

    /* Approach with rdf4j
    public String createRDFWithrdf4j(String turtle) throws IOException {

        ModelBuilder builder = new ModelBuilder();
        ValueFactory factory = SimpleValueFactory.getInstance();
        //org.eclipse.rdf4j.model.Model model = new DynamicModelFactory().createEmptyModel();
        org.eclipse.rdf4j.model.Model model;

        // set namespaces
        builder.setNamespace("rdfs", RDFS_NAMESPACE);
        builder.setNamespace("explanation",EXPLANATION_NAMESPACE);

        // set IRIs
        IRI component = factory.createIRI(componentURI);
        IRI hasExplanationForCreatedData = factory.createIRI("explanation:hasExplanationForCreatedData");

        // set literals
        org.eclipse.rdf4j.model.Literal contentDeLiteral = factory.createLiteral(contentDe, "de");
        org.eclipse.rdf4j.model.Literal contentEnLiteral = factory.createLiteral(contentEn, "en");

       // model.add(component,hasExplanationForCreatedData,contentDeLiteral);
       // model.add(component,hasExplanationForCreatedData,contentEnLiteral);

        builder.subject(component)
                        .add(hasExplanationForCreatedData,contentDeLiteral)
                                .add(hasExplanationForCreatedData, contentEnLiteral);

        model = builder.build();
        FileOutputStream out = new FileOutputStream("rdfFile.ttl");
        Rio.write(model, out, RDFFormat.TURTLE);


        // create Parser to read the turtle file
        StringReader in = new StringReader(turtle);
        // String is passed in Turtle format
        RDFParser rdfParser = Rio.createParser(RDFFormat.TURTLE);
        RDFWriter rdfWriter = Rio.createWriter(RDFFormat.RDFXML, new FileWriter("rdf.rdf"));

        rdfParser.setRDFHandler(rdfWriter);
        try{
            rdfParser.parse(in,"");
        }catch (RDFHandlerException e) {
            logger.info("Something didnt work out");
        }
        return "";

    }
    */

    /**
     * @param contentDe    Textual representation of the explanation in german
     * @param contentEn    Textual representation of the explanation in english
     * @param componentURI component URI
     * @return String formatted as RDF-Turtle
     */
    public String createRdfRepresentation(String contentDe, String contentEn, String componentURI, String header) throws IOException {

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

        return convertToDesiredFormat(header, model);
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
            ExplanationObject[] explanationObjects = objectMapper.treeToValue(resultsArraynode, ExplanationObject[].class);
            return explanationObjects;
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


}
