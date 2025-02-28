package com.wse.qanaryexplanationservice.services;

import com.wse.qanaryexplanationservice.helper.pojos.Variable;
import com.wse.qanaryexplanationservice.repositories.QanaryRepository;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.ResourceFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class ExplanationServiceTest {

    private final ServiceDataForTests testData = new ServiceDataForTests();
    private final String TEST_GRAPH = "test-graph";
    private final ClassLoader classLoader = this.getClass().getClassLoader();

    @Nested
    class PipelineTests {

        @MockBean
        private QanaryRepository qanaryRepository;
        @MockBean
        private TemplateExplanationsService templateExplanationsService;
        @Autowired
        private ExplanationService explanationService;
        private ResultSet results;

        @BeforeEach
        public void setUpRepository() throws IOException {
            results = testData.createResultSet(testData.getExampleQuerySolutionList());
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
            Assertions.assertTrue(results.hasNext());
            ResultSet resultSet = explanationService.getPipelineInformation(TEST_GRAPH);
            Assertions.assertNotNull(resultSet);
            Assertions.assertTrue(resultSet.hasNext());

            QuerySolution querySolution = resultSet.next();
            Assertions.assertEquals("test-graph", querySolution.get("graph").toString());
            querySolution = resultSet.next();
            assertEquals("component1", querySolution.get("component").toString());
            querySolution = resultSet.next();
            assertEquals("component2", querySolution.get("component").toString());
        }
    }

    @Nested
    class ExtractVarsAndTypeTest {
        private QanaryRepository qanaryRepository = new QanaryRepository();

        @Test
        public void extractVarsAndTypeTestWithNullQs() {
            QuerySolution qs = null;
            Assertions.assertThrows(NullPointerException.class, () -> {
                List<Variable> inputVariables = qanaryRepository.extractVarsAndType(",", qs, qanaryRepository.getSPARQL_VARNAME_INPUT_VARIABLES());
            });
        }

        @Test
        public void extractVarsAndTypeTestWithEmptyQs() {
            QuerySolutionMap qs = new QuerySolutionMap();
            qs.add("graph", ResourceFactory.createResource(TEST_GRAPH));
            List<Variable> inputVariables = qanaryRepository.extractVarsAndType(",", qs, qanaryRepository.getSPARQL_VARNAME_INPUT_VARIABLES());
            Assertions.assertEquals(0, inputVariables.size());
        }

        @Test
        public void extractVarsAndTypeTestWithInputDataVariables() {
            QuerySolutionMap qs = new QuerySolutionMap();
            qs.add("graph", ResourceFactory.createResource(TEST_GRAPH));
            qs.add("inputDataValues", ResourceFactory.createPlainLiteral("value1,value2,value3"));
            qs.add("inputDataTypes", ResourceFactory.createStringLiteral("type1,type2,type3"));
            List<Variable> inputVariables = qanaryRepository.extractVarsAndType(",", qs, qanaryRepository.getSPARQL_VARNAME_INPUT_VARIABLES());
            Assertions.assertEquals(3, inputVariables.size());
        }

        @Test
        public void extractVarsAndTypeTestWithOutputDataVariables() {
            QuerySolutionMap qs = new QuerySolutionMap();
            qs.add("graph", ResourceFactory.createResource(TEST_GRAPH));
            qs.add("outputDataValue", ResourceFactory.createPlainLiteral("value1"));
            qs.add("outputDataType", ResourceFactory.createStringLiteral("type1"));
            List<Variable> outputVariables = qanaryRepository.extractVarsAndType(",", qs, qanaryRepository.getSPARQL_VARNAME_OUTPUT_VARIABLES());
            Assertions.assertEquals(1, outputVariables.size());
        }
    }

}