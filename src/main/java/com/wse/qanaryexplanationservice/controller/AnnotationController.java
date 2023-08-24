package com.wse.qanaryexplanationservice.controller;


import com.wse.qanaryexplanationservice.pojos.ComponentPojo;
import com.wse.qanaryexplanationservice.pojos.ExplanationObject;
import com.wse.qanaryexplanationservice.services.AnnotationsService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class AnnotationController {

    @Autowired
    private AnnotationsService annotationsService;

    /**
     * @param graphURI graphURI to work with
     * @param graphID  graphId to work with
     * @return the list of results (ResultObjects)
     * @return the list of results (ExplanationObjects)
     */
    @CrossOrigin
    @GetMapping("/getannotations")
    @Operation(
            summary = "Endpoint to request every made annotation within a QA-process",
            description = "This endpoint returns a list of annotations made by the QA-process of the "
                    + "provided graphURI. Requires graphURI."
    )
    public ResponseEntity<ExplanationObject[]> getAnnotations(@RequestParam String graphURI) throws IOException {
        ExplanationObject[] explanationObjects = annotationsService.getAnnotations(graphURI);
        if (explanationObjects != null)
            return new ResponseEntity<>(explanationObjects, HttpStatus.OK);
        else
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
    }

    @CrossOrigin
    @GetMapping("/components")
    @Operation(
            summary = "Endpoint to receive any involved component in a QA-process",
            description = "To use that endpoint you have to provide a graphURI from a QA-process "
                    + "and it'll return a distinct list of involved components"
    )
    public ResponseEntity<ComponentPojo[]> getComponents(@RequestParam String graphURI) throws IOException {
        ComponentPojo[] result = annotationsService.getUsedComponents(graphURI);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }


}
