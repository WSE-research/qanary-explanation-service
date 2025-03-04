package com.wse.qanaryexplanationservice.services;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.ModelType;
import com.wse.qanaryexplanationservice.exceptions.ExplanationException;
import com.wse.qanaryexplanationservice.helper.ExplanationHelper;
import com.wse.qanaryexplanationservice.helper.dtos.ExplanationMetaData;
import com.wse.qanaryexplanationservice.helper.enums.AnnotationType;
import com.wse.qanaryexplanationservice.helper.enums.GptModel;
import com.wse.qanaryexplanationservice.helper.pojos.AutomatedTests.QanaryRequestPojos.QanaryResponseObject;
import com.wse.qanaryexplanationservice.helper.pojos.AutomatedTests.automatedTestingObject.TestDataObject;
import com.wse.qanaryexplanationservice.helper.pojos.*;
import com.wse.qanaryexplanationservice.repositories.GenerativeExplanationsRepository;
import com.wse.qanaryexplanationservice.repositories.QanaryRepository;
import eu.wdaqua.qanary.commons.triplestoreconnectors.QanaryTripleStoreConnector;
import org.apache.commons.lang.StringUtils;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

/**
 * This service is used to create [...]
 */
@Service
@Lazy
public class GenerativeExplanationsService {


    private final EncodingRegistry encodingRegistry = Encodings.newLazyEncodingRegistry();
    private final Logger logger = LoggerFactory.getLogger(GenerativeExplanationsService.class);

    private final Random random = new Random();
    private final TemplateExplanationsService tmplExpService;
    private final String PROMPT_TEMPLATE_PATH = "/prompt_templates/";
    private final String CHECK_EXISTENCE_OF_OTHER_METHODS_QUERY = "/queries/check_if_other_methods_exist.rq";
    private final String SELECT_ALL_METHODS_WITH_DATA_FROM_ROOT = "/queries/select_all_methods_with_data_from_root.rq";
    private final String PROMPT_AGGREGATED_DATA = "/prompt_templates/methods/aggregated/aggregated_data/";
    private final GenerativeExplanations generativeExplanations;
    private final GenerativeExplanationsRepository generativeExplanationsRepository;
    private final QanaryRepository qanaryRepository;
    @Value("${questionId.replacement}")
    private String questionIdReplacement;

    public GenerativeExplanationsService(TemplateExplanationsService tmplExpService, GenerativeExplanations generativeExplanations, GenerativeExplanationsRepository generativeExplanationsRepository, QanaryRepository qanaryRepository) {
        this.tmplExpService = tmplExpService;
        this.generativeExplanations = generativeExplanations;
        this.generativeExplanationsRepository = generativeExplanationsRepository;
        this.qanaryRepository = qanaryRepository;
    }

    /**
     * Method to produce a generative created explanation with x shots with the testing component @param component
     *
     * @param component Test component
     * @param shots     Number of shots used within the prompt
     * @param graphUri  GraphUri for the test component
     */
    public GenerativeExplanationObject createGenerativeExplanation(QanaryComponent component, int shots, String graphUri) throws Exception {
        GenerativeExplanationObject generativeExplanationObject = new GenerativeExplanationObject();
        String explanation = tmplExpService.createOutputExplanation(graphUri, component, "en");
        String dataset = generativeExplanations.createDataset(component, graphUri, component.getComponentMainType());
        TestDataObject testObject = new TestDataObject(
                component.getComponentMainType() == null ? null : AnnotationType.valueOf(component.getComponentMainType()),
                component.getComponentMainType() == null ? null : AnnotationType.valueOf(component.getComponentMainType()).ordinal(),
                component,
                null,
                explanation,
                dataset == null ? "Empty dataset" : dataset,
                graphUri,
                null, null, null, null
        );
        generativeExplanationObject.setTestComponent(testObject);
        for (int i = 0; i < shots; i++) {
            TestDataObject example = computeSingleTestObject(null);
            while (example == null)
                example = computeSingleTestObject(null);
            generativeExplanationObject.addExample(example);
        }
        return generativeExplanationObject;
    }

