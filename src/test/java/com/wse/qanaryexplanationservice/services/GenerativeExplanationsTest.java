package com.wse.qanaryexplanationservice.services;


import com.wse.qanaryexplanationservice.helper.AnnotationType;
import com.wse.qanaryexplanationservice.repositories.QuestionsRepository;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.ResourceFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class GenerativeExplanationsTest {

    private static final String EXAMPLE_QUESTION = "A question about the universe";
    @Autowired
    private GenerativeExplanations generativeExplanations;
    private ServiceDataForTests testData = new ServiceDataForTests();
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
        ArrayList<QuerySolutionMap> querySolutionMapList = new ArrayList<>() {{
            add(querySolutionMap);
        }};
        ResultSet testResultSet = testData.createResultSet(querySolutionMapList);
        setupRandomTest(testResultSet);
        String question = generativeExplanations.getRandomQuestion(23);
        Assertions.assertEquals(question, EXAMPLE_QUESTION);
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
        List<String> comps = generativeExplanations.selectRandomComponents(new ArrayList<>() {{
            add(AnnotationType.AnnotationOfInstance);
        }});
        assertTrue(!comps.isEmpty());
    }

    @Test
    public void createDatasetTest() {

    }


}
