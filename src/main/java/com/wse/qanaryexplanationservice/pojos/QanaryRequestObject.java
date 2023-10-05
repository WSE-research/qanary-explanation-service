package com.wse.qanaryexplanationservice.pojos;

public class QanaryRequestObject {

    private String question;
    private String additionaltriples;
    private String componentfilterinput;
    private String[] componentlist;

    public QanaryRequestObject(
        String question,
        String additionaltriples,
        String componentfilterinput,
        String component
    ) {
        this.question = question;
        this.additionaltriples = additionaltriples;
        this.componentfilterinput = componentfilterinput;
        this.componentlist = new String[] {component};
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

    public String[] getComponentlist() {
        return componentlist;
    }
}
