package com.wse.qanaryexplanationservice.controller;


import com.wse.qanaryexplanationservice.pojos.ResultObject;
import com.wse.qanaryexplanationservice.services.AnnotationsService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
public class AnnotationController {

    @Autowired
    private AnnotationsService annotationsService;

    /**
     * @param graphURI graphURI to work with
     * @return the list of results (ExplanationObjects)
     */
    @CrossOrigin
    @GetMapping("/annotations/{graphURI}")
    @Operation(
            summary = "Endpoint to request every made annotation within a QA-process",
            description = "This endpoint returns a list of annotations made by the QA-process of the "
                    + "provided graphURI. Requires graphURI."
    )
    public ResponseEntity<ResultObject[]> getAnnotations(@PathVariable String graphURI) throws IOException {
        ResultObject[] explanationObjects = annotationsService.getAnnotations(graphURI);
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
    public ResponseEntity<List<String>> getComponents(@RequestParam String graphURI) {
        List<String> result = annotationsService.getUsedComponents(graphURI);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }


}
