package com.wse.qanaryexplanationservice.controller;

public class ControllerDataForTests {

    private String givenResults = "{\"bindings\":[{\"annotationId\":{\"type\":\"uri\",\"value\":\"tag:stardog:api:0.5264017467650085\"},\"type\":{\"type\":\"uri\",\"value\":\"http://www.wdaqua.eu/qa#AnnotationOfInstance\"},\"body\":{\"type\":\"uri\",\"value\":\"http://dbpedia.org/resource/String_theory\"},\"target\":{\"type\":\"bnode\",\"value\":\"b0\"},\"createdBy\":{\"type\":\"uri\",\"value\":\"urn:qanary:NED-DBpediaSpotlight\"},\"createdAt\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#dateTime\",\"type\":\"typed-literal\",\"value\":\"2023-08-08T09:05:31.387Z\"}}]}";
    private String givenExplanations = "{\"bindings\":[{\"annotationId\":{\"type\":\"uri\",\"value\":\"tag:stardog:api:0.8751403921456865\"},\"type\":{\"type\":\"uri\",\"value\":\"http://www.wdaqua.eu/qa#AnnotationOfInstance\"},\"body\":{\"type\":\"uri\",\"value\":\"http://dbpedia.org/resource/String_theory\"},\"source\":{\"type\":\"uri\",\"value\":\"http://demos.swe.htwk-leipzig.de:40111/question/stored-question__text_fd913cc4-4088-4eaf-a922-9fbe998096cd\"},\"start\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#integer\",\"type\":\"typed-literal\",\"value\":\"0\"},\"end\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#integer\",\"type\":\"typed-literal\",\"value\":\"4\"},\"createdBy\":{\"type\":\"uri\",\"value\":\"urn:qanary:QB-DBpediaSpotlight\"},\"createdAt\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#dateTime\",\"type\":\"typed-literal\",\"value\":\"2023-08-08T08:28:38.029Z\"},\"score\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#decimal\",\"type\":\"typed-literal\",\"value\":\"0.9347568085631697\"}}," +
            "{\"annotationId\":{\"type\":\"uri\",\"value\":\"tag:stardog:api:0.6958628947427131\"},\"type\":{\"type\":\"uri\",\"value\":\"http://www.wdaqua.eu/qa#AnnotationOfInstance\"},\"body\":{\"type\":\"uri\",\"value\":\"http://dbpedia.org/resource/Real_number\"},\"source\":{\"type\":\"uri\",\"value\":\"http://demos.swe.htwk-leipzig.de:40111/question/stored-question__text_fd913cc4-4088-4eaf-a922-9fbe998096cd\"},\"start\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#integer\",\"type\":\"typed-literal\",\"value\":\"12\"},\"end\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#integer\",\"type\":\"typed-literal\",\"value\":\"16\"},\"createdBy\":{\"type\":\"uri\",\"value\":\"urn:qanary:NED-DBpediaSpotlight\"},\"createdAt\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#dateTime\",\"type\":\"typed-literal\",\"value\":\"2023-08-08T08:28:38.238Z\"},\"score\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#decimal\",\"type\":\"typed-literal\",\"value\":\"0.977747974809564\"}}," +
            "{\"annotationId\":{\"type\":\"uri\",\"value\":\"tag:stardog:api:0.7064216296025844\"},\"type\":{\"type\":\"uri\",\"value\":\"http://www.wdaqua.eu/qa#AnnotationOfInstance\"},\"body\":{\"type\":\"uri\",\"value\":\"http://dbpedia.org/resource/Superman\"},\"source\":{\"type\":\"uri\",\"value\":\"http://demos.swe.htwk-leipzig.de:40111/question/stored-question__text_fd913cc4-4088-4eaf-a922-9fbe998096cd\"},\"start\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#integer\",\"type\":\"typed-literal\",\"value\":\"25\"},\"end\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#integer\",\"type\":\"typed-literal\",\"value\":\"33\"},\"createdBy\":{\"type\":\"uri\",\"value\":\"urn:qanary:NED-DBpediaSpotlight\"},\"createdAt\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#dateTime\",\"type\":\"typed-literal\",\"value\":\"2023-08-08T08:28:38.443Z\"},\"score\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#decimal\",\"type\":\"typed-literal\",\"value\":\"0.999238163684283\"}}]}";
    private String givenExplanationsWithoutQbValues = "{\"bindings\":[{\"annotationId\":{\"type\":\"uri\",\"value\":\"tag:stardog:api:0.8751403921456865\"},\"type\":{\"type\":\"uri\",\"value\":\"http://www.wdaqua.eu/qa#AnnotationOfInstance\"},\"body\":{\"type\":\"uri\",\"value\":\"http://dbpedia.org/resource/String_theory\"},\"source\":{\"type\":\"uri\",\"value\":\"http://demos.swe.htwk-leipzig.de:40111/question/stored-question__text_fd913cc4-4088-4eaf-a922-9fbe998096cd\"},\"start\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#integer\",\"type\":\"typed-literal\",\"value\":\"0\"},\"end\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#integer\",\"type\":\"typed-literal\",\"value\":\"4\"},\"createdBy\":{\"type\":\"uri\",\"value\":\"urn:qanary:NED-DBpediaSpotlight\"},\"createdAt\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#dateTime\",\"type\":\"typed-literal\",\"value\":\"2023-08-08T08:28:38.029Z\"},\"score\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#decimal\",\"type\":\"typed-literal\",\"value\":\"0.9347568085631697\"}}," +
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
}