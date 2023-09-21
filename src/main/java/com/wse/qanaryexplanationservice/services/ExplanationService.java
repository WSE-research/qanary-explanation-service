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
import org.apache.commons.text.StringSubstitutor;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ExplanationService {

    // Query files
    private static final String QUESTION_QUERY = "/queries/question_query.rq";
    private static final String ANNOTATIONS_QUERY = "/queries/queries_for_annotation_types/fetch_all_annotation_types.rq";
    private static final String TEMPLATE_PLACEHOLDER_PREFIX = "${";
    private static final String TEMPLATE_PLACEHOLDER_SUFFIX = "}";
    // Mappings
    private static final Map<String, String> headerFormatMap = new HashMap<>() {{
        put("application/rdf+xml", "RDFXML");
        put("application/ld+json", "JSONLD");
        put("text/turtle", "TURTLE");
    }};
    // Holds request query for declared annotations types
    private static final Map<String, String> annotationsTypeAndQuery = new HashMap<>() {{
        // AnnotationOfInstance
        put("annotationofspotinstance", "/queries/queries_for_annotation_types/annotations_of_spot_intance_query.rq");
        put("annotationofinstance", "/queries/queries_for_annotation_types/annotations_of_instance_query.rq");
        put("annotationofanswersparql", "/queries/queries_for_annotation_types/annotations_of_answer_sparql.rq");
        put("annotationofrelation", "/queries/queries_for_annotation_types/annotations_of_relation_query.rq");
        put("annotationofanswerjson", "/queries/queries_for_annotation_types/annotations_of_answer_json_query.rq");
        put("annotationofquestionlanguage", "/queries/queries_for_annotation_types/annotations_of_question_language_query.rq");
    }};
    // Holds explanation templates for the declared annotation types
    private static final Map<String, String> annotationTypeExplanationTemplate = new HashMap<>() {{
        put("annotationofspotinstance", "/explanations/annotation_of_spot_instance/");
        put("annotationofinstance", "/explanations/annotation_of_instance/");
        put("annotationofanswersparql", "/explanations/annotation_of_answer_sparql/");
        put("annotationofrelation", "/explanations/annotation_of_relation/");
        put("annotationofanswerjson", "/explanations/annotation_of_answer_json/");
        put("annotationofquestionlanguage", "/explanations/annotation_of_question_language/");
    }};
    final String EXPLANATION_NAMESPACE = "urn:qanary:explanations#";
    private final String OUTER_TEMPLATE_PLACEHOLDER_PREFIX = "&{";
    private final String OUTER_TEMPLATE_PLACEHOLDER_SUFFIX = "}&";
    private final String OUTER_TEMPLATE_REGEX = "\\&\\{.*\\}\\&";
    private final ObjectMapper objectMapper;
    Logger logger = LoggerFactory.getLogger(ExplanationService.class);

    /*
    TODO: Is there a smart approach to avoid global changing variable?
    Key = annotation-type, Value = ResultSet
    Should avoid multiple execution of same sparql query
     */
    private Map<String, ResultSet> stringResultSetMap = new HashMap<>();
    @Autowired
    private ExplanationSparqlRepository explanationSparqlRepository;
    @Autowired
    private AnnotationsService annotationsService;

    public ExplanationService() {
        objectMapper = new ObjectMapper();
    }

    private static String getResult(String componentURI, String lang, List<String> explanations, String prefix) {
        String result = null;
        if (Objects.equals(lang, "en")) {
            result = "The component " + componentURI + " has added " + explanations.size() + " annotation(s) to the graph"
                    + prefix + ": " + StringUtils.join(explanations, " ");
        } else if (Objects.equals(lang, "de")) {
            result = "Die Komponente " + componentURI + " hat " + explanations.size() + " Annotation(en) zum Graph hinzugefügt"
                    + prefix + ": " + StringUtils.join(explanations, " ");
        }
        return result;
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

        List<String> types = new ArrayList<>();
        if (stringResultSetMap.isEmpty())
            types = fetchAllAnnotations(graphUri, componentUri);

        String contentDE = createTextualExplanation(graphUri, componentUri, "de", types);
        String contentEN = createTextualExplanation(graphUri, componentUri, "en", types);

        return createModelForSpecificComponent(contentDE, contentEN, componentUri);
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
            models.put(component.getComponent().getValue(), // === componentURI
                    createModel(graphURI, component.getComponent().getValue())); // create a model for that componentURI
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

    /**
     * Creates a comprehensive explanation in a given language
     *
     * @param types Inherits all annotation-types
     * @param lang  Given language
     * @return List with explanations for the given language. At the end it includes all explanations for every annotation type
     */
    public List<String> createComponentExplanation(String graphURI, String lang, List<String> types, String componentURI) throws IOException {
        return createSpecificExplanations(
                types.toArray(String[]::new),
                graphURI,
                lang,
                componentURI
        );
    }

    /**
     * Fetching all annotations a component has created.
     *
     * @return A list with all different annotation-types
     */
    public List<String> fetchAllAnnotations(String graphURI, String componentURI) throws IOException {
        String query = buildSparqlQuery(graphURI, componentURI, ANNOTATIONS_QUERY);

        ArrayList<String> types = new ArrayList<>();
        ResultSet resultSet = this.explanationSparqlRepository.executeSparqlQueryWithResultSet(query);

        // Iterate through the QuerySolutions and gather all annotation-types
        while (resultSet.hasNext()) {
            QuerySolution result = resultSet.next();
            RDFNode type = result.get("annotationType");
            String typeLocalName = type.asResource().getLocalName();
            logger.info("Annotation-Type found: {}", typeLocalName);
            if (!Objects.equals(typeLocalName, "AnswerJson"))
                types.add(typeLocalName.toLowerCase());
        }

        return types;
    }

    /**
     * Collects the explanation for every annotation type and concat the received lists to its own list
     *
     * @param usedTypes Includes all annotation types the given componentURI has created
     * @param lang      The language for the explanation
     * @return List with explanations. At the end it includes all explanations for every annotation type
     */
    public List<String> createSpecificExplanations(String[] usedTypes, String graphURI, String lang, String componentURI) throws IOException {

        List<String> explanations = new ArrayList<>();
        for (String type : usedTypes
        ) {
            explanations.addAll(createSpecificExplanation(type, graphURI, lang, componentURI));
        }
        return explanations;
    }

    /**
     * Creates an explanation for the passed type and language
     *
     * @param type The annotation type
     * @param lang The language for the explanation
     * @return A list of explanation containing a prefix explanation and one entry for every annotation of the givent type
     */
    public List<String> createSpecificExplanation(String type, String graphURI, String lang, String componentURI) throws IOException {
        logger.info("Type: {}", type);
        String query = buildSparqlQuery(graphURI, componentURI, annotationsTypeAndQuery.get(type));

        // For the first language that will be executed, for each annotation-type a component created
        if (!stringResultSetMap.containsKey(type))
            stringResultSetMap.put(type, this.explanationSparqlRepository.executeSparqlQueryWithResultSet(query));

        List<String> explanationsForCurrentType = addingExplanations(type, lang, stringResultSetMap.get(type));

        logger.info("Created explanations: {}", explanationsForCurrentType);
        return explanationsForCurrentType;
    }

    /**
     * @param type    The actual type for which the explanation is being generated, relevant for selection of correct template
     * @param lang    The actual language the explanation is created for
     * @param results The ResultSet for the actual type
     * @return A list of explanations for the given type in the given language
     */
    public List<String> addingExplanations(String type, String lang, ResultSet results) throws IOException {

        List<String> explanationsForCurrentType = new ArrayList<>();
        String langExplanationPrefix = getStringFromFile(annotationTypeExplanationTemplate.get(type) + lang + "_prefix");
        explanationsForCurrentType.add(langExplanationPrefix);
        String template = getStringFromFile(annotationTypeExplanationTemplate.get(type) + lang + "_list_item");

        while (results.hasNext()) {
            QuerySolution querySolution = results.next();
            explanationsForCurrentType.add(replaceProperties(convertQuerySolutionToMap(querySolution), template));
        }

        return explanationsForCurrentType;
    }

    /**
     * Replaces all placeholders in the template with attributes from the passed QuerySolution
     *
     * @param template Template including the defined pre- and suffixes
     * @return Template with replaced placeholders
     */
    public String replaceProperties(Map<String, String> convertedMap, String template) {

        // Replace all placeholders with values from map
        template = StringSubstitutor.replace(template, convertedMap, TEMPLATE_PLACEHOLDER_PREFIX, TEMPLATE_PLACEHOLDER_SUFFIX);

        template = checkAndReplaceOuterPlaceholder(template);

        logger.info("Template with inserted params: {}", template);
        return template;
    }

    public String checkAndReplaceOuterPlaceholder(String template) {
        Pattern pattern = Pattern.compile(OUTER_TEMPLATE_REGEX);
        Matcher matcher = pattern.matcher(template);

        while (matcher.find()) {
            String a = matcher.group();
            if (a.contains(TEMPLATE_PLACEHOLDER_PREFIX)) {
                template = template.replace(a, "");
            } else
                template = template.replace(
                        a,
                        a.replace(OUTER_TEMPLATE_PLACEHOLDER_PREFIX, "")
                                .replace(OUTER_TEMPLATE_PLACEHOLDER_SUFFIX, ""));
        }

        return template;
    }

    public Map<String, String> convertQuerySolutionToMap(QuerySolution querySolution) {
        QuerySolutionMap querySolutionMap = new QuerySolutionMap();
        querySolutionMap.addAll(querySolution);
        Map<String, RDFNode> querySolutionMapAsMap = querySolutionMap.asMap();
        return convertRdfNodeToStringValue(querySolutionMapAsMap);
    }

    /**
     * Converts RDFNodes to Strings without the XML datatype declaration and leaves resources as they are.
     *
     * @param map Key = variable from sparql-query, Value = its corresponding RDFNode
     * @return Map with value::String instead of value::RDFNode
     */
    public Map<String, String> convertRdfNodeToStringValue(Map<String, RDFNode> map) {
        return map.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> {
                    if (entry.getValue().isResource())
                        return entry.getValue().toString();
                    else
                        return entry.getValue().asNode().getLiteralValue().toString();
                }
        ));
    }

    /**
     * Reads a file and parses the content to a string
     *
     * @param path Given path
     * @return String with the file's content
     */
    public String getStringFromFile(String path) throws IOException {
        File file = new ClassPathResource(path).getFile();
        return new String(Files.readAllBytes(file.toPath()));
    }

    /**
     * Creates a textual explanation for all annotations made by the componentURI for a language lang. The explanation for the annotations are formatted as a list
     *
     * @param lang Currently supported en_list_item and de_list_item
     * @return Complete explanation for the componentURI including all information to each annotation
     */
    public String createTextualExplanation(String graphURI, String componentURI, String lang, List<String> types) throws IOException {

        List<String> createdExplanations = createComponentExplanation(graphURI, lang, types, componentURI);

        AtomicInteger i = new AtomicInteger();
        List<String> explanations = createdExplanations.stream().skip(1).map((explanation) -> i.incrementAndGet() + ". " + explanation).toList();
        String result = getResult(componentURI, lang, explanations, createdExplanations.get(0));
        stringResultSetMap.clear();
        return result;
    }

}
