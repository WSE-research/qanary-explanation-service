package com.wse.qanaryexplanationservice.helper;

import com.wse.qanaryexplanationservice.helper.dtos.ExplanationMetaData;
import com.wse.qanaryexplanationservice.helper.pojos.Method;
import com.wse.qanaryexplanationservice.helper.pojos.MethodItem;
import com.wse.qanaryexplanationservice.helper.pojos.Variable;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.RDFNode;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ExplanationHelper {

    public static final String VARIABLE_SEPARATOR = "////";
    public static final String TEMPLATE_PLACEHOLDER_PREFIX = "${";
    public static final String TEMPLATE_PLACEHOLDER_SUFFIX = "}";

    /**
     * Converts one QuerySolution to a Map with variable-value mappings, where the RDFNode values are converted to Strings
     *
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

    public static String convertVariablesToStringRepresentation(List<Variable> variables) {
        StringBuilder builder = new StringBuilder();
        if (!variables.isEmpty()) {
            for (Variable variable : variables) {
                builder.append("* ").append(variable.getType()).append(": ").append(variable.getValue()).append("\n");
            }
            return builder.toString();
        } else
            return "Void";
    }

    /*
    Generates processingInformation depending on the user's preference as well as the existing information
     */
    public static String generateProcessingInformation(MethodItem method, List<Method> childMethodList, ExplanationMetaData data) {
        return (data.getProcessingInformation().isDocstring() ? method.getDocstring() : "") +
                (data.getProcessingInformation().isSourcecode() ? method.getSourceCode() : "") +
                (childMethodList != null ? "Sub-method explanations:\n" + String.join("\n", childMethodList.stream().map(Method::getExplanation).toList()) : "");
    }

    /**
     * Converts one QuerySolution to a Map with variable-value mappings with RDFNode(s) as values
     *
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

    public static String replaceMethodExplanationPlaceholder(String template, MethodItem method, List<Method> childMethods, ExplanationMetaData data) {
        return template
                .replace(TEMPLATE_PLACEHOLDER_PREFIX + "method" + TEMPLATE_PLACEHOLDER_SUFFIX, method.getMethodName())
                .replace(TEMPLATE_PLACEHOLDER_PREFIX + "input" + TEMPLATE_PLACEHOLDER_SUFFIX, convertVariablesToStringRepresentation(method.getInputVariables()))
                .replace(TEMPLATE_PLACEHOLDER_PREFIX + "processedInformation" + TEMPLATE_PLACEHOLDER_SUFFIX, generateProcessingInformation(method, childMethods, data))
                .replace(TEMPLATE_PLACEHOLDER_PREFIX + "output" + TEMPLATE_PLACEHOLDER_SUFFIX, convertVariablesToStringRepresentation(method.getOutputVariables()))
                .replace(TEMPLATE_PLACEHOLDER_PREFIX + "caller" + TEMPLATE_PLACEHOLDER_SUFFIX, method.getCallerName())
                .replace(TEMPLATE_PLACEHOLDER_PREFIX + "annotatedAt" + TEMPLATE_PLACEHOLDER_SUFFIX, method.getAnnotatedAt());
    }

}
