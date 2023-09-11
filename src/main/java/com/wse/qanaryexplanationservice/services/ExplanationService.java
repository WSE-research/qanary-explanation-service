package com.wse.qanaryexplanationservice.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wse.qanaryexplanationservice.pojos.ComponentPojo;
import com.wse.qanaryexplanationservice.pojos.ExplanationObject;
import com.wse.qanaryexplanationservice.repositories.ExplanationSparqlRepository;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ExplanationService {

    // Query files
    private static final String QUESTION_QUERY = "/queries/question_query.rq";
    private static final String ANNOTATIONS_QUERY = "/queries/queries_for_annotation_types/fetch_all_annotation_types.rq";
    // Mappings
    private static final Map<String, String> headerFormatMap = new HashMap<>() {{
        put("application/rdf+xml", "RDFXML");
        put("application/ld+json", "JSONLD");
        put("text/turtle", "TURTLE");
    }};
    private static final Map<String, String> annotationsTypeAndQuery = new HashMap<>() {{
        // AnnotationOfInstance
        put("annotationofspotinstance", "/queries/queries_for_annotation_types/annotations_of_spot_intance_query.rq");
    }};

    // Holds the annotationtype with path
    private static final Map<String, String> annotationTypeExplanationTemplate = new HashMap<>() {{
        put("annotationofspotinstance", "/explanations/annotation_of_instance/");
    }};
    final String EXPLANATION_NAMESPACE = "urn:qanary:explanations#";
    private final ObjectMapper objectMapper;
    Logger logger = LoggerFactory.getLogger(ExplanationService.class);
    private Map<String, ResultSet> stringResultSetMap = new HashMap<>();
    @Autowired
    private ExplanationSparqlRepository explanationSparqlRepository;
    @Autowired
    private AnnotationsService annotationsService;

    public ExplanationService() {
        objectMapper = new ObjectMapper();
    }

    /**
     * Computes a textual explanation for a specific component on a specific graphURI
     *
     * @param graphUri     specific graphURI
     * @param componentUri specific componentURI
     * @return explanation as RDF Turtle
     */
    public String explainSpecificComponent(String graphUri, String componentUri, String header) throws Exception {
        logger.info("Passed header: {}", header);
        Model model = createModel(graphUri, componentUri);

        return convertToDesiredFormat(header, model);
    }

    // Returns a model for a specific component
    public Model createModel(String graphUri, String componentUri) throws Exception {

        String contentde = createTextualExplanation(graphUri, componentUri, "de");
        String contenten = createTextualExplanation(graphUri, componentUri, "en");

        return createModelForSpecificComponent(contentde, contenten, componentUri);
    }

    // Creating the query, executing it and transform the response to an array of ExplanationObject objects
    public ExplanationObject[] computeExplanationObjects(String graphUri, String componentUri, String fetchQueryEmpty) throws IOException {
        String queryToExecute = buildSparqlQuery(graphUri, componentUri, fetchQueryEmpty);
        JsonNode explanationObjectsJsonNode = explanationSparqlRepository.executeSparqlQuery(queryToExecute);
        return convertToExplanationObjects(explanationObjectsJsonNode);
    }


    /**
     * Creating the specific query, execute and transform response to array of ExplanationObject objects
     *
     * @param graphURI        Given graphURI
     * @param fetchQueryEmpty specific query which will be executed against the triplestore
     * @return Array of ExplanationObject objects or null if there are none
     */
    public ExplanationObject[] explainComponentDBpediaSpotlight(String graphURI, String fetchQueryEmpty) throws IOException {
        ExplanationObject[] explanationObjects = computeExplanationObjects(graphURI, null, fetchQueryEmpty);
        String question;
        if (explanationObjects != null && explanationObjects.length > 0) {
            question = getQuestion(explanationObjects[0]); // question uri is saved in every single Object, just take the first one
            return createEntitiesFromQuestion(explanationObjects, question);
        } else
            return null;
    }

    /**
     * Creates an explanation model for a specific componentURI. Further it can be formatted e.g.
     * as RDF-XML, JSONLD, Turtle
     *
     * @param contentDe    Textual representation of the explanation in german
     * @param contentEn    Textual representation of the explanation in english
     * @param componentURI componentURI
     * @return Model with explanations as statements
     */
    public Model createModelForSpecificComponent(String contentDe, String contentEn, String componentURI) {

        Model model = ModelFactory.createDefaultModel();

        // set Prefixes
        model.setNsPrefix("rdfs", RDFS.getURI());
        model.setNsPrefix("explanations", EXPLANATION_NAMESPACE);

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
     * Converts model to desired format (RDF-XML, Turtle, JSONLD) (depending on Accept-header)
     *
     * @param header Accept-header
     * @param model  Model which contains Statements
     * @return formatted String
     */
    public String convertToDesiredFormat(String header, Model model) {
        StringWriter writer = new StringWriter();

        model.write(writer, headerFormatMap.getOrDefault(header, "TURTLE"));
        return writer.toString();
    }


    // INFO: May be removed since there's a different approach for something like this
    // e.g. pass the restriction as a parameter to the query
    public String explainQueryBuilder(String graphURI, String rawQuery) throws IOException {
        ExplanationObject[] explanationObjects = computeExplanationObjects(graphURI, null, rawQuery);

        // Restriction to QueryBuilder
        String qb = "QB";

        // filter ExplanationObjects for objects with annotations made by query builder
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

    // Computes the entities as real Entities
    public ExplanationObject[] createEntitiesFromQuestion(ExplanationObject[] explanationObjects, String question) {
        for (ExplanationObject obj : explanationObjects
        ) {
            obj.setEntity(getEntity(obj, question));
        }
        return explanationObjects;
    }

    // get real entity from question with start- and end-value
    public String getEntity(ExplanationObject obj, String question) {
        return question.substring(obj.getStart().getValue(), obj.getEnd().getValue());
    }

    // get raw question from question source
    public String getQuestion(ExplanationObject firstObject) {
        return explanationSparqlRepository.fetchQuestion(firstObject.getSource().getValue());
    }

    // building request-query with passed attributes added to rawQuery
    public String buildSparqlQuery(String graphURI, String componentUri, String rawQuery) throws IOException {

        QuerySolutionMap bindingsForSparqlQuery = new QuerySolutionMap();
        bindingsForSparqlQuery.add("graphURI", ResourceFactory.createResource(graphURI));
        if (componentUri != null)    // Extension for compatibility w/ explanation for specific component
            bindingsForSparqlQuery.add("componentURI", ResourceFactory.createResource(componentUri));

        return QanaryTripleStoreConnector.readFileFromResourcesWithMap(rawQuery, bindingsForSparqlQuery);

    }

    // converts JsonNode to array of ExplanationObject
    public ExplanationObject[] convertToExplanationObjects(JsonNode explanationObjectsJsonNode) {
        try {
            // Handle mapping for LocalDateTime
            objectMapper.registerModule(new JavaTimeModule());
            // select the bindings-field inside the Json(Node)
            ArrayNode resultsArraynode = (ArrayNode) explanationObjectsJsonNode.get("bindings");
            logger.info("ArrayNode: {}", resultsArraynode);
            return objectMapper.treeToValue(resultsArraynode, ExplanationObject[].class);
        } catch (Exception e) {
            logger.error("Error while converting JsonNode to Objects");
            return null;
        }
    }

    /**
     * Converts all explanations for one component to one explicit textual explanation
     *
     * @param lang         desired language, hard coded translation and used attributes from the objects
     * @param componentURI needed for string
     * @return textual explanation for the objects
     */
    public String convertToTextualExplanation(ExplanationObject[] explanationObjects, String lang, String componentURI) throws Exception {
        DecimalFormat df = new DecimalFormat("#.####");
        StringBuilder textualRepresentation;
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
                String error = "Error while converting to textual Explanation, used default branch";
                logger.error("{}", error);
                throw new Exception(error);
            }
        }
        return textualRepresentation.toString().replaceAll("\n", " ").replaceAll("\\\\", "a");
    }

    /**
     * Explains a qa-system in the following steps:
     * 1. fetch involved components
     * 2. create an explanation for every involved component
     * 3. create an explanation model
     *
     * @param graphURI the only parameter given for a qa-system
     */
    public String explainQaSystem(String graphURI, String header) throws Exception {

        ComponentPojo[] components = annotationsService.getUsedComponents(graphURI);
        Map<String, Model> models = new HashMap<>();
        for (ComponentPojo component : components
        ) {
            models.put(component.getComponent().getValue(),
                    createModel(graphURI, component.getComponent().getValue()));
        }

        String questionURI = fetchQuestionUri(graphURI);
        Model systemExplanationModel = createSystemModel(models, components, questionURI, graphURI);

        return convertToDesiredFormat(header, systemExplanationModel);
    }

    // get the origin questionURI for a graphURI
    public String fetchQuestionUri(String graphURI) throws Exception {
        String query = buildSparqlQuery(graphURI, null, QUESTION_QUERY);

        JsonNode jsonNode = explanationSparqlRepository.executeSparqlQuery(query);

        try {
            String question = (jsonNode.get("bindings").get(0).get("source").get("value").asText());
            logger.info("QuestionURI = {}", question);
            return question;
        } catch (Exception e) {
            throw new Exception("Couldn't fetch the question!", e);
        }

    }

    /**
     * Creates an explanation model for a system explanation
     *
     * @param models     a map with the componentURI and its Model
     * @param components Array of involved components
     * @return an explanation model for a system explanation
     */
    public Model createSystemModel(Map<String, Model> models, ComponentPojo[] components, String questionURI, String graphURI) {

        Model systemExplanationModel = ModelFactory.createDefaultModel();

        // PREFIXES
        String wasProcessedInGraphString = "urn:qanary:wasProcessedInGraph";
        String wasProcessedByString = "urn:qanary:wasProcessedBy";
        // Set namespaces
        systemExplanationModel.setNsPrefix("rdfs", RDFS.getURI());
        systemExplanationModel.setNsPrefix("rdf", RDF.getURI());
        systemExplanationModel.setNsPrefix("explanations", EXPLANATION_NAMESPACE);
        // Set properties
        Property wasProcessedInGraph = systemExplanationModel.createProperty(wasProcessedInGraphString);
        Property wasProcessedBy = systemExplanationModel.createProperty(wasProcessedByString);
        // Set resources
        Resource questionResource = systemExplanationModel.createResource(questionURI);
        Resource graphResource = systemExplanationModel.createResource(graphURI);
        Resource sequence = systemExplanationModel.createResource(); // equals the outer sequence(s)
        // questionResource is the reference resource
        Property rdfType = systemExplanationModel.createProperty(RDF.getURI() + "type");
        questionResource.addProperty(wasProcessedInGraph, graphResource);
        questionResource.addProperty(wasProcessedBy, sequence);
        sequence.addProperty(RDF.type, RDF.Seq);

        // Iterates over Models with componentURI as key
        // for every model an inner sequence is created and the statements from the model are transformed to reified statements
        // (to save them as a "resource" in a Sequence)
        for (int i = 0; i < components.length; i++) {
            int j = 1;
            // get the model for the component at position "i" in component list // remember: models is Map with componentUri as key
            Model model = models.get(components[i].getComponent().getValue());
            // creating inner Sequence for the reified statements
            Resource innerSequence = systemExplanationModel.createResource();
            // adding the inner Sequence as a property to the outer sequence / the resource questionResource
            innerSequence.addProperty(rdfType, RDF.Seq);
            sequence.addProperty(RDF.li(i + 1), innerSequence);
            Iterator<Statement> itr = model.listStatements();
            // iterate over the statements in the model which contains all statements for the current component
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

    public List<String> createComponentExplanation(String graphURI, String componentURI, String lang) throws IOException {

        List<String> types = new ArrayList<>();
        if (stringResultSetMap.isEmpty())
            types = fetchAllAnnotation(graphURI, componentURI);

        return createSpecificExplanations(
                types.toArray(String[]::new),
                graphURI,
                lang
        );

    }

    public List<String> fetchAllAnnotation(String graphURI, String componentURI) throws IOException {
        String query = buildSparqlQuery(graphURI, componentURI, ANNOTATIONS_QUERY);

        ArrayList<String> types = new ArrayList<>();
        ResultSet resultSet = this.explanationSparqlRepository.executeSparqlQueryWithResultSet(query);

        while (resultSet.hasNext()) {
            QuerySolution result = resultSet.next();
            RDFNode type = result.get("annotationType");
            String typeLocalName = type.asResource().getLocalName();
            logger.info("Annotation-Type found: {}", typeLocalName);
            types.add(typeLocalName.toLowerCase()); // lower case for equality comparison w/ map keys
        }

        return types;

    }

    // Create a specific explanation for every annotation
    public List<String> createSpecificExplanations(String[] usedTypes, String graphURI, String lang) throws IOException {

        List<String> explanations = new ArrayList<>();

        for (String type : usedTypes
        ) {
            explanations.addAll(createSpecificExplanation(type, graphURI, lang));
        }

        return explanations;
    }

    public List<String> createSpecificExplanation(String type, String graphURI, String lang) throws IOException {
        String query = buildSparqlQuery(graphURI, null, annotationsTypeAndQuery.get(type));
        List<String> explanationsForCurrentType = new ArrayList<>();
        ResultSet results = null;
        if (!stringResultSetMap.containsKey(type))
            results = this.explanationSparqlRepository.executeSparqlQueryWithResultSet(query);

        // TODO: Something similar to the Mapping approach? More generalization?
        if (Objects.equals(type, "annotationofspotinstance")) {

            File templateFile = new ClassPathResource(annotationTypeExplanationTemplate.get(type) + lang).getFile();
            String template = new String(Files.readAllBytes(templateFile.toPath()));

            while (results.hasNext()) {
                String filledTemplate = template;
                QuerySolution currentObject = results.next();
                filledTemplate = filledTemplate.replace("$createdAt", currentObject.get("createdAt").asLiteral().getString());
                filledTemplate = filledTemplate.replace("$start", String.valueOf(currentObject.get("start").asLiteral().getInt()));
                filledTemplate = filledTemplate.replace("$end", String.valueOf(currentObject.get("end").asLiteral().getInt()));
                explanationsForCurrentType.add(filledTemplate);
            }
        }

        logger.info("Created explanations: {}", explanationsForCurrentType);

        return explanationsForCurrentType;
    }


    /**
     * Creates a textual explanation for all annotations made by the componentURI for a language lang. The explanation for the annotations are formatted as a list
     *
     * @param lang Currently supported en and de
     * @return Complete explanation for the componentURI including all information to each annotation
     */
    public String createTextualExplanation(String graphURI, String componentURI, String lang) throws IOException {

        List<String> createdExplanations = createComponentExplanation(graphURI, componentURI, lang);

        AtomicInteger i = new AtomicInteger();
        List<String> explanations = createdExplanations.stream().map((explanation) -> String.valueOf(i.incrementAndGet()) + ". " + explanation).toList();

        String result = "The component " + componentURI + " has added " + explanations.size() + " annotation(s) to the triplestore: "
                + StringUtils.join(explanations, "\n");
        stringResultSetMap.clear();
        return result;
    }

}
