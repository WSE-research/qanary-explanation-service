package com.wse.qanaryexplanationservice.controller;

import com.wse.qanaryexplanationservice.helper.pojos.AutomatedTests.automatedTestingObject.AutomatedTestRequestBody;
import com.wse.qanaryexplanationservice.repositories.AutomatedTestingRepository;
import com.wse.qanaryexplanationservice.services.AutomatedTestingService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class AutomatedTestController {

    private final String authToken = ""; // Provide via GitHub actions
    @Autowired
    private AutomatedTestingService automatedTestingService;
    @Autowired
    private AutomatedTestingRepository automatedTestingRepository;

    // Note: Optional to the Path Variable "authToken", other approaches are possible too (CrossOrigin, Auth, ...)
    @PostMapping(value = "/automatedtesting/{doGptRequest}", consumes = {
            "application/json"
    })
    @Operation(
            summary = "Endpoint to execute automated Tests",
            description = "The passed Body should be a JSON containing the following information:" +
                    "{" +
                    "'testingType':'AnnotationOfInstance'," +
                    "'examples': [" +
                    "{" +
                    "'type:'AnnotationOfSpotInstance'" +
                    "'uniqueComponent': true" +
                    "]," +
                    "'runs': '2'" +
                    "}" +
                    "The array of examples can be extended up to three examples. The property uniqueComponent defined if a component should only be used" +
                    "once in a experiment. Further, the property runs defines the number of experiments you want to execute."
    )
    public ResponseEntity<String> automatedExperiments(
            @RequestBody AutomatedTestRequestBody requestBody,
            @PathVariable boolean doGptRequest
    ) {
        try {
            return new ResponseEntity<>(automatedTestingService.createTestWorkflow(requestBody, doGptRequest), HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>("Invalid Annotation Type!", HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>(e.toString(), HttpStatus.NOT_ACCEPTABLE); // TODO: Later, implement way to provide chatgpt key as param as well
        }
    }

}
