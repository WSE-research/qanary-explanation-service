package com.wse.webservice_for_componentExplanation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class StringToJsonNode {

    private ObjectMapper objectMapper;

    public StringToJsonNode() {
        objectMapper = new ObjectMapper();
    }

    public JsonNode convertStingToJsonNode(String text) throws JsonProcessingException {
        return objectMapper.readValue(text, JsonNode.class);
    }

}
