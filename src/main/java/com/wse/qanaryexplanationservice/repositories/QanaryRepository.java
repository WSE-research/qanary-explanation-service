package com.wse.qanaryexplanationservice.repositories;

import com.wse.qanaryexplanationservice.helper.pojos.AutomatedTests.QanaryRequestPojos.QanaryRequestObject;
import com.wse.qanaryexplanationservice.helper.pojos.AutomatedTests.QanaryRequestPojos.QanaryResponseObject;
import org.apache.jena.query.*;
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

import java.time.Duration;

/**
 * This class provides different request methods against the Qanary pipeline or the underlying triplestore
 */
@Repository
public class QanaryRepository {
    private static final String QUESTION_ANSWERING_ENDPOINT = "/startquestionansweringwithtextquestion";
    private static final String HTTP_SCHEME = "http";
    private static final int TIMEOUT_SECONDS = 60;

    private final WebClient webClient;
    private final Logger logger = LoggerFactory.getLogger(QanaryRepository.class);

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
}
