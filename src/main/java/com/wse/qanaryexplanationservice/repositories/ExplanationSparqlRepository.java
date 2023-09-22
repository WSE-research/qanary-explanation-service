package com.wse.qanaryexplanationservice.repositories;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Repository;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.MalformedURLException;

@Repository
public class ExplanationSparqlRepository extends AbstractRepository {

    public ExplanationSparqlRepository(Environment environment) throws MalformedURLException {
        super(environment);
        objectMapper = new ObjectMapper();
        webClient = WebClient.create();
    }
    public ExplanationSparqlRepository() throws MalformedURLException {
        super(null);
    }

}
