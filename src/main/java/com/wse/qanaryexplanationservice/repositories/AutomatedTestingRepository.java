package com.wse.qanaryexplanationservice.repositories;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wse.qanaryexplanationservice.helper.pojos.AutomatedTests.QanaryRequestPojos.QanaryRequestObject;
import com.wse.qanaryexplanationservice.helper.pojos.AutomatedTests.QanaryRequestPojos.QanaryResponseObject;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdfconnection.RDFConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Repository;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.net.MalformedURLException;
import java.time.Duration;

@Repository
@Configuration
public class AutomatedTestingRepository extends AbstractRepository {
    private Logger logger = LoggerFactory.getLogger(AutomatedTestingRepository.class);

    public AutomatedTestingRepository(Environment environment) throws MalformedURLException {
        super(environment);
        this.objectMapper = new ObjectMapper();
        this.webClient = WebClient.builder().clientConnector(new ReactorClientHttpConnector(HttpClient.create().responseTimeout(Duration.ofSeconds(60)))).build();
    }

    public QanaryResponseObject executeQanaryPipeline(QanaryRequestObject qanaryRequestObject) {

        MultiValueMap<String, String> multiValueMap = new LinkedMultiValueMap();
        multiValueMap.add("question", qanaryRequestObject.getQuestion());

        for (String component : qanaryRequestObject.getComponentlist()
        ) {
            logger.info(component);
            multiValueMap.add("componentlist[]", component);
        }

        QanaryResponseObject responseObject = webClient.post().uri(uriBuilder -> uriBuilder // TODO: localhost as env-variable
                        .scheme("http").host("localhost").port(8080).path("/startquestionansweringwithtextquestion")
                        .queryParams(multiValueMap)
                        .build())
                .retrieve()
                .bodyToMono(QanaryResponseObject.class)
                .block();

        logger.info("Response Object: {}", responseObject);

        return responseObject;
    }

    public ResultSet takeRandomQuestion(String query) throws RuntimeException {
        logger.info("Taking random question");
        RDFConnection rdfConnection1 = RDFConnection.connect("http://localhost:8890/sparql");
        QueryExecution queryExecution = rdfConnection1.query(query);
        return queryExecution.execSelect();
    }

}
