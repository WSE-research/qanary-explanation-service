package com.wse.qanaryexplanationservice.pojos;

import jnr.ffi.annotations.In;
import org.checkerframework.checker.units.qual.A;

import java.util.ArrayList;

public class InputQueryExample {

    public InputQueryExample(String explanations, String query) {
        this.explanations = explanations;
        this.query = query;
    }

    public static ArrayList<InputQueryExample> queryExamplesList() {
        ArrayList<InputQueryExample> list = new ArrayList<>();
        list.add(new InputQueryExample(
                "he query fetches the body from the graph which is from type AnnotationOfTextRepresentation. The body represents the URI of the question's textual representation.",
                "PREFIX  qa:   <http://www.wdaqua.eu/qa#>\n" +
                        "PREFIX  oa:   <http://www.w3.org/ns/openannotation/core/>\n" +
                        "\n" +
                        "SELECT  ?uri\n" +
                        "FROM <urn:graph:52f4edf5-db4d-4425-8f45-fbb3f2cc1428>\n" +
                        "WHERE\n" +
                        "  { ?q  a             qa:Question .\n" +
                        "    ?a  a             qa:AnnotationOfTextRepresentation ;\n" +
                        "        oa:hasTarget  ?q ;\n" +
                        "        oa:hasBody    ?uri\n" +
                        "  }"
                )
        );
        list.add(new InputQueryExample(
                "All Annotations of the type AnnotationOfSpotInstance has been requested, which inherits the date of its creation, the URI for the origin question, and the start as well as the end position of entity which is represented by the annotation.",
                "PREFIX  qa:   <http://www.wdaqua.eu/qa#>\n" +
                        "PREFIX  oa:   <http://www.w3.org/ns/openannotation/core/>\n" +
                        "PREFIX  rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                        "\n" +
                        "SELECT  *\n" +
                        "FROM <urn:graph:23477bf2-99a5-423d-9598-0bbbf95db959>\n" +
                        "WHERE\n" +
                        "  { ?annotationId\n" +
                        "              rdf:type        qa:AnnotationOfSpotInstance ;\n" +
                        "              oa:hasTarget    _:b0 .\n" +
                        "    _:b0      rdf:type        oa:SpecificResource ;\n" +
                        "              oa:hasSource    ?hasSource ;\n" +
                        "              oa:hasSelector  _:b1 .\n" +
                        "    _:b1      rdf:type        oa:TextPositionSelector ;\n" +
                        "              oa:start        ?start ;\n" +
                        "              oa:end          ?end .\n" +
                        "    ?annotationId\n" +
                        "              oa:annotatedAt  ?annotatedAt ;\n" +
                        "              oa:annotatedBy  ?annotatedBy\n" +
                        "  }"
        ));

        list.add(new InputQueryExample(
            "All Annotations of the type AnnotationOfQuestionLanguage has been requested, which inherits the date of its creation, the URI for the origin question as target and the information of the origin question's language as body.",
                "PREFIX  qa:   <http://www.wdaqua.eu/qa#>\n" +
                        "PREFIX  xsd:  <http://www.w3.org/2001/XMLSchema#>\n" +
                        "PREFIX  oa:   <http://www.w3.org/ns/openannotation/core/>\n" +
                        "\n" +
                        "SELECT  *\n" +
                        "FROM <urn:graph:1954c109-8876-4c2c-9877-931aa0c2a684>\n" +
                        "WHERE\n" +
                        "  { ?annotationId\n" +
                        "              a               qa:AnnotationOfQuestionLanguage ;\n" +
                        "              oa:hasTarget    ?hasTarget ;\n" +
                        "              oa:hasBody      ?hasBody ;\n" +
                        "              oa:annotatedBy  ?annotatedBy ;\n" +
                        "              oa:annotatedAt  ?annotatedAt\n" +
                        "  }"
        ));

        list.add(new InputQueryExample(
"All Annotations of the type AnnotationOfInstance has been requested, which inherits the date of its creation, the URI for the origin question, a knowledge-graph resource as well as the correlating start and end position for the found entity. If provided, a score is fetched too.",
                "PREFIX  qa:   <http://www.wdaqua.eu/qa#>\n" +
                        "PREFIX  oa:   <http://www.w3.org/ns/openannotation/core/>\n" +
                        "PREFIX  rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                        "\n" +
                        "SELECT  *\n" +
                        "FROM <urn:graph:f9c3fa28-af0d-4a7a-a257-5b423d0a2651>\n" +
                        "WHERE\n" +
                        "  { ?annotationId\n" +
                        "              rdf:type        qa:AnnotationOfInstance ;\n" +
                        "              oa:hasTarget    _:b0 .\n" +
                        "    _:b0      rdf:type        oa:SpecificResource ;\n" +
                        "              oa:hasSource    ?hasSource ;\n" +
                        "              oa:hasSelector  _:b1 .\n" +
                        "    _:b1      rdf:type        oa:TextPositionSelector ;\n" +
                        "              oa:start        ?start ;\n" +
                        "              oa:end          ?end .\n" +
                        "    ?annotationId\n" +
                        "              oa:hasBody      ?hasBody ;\n" +
                        "              oa:annotatedBy  ?annotatedBy ;\n" +
                        "              oa:annotatedAt  ?annotatedAt\n" +
                        "    OPTIONAL\n" +
                        "      { ?annotationId\n" +
                        "                  qa:score  ?score\n" +
                        "      }\n" +
                        "  }"
        ));

        list.add(new InputQueryExample(
"All Annotations of the type AnnotationOfRelation have been requested which inherits the meta data such as the component which added the annotation as well as the date when it was added. The annotation refers to the source question and has a dbpedia relation as body. If provided, the result also returns the start and end of the text position where the relation was found. Maybe it has a score.",
                "PREFIX  qa:   <http://www.wdaqua.eu/qa#>\n" +
                        "PREFIX  oa:   <http://www.w3.org/ns/openannotation/core/>\n" +
                        "\n" +
                        "SELECT  *\n" +
                        "FROM <urn:graph:8dd4244a-a741-4793-b505-4565e0eaf930>\n" +
                        "WHERE\n" +
                        "  { ?annotationId\n" +
                        "              a             qa:AnnotationOfRelation ;\n" +
                        "              oa:hasTarget  _:b0 .\n" +
                        "    _:b0      a             oa:SpecificResource ;\n" +
                        "              oa:hasSource  ?hasSource\n" +
                        "    OPTIONAL\n" +
                        "      { ?annotationId\n" +
                        "                  oa:hasTarget    _:b1 .\n" +
                        "        _:b1      oa:hasSelector  _:b2 .\n" +
                        "        _:b2      a               oa:TextPositionSelector ;\n" +
                        "                  oa:start        ?start ;\n" +
                        "                  oa:end          ?end\n" +
                        "      }\n" +
                        "    OPTIONAL\n" +
                        "      { ?annotationId\n" +
                        "                  qa:score  ?score\n" +
                        "      }\n" +
                        "    ?annotationId\n" +
                        "              oa:hasBody      ?hasBody ;\n" +
                        "              oa:annotatedBy  ?annotatedBy ;\n" +
                        "              oa:annotatedAt  ?annotatedAt\n" +
                        "  }"
        ));

        list.add(new InputQueryExample(
"All Annotations of the type AnnotationOfClass have been requested, what inherits the meta data such as the component which added the annotation as well as the time stamp of that. The annotations also contain the source question and resource referencing the found class type.",
                "PREFIX  qa:   <http://www.wdaqua.eu/qa#>\n" +
                        "PREFIX  xsd:  <http://www.w3.org/2001/XMLSchema#>\n" +
                        "PREFIX  oa:   <http://www.w3.org/ns/openannotation/core/>\n" +
                        "\n" +
                        "SELECT  *\n" +
                        "FROM <urn:graph:1cae89ab-136e-459c-b791-2bf76cb0fc07>\n" +
                        "WHERE\n" +
                        "  { ?annotationId\n" +
                        "              a               qa:AnnotationOfClass ;\n" +
                        "              oa:hasTarget    _:b0 .\n" +
                        "    _:b0      a               oa:SpecificResource ;\n" +
                        "              oa:hasSource    ?hasSource .\n" +
                        "    ?annotationId\n" +
                        "              oa:hasBody      ?hasBody ;\n" +
                        "              oa:annotatedBy  ?annotatedBy ;\n" +
                        "              oa:annotatedAt  ?annotatedAt\n" +
                        "  }"
        ));

        return list;
    }

    private String explanations;
    private String query;

    public String getQuery() {
        return query;
    }

    public String getExplanations() {
        return explanations;
    }

    public void setExplanations(String explanations) {
        this.explanations = explanations;
    }

    public void setQuery(String query) {
        this.query = query;
    }
}