    public TestDataObject computeSingleTestObject(QanaryComponent qanaryComponent) {

        try {

            QanaryComponent component = qanaryComponent == null ? createRandomQanaryComponent() : qanaryComponent;

            logger.info("Random qanary component: {}", component);

            // Selection Question
            logger.info("Selecting question");
            Integer randomQuestionID = random.nextInt(GenerativeExplanations.QADO_DATASET_QUESTION_COUNT);
            String question = generativeExplanations.getRandomQuestion(randomQuestionID);

            // Resolve dependencies and select random components
            logger.info("Resolve dependencies and select components");
            ArrayList<AnnotationType> annotationTypes = new ArrayList<>(Arrays.asList(generativeExplanations.getDependencyList(component.getComponentMainType())));
            logger.info("Annotation List: {}", annotationTypes);
            List<QanaryComponent> componentListForQanaryPipeline = generativeExplanations.selectRandomComponents(annotationTypes);
            componentListForQanaryPipeline.add(component); // Separation of concerns, add this to the selectRandomComps method
            logger.info("List of comps: {}", componentListForQanaryPipeline);

            // Execute Qanary pipeline and store graphURI + questionID
            logger.info("Execute Qanary pipeline");
            QanaryResponseObject qanaryResponse = generativeExplanations.executeQanaryPipeline(question, componentListForQanaryPipeline.stream().map(QanaryComponent::getComponentName).toList());
            String graphURI = qanaryResponse.getOutGraph();
            String questionID = replaceQuestionId(qanaryResponse.getQuestion());

            // Create dataset
            logger.info("Create dataset");
            String dataset = generativeExplanations.createDataset(component, graphURI, component.getComponentMainType());
            if (dataset == null)
                throw new Exception();
            // Create Explanation for selected component
            logger.info("Create explanation");
            String explanation = tmplExpService.createOutputExplanation(graphURI, component, "en");
            return new TestDataObject(
                    AnnotationType.valueOf(component.getComponentMainType()),
                    AnnotationType.valueOf(component.getComponentMainType()).ordinal(),
                    component,
                    question,
                    explanation,
                    dataset,
                    graphURI,
                    questionID,
                    randomQuestionID,
                    null,
                    componentListForQanaryPipeline.toString());
        } catch (Exception e) {
            logger.error("{}", e.getMessage());
            return null;
        }
    }

    public String replaceQuestionId(String replaceableString) {
        return replaceableString.replace(questionIdReplacement + "/question/stored-question__text_", "questionID:");
    }

    public QanaryComponent createRandomQanaryComponent() {
        AnnotationType type = AnnotationType.values()[random.nextInt(AnnotationType.values().length)];
        QanaryComponent qanaryComponent = selectRandomComponentWithAnnotationType(type.name());
        qanaryComponent.setComponentMainType(type.name());
        return qanaryComponent;
    }

    public QanaryComponent selectRandomComponentWithAnnotationType(String annotationType) {
        QanaryComponent[] components = GenerativeExplanations.TYPE_AND_COMPONENTS.get(annotationType);
        return components[random.nextInt(components.length)];
    }

    public String createPrompt(int shots, GenerativeExplanationObject generativeExplanationObject) throws IOException {
        String prompt = getStringFromFile(
                generativeExplanations.getPromptTemplate(shots)
        );

        prompt = prompt.replace("<TASK_RDF_DATA_TEST>", generativeExplanationObject.getTestComponent().getDataSet());

        ArrayList<TestDataObject> testDataObjects = generativeExplanationObject.getExampleComponents();
        int i = 1;
        for (TestDataObject example : testDataObjects) {
            prompt = prompt.replace("<QUESTION_ID_EXAMPLE_" + i + ">", example.getQuestionID()) // TODO: Question != questionID
                    .replace("<EXAMPLE_EXPLANATION_" + i + ">", example.getExplanation())
                    .replace("<EXAMPLE_RDF_DATA_" + i + ">", example.getDataSet());
            i += 1;
        }

        return prompt;

    }

    public String getStringFromFile(String path) throws IOException {
        return QanaryTripleStoreConnector.readFileFromResources(path);
    }

    public String sendPrompt(String prompt, GptModel gptModel) throws Exception {
        Encoding encoding = encodingRegistry.getEncodingForModel(ModelType.GPT_3_5_TURBO);
        int tokens = encoding.countTokens(prompt);
        logger.info("Calculated Token: {}", tokens);
        logger.info("Prompt: {}", prompt);
        return generativeExplanationsRepository.sendGptPrompt(prompt, tokens, gptModel);
    }

