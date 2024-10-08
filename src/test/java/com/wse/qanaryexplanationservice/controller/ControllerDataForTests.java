package com.wse.qanaryexplanationservice.controller;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import java.util.*;

public class ControllerDataForTests {

    private final String[] qaExplanationModels = {
            "qa-system-explanation-models/modelForRandomComponent1.ttl",
            "qa-system-explanation-models/modelForRandomComponent2.ttl"
    };
    private static final List<String> components = Arrays.asList("randomComponent1", "randomComponent2");

    private static final String EXPECTED_MODEL_FILE = "qa-system-explanation-models/expectedModel.ttl";

    private static final String givenResults = "{\"bindings\":[{\"annotationId\":{\"type\":\"uri\",\"value\":\"tag:stardog:api:0.5264017467650085\"},\"type\":{\"type\":\"uri\",\"value\":\"http://www.wdaqua.eu/qa#AnnotationOfInstance\"},\"body\":{\"type\":\"uri\",\"value\":\"http://dbpedia.org/resource/String_theory\"},\"target\":{\"type\":\"bnode\",\"value\":\"b0\"},\"createdBy\":{\"type\":\"uri\",\"value\":\"urn:qanary:NED-DBpediaSpotlight\"},\"createdAt\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#dateTime\",\"type\":\"typed-literal\",\"value\":\"2023-08-08T09:05:31.387Z\"}}]}";
    private static final String givenExplanations = "{\"bindings\":[{\"annotationId\":{\"type\":\"uri\",\"value\":\"tag:stardog:api:0.8751403921456865\"},\"type\":{\"type\":\"uri\",\"value\":\"http://www.wdaqua.eu/qa#AnnotationOfInstance\"},\"body\":{\"type\":\"uri\",\"value\":\"http://dbpedia.org/resource/String_theory\"},\"source\":{\"type\":\"uri\",\"value\":\"http://demos.swe.htwk-leipzig.de:40111/question/stored-question__text_fd913cc4-4088-4eaf-a922-9fbe998096cd\"},\"start\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#integer\",\"type\":\"typed-literal\",\"value\":\"0\"},\"end\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#integer\",\"type\":\"typed-literal\",\"value\":\"4\"},\"createdBy\":{\"type\":\"uri\",\"value\":\"urn:qanary:QB-DBpediaSpotlight\"},\"createdAt\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#dateTime\",\"type\":\"typed-literal\",\"value\":\"2023-08-08T08:28:38.029Z\"},\"score\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#decimal\",\"type\":\"typed-literal\",\"value\":\"0.9347568085631697\"}}," +
            "{\"annotationId\":{\"type\":\"uri\",\"value\":\"tag:stardog:api:0.6958628947427131\"},\"type\":{\"type\":\"uri\",\"value\":\"http://www.wdaqua.eu/qa#AnnotationOfInstance\"},\"body\":{\"type\":\"uri\",\"value\":\"http://dbpedia.org/resource/Real_number\"},\"source\":{\"type\":\"uri\",\"value\":\"http://demos.swe.htwk-leipzig.de:40111/question/stored-question__text_fd913cc4-4088-4eaf-a922-9fbe998096cd\"},\"start\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#integer\",\"type\":\"typed-literal\",\"value\":\"12\"},\"end\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#integer\",\"type\":\"typed-literal\",\"value\":\"16\"},\"createdBy\":{\"type\":\"uri\",\"value\":\"urn:qanary:NED-DBpediaSpotlight\"},\"createdAt\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#dateTime\",\"type\":\"typed-literal\",\"value\":\"2023-08-08T08:28:38.238Z\"},\"score\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#decimal\",\"type\":\"typed-literal\",\"value\":\"0.977747974809564\"}}," +
            "{\"annotationId\":{\"type\":\"uri\",\"value\":\"tag:stardog:api:0.7064216296025844\"},\"type\":{\"type\":\"uri\",\"value\":\"http://www.wdaqua.eu/qa#AnnotationOfInstance\"},\"body\":{\"type\":\"uri\",\"value\":\"http://dbpedia.org/resource/Superman\"},\"source\":{\"type\":\"uri\",\"value\":\"http://demos.swe.htwk-leipzig.de:40111/question/stored-question__text_fd913cc4-4088-4eaf-a922-9fbe998096cd\"},\"start\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#integer\",\"type\":\"typed-literal\",\"value\":\"25\"},\"end\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#integer\",\"type\":\"typed-literal\",\"value\":\"33\"},\"createdBy\":{\"type\":\"uri\",\"value\":\"urn:qanary:NED-DBpediaSpotlight\"},\"createdAt\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#dateTime\",\"type\":\"typed-literal\",\"value\":\"2023-08-08T08:28:38.443Z\"},\"score\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#decimal\",\"type\":\"typed-literal\",\"value\":\"0.999238163684283\"}}]}";
    private static final String givenExplanationsWithoutQbValues = "{\"bindings\":[{\"annotationId\":{\"type\":\"uri\",\"value\":\"tag:stardog:api:0.8751403921456865\"},\"type\":{\"type\":\"uri\",\"value\":\"http://www.wdaqua.eu/qa#AnnotationOfInstance\"},\"body\":{\"type\":\"uri\",\"value\":\"http://dbpedia.org/resource/String_theory\"},\"source\":{\"type\":\"uri\",\"value\":\"http://demos.swe.htwk-leipzig.de:40111/question/stored-question__text_fd913cc4-4088-4eaf-a922-9fbe998096cd\"},\"start\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#integer\",\"type\":\"typed-literal\",\"value\":\"0\"},\"end\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#integer\",\"type\":\"typed-literal\",\"value\":\"4\"},\"createdBy\":{\"type\":\"uri\",\"value\":\"urn:qanary:NED-DBpediaSpotlight\"},\"createdAt\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#dateTime\",\"type\":\"typed-literal\",\"value\":\"2023-08-08T08:28:38.029Z\"},\"score\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#decimal\",\"type\":\"typed-literal\",\"value\":\"0.9347568085631697\"}}," +
            "{\"annotationId\":{\"type\":\"uri\",\"value\":\"tag:stardog:api:0.6958628947427131\"},\"type\":{\"type\":\"uri\",\"value\":\"http://www.wdaqua.eu/qa#AnnotationOfInstance\"},\"body\":{\"type\":\"uri\",\"value\":\"http://dbpedia.org/resource/Real_number\"},\"source\":{\"type\":\"uri\",\"value\":\"http://demos.swe.htwk-leipzig.de:40111/question/stored-question__text_fd913cc4-4088-4eaf-a922-9fbe998096cd\"},\"start\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#integer\",\"type\":\"typed-literal\",\"value\":\"12\"},\"end\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#integer\",\"type\":\"typed-literal\",\"value\":\"16\"},\"createdBy\":{\"type\":\"uri\",\"value\":\"urn:qanary:NED-DBpediaSpotlight\"},\"createdAt\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#dateTime\",\"type\":\"typed-literal\",\"value\":\"2023-08-08T08:28:38.238Z\"},\"score\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#decimal\",\"type\":\"typed-literal\",\"value\":\"0.977747974809564\"}}," +
            "{\"annotationId\":{\"type\":\"uri\",\"value\":\"tag:stardog:api:0.7064216296025844\"},\"type\":{\"type\":\"uri\",\"value\":\"http://www.wdaqua.eu/qa#AnnotationOfInstance\"},\"body\":{\"type\":\"uri\",\"value\":\"http://dbpedia.org/resource/Superman\"},\"source\":{\"type\":\"uri\",\"value\":\"http://demos.swe.htwk-leipzig.de:40111/question/stored-question__text_fd913cc4-4088-4eaf-a922-9fbe998096cd\"},\"start\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#integer\",\"type\":\"typed-literal\",\"value\":\"25\"},\"end\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#integer\",\"type\":\"typed-literal\",\"value\":\"33\"},\"createdBy\":{\"type\":\"uri\",\"value\":\"urn:qanary:NED-DBpediaSpotlight\"},\"createdAt\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#dateTime\",\"type\":\"typed-literal\",\"value\":\"2023-08-08T08:28:38.443Z\"},\"score\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#decimal\",\"type\":\"typed-literal\",\"value\":\"0.999238163684283\"}}]}";

    public String getGivenResults() {
        return givenResults;
    }

    public String getGivenExplanations() {
        return givenExplanations;
    }

    public String getGivenExplanationsWithoutQbValues() {
        return givenExplanationsWithoutQbValues;
    }

    List<Model> createModels() {
        List<Model> models = new ArrayList<>();
        for (String modelFile : qaExplanationModels
        ) {
            Model model = ModelFactory.createDefaultModel();
            model.read(modelFile, "Turtle");
            models.add(model);
        }
        return models;
    }

    public List<Model> getModels() {
        return createModels();
    }

    public List<String> getComponents() {
        return components;
    }

    public Map<String, Model> getQaSystemExplanationMap() {
        List<Model> models = getModels();
        return new HashMap<>() {{
            put(getComponents().get(0), models.get(0));
            put(getComponents().get(1), models.get(1));
        }};
    }

    public Model getExpectedModelForQaSystemExplanation() {
        Model model = ModelFactory.createDefaultModel();
        model.read(EXPECTED_MODEL_FILE, "TURTLE");
        return model;
    }
}
