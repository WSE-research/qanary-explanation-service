package com.wse.qanaryexplanationservice.pojos.AutomatedTests.QanaryObjects;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;
import java.util.List;

public class QanaryRequestObject {

    private String question;
    private String additionaltriples;
    private String componentfilterinput;
    private List<String> componentlist;
    private MultiValueMap<String,String> componentListAsMap;

    public QanaryRequestObject(
            String question,
            String additionaltriples,
            String componentfilterinput,
            List<String> componentList
    ) {
        this.componentListAsMap = new LinkedMultiValueMap<>();
        this.question = question;
        this.additionaltriples = additionaltriples;
        this.componentfilterinput = componentfilterinput;
        this.componentlist = componentList;
        transformComponentsToUrl();
    }

    public void transformComponentsToUrl() {
        for (String component : this.componentlist
        ) {
            componentListAsMap.add("componentlist[]", component);
        }
    }

    public MultiValueMap<String,String> getComponentListAsMap() {
        return componentListAsMap;
    }

    public String getQuestion() {
        return question;
    }

    public String getAdditionaltriples() {
        return additionaltriples;
    }

    public String getComponentfilterinput() {
        return componentfilterinput;
    }

    public List<String> getComponentlist() {
        return componentlist;
    }
}