    /**
     * Replaces the prompt depending on the passed number of shots, in case of shots > 0, than pre-defined examples are used.
     *
     * @param shots Determines the selected prompt
     */
    public String getInputDataExplanationPrompt(QanaryComponent component, String body, int shots) throws Exception {
        String prompt = getStringFromFile(generativeExplanations.getPromptTemplateInputData(shots));
        prompt = prompt.replace("${QUERY}", body).replace("${COMPONENT}", component.getPrefixedComponentName());
        if (shots > 0) {
            InputQueryExample inputQueryExample = GenerativeExplanations.INPUT_QUERIES_AND_EXAMPLE.get(random.nextInt(GenerativeExplanations.INPUT_QUERIES_AND_EXAMPLE.size()));
            prompt = prompt.replace("${ZEROSHOT_QUERY}", inputQueryExample.getQuery()).replace("${ZEROSHOT_EXPLANATION", inputQueryExample.getExplanations()); // select random pre-defined statements
            if (shots > 1) {
                InputQueryExample inputQueryExample2 = GenerativeExplanations.INPUT_QUERIES_AND_EXAMPLE.get(random.nextInt(GenerativeExplanations.INPUT_QUERIES_AND_EXAMPLE.size()));
                prompt = prompt.replace("${ONESHOT_QUERY}", inputQueryExample2.getQuery()).replace("${ONESHOT_EXPLANATION", inputQueryExample2.getExplanations()); // select random pre-defined statements
                if (shots > 2) {
                    InputQueryExample inputQueryExample3 = GenerativeExplanations.INPUT_QUERIES_AND_EXAMPLE.get(random.nextInt(GenerativeExplanations.INPUT_QUERIES_AND_EXAMPLE.size()));
                    prompt = prompt.replace("${TWOSHOT_QUERY}", inputQueryExample3.getQuery()).replace("${TWOSHOT_EXPLANATION", inputQueryExample3.getExplanations()); // select random pre-defined statements
                }
            }
        }
        return prompt;
    }

    /**
     * Care:
     * TODO: Support one-/two-/multi-shot prompts, replace outer placeholders
     *
     * @return
     */
    public String explainSingleMethod(ExplanationMetaData data, MethodItem method) throws Exception {
        int shots = data.getGptRequest().getShots();
        String prompt = buildMethodExplanationPrompt(PROMPT_TEMPLATE_PATH + "methods/" + data.getLang() + "_" + shots, data, null, method);
        return this.sendPrompt(prompt, data.getGptRequest().getGptModel());
    }

    public String buildMethodExplanationPrompt(String path, ExplanationMetaData data, List<Method> childMethods, MethodItem method) throws IOException, ExplanationException {
        int shots = data.getGptRequest().getShots();
        String promptTemplate = ExplanationHelper.getStringFromFile(path);
        promptTemplate = ExplanationHelper.replaceMethodExplanationPlaceholder(promptTemplate, method, childMethods, data);
        if (shots == 0) {
            return promptTemplate;
        } else {
            String error = "Multi-shot is not yet implemented.";
            logger.error(error);
            throw new ExplanationException(error);
        }
    }

    public Method explainSingleMethodReturnMethod(ExplanationMetaData data, MethodItem method) throws Exception {
        int shots = data.getGptRequest().getShots();
        String prompt = buildMethodExplanationPrompt(PROMPT_TEMPLATE_PATH + "methods/" + data.getLang() + "_" + shots, data, null, method);
        String explanation = this.sendPrompt(prompt, data.getGptRequest().getGptModel());
        return new Method(method.getMethod(), method.getMethodName(), explanation, method.getDocstringRepresentation(), method.getSourceCodeRepresentation(), prompt);
    }


    // TODO: Needed for multi-shot method explanation
    public String getMethodExample(String graph, QanaryComponent component, String method, int shots) throws IOException {
        ResultSet requestComponentAndMethodResult = qanaryRepository.selectWithResultSet(
                QanaryTripleStoreConnector.readFileFromResources(CHECK_EXISTENCE_OF_OTHER_METHODS_QUERY)
                        .replace("?graph", "<" + graph + ">")
                        .replace("?component", "<" + component.getPrefixedComponentName() + ">")
                        .replace("?methodName", "\"" + method + "\"")
        );
        QuerySolution qs = null;
        try {
            qs = requestComponentAndMethodResult.next();
        } catch (NoSuchElementException e) {
            logger.error("No other methods were executed, therefore no examples can be computed.");
            throw new RuntimeException("No other methods were executed, therefore no examples for the prompt could be generated."); // Handle Runtime exceptions in the controller; return other HttpStatus
        }

        // TODO: How to continue?
        // When we create only one explanation we'd need to check for this used method in the next call.


        return null;

    }

    public String getTemplateExplanation(String graphUri, QanaryComponent component, String lang) throws IOException {
        return tmplExpService.createOutputExplanation(graphUri, component, lang);
    }

