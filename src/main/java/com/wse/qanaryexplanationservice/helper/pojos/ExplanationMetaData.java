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

    private final Logger logger = LoggerFactory.getLogger(ExplanationMetaData.class);
    private QanaryComponent qanaryComponent;
    private String methodName;
    private URI graph;
    private String prefixTemplate;
    private String itemTemplate;
    private String requestQuery;
    private String lang;
    private GPTRequest gptRequest;

    public ExplanationMetaData(String qanaryComponent, String methodName, String graph, String itemTemplate, String prefixTemplate, String query, String lang, GPTRequest gptRequest) throws URISyntaxException {
        this.methodName = methodName != null ? methodName : null;
        this.qanaryComponent = new QanaryComponent(qanaryComponent);
        this.graph = new URI(graph);
        this.itemTemplate = itemTemplate;
        this.prefixTemplate = prefixTemplate;
        this.requestQuery = query;
        this.lang = lang == null ? "en" : lang;
        this.gptRequest = gptRequest;
    }

    public GPTRequest getGptRequest() {
        return gptRequest;
    }

    public void setGptRequest(GPTRequest gptRequest) {
        this.gptRequest = gptRequest;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
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

    public String getRequestQuery() {
        return requestQuery;
    }

    public void setRequestQuery(String requestQuery) {
        this.requestQuery = requestQuery;
    }
}
