import "./DataViewer.css";
import InputLabel from '@mui/material/InputLabel';
import MenuItem from '@mui/material/MenuItem';
import FormControl from '@mui/material/FormControl';
import Select from '@mui/material/Select';
import TextField from '@mui/material/TextField';
import { useState } from 'react';
import {Button} from "@mui/material";

export default function DataViewer() {

    const annotationTypes = ["Instance", "SpotInstance", "Relation", "QuestionTranslation", "AnswerSPARQL", "AnswerJSON"];
    const [shotCount, setShotCount] = useState(1);
    const [annTypes, setAnnTypes] = useState();
    const [experiments,setExperiments] = useState(null);
    const [experimentsIndex, setExperimentsIndex] = useState();
    const [currentIndex, setCurrentIndex] = useState(0);

    const handleChange = (event) => {
        setShotCount(event.target.value)
        setAnnTypes(new Array(shotCount));
    }

    const submitTypes = () => {
        const allValues = new Array(shotCount);
        let body = {
            "testType": document.getElementById('test-type').innerHTML,
            "shots": shotCount
        };
        for(let i = 0; i < shotCount; i++) {
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
                "Content-Type":"application/json"
            }
        })
            .then(response => response.json())
            .then(data =>  {
                setExperiments(data.explanations);
                setExperimentsIndex(data.explanations.length);
            })
            .catch(error => console.error(error));
    }

    return(
        <div>
    <div className={"container"}>
        <div className={"left"}>
            <FormControl>
                <InputLabel>Shots</InputLabel>
                <Select
                    label="Shots"
                    value={shotCount}
                    onChange={handleChange}
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
                        >
                            {annotationTypes.map((annType) => (
                                <MenuItem key={annType} value={annType}>{"AnnotationOf" + annType}</MenuItem>
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
                          id={`example-type-${index}`} // TODO: Rename id
                          select
                          label="Annotation-Type"
                          helperText="Please select a Annotation type"
                          variant="standard"
                          style={{marginTop: 15}}
                        >
                            {annotationTypes.map((annType) => (
                                <MenuItem className={"no-margin"} key={annType} value={annType}>{"AnnotationOf" + annType}</MenuItem>
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
                    experiments 
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
                    experiments 
                    ?
                    experiments[currentIndex].gptExplanation
                    :
                    null
                }
            </p>
        </div>
    </div>
    <div className="explanationSelection">
        <span onClick={() =>
        (currentIndex > 0) ? setCurrentIndex(currentIndex-1) : null
        }>Zurück</span>
        {currentIndex} / {experimentsIndex}
        <span onClick={() => 
        (currentIndex < experimentsIndex) ? setCurrentIndex(currentIndex+1) : null
        }>Weiter</span>
    </div>


    </div>
    );

}