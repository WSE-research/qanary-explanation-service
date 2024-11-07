package com.wse.qanaryexplanationservice.helper.pojos;

import com.wse.qanaryexplanationservice.services.TemplateExplanationsService;
import org.apache.jena.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Contains relevant data for the creation of (template-based) explanations
 */
public class ExplanationMetaData {

    private QanaryComponent qanaryComponent;
    private URI graph;
    private String template;
    private boolean doGenerative;
    private final Logger logger = LoggerFactory.getLogger(ExplanationMetaData.class);
    private static String DEFAULT_METHOD_TEMPLATE_PATH = "/explanations/methods/en"; // TODO: Set lang?
    private String requestQuery;

    public ExplanationMetaData(String qanaryComponent, String graph, String template, boolean doGenerative, String query) throws URISyntaxException {
        this.qanaryComponent = new QanaryComponent(qanaryComponent);
        this.graph = new URI(graph);
        this.doGenerative = doGenerative;
        this.template = checkTemplateValidity(template);
    }

    // TODO: Relevant to implement? How to do it, if we want to use it for different templates?
    public String checkTemplateValidity(String template) {
        if(template == null) {
            logger.warn("Using default template as no template was passed.");
            return TemplateExplanationsService.getStringFromFile(DEFAULT_METHOD_TEMPLATE_PATH);
        } else {
            // TODO: Validity check, see method comment
            return template;
        }
    }

    public boolean isDoGenerative() {
        return doGenerative;
    }

    public void setDoGenerative(boolean doGenerative) {
        this.doGenerative = doGenerative;
    }

    public QanaryComponent getQanaryComponent() {
        return qanaryComponent;
    }

    public String getTemplate() {
        return template;
    }

    public URI getGraph() {
        return graph;
    }

    public void setGraph(URI graph) {
        this.graph = graph;
    }

    public void setQanaryComponent(QanaryComponent qanaryComponent) {
        this.qanaryComponent = qanaryComponent;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public String getRequestQuery() {
        return requestQuery;
    }

    public void setRequestQuery(String requestQuery) {
        this.requestQuery = requestQuery;
    }
}
