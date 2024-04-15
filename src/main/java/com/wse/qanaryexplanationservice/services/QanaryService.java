package com.wse.qanaryexplanationservice.services;

import com.wse.qanaryexplanationservice.pojos.AutomatedTests.QanaryObjects.QanaryRequestObject;
import com.wse.qanaryexplanationservice.repositories.QanaryRepository;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QanaryService {

    public static JSONObject executePipeline(String question, List<String> components) {
        QanaryRequestObject qanaryRequestObject = new QanaryRequestObject(question, null, null, components);

        return new JSONObject(QanaryRepository.executeQanaryPipeline(qanaryRequestObject));
    }

}
