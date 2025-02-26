package com.wse.qanaryexplanationservice.repositories;

import com.wse.qanaryexplanationservice.exceptions.ExplanationException;
import com.wse.qanaryexplanationservice.helper.ExplanationHelper;
import com.wse.qanaryexplanationservice.helper.pojos.AutomatedTests.QanaryRequestPojos.QanaryRequestObject;
import com.wse.qanaryexplanationservice.helper.pojos.AutomatedTests.QanaryRequestPojos.QanaryResponseObject;
import com.wse.qanaryexplanationservice.helper.pojos.ExplanationMetaData;
import com.wse.qanaryexplanationservice.helper.pojos.MethodItem;
import com.wse.qanaryexplanationservice.helper.pojos.Variable;
import com.wse.qanaryexplanationservice.services.ExplanationService;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.ResourceFactory;
import org.aspectj.weaver.ast.Var;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Repository;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtuosoQueryExecution;
import virtuoso.jena.driver.VirtuosoQueryExecutionFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class provides different request methods against the Qanary pipeline or the underlying triplestore
 */
@Repository
public class QanaryRepository {
    private static final String QUESTION_ANSWERING_ENDPOINT = "/startquestionansweringwithtextquestion";
    private static final String HTTP_SCHEME = "http";
    private static final int TIMEOUT_SECONDS = 60;
    private final String SELECT_ONE_METHOD_WITH_ID = "/queries/fetch_one_method_id.rq";
    private final WebClient webClient;
    private final Logger logger = LoggerFactory.getLogger(QanaryRepository.class);
    private final Map<String,String> SPARQL_VARNAME_INPUT_VARIABLES = new HashMap<>() {{
        put("type", "inputDataTypes");
        put("value", "inputDataValues");
    }};
    private final Map<String,String> SPARQL_VARNAME_OUTPUT_VARIABLES = new HashMap<>() {{
        put("type", "outputDataType");
        put("value", "outputDataValue");
    }};

    public Map<String, String> getSPARQL_VARNAME_INPUT_VARIABLES() {
        return SPARQL_VARNAME_INPUT_VARIABLES;
    }

    public Map<String, String> getSPARQL_VARNAME_OUTPUT_VARIABLES() {
        return SPARQL_VARNAME_OUTPUT_VARIABLES;
    }


    @Value("${virtuoso.triplestore.endpoint}")
    private String virtuosoEndpoint;
    @Value("${virtuoso.triplestore.username}")
    private String virtuosoUser;
    @Value("${virtuoso.triplestore.password}")
    private String virtuosoPassword;
    @Value("${qanary.pipeline.host}")
    private String QANARY_HOST;
    @Value("${qanary.pipeline.port}")
    private int QANARY_PORT;

    private VirtGraph connection;

    public QanaryRepository() {
        this.webClient = createWebClient();
    }

