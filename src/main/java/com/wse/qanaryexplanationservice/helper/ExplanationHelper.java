package com.wse.qanaryexplanationservice.helper;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.RDFNode;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ExplanationHelper {

    public static Map<String, String> convertQuerySolutionToMap(QuerySolution querySolution) {
        QuerySolutionMap querySolutionMap = new QuerySolutionMap();
        querySolutionMap.addAll(querySolution);
        Map<String, RDFNode> querySolutionMapAsMap = querySolutionMap.asMap();
        return convertRdfNodeToStringValue(querySolutionMapAsMap);
    }

    /**
     * Converts RDFNodes to Strings without the XML datatype declaration and leaves resources as they are.
     *
     * @param map Key = variable from sparql-query, Value = its corresponding RDFNode
     * @return Map with value::String instead of value::RDFNode
     */
    public static Map<String, String> convertRdfNodeToStringValue(Map<String, RDFNode> map) {
        return map.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> {
                    if (entry.getValue().isResource())
                        return entry.getValue().toString();
                    else
                        return entry.getValue().asNode().getLiteralValue().toString();
                }
        ));
    }

    /**
     * Reads a file and parses the content to a string
     *
     * @param path Given path
     * @return String with the file's content
     */
    public static String getStringFromFile(String path) throws IOException {
        ClassPathResource cpr = new ClassPathResource(path);
        try {
            byte[] bdata = FileCopyUtils.copyToByteArray(cpr.getInputStream());
            return new String(bdata, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IOException();
        }
    }

}
