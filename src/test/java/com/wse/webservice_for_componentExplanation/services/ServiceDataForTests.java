package com.wse.webservice_for_componentExplanation.services;

public class ServiceDataForTests {

    private final String jsonExplanationObjects = "{\"bindings\":[{\"annotationId\":{\"type\":\"uri\",\"value\":\"tag:stardog:api:0.7885976199321202\"},\"type\":{\"type\":\"uri\",\"value\":\"http://www.wdaqua.eu/qa#AnnotationOfInstance\"},\"body\":{\"type\":\"uri\",\"value\":\"http://dbpedia.org/resource/String_theory\"},\"source\":{\"type\":\"uri\",\"value\":\"http://demos.swe.htwk-leipzig.de:40111/question/stored-question__text_0d5a7c26-ec6a-4702-8d7f-999849938d0e\"},\"start\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#integer\",\"type\":\"typed-literal\",\"value\":\"0\"},\"end\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#integer\",\"type\":\"typed-literal\",\"value\":\"4\"},\"createdBy\":{\"type\":\"uri\",\"value\":\"urn:qanary:NED-DBpediaSpotlight\"},\"createdAt\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#dateTime\",\"type\":\"typed-literal\",\"value\":\"2023-08-09T06:39:32.615Z\"},\"score\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#decimal\",\"type\":\"typed-literal\",\"value\":\"0.9347568085631697\"}},{\"annotationId\":{\"type\":\"uri\",\"value\":\"tag:stardog:api:0.12726840785317994\"},\"type\":{\"type\":\"uri\",\"value\":\"http://www.wdaqua.eu/qa#AnnotationOfInstance\"},\"body\":{\"type\":\"uri\",\"value\":\"http://dbpedia.org/resource/Real_number\"},\"source\":{\"type\":\"uri\",\"value\":\"http://demos.swe.htwk-leipzig.de:40111/question/stored-question__text_0d5a7c26-ec6a-4702-8d7f-999849938d0e\"},\"start\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#integer\",\"type\":\"typed-literal\",\"value\":\"12\"},\"end\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#integer\",\"type\":\"typed-literal\",\"value\":\"16\"},\"createdBy\":{\"type\":\"uri\",\"value\":\"urn:qanary:NED-DBpediaSpotlight\"},\"createdAt\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#dateTime\",\"type\":\"typed-literal\",\"value\":\"2023-08-09T06:39:32.826Z\"},\"score\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#decimal\",\"type\":\"typed-literal\",\"value\":\"0.977747974809564\"}},{\"annotationId\":{\"type\":\"uri\",\"value\":\"tag:stardog:api:0.0010094414975868604\"},\"type\":{\"type\":\"uri\",\"value\":\"http://www.wdaqua.eu/qa#AnnotationOfInstance\"},\"body\":{\"type\":\"uri\",\"value\":\"http://dbpedia.org/resource/Batman\"},\"source\":{\"type\":\"uri\",\"value\":\"http://demos.swe.htwk-leipzig.de:40111/question/stored-question__text_0d5a7c26-ec6a-4702-8d7f-999849938d0e\"},\"start\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#integer\",\"type\":\"typed-literal\",\"value\":\"25\"},\"end\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#integer\",\"type\":\"typed-literal\",\"value\":\"31\"},\"createdBy\":{\"type\":\"uri\",\"value\":\"urn:qanary:NED-DBpediaSpotlight\"},\"createdAt\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#dateTime\",\"type\":\"typed-literal\",\"value\":\"2023-08-09T06:39:33.034Z\"},\"score\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#decimal\",\"type\":\"typed-literal\",\"value\":\"0.9999536254316278\"}}]}";
    private final String jsonResultObjects = "{\"bindings\":[{\"annotationId\":{\"type\":\"uri\",\"value\":\"tag:stardog:api:0.7885976199321202\"},\"type\":{\"type\":\"uri\",\"value\":\"http://www.wdaqua.eu/qa#AnnotationOfInstance\"},\"body\":{\"type\":\"uri\",\"value\":\"http://dbpedia.org/resource/String_theory\"},\"target\":{\"type\":\"bnode\",\"value\":\"b0\"},\"createdBy\":{\"type\":\"uri\",\"value\":\"urn:qanary:NED-DBpediaSpotlight\"},\"createdAt\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#dateTime\",\"type\":\"typed-literal\",\"value\":\"2023-08-09T06:39:32.615Z\"}},{\"annotationId\":{\"type\":\"uri\",\"value\":\"tag:stardog:api:0.12726840785317994\"},\"type\":{\"type\":\"uri\",\"value\":\"http://www.wdaqua.eu/qa#AnnotationOfInstance\"},\"body\":{\"type\":\"uri\",\"value\":\"http://dbpedia.org/resource/Real_number\"},\"target\":{\"type\":\"bnode\",\"value\":\"b1\"},\"createdBy\":{\"type\":\"uri\",\"value\":\"urn:qanary:NED-DBpediaSpotlight\"},\"createdAt\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#dateTime\",\"type\":\"typed-literal\",\"value\":\"2023-08-09T06:39:32.826Z\"}},{\"annotationId\":{\"type\":\"uri\",\"value\":\"tag:stardog:api:0.0010094414975868604\"},\"type\":{\"type\":\"uri\",\"value\":\"http://www.wdaqua.eu/qa#AnnotationOfInstance\"},\"body\":{\"type\":\"uri\",\"value\":\"http://dbpedia.org/resource/Batman\"},\"target\":{\"type\":\"bnode\",\"value\":\"b2\"},\"createdBy\":{\"type\":\"uri\",\"value\":\"urn:qanary:NED-DBpediaSpotlight\"},\"createdAt\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#dateTime\",\"type\":\"typed-literal\",\"value\":\"2023-08-09T06:39:33.034Z\"}},{\"annotationId\":{\"type\":\"uri\",\"value\":\"tag:stardog:api:0.9600168603465082\"},\"type\":{\"type\":\"uri\",\"value\":\"http://www.wdaqua.eu/qa#AnnotationOfAnswerSPARQL\"},\"body\":{\"type\":\"literal\",\"value\":\"PREFIX  rdfs: <http://www.w3.org/2000/01/rdf-schema#>\\nPREFIX  dct:  <http://purl.org/dc/terms/>\\nPREFIX  dbr:  <http://dbpedia.org/resource/>\\nPREFIX  rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\\nPREFIX  foaf: <http://xmlns.com/foaf/0.1/>\\n\\nSELECT  *\\nWHERE\\n  { ?resource  foaf:name  ?answer ;\\n              rdfs:label  ?label\\n    FILTER ( lang(?label) = \\\"en\\\" )\\n    ?resource  dct:subject  dbr:Category:Superheroes_with_alter_egos\\n    FILTER ( ! strstarts(lcase(?label), lcase(?answer)) )\\n    VALUES ?resource { dbr:Batman }\\n  }\\nORDER BY ?resource\\n\"},\"target\":{\"type\":\"uri\",\"value\":\"http://demos.swe.htwk-leipzig.de:40111/question/stored-question__text_0d5a7c26-ec6a-4702-8d7f-999849938d0e\"},\"createdBy\":{\"type\":\"uri\",\"value\":\"urn:qanary:QB-SimpleRealNameOfSuperHero\"},\"createdAt\":{\"datatype\":\"http://www.w3.org/2001/XMLSchema#dateTime\",\"type\":\"typed-literal\",\"value\":\"2023-08-09T06:39:33.888Z\"}}]}";

    public String getJsonForExplanationObjects() {
        return jsonExplanationObjects;
    }

    public String getJsonForResultObjects() {
        return jsonResultObjects;
    }
}
