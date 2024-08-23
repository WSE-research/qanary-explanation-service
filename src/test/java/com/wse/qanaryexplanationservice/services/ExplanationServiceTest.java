package com.wse.qanaryexplanationservice.services;

import com.wse.qanaryexplanationservice.helper.dtos.QanaryExplanationData;
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
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
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
    @Autowired
    private TemplateExplanationsService templateExplanationsService;

    @BeforeEach
    public void setUpRepository() throws IOException {
        ResultSet results = testData.createResultSet(testData.getExampleQuerySolutionList());
        Mockito.when(qanaryRepository.selectWithResultSet(any())).thenReturn(results);
        Mockito.when(qanaryRepository.getQuestionFromQuestionId(any())).thenReturn("Example Question?");
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

    @Test
    public void explainPipeline() throws IOException {
        QanaryExplanationData testData = new QanaryExplanationData();
        testData.setGraph("testGraph");
        testData.setQuestionId("testQuestionId");
        testData.setComponent("MyQanaryPipeline");
        Map<String,String> map = new HashMap<>();
        map.put("component1", "explanation1"); map.put("component2", "explanation");
        testData.setExplanations(map);
        String explanation = this.explanationService.explain(testData);
        assertFalse(explanation.contains("${"));
        assertTrue(explanation.contains("The pipeline component MyQanaryPipeline has received the question \"Example Question?\" that is represented with the questionId \"testQuestionId\". It executed the\n" +
                "components component1, component2 on the knowledge graph \"testGraph\" with the following explanations:\n" +
                "\n" +
                "component1: explanation1\n" +
                "\n" +
                "component2: explanation"));
    }

}