    private WebClient createWebClient() {
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create().responseTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))))
                .build();
    }

    public QanaryResponseObject executeQanaryPipeline(QanaryRequestObject qanaryRequestObject) {
        MultiValueMap<String, String> requestParams = createRequestParams(qanaryRequestObject);

        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .scheme(HTTP_SCHEME)
                        .host(QANARY_HOST)
                        .port(QANARY_PORT)
                        .path(QUESTION_ANSWERING_ENDPOINT)
                        .queryParams(requestParams)
                        .build())
                .retrieve()
                .bodyToMono(QanaryResponseObject.class)
                .block();
    }

    private MultiValueMap<String, String> createRequestParams(QanaryRequestObject qanaryRequestObject) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("question", qanaryRequestObject.getQuestion());
        params.addAll(qanaryRequestObject.getComponentListAsMap());
        return params;
    }

    public ResultSet selectWithResultSet(String sparql) throws QueryException {
        ensureConnection();
        Query query = QueryFactory.create(sparql);
        VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(query, this.connection);
        return ResultSetFactory.makeRewindable(vqe.execSelect());
    }

    public boolean askQuestion(String question) throws QueryException {
        ensureConnection();
        Query query = QueryFactory.create(question);
        VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(query, this.connection);
        return vqe.execAsk();
    }

    private void ensureConnection() {
        if (connection == null) {
            logger.info("Initializing connection for Qanary repository: {}", this.virtuosoEndpoint);
            connection = new VirtGraph(this.virtuosoEndpoint, this.virtuosoUser, this.virtuosoPassword);
        }
    }

    public String getQuestionFromQuestionId(String questionId) {
        logger.info("Getting question from url: {}", questionId);
        return webClient.get()
                .uri(questionId)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    public String select_one_method(ExplanationMetaData explanationMetaData) throws IOException {
        QuerySolutionMap qsm = new QuerySolutionMap();
        qsm.add("methodIdentifier", ResourceFactory.createResource(explanationMetaData.getMethod()));
        qsm.add("graph", ResourceFactory.createResource(explanationMetaData.getGraph().toASCIIString()));
        qsm.add("component", ResourceFactory.createResource(explanationMetaData.getQanaryComponent().getPrefixedComponentName()));
        qsm.add("separator", ResourceFactory.createStringLiteral(ExplanationHelper.VARIABLE_SEPARATOR));

        return QanaryTripleStoreConnector.readFileFromResourcesWithMap(SELECT_ONE_METHOD_WITH_ID, qsm);
    }

    public MethodItem transformQuerySolutionToMethodItem(QuerySolution qs) throws ExplanationException {
        if(qs == null){
            throw new ExplanationException("SPARQL query returned no results. Therefore, no explanation can be provided.");
        }
        String caller = safeGetString(qs, "caller");
        String callerName = safeGetString(qs, "callerName");
        String method = safeGetString(qs, "method");
        String annotatedAt = safeGetString(qs, "annotatedAt");
        String annotatedBy = safeGetString(qs, "annotatedBy");
        List<Variable> inputVariables = this.extractVarsAndType(ExplanationHelper.VARIABLE_SEPARATOR, qs, this.SPARQL_VARNAME_INPUT_VARIABLES);
        List<Variable> outputVariables = this.extractVarsAndType(ExplanationHelper.VARIABLE_SEPARATOR, qs, this.SPARQL_VARNAME_OUTPUT_VARIABLES);
        return new MethodItem(
                caller,
                callerName,
                method,
                inputVariables,
                outputVariables,
                annotatedAt,
                annotatedBy);
    }

    public List<Variable> extractVarsAndType(String separator, QuerySolution querySolution, Map<String,String> variableNamesMap) {
        String dataValues = this.safeGetString(querySolution, variableNamesMap.get("value"));
        String dataTypes = this.safeGetString(querySolution, variableNamesMap.get("type"));
        if(dataTypes == null | dataValues == null)
            return new ArrayList<>();
        String[] dataValueArray = dataValues.split(separator);
        String[] dataTypesArray = dataTypes.split(separator);
        List<Variable> variables = new ArrayList<>();
        if (dataTypesArray.length == dataValueArray.length) {
            for (int i = 0; i < dataValueArray.length; i++) {
                variables.add(new Variable(dataTypesArray[i], dataValueArray[i]));
            }
        } else {
            throw new IllegalStateException("Mismatch between input data values and types.");
        }
        return variables;
    }


    // New helper method to safely retrieve a variable from QuerySolution
    public static String safeGetString(QuerySolution qs, String key) {
        if (qs.contains(key) && !qs.get(key).toString().strip().isEmpty()) {
            return qs.get(key).toString();
        }
        return null;
    }

    public MethodItem requestMethodItem(ExplanationMetaData data, String method)
            throws Exception {
        data.setMethod(method);
        String query = select_one_method(data);
        ResultSet result = this.selectWithResultSet(query);
        MethodItem methodItem = transformQuerySolutionToMethodItem(result.next());
        methodItem.setMethod(method);
        return methodItem;
    }
}
