package com.wse.qanaryexplanationservice.controller;

import com.wse.qanaryexplanationservice.pojos.ResultObject;
import com.wse.qanaryexplanationservice.services.AnnotationsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class AnnotationController {

    @Autowired
    private AnnotationsService annotationsService;

    /**
     * @param graphURI graphURI to work with
     * @return the list of results (ResultObjects)
     */
    @CrossOrigin
    @GetMapping("/annotations/{graphURI}")
    public ResponseEntity<ResultObject[]> getAnnotations(
            @PathVariable(required = true) String graphURI
    ) throws IOException {
        ResultObject[] resultObjects = annotationsService.getAnnotations(graphURI);
        if (resultObjects != null)
            return new ResponseEntity<>(resultObjects, HttpStatus.OK);
        else
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
    }


}
