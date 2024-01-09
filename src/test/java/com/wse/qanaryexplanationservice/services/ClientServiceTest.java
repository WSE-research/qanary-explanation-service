package com.wse.qanaryexplanationservice.services;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
public class ClientServiceTest {

    @Autowired
    private ClientService clientService;

    @Test
    public void insertJsonTestWithCorrectJSONObject() {
        JSONObject correctTestObject = new JSONObject();
        JSONArray explanations = new JSONArray();

    }
    @Test
    public void insertJsonTestWithWrongJSONObject() {
        JSONObject wrongTestObject = new JSONObject(); // empty JSONObject
        assertThrows(RuntimeException.class, () -> clientService.insertJson(wrongTestObject));
    }

}
