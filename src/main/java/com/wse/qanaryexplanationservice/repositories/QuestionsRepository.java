package com.wse.qanaryexplanationservice.repositories;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdfconnection.RDFConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository
public class QuestionsRepository {

    private static RDFConnection connection;
    private static Logger logger = LoggerFactory.getLogger(QuestionsRepository.class);

    public static ResultSet selectQuestion(String sparql) {
        QueryExecution queryExecution = connection.query(sparql);
        return queryExecution.execSelect();
    }

    @Value("${questions.triplestore.endpoint}")
    private void setConnection(String questionsTriplestoreUri) {
        connection = RDFConnection.connect(questionsTriplestoreUri);
    }

}
