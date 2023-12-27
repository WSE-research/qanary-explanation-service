package com.wse.qanaryexplanationservice.repositories;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Repository;

import java.net.MalformedURLException;

@Repository
public class ClientRepository extends AbstractRepository{

    protected ClientRepository(Environment environment) throws MalformedURLException {
        super(environment);
    }

}
