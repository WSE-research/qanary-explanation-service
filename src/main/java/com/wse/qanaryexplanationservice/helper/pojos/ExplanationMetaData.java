package com.wse.qanaryexplanationservice.helper.pojos;

import com.wse.qanaryexplanationservice.helper.GptModel;
import com.wse.qanaryexplanationservice.services.TemplateExplanationsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Contains relevant data for the creation of (template-based) explanations for both methods and components
 */
public class ExplanationMetaData {

    private QanaryComponent qanaryComponent;
    private String method;
    private URI graph;
    private String prefixTemplate;
    private String itemTemplate;
    private String lang;
    private AggregationSettings aggregationSettings;
    private GPTRequest gptRequest;

    public ExplanationMetaData(String qanaryComponent, String method, String graph, String itemTemplate, String prefixTemplate, String lang, AggregationSettings aggregationSettings, GPTRequest gptRequest) throws URISyntaxException {
        this.method = method;
        this.qanaryComponent = new QanaryComponent(qanaryComponent);
        this.graph = new URI(graph);
        this.itemTemplate = itemTemplate;
        this.prefixTemplate = prefixTemplate;
        this.lang = lang == null ? "en" : lang;
        this.aggregationSettings = aggregationSettings;
        this.gptRequest = gptRequest;
    }

    public GPTRequest getGptRequest() {
        return gptRequest;
    }

    public void setGptRequest(GPTRequest gptRequest) {
        this.gptRequest = gptRequest;
    }

    public AggregationSettings getAggregationSettings() {
        return aggregationSettings;
    }

    public void setAggregationSettings(AggregationSettings aggregationSettings) {
        this.aggregationSettings = aggregationSettings;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
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

}
