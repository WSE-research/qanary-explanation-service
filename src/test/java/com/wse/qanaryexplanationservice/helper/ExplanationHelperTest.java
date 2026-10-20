package com.wse.qanaryexplanationservice.helper;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.junit.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ExplanationHelperTest {

    private final ExplanationHelper explanationHelper = new ExplanationHelper();
    private final Resource exampleResource = new ResourceImpl("http://example.org/resource");
    private final Literal exampleLiteral = ModelFactory.createDefaultModel().createLiteral("Test Literal");


    @Test
    public void convertQuerySolutionToMapWithRdfNodesWithNull() {
        QuerySolution querySolution = null;
        assertThrows(NullPointerException.class, () -> {
            ExplanationHelper.convertQuerySolutionToMapWithRdfNodes(querySolution);
        });
    }

    @Test
    public void convertQuerySolutionToMapWithRdfNodesWithNonNull() {
        QuerySolutionMap qs = new QuerySolutionMap();
        qs.add("subject", exampleResource);
        qs.add("label", exampleLiteral);
        Map<String, RDFNode> result = ExplanationHelper.convertQuerySolutionToMapWithRdfNodes(qs);

        assertEquals(2, result.size());
        assertTrue(result.containsKey("subject"));
        assertTrue(result.containsKey("label"));
        assertEquals(exampleResource, result.get("subject"));
        assertEquals(exampleLiteral, result.get("label"));
    }

    @Test
    public void convertQuerySolutionToMapWithNull() {
        QuerySolution querySolution = null;
        assertThrows(NullPointerException.class, () -> {
            ExplanationHelper.convertQuerySolutionToMap(querySolution);
        });
    }

    @Test
    public void convertQuerySolutionToMapWithNonNull() {
        QuerySolutionMap qs = new QuerySolutionMap();
        qs.add("subject", exampleResource);
        qs.add("label", exampleLiteral);

        Map<String, String> result = ExplanationHelper.convertQuerySolutionToMap(qs);
        assertTrue(result.containsKey("subject"));
        assertTrue(result.containsKey("label"));
        assertEquals(exampleResource.toString(), result.get("subject"));
        assertEquals(exampleLiteral.toString(), result.get("label"));
    }




}
