package com.wse.qanaryexplanationservice.repositories;

import com.wse.qanaryexplanationservice.pojos.AutomatedTests.QanaryObjects.QanaryRequestObject;
import com.wse.qanaryexplanationservice.pojos.AutomatedTests.QanaryObjects.QanaryResponseObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

@Repository
public class QanaryRepository {

    private static WebClient webClient;
    private static Logger logger = LoggerFactory.getLogger(QanaryRequestObject.class);

    @Value("${qanary.pipeline.host}")
    private static String qanaryPipelineHost;
    @Value("${qanary.pipeline.port}")
    private static int qanaryPipelinePort;

    public static QanaryResponseObject executeQanaryPipeline(QanaryRequestObject qanaryRequestObject) {

        MultiValueMap<String, String> multiValueMap = new LinkedMultiValueMap();
        multiValueMap.add("question", qanaryRequestObject.getQuestion());
        multiValueMap.addAll(qanaryRequestObject.getComponentListAsMap());

        QanaryResponseObject responseObject = webClient.post().uri(uriBuilder -> uriBuilder // TODO: use new endpoint for question answering
                        .scheme("http").host(qanaryPipelineHost).port(qanaryPipelinePort).path("/startquestionansweringwithtextquestion")
                        .queryParams(multiValueMap)
                        .build())
                .retrieve()
                .bodyToMono(QanaryResponseObject.class)
                .block();

        logger.info("Response Object: {}", responseObject);

        return responseObject;
    }

}
