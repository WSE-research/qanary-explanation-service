import "./DatasetViewer.css";
import data from "../json.json";
import { useState } from "react";

export default function DatasetViewer() {

    const goldenExplanations = data.explanations.map((datum) => {
        return datum.testData.explanation;
    })

    const gptExplanations = data.explanations.map((datum) => {
        return datum.gptExplanation;
    })

    const arrayLength = data.explanations.length;

    function decreaseCounter() {
        if(counter > 0) {
            setCounter(counter-1);
            setExplanation();
        }
    }

    function increaseCounter() {
        if(counter < arrayLength) {
            setCounter(counter+1)
            console.log(counter)
            setExplanation();
        }
    }

    function setExplanation() {
        setGoldenExplanation(goldenExplanations[counter])
        setGptExplanation(gptExplanations[counter])
    }

    const [goldenExplanation, setGoldenExplanation] = useState("");
    const [gptExplanation, setGptExplanation] = useState("");
    const [counter, setCounter] = useState(0);

    return(
        <div className="container">
            {counter}
            <div className="viewerContainer">
                <div className="viewer">
                    <p style={{ fontSize: 28, top: '10 %'}}>Golden standard</p>
                    <div className="explanation">
                        <p className="explanationText">{goldenExplanation}</p>
                    </div>
                </div>
            </div>
            <div className="viewerContainer">
                <div className="viewer">
                    <p style={{ fontSize: 28, top: '10 %'}}>GPT Explanation</p>
                    <div className="explanation">
                        <div className="explanationText">{gptExplanation}</div>
                    </div>
                </div>
            </div>
            <div className="buttons">
                <div style={{marginTop: 50}}>
                    <button onClick={decreaseCounter}>
                        Vorheriges
                    </button>
                </div>
                <div style={{bottom: 0, marginBottom: 50}}>
                    <button onClick={increaseCounter}>
                        Nachfolgendes
                    </button>
                </div>
            </div>
        </div>
    )

}