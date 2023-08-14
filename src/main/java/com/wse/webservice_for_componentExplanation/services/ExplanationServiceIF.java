package com.wse.webservice_for_componentExplanation.services;

import java.io.IOException;

public interface ExplanationServiceIF<T> {

    T[] explainComponent(String graphID) throws IOException;


}
