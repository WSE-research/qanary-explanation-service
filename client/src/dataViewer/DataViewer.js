import "./DataViewer.css";
import InputLabel from '@mui/material/InputLabel';
import MenuItem from '@mui/material/MenuItem';
import FormControl from '@mui/material/FormControl';
import Select from '@mui/material/Select';
import TextField from '@mui/material/TextField';
import {useEffect, useState} from 'react';
import {Button} from "@mui/material";

export default function DataViewer() {

    const annotationTypes = ["AnnotationOfInstance", "AnnotationOfSpotInstance", "AnnotationOfRelation", "AnnotationOfQuestionTranslation", "AnnotationOfAnswerSPARQL", "AnnotationOfAnswerJSON"];
    const [shotCount, setShotCount] = useState(1);
    const [experiments, setExperiments] = useState(null);
    const [experimentsIndex, setExperimentsIndex] = useState();
    const [currentIndex, setCurrentIndex] = useState();
    const [numberOfAnnotations, setNumberOfAnnotations] = useState();
    const [qualityAnnotations, setQualityAnnotations] = useState();
    const [qualityPrefix, setQualityPrefix] = useState();
    const [showScoreAndSlider, setShowScoreAndSlider] = useState(false);

    useEffect(() => {
        experimentsIndex > 0 ? setShowScoreAndSlider(true) : setShowScoreAndSlider(false)
    }, [experimentsIndex]);

    useEffect(() => {
        if (experiments) {   // only invoke if experiments are provided
            if (experiments[currentIndex].hasScore) {    // if score is provided, set states
                let hasScore = experiments[currentIndex].hasScore
                setNumberOfAnnotations(hasScore.numberOfAnnotations);
                setQualityAnnotations(hasScore.qualityAnnotations);
                setQualityPrefix(hasScore.qualityPrefix);
            } else {  // If no score is provided, clear previous states
                setQualityPrefix("")
                setQualityAnnotations("")
                setNumberOfAnnotations("")
            }
        }
    }, [currentIndex, experiments]);

    const submitTypes = () => {
        setExperiments(null);
        setExperimentsIndex(null)
        const allValues = new Array(shotCount);
        let body = {
            "testType": document.getElementById('test-type').innerHTML,
            "shots": shotCount
        };
        for (let i = 0; i < shotCount; i++) {
            const selectElement = document.getElementById('example-type-' + i);
            allValues[i] = selectElement.innerHTML;
        }
        body.annotationTypes = allValues;
        sendRequest(JSON.stringify(body));
        setShotCount(1);
    }

    function sendRequest(body) {
        fetch("http://localhost:4000/experiments/explanations", {
            method: "POST",
            body: body,
            headers: {
                "Content-Type": "application/json"
            }
        })
            .then(response => response.json())
            .then(data => {
                setExperimentsIndex(data.explanations.length);
                if (data.explanations.length > 0) {
                    setCurrentIndex(0);
                    setExperiments(data.explanations);
                }
            })
            .catch(error => console.error(error));
    }

    return (
        <div>
            <div className={"container"}>
                <div className={"left"}>
                    <FormControl>
                        <InputLabel>Shots</InputLabel>
                        <Select
                            label="Shots"
                            value={shotCount}
                            onChange={(event) => setShotCount(event.target.value)}
                        >
                            <MenuItem value={1}>1</MenuItem>
                            <MenuItem value={2}>2</MenuItem>
                            <MenuItem value={3}>3</MenuItem>
                        </Select>

                    </FormControl>
                    <Button variant="outlined" onClick={submitTypes}>Bestätigen</Button>
                </div>
                <div className={"right"}>
                    <div>
                        <h3>Test Data</h3>
                        <TextField
                            id="test-type"
                            select
                            label="Annotation-Type"
                            helperText="Please select a Annotation type"
                            variant="standard"
                            defaultValue=''
                        >
                            {annotationTypes.map((annType) => (
                                <MenuItem key={annType} value={annType}>{annType}</MenuItem>
                            ))}
                        </TextField>
                    </div>

                    <div>
                        <h3>Example Data</h3>
                        <div className="exampleData">
                            {
                                [...Array(shotCount)].map((value, index) => (
                                    <TextField
                                        key={index}
                                        id={`example-type-${index}`}
                                        select
                                        label="Annotation-Type"
                                        helperText="Please select a Annotation type"
                                        variant="standard"
                                        defaultValue=''
                                        style={{marginTop: 15}}
                                    >
                                        {annotationTypes.map((annType) => (
                                            <MenuItem className={"no-margin"} key={annType}
                                                      value={annType}>{annType}</MenuItem>
                                        ))}
                                    </TextField>
                                ))
                            }
                        </div>
                    </div>
                </div>
            </div>

            <div className="explanationContainer">
                <div>
                    <h3>Golden Standard</h3>
                    <p>
                        {
                            (experiments)
                                ?
                                experiments[currentIndex].explanation
                                :
                                null
                        }
                    </p>
                </div>
                <div>
                    <h3>GPT Explanation</h3>
                    <p>
                        {
                            (experiments)
                                ?
                                experiments[currentIndex].gptExplanation
                                :
                                null
                        }
                    </p>
                </div>
            </div>

            {
                showScoreAndSlider ?
                    <div className={"scoresContainer"}>
                        <div className={"scores"}>
                            <TextField
                                id="numberOfAnnotations"
                                InputLabelProps={{shrink: true}}
                                label="Anzahl Annotationen"
                                shrink="true"
                                value={numberOfAnnotations}
                                onChange={(event: React.ChangeEvent<HTMLInputElement>) => {
                                    setNumberOfAnnotations(event.target.value);
                                }}
                            />
                            <TextField
                                id="qualityAnnotations"
                                label="Qualität Annotationen"
                                InputLabelProps={{shrink: true}}
                                value={qualityAnnotations}
                                onChange={(event: React.ChangeEvent<HTMLInputElement>) => {
                                    setQualityAnnotations(event.target.value);
                                }}
                            />
                            <TextField
                                id="qualityPrefix"
                                label="Qualität Prefix"
                                InputLabelProps={{shrink: true}}
                                value={qualityPrefix}
                                onChange={(event: React.ChangeEvent<HTMLInputElement>) => {
                                    setQualityPrefix(event.target.value);
                                }}
                            />
                        </div>
                        <button onClick={() => console.log(numberOfAnnotations)}>Update Experiment</button>
                    </div>
                    :
                    <div>

                    </div>
            }

            {
                showScoreAndSlider ?
                    <div className="explanationSelection">
                        <span
                            onClick={() => (currentIndex > 0) ? setCurrentIndex(currentIndex - 1) : null}>Zurück</span>
                        {currentIndex + 1} / {experimentsIndex}
                        <span
                            onClick={() => (currentIndex < experimentsIndex - 1) ? setCurrentIndex(currentIndex + 1) : null}>Weiter</span>
                    </div>
                    :
                    null
            }

        </div>
    );

}