    // Can be generalized with single method prompt, maybe in the ExplanationHelper (method that does that replacement for all)
    public String explainAggregatedMethodWithExplanations(MethodItem parent, List<Method> childMethods, ExplanationMetaData data) throws Exception {
        int shots = data.getGptRequest().getShots(); // Add data for current component
        String promptTemplate = ExplanationHelper.getStringFromFile(PROMPT_TEMPLATE_PATH + "methods/aggregated/" + data.getLang() + "_" + shots);
        promptTemplate = ExplanationHelper.replaceMethodExplanationPlaceholder(promptTemplate, parent, childMethods, data);
        return this.sendPrompt(promptTemplate, data.getGptRequest().getGptModel());
    }

    public Method explainAggregatedMethodWithExplanationsReturnMethod(MethodItem parent, List<Method> childMethods, ExplanationMetaData data) throws Exception {
        int shots = data.getGptRequest().getShots();
        String prompt = buildMethodExplanationPrompt(PROMPT_TEMPLATE_PATH + "methods/aggregated/" + data.getLang() + "_" + shots, data, childMethods, parent);
        String explanation = this.sendPrompt(prompt, data.getGptRequest().getGptModel());

        return new Method(parent.getMethod(), parent.getMethodName(), explanation, parent.getDocstringRepresentation(), parent.getSourceCodeRepresentation(), prompt);
    }

    // We get the list of children and need to // TODO: Probably cache
    public String explainMethodAggregatedWithData(MethodItem parent, ExplanationMetaData data) throws Exception {
        // Alternatively we could rebuild the parent - childs relationship with the complete map
        String prompt = buildPromptForAggregatedWithData(parent, data);
        return this.sendPrompt(prompt, data.getGptRequest().getGptModel());
    }

    public String buildPromptForAggregatedWithData(MethodItem parent, ExplanationMetaData data) throws ExplanationException, IOException {
        List<MethodItem> methodsWithData = getAllMethodsWithDataFromParent(parent.getMethod(), data.getGraph().toASCIIString());
        String promptTemplate = ExplanationHelper.getStringFromFile(PROMPT_AGGREGATED_DATA + data.getLang() + "_" + data.getGptRequest().getShots())
                // add parent data TODO
                .replace("${parentData}", parent.toString())
                .replace("${methodName}", parent.getMethodName())
                .replace("${data}", StringUtils.join(methodsWithData.stream().map(MethodItem::toString).toList(), "\n\n")); // Adjust prompt as discussed (4-part)
        return promptTemplate;
    }

    public Method explainMethodAggregatedWithDataReturnMethod(MethodItem parent, ExplanationMetaData data) throws Exception {
        String prompt = buildPromptForAggregatedWithData(parent, data);
        String explanation = sendPrompt(prompt, data.getGptRequest().getGptModel());
        return new Method(parent.getMethod(), parent.getMethodName(), explanation, parent.getDocstringRepresentation(), parent.getSourceCodeRepresentation(), prompt);
    }

    /**
     * Fetches the data for all methods that are inherently called by @param parent
     *
     * @param parent   The root method
     * @param graphUri Process graph
     * @return MethodItem instances for each returned method
     */
    // TODO: If used in the new approach, add docstring and sourcecode to query and to the prompt
    @Scheduled(fixedRate = 3600000)  // Every 1 hour
    @CacheEvict(value = "methodsWithData", allEntries = true)
    public List<MethodItem> getAllMethodsWithDataFromParent(String parent, String graphUri) throws IOException, ExplanationException {
        List<MethodItem> methodsWithData = new ArrayList<>();
        QuerySolutionMap qsm = new QuerySolutionMap();
        qsm.add("graph", ResourceFactory.createResource(graphUri));
        qsm.add("rootId", ResourceFactory.createResource(parent));
        String query = QanaryTripleStoreConnector.readFileFromResourcesWithMap(SELECT_ALL_METHODS_WITH_DATA_FROM_ROOT, qsm);
        ResultSet methodsWithDataResultSet = qanaryRepository.selectWithResultSet(query);

        while (methodsWithDataResultSet.hasNext()) {
            QuerySolution qs = methodsWithDataResultSet.next();
            MethodItem method = qanaryRepository.transformQuerySolutionToMethodItem(qs); // TODO: Here, the method toString() is used in prompt, adjust with regard to new Variable class
            method.setMethod(QanaryRepository.safeGetString(qs, "leaf"));
            methodsWithData.add(method);
        }

        logger.debug("Methods with data: {}", methodsWithData.size());
        return methodsWithData;
    }

}
