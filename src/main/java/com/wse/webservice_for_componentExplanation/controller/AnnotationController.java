package com.wse.webservice_for_componentExplanation.controller;

import com.wse.webservice_for_componentExplanation.pojos.ComponentPojo;
import com.wse.webservice_for_componentExplanation.pojos.ExplanationObject;
import com.wse.webservice_for_componentExplanation.pojos.ExplanationObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

import com.wse.webservice_for_componentExplanation.services.GetAnnotationsService;

@RestController
public class AnnotationController {

    @Autowired
    private GetAnnotationsService getAnnotationsService;

    /**
     * @param graphID graphId to work with
     * @return the list of results (ExplanationObjects)
     */
    @CrossOrigin
    @GetMapping("/getannotations")
    public ResponseEntity<ExplanationObject[]> getAnnotations(@RequestParam String graphID) throws IOException {
        ExplanationObject[] explanationObjects = getAnnotationsService.getAnnotations(graphID);
        if (explanationObjects != null)
            return new ResponseEntity<>(explanationObjects, HttpStatus.OK);
        else
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
    }

    @CrossOrigin
    @GetMapping("/components")
    public ResponseEntity<ComponentPojo[]> getComponents(@RequestParam String graphID) throws IOException {
        ComponentPojo[] result = getAnnotationsService.getUsedComponents(graphID);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }


}
