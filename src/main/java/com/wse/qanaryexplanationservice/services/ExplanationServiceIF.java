package com.wse.qanaryexplanationservice.services;

import java.io.IOException;

public interface ExplanationServiceIF<T> {

    T[] explainComponent(String graphID) throws IOException;


}
