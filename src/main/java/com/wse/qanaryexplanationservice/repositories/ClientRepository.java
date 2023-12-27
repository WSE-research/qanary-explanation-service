package com.wse.qanaryexplanationservice.repositories;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Repository;

import java.net.MalformedURLException;

@Repository
public class ClientRepository extends AbstractRepository {

    public ClientRepository(Environment environment) throws MalformedURLException {
        super(environment);
        this.objectMapper = new ObjectMapper();
    }

}
