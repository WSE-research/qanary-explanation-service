package com.wse.qanaryexplanationservice.services;

import com.wse.qanaryexplanationservice.helper.AnnotationType;
import com.wse.qanaryexplanationservice.helper.pojos.AutomatedTests.QanaryRequestPojos.QanaryRequestObject;
import com.wse.qanaryexplanationservice.helper.pojos.AutomatedTests.QanaryRequestPojos.QanaryResponseObject;
import com.wse.qanaryexplanationservice.helper.pojos.InputQueryExample;
import com.wse.qanaryexplanationservice.repositories.QanaryRepository;
import com.wse.qanaryexplanationservice.repositories.QuestionsRepository;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.util.*;

/**
 * Service which provides different methods to create explanations with generative AI.
 */
@Service
public class GenerativeExplanations {

    protected static final ArrayList<InputQueryExample> INPUT_QUERIES_AND_EXAMPLE = InputQueryExample.queryExamplesList();
    protected static final Map<String, String[]> TYPE_AND_COMPONENTS = new HashMap<>();
    protected static final Map<AnnotationType, AnnotationType[]> DEPENDENCY_MAP_FOR_ANNOTATION_TYPES = new TreeMap<>() {{
        put(AnnotationType.AnnotationOfInstance, new AnnotationType[]{});
        put(AnnotationType.AnnotationOfRelation, new AnnotationType[]{
                AnnotationType.AnnotationOfQuestionLanguage
        });
        put(AnnotationType.AnnotationOfSpotInstance, new AnnotationType[]{});
        //put(AnnotationType.annotationofquestiontranslation, null);
        put(AnnotationType.AnnotationOfQuestionLanguage, new AnnotationType[]{});
        put(AnnotationType.AnnotationOfAnswerSPARQL, new AnnotationType[]{
                AnnotationType.AnnotationOfInstance,
                AnnotationType.AnnotationOfRelation,
                AnnotationType.AnnotationOfSpotInstance,
                AnnotationType.AnnotationOfQuestionLanguage
        });
        put(AnnotationType.AnnotationOfAnswerJson, new AnnotationType[]{
                AnnotationType.AnnotationOfAnswerSPARQL,
                AnnotationType.AnnotationOfInstance,
                AnnotationType.AnnotationOfRelation,
                AnnotationType.AnnotationOfSpotInstance,
                AnnotationType.AnnotationOfQuestionLanguage
        });
    }};
    protected static final int QADO_DATASET_QUESTION_COUNT = 394;
    private static final String DATASET_QUERY = "/queries/evaluation_dataset_query.rq";
    private static final Map<String, String> PREFIXES = new HashMap<>() {{
        put("http://www.w3.org/ns/openannotation/core/", "oa:");
        put("http://www.wdaqua.eu/qa#", "qa:");
        put("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "rdf:");
        put("http://www.w3.org/2000/01/rdf-schema#", "rdfs:");
        put("http://www.w3.org/2002/07/owl#", "owl:");
        put("^^http://www.w3.org/2001/XMLSchema#integer", "");
        put("^^http://www.w3.org/2001/XMLSchema#dateTime", "");
        put("^^http://www.w3.org/2001/XMLSchema#decimal", "");
        put("^^http://www.w3.org/2001/XMLSchema#float", "");
    }};
    private static final Map<Integer, String> EXAMPLE_COUNT_AND_TEMPLATE = new HashMap<>() {{
        put(1, "/testtemplates/oneshot");
        put(2, "/testtemplates/twoshot");
        put(3, "/testtemplates/threeshot");
    }};

    private static final Map<Integer, String> EXAMPLE_COUNT_AND_TEMPLATE_INPUT_DATA = new HashMap<>() {{
        put(1, "/testtemplates/inputdata/oneshot");
        put(2, "/testtemplates/inputdata/twoshot");
        put(0, "/testtemplates/inputdata/zeroshot");
    }};
    private static final String EXPLANATION_NAMESPACE = "urn:qanary:explanations#";
    private static final String QUESTION_QUERY = "/queries/random_question_query.rq";
    private static final Logger logger = LoggerFactory.getLogger(GenerativeExplanations.class);

    public GenerativeExplanations(Environment environment) {
        for (AnnotationType annType : AnnotationType.values()
        ) {
            TYPE_AND_COMPONENTS.put(annType.name(), environment.getProperty("qanary.components." + annType.name().toLowerCase(), String[].class));
        }
    }

    @Value("${questionId.replacement}")
    public void setQuestionIdReplacement(String questionIdUri) {
        this.PREFIXES.put(questionIdUri + "/question/stored-question__text_", "questionID:");
    }

    /**
     * Executes a SPARQL query on the triplestore to fetch a question from the (existing!) QADO-dataset
     *
     * @param questionNumber The number of the question
     * @return a random question as plain String
     */
    public String getRandomQuestion(Integer questionNumber) throws IOException {

        QuerySolutionMap querySolutionMap = new QuerySolutionMap();
        querySolutionMap.add("id", ResourceFactory.createTypedLiteral(questionNumber.toString(), XSDDatatype.XSDnonNegativeInteger));

        try {
            String query = QanaryTripleStoreConnector.readFileFromResourcesWithMap(QUESTION_QUERY, querySolutionMap);
            ResultSet resultSet = QuestionsRepository.selectQuestion(query);
            return resultSet.next().get("hasQuestion").asLiteral().getString();
        } catch (IOException e) {
            String errorMessage = "Error while fetching a random question";
            logger.error("Error: {}", errorMessage);
            throw new IOException(errorMessage);
        } catch (IndexOutOfBoundsException e) {
            String errorMessage = "The executed SPARQL query returned zero results";
            logger.error(errorMessage);
            throw new RuntimeException(errorMessage);
        }
    }

    /**
     * Calls the corresponding repository to execute the Qanary pipeline with the given components and question.
     *
     * @param randomComponents Holds the components which will be executed in the correct order (respects dependencies)
     * @return QanaryResponseObject involving the questionID as well as the graphURI
     */
    public QanaryResponseObject executeQanaryPipeline(String question, List<String> randomComponents) throws Exception {
        QanaryRequestObject qanaryRequestObject = new QanaryRequestObject(question, null, null, randomComponents);

        try {
            return QanaryRepository.executeQanaryPipeline(qanaryRequestObject);
        } catch (WebClientResponseException e) {
            String errorMessage = "Error while executing Qanary pipeline, Error message: " + e.getMessage();
            logger.error(errorMessage);
            throw new Exception(e);
        }
    }

    /**
     * @param list List of annotation-types - in the usual workflow this comes from the dependency resolver
     * @return List of components in the order of their annotation type (and therefore their dependencies)
     */
    public List<String> selectRandomComponents(ArrayList<AnnotationType> list) {
        Random random = new Random();
        // Is null when no dependencies were resolved and therefore only one component shall be executed
        if (list == null)
            return new ArrayList<>();

        Collections.sort(list); // sorts them by the enum definition, which equals the dependency tree (the last is the target-component)
        List<String> componentList = new ArrayList<>();

        for (AnnotationType annType : list
        ) {
            String[] componentsList = this.TYPE_AND_COMPONENTS.get(annType.name());
            int selectedComponentAsInt = random.nextInt(componentsList.length);
            componentList.add(componentsList[selectedComponentAsInt]);
        }
        return componentList;
    }

    /**
     * Transforms ResultSet QuerySolutions to triple-"sentence" representation by appending s, p, o and a "."
     *
     * @return Dataset as String
     */
    public String createDataset(String componentURI, String graphURI, String annotationType) throws Exception {

        try {
            ResultSet triples = fetchTriples(graphURI, componentURI, annotationType);
            StringBuilder dataSet = new StringBuilder();
            while (triples.hasNext()) {
                QuerySolution querySolution = triples.next();
                dataSet.append(querySolution.getResource("s"))
                        .append(" ").append(querySolution.getResource("p"))
                        .append(" ").append(querySolution.get("o"))
                        .append(" .\n");
            }
            String dataSetAsString = dataSet.toString();

            // Replace PREFIXES
            for (Map.Entry<String, String> entry : PREFIXES.entrySet()) {
                dataSetAsString = dataSetAsString.replace(entry.getKey(), entry.getValue());
            }
            return dataSetAsString;
        } catch (RuntimeException e) {
            logger.error("{}", e.getMessage());
            return "Empty dataset";
        }
    }

    public AnnotationType[] getDependencyList(String annotationType) {
        logger.info("Type: {}", annotationType);
        return this.DEPENDENCY_MAP_FOR_ANNOTATION_TYPES.get(AnnotationType.valueOf(annotationType));
    }

    /**
     * Fetches triples for specific graph + component
     *
     * @return Triples as ResultSet
     * @throws Exception If a component hasn't made any annotations to the graph the query will result in an empty ResultSet
     */
    public ResultSet fetchTriples(String graphURI, String componentURI, String annotationType) throws Exception {
        QuerySolutionMap bindingsForQuery = new QuerySolutionMap();
        bindingsForQuery.add("graphURI", ResourceFactory.createResource(graphURI));
        bindingsForQuery.add("componentURI", ResourceFactory.createResource("urn:qanary:" + componentURI));
        bindingsForQuery.add("annotatedBy", ResourceFactory.createResource("urn:qanary:" + componentURI));
        try {
            String query = QanaryTripleStoreConnector.readFileFromResourcesWithMap(DATASET_QUERY, bindingsForQuery);
            query = query.replace("?annotationType", "qa:" + annotationType);
            ResultSet resultSet = QanaryRepository.selectWithResultSet(query);
            if (!resultSet.hasNext())
                throw new RuntimeException("Fetching triples failed, ResultSet is null");
            else
                return resultSet;
        } catch (IOException e) {
            logger.error("Error while fetching triples: {}", e.getMessage());
            throw new Exception(e);
        }
    }

    public String getPromptTemplate(int shots) {
        return this.EXAMPLE_COUNT_AND_TEMPLATE.get(shots);
    }

    public String getPromptTemplateInputData(int shots) {
        return this.EXAMPLE_COUNT_AND_TEMPLATE_INPUT_DATA.get(shots);
    }

}
