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

    public void setApproach(String approach) {
        this.approach = approach;
    }

    public String getLeafs() {
        return leafs;
    }

    public void setLeafs(String leafs) {
        this.leafs = leafs;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "Leafs: " + this.leafs + "\n" +
                "Type: " + this.type + "\n" +
                "Approach: " + this.approach;
    }
}
