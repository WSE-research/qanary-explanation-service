package com.wse.qanaryexplanationservice.helper.pojos;

import com.wse.qanaryexplanationservice.services.TemplateExplanationsService;
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
    private String prefixTemplate;
    private String itemTemplate;
    private boolean doGenerative;
    private final Logger logger = LoggerFactory.getLogger(ExplanationMetaData.class);
    private String requestQuery;
    private final String DEFAULT_METHOD_TEMPLATE_ROOT_PATH = "/explanations/methods/";

    public ExplanationMetaData(String qanaryComponent, String graph, String itemTemplate, String prefixTemplate, boolean doGenerative, String query) throws URISyntaxException {
        this.qanaryComponent = new QanaryComponent(qanaryComponent);
        this.graph = new URI(graph);
        this.doGenerative = doGenerative;
        this.itemTemplate = itemTemplate != null ? itemTemplate : TemplateExplanationsService.getStringFromFile(DEFAULT_METHOD_TEMPLATE_ROOT_PATH + "item/en");
        this.prefixTemplate = prefixTemplate != null ? prefixTemplate : TemplateExplanationsService.getStringFromFile(DEFAULT_METHOD_TEMPLATE_ROOT_PATH + "prefix/en");
        this.requestQuery = query;
    }

    public String getPrefixTemplate() {
        return prefixTemplate;
    }

    public String getItemTemplate() {
        return itemTemplate;
    }

    public void setItemTemplate(String itemTemplate) {
        this.itemTemplate = itemTemplate;
    }

    public void setPrefixTemplate(String prefixTemplate) {
        this.prefixTemplate = prefixTemplate;
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

    public URI getGraph() {
        return graph;
    }

    public void setGraph(URI graph) {
        this.graph = graph;
    }

    public void setQanaryComponent(QanaryComponent qanaryComponent) {
        this.qanaryComponent = qanaryComponent;
    }

    public String getRequestQuery() {
        return requestQuery;
    }

    public void setRequestQuery(String requestQuery) {
        this.requestQuery = requestQuery;
    }
}
