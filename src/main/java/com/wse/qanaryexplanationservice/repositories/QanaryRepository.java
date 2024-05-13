package com.wse.qanaryexplanationservice.repositories;

import com.wse.qanaryexplanationservice.helper.pojos.AutomatedTests.QanaryRequestPojos.QanaryRequestObject;
import com.wse.qanaryexplanationservice.helper.pojos.AutomatedTests.QanaryRequestPojos.QanaryResponseObject;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdfconnection.RDFConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Repository;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Repository
public class QanaryRepository {

    private final static WebClient webClient = WebClient.builder().clientConnector(new ReactorClientHttpConnector(HttpClient.create().responseTimeout(Duration.ofSeconds(60)))).build();
    private final static Logger logger = LoggerFactory.getLogger(QanaryRequestObject.class);
    private static String QANARY_PIPELINE_HOST;
    private static int QANARY_PIPELINE_PORT;
    private static String VIRTUOSO_ENDPOINT;
    private static RDFConnection connection;

    public QanaryRepository() {
    }

    public static RDFConnection getConnection() {
        return connection;
    }

    public static QanaryResponseObject executeQanaryPipeline(QanaryRequestObject qanaryRequestObject) {

        MultiValueMap<String, String> multiValueMap = new LinkedMultiValueMap();
        multiValueMap.add("question", qanaryRequestObject.getQuestion());
        multiValueMap.addAll(qanaryRequestObject.getComponentListAsMap());

        QanaryResponseObject responseObject = webClient.post().uri(uriBuilder -> uriBuilder // TODO: use new endpoint for question answering
                        .scheme("http").host(QANARY_PIPELINE_HOST).port(QANARY_PIPELINE_PORT).path("/startquestionansweringwithtextquestion")
                        .queryParams(multiValueMap)
                        .build())
                .retrieve()
                .bodyToMono(QanaryResponseObject.class)
                .block();

        logger.info("Response Object: {}", responseObject);

        return responseObject;
    }

    public static ResultSet selectWithResultSet(String sparql) {
        QueryExecution queryExecution = connection.query(sparql);
        return queryExecution.execSelect();
    }

    @Value("${qanary.pipeline.host}")
    public void setQanaryPipelineHost(String qanaryPipelineHost) {
        QANARY_PIPELINE_HOST = qanaryPipelineHost;
    }

    @Value("${qanary.pipeline.port}")
    public void setQanaryPipelinePort(int qanaryPipelinePort) {
        QANARY_PIPELINE_PORT = qanaryPipelinePort;
    }

    @Value("${sparql.endpoint}")
    public void setVirtuosoEndpoint(String sparqlEndpoint) {
        connection = RDFConnection.connect(sparqlEndpoint);
    }

}
