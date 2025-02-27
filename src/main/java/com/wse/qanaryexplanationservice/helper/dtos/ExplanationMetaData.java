package com.wse.qanaryexplanationservice.helper.dtos;

import com.wse.qanaryexplanationservice.helper.pojos.AggregationSettings;
import com.wse.qanaryexplanationservice.helper.pojos.GPTRequest;
import com.wse.qanaryexplanationservice.helper.pojos.ProcessingInformation;
import com.wse.qanaryexplanationservice.helper.pojos.QanaryComponent;

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
    private boolean tree;
    private ProcessingInformation processingInformation;

    public ExplanationMetaData(String qanaryComponent, String method, String graph, boolean tree, String itemTemplate, String prefixTemplate, String lang, AggregationSettings aggregationSettings, GPTRequest gptRequest, ProcessingInformation processingInformation) throws URISyntaxException {
        this.method = method;
        this.qanaryComponent = new QanaryComponent(qanaryComponent);
        this.graph = new URI(graph);
        this.tree = tree;
        this.itemTemplate = itemTemplate;
        this.prefixTemplate = prefixTemplate;
        this.lang = lang == null ? "en" : lang;
        this.aggregationSettings = aggregationSettings;
        this.gptRequest = gptRequest;
        this.processingInformation = processingInformation;
    }

    public ProcessingInformation getProcessingInformation() {
        return processingInformation;
    }

    public void setProcessingInformation(ProcessingInformation processingInformation) {
        this.processingInformation = processingInformation;
    }

    public boolean getTree() {
        return tree;
    }

    public void setTree(boolean tree) {
        this.tree = tree;
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

    public void setPrefixTemplate(String prefixTemplate) {
        this.prefixTemplate = prefixTemplate;
    }

    public String getItemTemplate() {
        return itemTemplate;
    }

    public void setItemTemplate(String itemTemplate) {
        this.itemTemplate = itemTemplate;
    }

    public QanaryComponent getQanaryComponent() {
        return qanaryComponent;
    }

    public void setQanaryComponent(QanaryComponent qanaryComponent) {
        this.qanaryComponent = qanaryComponent;
    }

    public URI getGraph() {
        return graph;
    }

    public void setGraph(URI graph) {
        this.graph = graph;
    }

    @Override
    public String toString() {
        return "Qanary component: " + this.qanaryComponent.getPrefixedComponentName() + "\n" +
                "Method: " + this.method + "\n" +
                "Graph: " + this.graph.toASCIIString() + "\n" +
                "Language: " + this.lang + "\n" +
                "Tree: " + this.tree + "\n" +
                "Aggregation Settings: " + this.aggregationSettings.toString() + "\n" +
                "Gpt Request: " + this.gptRequest.toString();
    }
}
