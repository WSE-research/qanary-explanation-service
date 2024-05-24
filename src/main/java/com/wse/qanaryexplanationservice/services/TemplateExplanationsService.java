package com.wse.qanaryexplanationservice.services;

import com.wse.qanaryexplanationservice.repositories.QanaryRepository;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class TemplateExplanationsService {

    protected static final String INPUT_DATA_SELECT_QUERY = "/queries/explanations/input_data/input_data_select.rq";
    private static final String QUESTION_QUERY = "/queries/question_query.rq";
    private static final String ANNOTATIONS_QUERY = "/queries/fetch_all_annotation_types.rq";
    private static final String COMPONENTS_SPARQL_QUERY = "/queries/components_sparql_query.rq";
    private static final String TEMPLATE_PLACEHOLDER_PREFIX = "${";
    private static final String TEMPLATE_PLACEHOLDER_SUFFIX = "}";
    private static final String OUTER_TEMPLATE_PLACEHOLDER_PREFIX = "&{";
    private static final String OUTER_TEMPLATE_PLACEHOLDER_SUFFIX = "}&";
    private static final String OUTER_TEMPLATE_REGEX = "&\\{.*}&";
    // Mappings: Header <-> Model-format
    private static final Map<String, String> headerFormatMap = new HashMap<>() {{
        put("application/rdf+xml", "RDFXML");
        put("application/ld+json", "JSONLD");
        put("text/turtle", "TURTLE");
    }};
    // Holds request query for declared annotations types
    private static final Map<String, String> annotationsTypeAndQuery = new HashMap<>() {{
        // AnnotationOfInstance
        put("annotationofspotinstance", "/queries/select_all_AnnotationOfSpotInstance.rq");
        put("annotationofinstance", "/queries/select_all_AnnotationOfInstance.rq");
        put("annotationofanswersparql", "/queries/select_all_AnnotationOfAnswerSPARQL.rq");
        put("annotationofrelation", "/queries/select_all_AnnotationOfRelation.rq");
        put("annotationofanswerjson", "/queries/select_all_AnnotationOfAnswerJson.rq");
        put("annotationofquestiontranslation", "/queries/select_all_AnnotationOfQuestionTranslation.rq");
        put("annotationofquestionlanguage", "/queries/select_all_AnnotationOfQuestionLanguage.rq");
    }};
    // Holds explanation templates for the declared annotation types
    private static final Map<String, String> annotationTypeExplanationTemplate = new HashMap<>() {{
        put("annotationofspotinstance", "/explanations/annotation_of_spot_instance/");
        put("annotationofinstance", "/explanations/annotation_of_instance/");
        put("annotationofanswersparql", "/explanations/annotation_of_answer_sparql/");
        put("annotationofrelation", "/explanations/annotation_of_relation/");
        put("annotationofanswerjson", "/explanations/annotation_of_answer_json/");
        put("annotationofquestiontranslation", "/explanations/annotation_of_question_translation/");
        put("annotationofquestionlanguage", "/explanations/annotation_of_question_language/");
    }};
    private static final String EXPLANATION_NAMESPACE = "urn:qanary:explanations#";
    Logger logger = LoggerFactory.getLogger(TemplateExplanationsService.class);
    private Map<String, ResultSet> stringResultSetMap = new HashMap<>();
    @Value("${explanations.dataset.limit}")
    private int EXPLANATIONS_DATASET_LIMIT;
    @Value("${questionId.replacement}")
    private String questionIdReplacement;

    public TemplateExplanationsService() {
    }

    /**
     * Collects list of explanations and creates the complete explanation including intro, prefix and items
     *
     * @param explanations List of explanations following the used templates
     * @param prefix       Text phrase between intro and items, can be an empty string
     * @return Explanation as String
     */
    private String getResult(String componentURI, String lang, List<String> explanations, String prefix) {
        String result = null;
        logger.info("Explanations {}", explanations.size());
        if (Objects.equals(lang, "en")) {
            result = "The component " + componentURI + " has added " + (explanations.size() == 5 ? "at least " : "") + explanations.size() + " annotation(s) to the graph"
                    + prefix + ": " + StringUtils.join(explanations, " ");
        } else if (Objects.equals(lang, "de")) {
            result = "Die Komponente " + componentURI + " hat " + (explanations.size() == 5 ? "mindestens " : "") + explanations.size() + " Annotation(en) zum Graph hinzugefügt"
                    + prefix + ": " + StringUtils.join(explanations, " ");
        }
        return result;
    }

    /**
     * Computes a textual explanation for a specific component on a specific graphURI
     *
     * @param graphUri     specific graphURI
     * @param componentUri specific componentURI
     * @return Explanation in accepted format, default: Turtle
     */
    public String explainSpecificComponent(String graphUri, String componentUri, String header) throws Exception {
        logger.info("Passed header: {}", header);
        Model model = createModel(graphUri, componentUri);

        return convertToDesiredFormat(header, model);
    }

    /**
     * Creates language-specific explanations and computes explanation model
     *
     * @return Model including
     */
    public Model createModel(String graphUri, String componentUri) throws IOException {

        List<String> types = new ArrayList<>();
        if (stringResultSetMap.isEmpty())
            types = fetchAllAnnotations(graphUri, componentUri);
        String contentDE = createTextualExplanation(graphUri, componentUri, "de", types);
        String contentEN = createTextualExplanation(graphUri, componentUri, "en", types);

        return createModelForSpecificComponent(contentDE, contentEN, componentUri);
    }

    /**
     * Creates an explanation model for a specific componentURI.
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
        // Property hasExplanation = model.createProperty(EXPLANATION_NAMESPACE, "hasExplanation");

        // creates Resource, in this case the componentURI
        Resource componentUriResource = model.createResource(componentURI);

        // add triples to the model
        // model.add(hasExplanationForCreatedDataProperty, rdfsSubPropertyOf, hasExplanation);
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

    /**
     * TODO: RENAME!
     *
     * @param rawQuery Query-String without set values
     * @return Query-String with set values
     */
    public String buildSparqlQuery(String graphURI, String componentUri, String rawQuery) throws IOException {
        QuerySolutionMap bindingsForSparqlQuery = new QuerySolutionMap();
        bindingsForSparqlQuery.add("graph", ResourceFactory.createResource(graphURI));
        if (componentUri != null)    // Extension for compatibility w/ explanation for specific component
            bindingsForSparqlQuery.add("annotatedBy", ResourceFactory.createResource(componentUri));

        return QanaryTripleStoreConnector.readFileFromResourcesWithMap(rawQuery, bindingsForSparqlQuery);

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

        List<String> components = getUsedComponents(graphURI);
        Map<String, Model> models = new HashMap<>();

        for (String component : components
        ) {
            models.put(component, createModel(graphURI, component));
        }

        String questionURI = fetchQuestionUri(graphURI);
        Model systemExplanationModel = createSystemModel(models, components, questionURI, graphURI);

        return convertToDesiredFormat(header, systemExplanationModel);
    }

    public List<String> getUsedComponents(String graphID) throws IOException {
        QuerySolutionMap bindings = new QuerySolutionMap();
        bindings.add("graph", ResourceFactory.createResource(graphID));
        String query = QanaryTripleStoreConnector.readFileFromResourcesWithMap(COMPONENTS_SPARQL_QUERY, bindings);
        ResultSet resultSet = QanaryRepository.selectWithResultSet(query);
        List<String> components = new ArrayList<>();
        while (resultSet.hasNext()) {
            components.add(resultSet.next().get("component").toString());
        }
        return components;
    }

    // get the origin questionURI for a graphURI
    public String fetchQuestionUri(String graphURI) throws Exception {
        String query = buildSparqlQuery(graphURI, null, QUESTION_QUERY);
        ResultSet resultSet = QanaryRepository.selectWithResultSet(query);

        try {
            String questionURI = resultSet.next().get("source").toString();
            logger.info("QuestionURI = {}", questionURI);
            return questionURI;
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
    public Model createSystemModel(Map<String, Model> models, List<String> components, String questionURI, String graphURI) {

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
        for (int i = 0; i < components.size(); i++) {
            int j = 1;
            // get the model for the component at position "i" in component list // remember: models is Map with componentUri as key
            Model model = models.get(components.get(i));
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
     * Fetching all annotations a component has created.
     *
     * @return A list with all different annotation-types
     */
    public List<String> fetchAllAnnotations(String graphURI, String componentURI) throws IOException {
        String query = buildSparqlQuery(graphURI, componentURI, ANNOTATIONS_QUERY); // can be simplified by excluding with FILTER NOT ...

        ArrayList<String> types = new ArrayList<>();
        ResultSet resultSet = QanaryRepository.selectWithResultSet(query);

        // Iterate through the QuerySolutions and gather all annotation-types
        while (resultSet.hasNext()) {
            QuerySolution result = resultSet.next();
            RDFNode type = result.get("annotationType");
            String typeLocalName = type.asResource().getLocalName();
            logger.info("Annotation-Type found: {}", typeLocalName); //TODO: check this function; required?
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
    public Map<String, List<String>> createSpecificExplanations(List<String> usedTypes, String graphURI, String lang, String componentURI) throws IOException {
        Map<String, List<String>> explanations = new HashMap<>();
        for (String type : usedTypes
        ) {
            explanations.put(type, createSpecificExplanation(type, graphURI, lang, componentURI));
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
        String query = buildSparqlQuery(graphURI, componentURI, annotationsTypeAndQuery.get(type));

        // For the first language that will be executed, for each annotation-type a component created // TODO: could be cached.
        if (!stringResultSetMap.containsKey(type))
            stringResultSetMap.put(type, QanaryRepository.selectWithResultSet(query));

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

        while (results.hasNext() && results.getRowNumber() < EXPLANATIONS_DATASET_LIMIT) {
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
        template = StringSubstitutor
                .replace(template, convertedMap, TEMPLATE_PLACEHOLDER_PREFIX, TEMPLATE_PLACEHOLDER_SUFFIX)
                .replace(questionIdReplacement + "/question/stored-question__text_", "questionID:");
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
    public String getStringFromFile(String path) throws RuntimeException {
        ClassPathResource cpr = new ClassPathResource(path);
        try {
            byte[] bdata = FileCopyUtils.copyToByteArray(cpr.getInputStream());
            return new String(bdata, StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("{}", e.getMessage());
            throw new RuntimeException();
        }
    }

    /**
     * Creates the explanation and returns the english one
     *
     * @return The explanation for given graphURI and componentURI
     */
    public String getExplanation(String graphURI, String componentURI) throws IOException, IndexOutOfBoundsException {

        Model explanationModel = this.createModel(graphURI, "urn:qanary:" + componentURI);
        explanationModel.setNsPrefix("explanations", EXPLANATION_NAMESPACE);
        Property hasExplanationForCreatedDataProperty = explanationModel.createProperty(EXPLANATION_NAMESPACE, "hasExplanationForCreatedData");
        Statement statement = explanationModel.getRequiredProperty(ResourceFactory.createResource("urn:qanary:" + componentURI), hasExplanationForCreatedDataProperty, "en");

        return statement.getString();
    }

    /**
     * Creates a textual explanation for all annotations made by the componentURI for a language lang. The explanation for the annotations are formatted as a list
     *
     * @param lang Currently supported en_list_item and de_list_item
     * @return Complete explanation for the componentURI including all information to each annotation
     * TODO later: Assemble explanations in a different way.
     */
    public String createTextualExplanation(String graphURI, String componentURI, String lang, List<String> types) throws IOException, IndexOutOfBoundsException {

        if (types.size() == 0) { // Component did not add any Annotation
            return "The component " + componentURI + " inserted 0 annotations to the triplestore.";
        }

        Map<String, List<String>> createdExplanations = createSpecificExplanations(types, graphURI, lang, componentURI);

        AtomicInteger i = new AtomicInteger();

        createdExplanations.forEach((component, list) -> {
            createdExplanations.put(component, list.stream().skip(1).map((explanation) -> i.incrementAndGet() + ". " + explanation).toList());
        });

        stringResultSetMap.clear();
        StringBuilder result = new StringBuilder();
        createdExplanations.forEach((component, list) -> {
            result.append(getResult(component, lang, list, list.get(0)));
            result.append("\n\n");
        });
        return result.toString();
    }


    /**
     * Creates a rulebased explanation for the input data of the passed component
     *
     * @param graph     Knowledge graph where the data is stored
     * @param component Component name (with prefixes!) for which the explanation is created
     * @return Explanation as string
     * @throws IOException If template selection from file fails
     */
    public String createInputExplanation(String graph, String component) throws IOException {
        QuerySolutionMap bindings = new QuerySolutionMap();
        bindings.add("graph", ResourceFactory.createResource(graph));
        bindings.add("component", ResourceFactory.createResource(component));

        String queryTemplate = QanaryTripleStoreConnector.readFileFromResourcesWithMap(INPUT_DATA_SELECT_QUERY, bindings);

        int resultSetSize = 0;
        List<String> explanationsForQueries = new ArrayList<>();
        ResultSet results = QanaryRepository.selectWithResultSet(queryTemplate);
        if (!results.hasNext())
            return "The component " + component + " hasn't used any queries.";
        while (results.hasNext()) {
            QuerySolution currentSolution = results.nextSolution();
            try {
                explanationsForQueries.add(createExplanationForQuery(currentSolution, graph, component));
                resultSetSize++;
            } catch (RuntimeException e) {
            }
        }

        // create Prefix
        String prefix = getStringFromFile("/explanations/input_data/prefixes/en"); // adopt for any other languages
        prefix = prefix.replace("${numberOfQueries}", resultSetSize +
                (resultSetSize == 1 ? " SPARQL query" : " SPARQL queries")
        );
        prefix = prefix.replace("${component}", component).replace("${graph}", graph);
        // Zusammenbauen

        logger.info("Items: {}", explanationsForQueries);
        logger.info("PREFIX: {}", prefix);

        return composeExplanation(explanationsForQueries, prefix);
    }

    public String composeExplanation(List<String> listItems, String prefix) {
        StringBuilder finalExplanation = new StringBuilder();
        finalExplanation.append(prefix);
        for (int i = 0; i < listItems.size(); i++) {
            finalExplanation.append("\n").append(i + 1).append(". ").append(listItems.get(i));
        }
        return finalExplanation.toString();
    }

    public String createExplanationForQuery(QuerySolution currentSolution, String graph, String component) throws IOException {
        String annotationType = disassembleAnnotationTypeFromQuery(currentSolution.get("body").toString());
        Map<String, String> items = new HashMap<>() {{
            put("graph", graph);
            put("component", component);
            put("annotationType", annotationType);
        }};
        // for each language
        String listItem = "";
        try {
            listItem = getStringFromFile("/explanations/input_data/" + annotationType + "/english");
        } catch (RuntimeException e) {
            logger.error("{}", e.getMessage());
            listItem = "For annotation type " + annotationType + " no explanation is available.";
        }
        return StringSubstitutor.replace(listItem, items, TEMPLATE_PLACEHOLDER_PREFIX, TEMPLATE_PLACEHOLDER_SUFFIX);
    }

    public String disassembleAnnotationTypeFromQuery(String sparql) throws IOException {
        BufferedReader reader = new BufferedReader(new StringReader(sparql));
        String line = reader.readLine();
        while (line != null) {
            if (line.contains("AnnotationOf")) {
                String modifiedLine = line.substring(line.indexOf("qa:AnnotationOf")); // Current String equals "AnnotationOfSOMEWHAT ...." -> Need to remove the last part
                String annotationType = modifiedLine.replace(modifiedLine.substring(modifiedLine.indexOf(" "), modifiedLine.length()), "").replace("qa:", "");
                logger.info("Found annotationType: {}", annotationType);
                return annotationType;
            }
            line = reader.readLine();
        }
        logger.error("No template available for SPARQL query: {}", sparql);
        throw new RuntimeException("No annotation type could be dissambled");
    }

}