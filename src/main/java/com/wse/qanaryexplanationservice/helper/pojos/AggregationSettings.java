package com.wse.qanaryexplanationservice.helper.pojos;

public class AggregationSettings {

    String leafs;
    String type;
    String approach;

    public AggregationSettings(String leafs, String type, String approach) {
        this.leafs = leafs;
        this.type = type;
        this.approach = approach;
    }

    public String getApproach() {
        return approach;
    }

    public String getLeafs() {
        return leafs;
    }

    public String getType() {
        return type;
    }

    public void setApproach(String approach) {
        this.approach = approach;
    }

    public void setLeafs(String leafs) {
        this.leafs = leafs;
    }

    public void setType(String type) {
        this.type = type;
    }
}
