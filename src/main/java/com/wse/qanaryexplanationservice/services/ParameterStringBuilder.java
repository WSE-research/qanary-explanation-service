package com.wse.qanaryexplanationservice.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Helper class to convert the parameter-map to a string, so it can be passed to an HTTP-Request
 */
public class ParameterStringBuilder {

    private static final Logger logger = LoggerFactory.getLogger(ParameterStringBuilder.class);

    public static String getParamsString(Map<String, String> params) {
        StringBuilder result = new StringBuilder();

        for (Map.Entry<String, String> entry : params.entrySet()) {
            logger.info("Entry: {}", entry);
            result.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            result.append("=");
            result.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            result.append("&");
        }

        String resultString = result.toString();
        logger.info("REsult: {}", resultString.substring(0, resultString.length() - 1).replace("+", "%20"));
        return !resultString.isEmpty()
                ? resultString.substring(0, resultString.length() - 1).replace("+", "%20")
                : resultString;
    }
}
