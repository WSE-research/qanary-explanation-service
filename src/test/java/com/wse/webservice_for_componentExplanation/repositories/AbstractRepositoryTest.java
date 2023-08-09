package com.wse.webservice_for_componentExplanation.repositories;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.net.MalformedURLException;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class AbstractRepositoryTest {

    @Mock
    private AbstractRepository abstractRepository;


    @Test
    public void fetchEmptyQuestionTest() {
        String testQuestion = null;
        assertNull(abstractRepository.fetchQuestion(testQuestion));
    }

    public void setupWebclientForExistingQuestionTest() throws MalformedURLException {

    }

    @Test
    public void fetchExistingQuestionTest() throws MalformedURLException {

    }

    private void setup_executeSparqlQueryTest() throws IOException {
        Mockito.when(abstractRepository.getInputStream(any())).thenReturn(null);
    }

    // Check for correct sparqlQuery done in service tests
    @Test
    public void executeSparqlQueryTest() throws IOException {
        setup_executeSparqlQueryTest();
        String sparqlQuery = "";

        JsonNode node = abstractRepository.executeSparqlQuery(sparqlQuery);
        assertNull(node);
    }


}
