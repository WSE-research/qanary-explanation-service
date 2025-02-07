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

    /**
     * Converts one QuerySolution to a Map with variable-value mappings, where the RDFNode values are converted to Strings
     * @param qs Single QuerySolution
     * @return Variable (String)-Value (String) Mapping of one QuerySolution
     */
    public static Map<String, String> convertQuerySolutionToMap(QuerySolution qs) {
        Map<String, RDFNode> rdfNodeMap = convertQuerySolutionToMapWithRdfNodes(qs);
        return rdfNodeMap.entrySet().stream().collect(Collectors.toMap(
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
     * Converts one QuerySolution to a Map with variable-value mappings with RDFNode(s) as values
     * @param qs Single QuerySolution
     * @return Variable (String)-Value (RDFNode) Mapping of one QuerySolution
     */
    public static Map<String, RDFNode> convertQuerySolutionToMapWithRdfNodes(QuerySolution qs) {
        QuerySolutionMap querySolutionMap = new QuerySolutionMap();
        querySolutionMap.addAll(qs);
        return querySolutionMap.asMap();
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
