package com.wse.qanaryexplanationservice.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AutomatedTestingService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // stores the correct template for different x-shot approaches
    private Map<Integer, String> exampleCountAndTemplate = new HashMap<>() {{
        put(1, "/testtemplates/oneshot");
        put(2, "/testtemplates/twoshot");
        put(3, "/testtemplates/threeshot");
    }};

    public void automatedTest(String requestBody) throws JsonProcessingException {
        // Create Object as output-template where properties are within this process // TODO:
        JsonNode bodyAsJsonNode = objectMapper.readTree(requestBody);

        // select correct template
        String gptTemplate = exampleCountAndTemplate.get(bodyAsJsonNode.get("exampleAmount"));

        
    }
}
