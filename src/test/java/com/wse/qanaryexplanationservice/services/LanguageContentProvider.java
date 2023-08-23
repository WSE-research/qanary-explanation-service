package com.wse.qanaryexplanationservice.services;

public class LanguageContentProvider {

    private String contentDe;

    private String contentEn;

    public LanguageContentProvider(String contentDe, String contentEn) {
        this.contentDe = contentDe;
        this.contentEn = contentEn;
    }

    public String getContentDe() {
        return contentDe;
    }

    public String getContentEn() {
        return contentEn;
    }
}
