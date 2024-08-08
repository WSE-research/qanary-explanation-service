package com.wse.qanaryexplanationservice.services;

import com.wse.qanaryexplanationservice.repositories.QanaryRepository;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class ExplanationServiceTest {

    private final ServiceDataForTests testData = new ServiceDataForTests();
    private final String TEST_GRAPH = "test-graph";
    private final ClassLoader classLoader = this.getClass().getClassLoader();
    @Autowired
    private ExplanationService explanationService;
    @MockBean
    private QanaryRepository qanaryRepository;
    @MockBean
    private TemplateExplanationsService templateExplanationsService;

    @BeforeEach
    public void setUpRepository() {
        ResultSet results = testData.createResultSet(testData.getExampleQuerySolutionList());
        Mockito.when(qanaryRepository.selectWithResultSet(any())).thenReturn(results);
        Mockito.when(qanaryRepository.getQuestionFromQuestionId(any())).thenReturn("Example Question?");
        Mockito.when(templateExplanationsService.getPipelineInputExplanation(any())).thenReturn("A");
        Mockito.when(templateExplanationsService.getPipelineOutputExplanation((ResultSet) any(), any())).thenReturn("B");
    }

    /**
     * TODO: Centralize values and parameterize test
     */
    @Test
    public void getPipelineInformationTest() throws IOException {
        ResultSet resultSet = explanationService.getPipelineInformation(TEST_GRAPH);
        Assertions.assertNotNull(resultSet);

        QuerySolution querySolution = resultSet.next();
        Assertions.assertEquals("test-graph", querySolution.get("graph").toString());
        querySolution = resultSet.next();
        assertEquals("component1", querySolution.get("component").toString());
        querySolution = resultSet.next();
        assertEquals("component2", querySolution.get("component").toString());
    }

}