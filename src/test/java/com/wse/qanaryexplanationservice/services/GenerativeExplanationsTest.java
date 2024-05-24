package com.wse.qanaryexplanationservice.services;


import com.wse.qanaryexplanationservice.helper.AnnotationType;
import com.wse.qanaryexplanationservice.helper.pojos.AutomatedTests.automatedTestingObject.AutomatedTest;
import com.wse.qanaryexplanationservice.helper.pojos.AutomatedTests.automatedTestingObject.Example;
import com.wse.qanaryexplanationservice.helper.pojos.AutomatedTests.automatedTestingObject.TestDataObject;
import com.wse.qanaryexplanationservice.repositories.QanaryRepository;
import com.wse.qanaryexplanationservice.repositories.QuestionsRepository;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.ResourceFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.event.annotation.BeforeTestClass;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class GenerativeExplanationsTest {

    @Autowired
    private GenerativeExplanations generativeExplanations;
    private ServiceDataForTests testData = new ServiceDataForTests();

    private static final String EXAMPLE_QUESTION = "A question about the universe";
    private MockedStatic<QuestionsRepository> repositoryMockedStatic;

    @BeforeEach
    public void beforeEach() {
        repositoryMockedStatic = mockStatic(QuestionsRepository.class);
    }
    @AfterEach
    public void afterEach() {
        repositoryMockedStatic.close();
    }

    public void setupRandomTest(ResultSet resultSet) {
        repositoryMockedStatic.when(() -> QuestionsRepository.selectQuestion(any())).thenReturn(resultSet);
    }

    ////////// getRandomQuestion ///////////

    @Test
    public void getRandomQuestionTestSuccessful() throws IOException, IOException {
        QuerySolutionMap querySolutionMap = new QuerySolutionMap();
        querySolutionMap.add("hasQuestion", ResourceFactory.createStringLiteral(EXAMPLE_QUESTION));
        ArrayList<QuerySolutionMap> querySolutionMapList = new ArrayList<>() {{add(querySolutionMap);}};
        ResultSet testResultSet = testData.createResultSet(querySolutionMapList);
        setupRandomTest(testResultSet);
        String question = generativeExplanations.getRandomQuestion(23);
        Assertions.assertEquals(question,EXAMPLE_QUESTION);
    }

    @Test
    public void getRandomQuestionTestFaiuĺure() throws IOException {
        ArrayList<QuerySolutionMap> querySolutionMapList = new ArrayList<>();
        ResultSet testResultSet = testData.createResultSet(querySolutionMapList);
        setupRandomTest(testResultSet);
        Exception exception = assertThrows(RuntimeException.class, () -> {
            generativeExplanations.getRandomQuestion(23);
        });
        assertEquals(exception.getMessage(),"The executed SPARQL query returned zero results");
    }

    ////////// selectRandomComponents ////////////

    @Test
    public void selectRandomComponentsTestNull() {
        List<String> comps = generativeExplanations.selectRandomComponents(null);
        assertEquals(new ArrayList<>(), comps);
    }
    @Test
    public void selectRandomComponentsTestEmpty() {
        List<String> comps = generativeExplanations.selectRandomComponents(new ArrayList<>());
        assertEquals(new ArrayList<>(), comps);
    }
    @Test
    public void selectRandomComponentsTestNotEmpty() {
        List<String> comps = generativeExplanations.selectRandomComponents(new ArrayList<>() {{add(AnnotationType.AnnotationOfInstance);}});
        assertTrue(!comps.isEmpty());
    }

    @Test
    public void createDatasetTest() {

    }


}
