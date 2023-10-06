package com.wse.qanaryexplanationservice.repositories;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wse.qanaryexplanationservice.pojos.QanaryRequestObject;
import com.wse.qanaryexplanationservice.services.ParameterStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Repository;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@Repository
public class AutomatedTestingRepository extends AbstractRepository {

    private final URL QANARY_ENDPOINT;
    private Logger logger = LoggerFactory.getLogger(AutomatedTestingRepository.class);

    public AutomatedTestingRepository(Environment environment) throws MalformedURLException {
        super(environment);
        QANARY_ENDPOINT = new URL("http://195.90.200.248:8090/startquestionansweringwithtextquestion");
        this.objectMapper = new ObjectMapper();
    }

    public JsonNode executeQanaryPipeline(QanaryRequestObject qanaryRequestObject) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) QANARY_ENDPOINT.openConnection();

        connection.setRequestMethod("POST");

        Map<String, String> parameters = new HashMap<>();
        parameters.put("question", qanaryRequestObject.getQuestion());
        //  parameters.put("additionaltriples",qanaryRequestObject.getAdditionaltriples());
        //  parameters.put("componentfilterinput",qanaryRequestObject.getComponentfilterinput());
        parameters.put("componentlist[]", qanaryRequestObject.getComponentlist()[0]);

        connection.setDoOutput(true);

        DataOutputStream out = new DataOutputStream(connection.getOutputStream());
        out.writeBytes(ParameterStringBuilder.getParamsString(parameters));
        out.flush();
        logger.info(out.toString());
        out.close();


        InputStream responseStream = connection.getInputStream();

        return objectMapper.readValue(responseStream, JsonNode.class);

    }
}
