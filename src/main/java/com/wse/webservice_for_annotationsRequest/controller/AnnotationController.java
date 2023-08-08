package com.wse.webservice_for_annotationsRequest.controller;

import com.wse.webservice_for_annotationsRequest.pojos.ResultObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import com.wse.webservice_for_annotationsRequest.services.getAnnotationsService;

@RestController
public class AnnotationController {

    @Autowired
    private getAnnotationsService getAnnotationsService;

    /**
     *
     * @param graphID graphId to work with
     * @return the list of results (ResultObjects)
     */
    @CrossOrigin
    @GetMapping("/getannotations")
    public ResponseEntity<ResultObject[]> getAnnotations(@RequestParam String graphID) throws IOException {

        return new ResponseEntity<>(getAnnotationsService.getAnnotations(graphID), HttpStatus.OK);
    }


}
