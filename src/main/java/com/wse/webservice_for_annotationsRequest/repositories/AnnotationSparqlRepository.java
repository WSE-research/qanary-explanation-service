package com.wse.webservice_for_annotationsRequest.repositories;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Repository;

import java.net.MalformedURLException;

@Repository
public class AnnotationSparqlRepository extends AbstractRepository {

    public AnnotationSparqlRepository(Environment environment) throws MalformedURLException {
        super(environment);
        objectMapper = new ObjectMapper();
    }

}
