package com.wse.qanaryexplanationservice.controller;

import com.wse.qanaryexplanationservice.pojos.ResultObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

import com.wse.qanaryexplanationservice.services.GetAnnotationsService;

@RestController
public class AnnotationController {

    @Autowired
    private GetAnnotationsService getAnnotationsService;

    /**
     * @param graphID graphId to work with
     * @return the list of results (ResultObjects)
     */
    @CrossOrigin
    @GetMapping("/getannotations")
    public ResponseEntity<ResultObject[]> getAnnotations(@RequestParam String graphID) throws IOException {
        ResultObject[] resultObjects = getAnnotationsService.getAnnotations(graphID);
        if (resultObjects != null)
            return new ResponseEntity<>(resultObjects, HttpStatus.OK);
        else
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
    }


}
