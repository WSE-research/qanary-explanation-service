package com.wse.qanaryexplanationservice.pojos;

import java.util.List;

public class QanaryRequestObject {

    private String question;
    private String additionaltriples;
    private String componentfilterinput;
    private List<String> componentlist;

    public QanaryRequestObject(
            String question,
            String additionaltriples,
            String componentfilterinput,
            List<String> componentList
    ) {
        this.question = question;
        this.additionaltriples = additionaltriples;
        this.componentfilterinput = componentfilterinput;
        this.componentlist = componentList;
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
