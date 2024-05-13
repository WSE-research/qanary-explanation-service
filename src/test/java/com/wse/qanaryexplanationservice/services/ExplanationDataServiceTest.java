package com.wse.qanaryexplanationservice.services;

import com.wse.qanaryexplanationservice.helper.pojos.AutomatedTests.QanaryRequestPojos.QanaryResponseObject;
import com.wse.qanaryexplanationservice.helper.pojos.AutomatedTests.automatedTestingObject.AutomatedTestRequestBody;
import com.wse.qanaryexplanationservice.helper.pojos.AutomatedTests.automatedTestingObject.Example;
import com.wse.qanaryexplanationservice.repositories.AutomatedTestingRepository;
import com.wse.qanaryexplanationservice.repositories.ExplanationSparqlRepository;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class ExplanationDataServiceTest {

    private final Logger logger = LoggerFactory.getLogger(ExplanationDataServiceTest.class);
    @MockBean
    AutomatedTestingRepository automatedTestingRepository;
    @MockBean
    private ExplanationSparqlRepository explanationSparqlRepository;
    @MockBean
    private ExplanationService explanationService;

    @Test
    public void setupExampleDataTest() {

    }

    @Nested
    class InsertAutomatedTestsTest {

        @Autowired
        AutomatedTestingService automatedTestingService;
        private AutomatedTestRequestBody automatedTestRequestBody = new AutomatedTestRequestBody();
        private ArrayList<Example> examples = new ArrayList<Example>();
        private ServiceDataForTests serviceDataForTests = new ServiceDataForTests();
        private List<QuerySolutionMap> liste = new ArrayList<>();
        private ResultSet resultSet;

        @Autowired
        private ExplanationDataService explanationDataService;

        @BeforeEach
        public void setup() throws IOException {

            examples.add(new Example("annotationofinstance", true));
            QuerySolutionMap temp = new QuerySolutionMap();
            temp.add("hasQuestion", ResourceFactory.createPlainLiteral("Example_Question"));
            for (int i = 0; i < 20; i++) {
                liste.add(temp);
            }
            resultSet = serviceDataForTests.createResultSet(liste);
            automatedTestRequestBody.setTestingType("annotationofinstance");
            automatedTestRequestBody.setRuns(1);
            automatedTestRequestBody.setExamples(examples.toArray(Example[]::new));
            QanaryResponseObject qanaryResponseObject = new QanaryResponseObject();
            qanaryResponseObject.setEndpoint("endpoint");
            qanaryResponseObject.setInGraph("ingraph");
            qanaryResponseObject.setOutGraph(qanaryResponseObject.getInGraph());
            qanaryResponseObject.setQuestion("Example_question");
            when(automatedTestingRepository.takeRandomQuestion(any())).thenReturn(resultSet);
            when(automatedTestingRepository.executeQanaryPipeline(any())).thenReturn(qanaryResponseObject);
            when(automatedTestingRepository.executeSparqlQueryWithResultSet(any())).thenReturn(resultSet);
            when(explanationService.createModel(any(), any())).thenReturn(ModelFactory.createDefaultModel());
        }

        @Test
        public void leer() {
            assertEquals("Example_Question", resultSet.next().get("hasQuestion").asLiteral().getString());
        }

    }


}
