package com.wse.qanaryexplanationservice.controller;

import com.wse.qanaryexplanationservice.pojos.ComponentPojo;
import com.wse.qanaryexplanationservice.pojos.ResultObjectDTOs.Component;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ControllerDataForTests {

    private final String[] qaExplanationModels = {
            "qa-system-explanation-models/modelForRandomComponent1.ttl",
            "qa-system-explanation-models/modelForRandomComponent2.ttl",
            "qa-system-explanation-models/modelForRandomComponent3.ttl"
    };
    private final ComponentPojo[] components = {
            new ComponentPojo(new Component("type", "randomComponent1")),
            new ComponentPojo(new Component("type", "randomComponent2")),
            new ComponentPojo(new Component("type", "randomComponent3"))
    };

    private final String EXPECTED_MODEL_FILE = "expectedModel.ttl";

    private String givenResults = "{\"bindings\":[{\"annotationId\":{\"type\":\"uri\",\"value\":\"tag:stardog:api:0.5264017467650085\"},\"type\":{\"type\":\"uri\",\"value\":\"http://www.wdaqua.eu/qa#AnnotationOfInstance\"},\"body\":{\"type\":\"uri\",\"value\":\"http://dbpedia.org/resource/String_theory\"},\"target\":{\"type\":\"bnode\",\"value\":\"b0\"},\"createdBy\":{\"type\":\"uri\",\"value\":\"urn:qanary:NED-DBpediaSpotlight\"},\"createdAt\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#dateTime\",\"type\":\"typed-literal\",\"value\":\"2023-08-08T09:05:31.387Z\"}}]}";
    private String givenExplanations = "{\"bindings\":[{\"annotationId\":{\"type\":\"uri\",\"value\":\"tag:stardog:api:0.8751403921456865\"},\"type\":{\"type\":\"uri\",\"value\":\"http://www.wdaqua.eu/qa#AnnotationOfInstance\"},\"body\":{\"type\":\"uri\",\"value\":\"http://dbpedia.org/resource/String_theory\"},\"source\":{\"type\":\"uri\",\"value\":\"http://demos.swe.htwk-leipzig.de:40111/question/stored-question__text_fd913cc4-4088-4eaf-a922-9fbe998096cd\"},\"start\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#integer\",\"type\":\"typed-literal\",\"value\":\"0\"},\"end\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#integer\",\"type\":\"typed-literal\",\"value\":\"4\"},\"createdBy\":{\"type\":\"uri\",\"value\":\"urn:qanary:QB-DBpediaSpotlight\"},\"createdAt\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#dateTime\",\"type\":\"typed-literal\",\"value\":\"2023-08-08T08:28:38.029Z\"},\"score\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#decimal\",\"type\":\"typed-literal\",\"value\":\"0.9347568085631697\"}}," +
            "{\"annotationId\":{\"type\":\"uri\",\"value\":\"tag:stardog:api:0.6958628947427131\"},\"type\":{\"type\":\"uri\",\"value\":\"http://www.wdaqua.eu/qa#AnnotationOfInstance\"},\"body\":{\"type\":\"uri\",\"value\":\"http://dbpedia.org/resource/Real_number\"},\"source\":{\"type\":\"uri\",\"value\":\"http://demos.swe.htwk-leipzig.de:40111/question/stored-question__text_fd913cc4-4088-4eaf-a922-9fbe998096cd\"},\"start\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#integer\",\"type\":\"typed-literal\",\"value\":\"12\"},\"end\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#integer\",\"type\":\"typed-literal\",\"value\":\"16\"},\"createdBy\":{\"type\":\"uri\",\"value\":\"urn:qanary:NED-DBpediaSpotlight\"},\"createdAt\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#dateTime\",\"type\":\"typed-literal\",\"value\":\"2023-08-08T08:28:38.238Z\"},\"score\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#decimal\",\"type\":\"typed-literal\",\"value\":\"0.977747974809564\"}}," +
            "{\"annotationId\":{\"type\":\"uri\",\"value\":\"tag:stardog:api:0.7064216296025844\"},\"type\":{\"type\":\"uri\",\"value\":\"http://www.wdaqua.eu/qa#AnnotationOfInstance\"},\"body\":{\"type\":\"uri\",\"value\":\"http://dbpedia.org/resource/Superman\"},\"source\":{\"type\":\"uri\",\"value\":\"http://demos.swe.htwk-leipzig.de:40111/question/stored-question__text_fd913cc4-4088-4eaf-a922-9fbe998096cd\"},\"start\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#integer\",\"type\":\"typed-literal\",\"value\":\"25\"},\"end\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#integer\",\"type\":\"typed-literal\",\"value\":\"33\"},\"createdBy\":{\"type\":\"uri\",\"value\":\"urn:qanary:NED-DBpediaSpotlight\"},\"createdAt\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#dateTime\",\"type\":\"typed-literal\",\"value\":\"2023-08-08T08:28:38.443Z\"},\"score\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#decimal\",\"type\":\"typed-literal\",\"value\":\"0.999238163684283\"}}]}";
    private String givenExplanationsWithoutQbValues = "{\"bindings\":[{\"annotationId\":{\"type\":\"uri\",\"value\":\"tag:stardog:api:0.8751403921456865\"},\"type\":{\"type\":\"uri\",\"value\":\"http://www.wdaqua.eu/qa#AnnotationOfInstance\"},\"body\":{\"type\":\"uri\",\"value\":\"http://dbpedia.org/resource/String_theory\"},\"source\":{\"type\":\"uri\",\"value\":\"http://demos.swe.htwk-leipzig.de:40111/question/stored-question__text_fd913cc4-4088-4eaf-a922-9fbe998096cd\"},\"start\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#integer\",\"type\":\"typed-literal\",\"value\":\"0\"},\"end\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#integer\",\"type\":\"typed-literal\",\"value\":\"4\"},\"createdBy\":{\"type\":\"uri\",\"value\":\"urn:qanary:NED-DBpediaSpotlight\"},\"createdAt\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#dateTime\",\"type\":\"typed-literal\",\"value\":\"2023-08-08T08:28:38.029Z\"},\"score\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#decimal\",\"type\":\"typed-literal\",\"value\":\"0.9347568085631697\"}}," +
            "{\"annotationId\":{\"type\":\"uri\",\"value\":\"tag:stardog:api:0.6958628947427131\"},\"type\":{\"type\":\"uri\",\"value\":\"http://www.wdaqua.eu/qa#AnnotationOfInstance\"},\"body\":{\"type\":\"uri\",\"value\":\"http://dbpedia.org/resource/Real_number\"},\"source\":{\"type\":\"uri\",\"value\":\"http://demos.swe.htwk-leipzig.de:40111/question/stored-question__text_fd913cc4-4088-4eaf-a922-9fbe998096cd\"},\"start\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#integer\",\"type\":\"typed-literal\",\"value\":\"12\"},\"end\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#integer\",\"type\":\"typed-literal\",\"value\":\"16\"},\"createdBy\":{\"type\":\"uri\",\"value\":\"urn:qanary:NED-DBpediaSpotlight\"},\"createdAt\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#dateTime\",\"type\":\"typed-literal\",\"value\":\"2023-08-08T08:28:38.238Z\"},\"score\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#decimal\",\"type\":\"typed-literal\",\"value\":\"0.977747974809564\"}}," +
            "{\"annotationId\":{\"type\":\"uri\",\"value\":\"tag:stardog:api:0.7064216296025844\"},\"type\":{\"type\":\"uri\",\"value\":\"http://www.wdaqua.eu/qa#AnnotationOfInstance\"},\"body\":{\"type\":\"uri\",\"value\":\"http://dbpedia.org/resource/Superman\"},\"source\":{\"type\":\"uri\",\"value\":\"http://demos.swe.htwk-leipzig.de:40111/question/stored-question__text_fd913cc4-4088-4eaf-a922-9fbe998096cd\"},\"start\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#integer\",\"type\":\"typed-literal\",\"value\":\"25\"},\"end\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#integer\",\"type\":\"typed-literal\",\"value\":\"33\"},\"createdBy\":{\"type\":\"uri\",\"value\":\"urn:qanary:NED-DBpediaSpotlight\"},\"createdAt\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#dateTime\",\"type\":\"typed-literal\",\"value\":\"2023-08-08T08:28:38.443Z\"},\"score\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#decimal\",\"type\":\"typed-literal\",\"value\":\"0.999238163684283\"}}]}";

    // creating a model from a file
    public ControllerDataForTests() {
    }

    public String getGivenResults() {
        return givenResults;
    }

    public String getGivenExplanations() {
        return givenExplanations;
    }

    public String getGivenExplanationsWithoutQbValues() {
        return givenExplanationsWithoutQbValues;
    }

    List<Model> createModels() throws FileNotFoundException {
        List<Model> models = new ArrayList<>();
        for (String modelFile : qaExplanationModels
        ) {
            Model model = ModelFactory.createDefaultModel();
            model.read(modelFile, "Turtle");
            models.add(model);
        }
        return models;
    }

    public List<Model> getModels() throws FileNotFoundException {
        return createModels();
    }

    public ComponentPojo[] getComponents() {
        return components;
    }

    public Map<String, Model> getQaSystemExplanationMap() throws FileNotFoundException {
        List<Model> models = getModels();
        return new HashMap<>() {{
            put(getComponents()[0].getComponent().getValue(), models.get(0));
            put(getComponents()[1].getComponent().getValue(), models.get(1));
            put(getComponents()[2].getComponent().getValue(), models.get(2));
        }};
    }

    public Model getExpectedModelForQaSystemExplanation() {
        Model model = ModelFactory.createDefaultModel();
        model.read(EXPECTED_MODEL_FILE);
        return model;
    }
}
