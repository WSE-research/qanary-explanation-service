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

    private final WebClient webClient = WebClient.builder().clientConnector(new ReactorClientHttpConnector(HttpClient.create().responseTimeout(Duration.ofSeconds(60)))).build();
    private final Logger logger = LoggerFactory.getLogger(QanaryRequestObject.class);
    private final String QANARY_PIPELINE_HOST;
    private final int QANARY_PIPELINE_PORT;
    private VirtGraph connection;

    public QanaryRepository(
            @Value("${virtuoso.triplestore.endpoint}") String virtuosoEndpoint,
            @Value("${virtuoso.triplestore.username}") String virtuosoUser,
            @Value("${virtuoso.triplestore.password}") String virtuosoPassword,
            @Value("${qanary.pipeline.host}") String qanaryHost,
            @Value("${qanary.pipeline.port}") int qanaryPort
    ) {
        this.initConnection(virtuosoEndpoint, virtuosoUser, virtuosoPassword);
        this.QANARY_PIPELINE_HOST = qanaryHost;
        this.QANARY_PIPELINE_PORT = qanaryPort;
    }

    public QanaryResponseObject executeQanaryPipeline(QanaryRequestObject qanaryRequestObject) {

        MultiValueMap<String, String> multiValueMap = new LinkedMultiValueMap();
        multiValueMap.add("question", qanaryRequestObject.getQuestion());
        multiValueMap.addAll(qanaryRequestObject.getComponentListAsMap());

        return webClient.post().uri(uriBuilder -> uriBuilder // TODO: use new endpoint for question answering
                        .scheme("http").host(QANARY_PIPELINE_HOST).port(QANARY_PIPELINE_PORT).path("/startquestionansweringwithtextquestion")
                        .queryParams(multiValueMap)
                        .build())
                .retrieve()
                .bodyToMono(QanaryResponseObject.class)
                .block();
    }

    public ResultSet selectWithResultSet(String sparql) throws QueryException {
        Query query = QueryFactory.create(sparql);
        VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(query, this.connection);
        ResultSetRewindable results = ResultSetFactory.makeRewindable(vqe.execSelect());
        return results;
    }

    public void initConnection(String virtEndpoint, String virtUser, String virtPassword) {
        logger.info("Init connection for Qanary repository: {}", virtEndpoint);
        connection = new VirtGraph(virtEndpoint, virtUser, virtPassword);
    }

}
