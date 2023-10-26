import "./DatasetViewer.css";
import data from "../json2.json";
import { useState } from "react";

export default function DatasetViewer() {

    const [counter, setCounter] = useState(0);

    const goldenExplanations = data.explanations.map((datum) => {
        return datum.testData.explanation;
    })

    const gptExplanations = data.explanations.map((datum) => {
        return datum.gptExplanation;
    })

    const arrayLength = data.explanations.length;

    return(
        <div className="container">
            Current Explanation: {counter+1} / {arrayLength}
            <div className="viewerContainer">
                <div className="viewer">
                    <p style={{ fontSize: 28, top: '10 %'}}>Golden standard</p>
                    <div className="explanation">
                        <p className="explanationText">{goldenExplanations[counter]}</p>
                    </div>
                </div>
            </div>
            <div className="viewerContainer">
                <div className="viewer">
                    <p style={{ fontSize: 28, top: '10 %'}}>GPT Explanation</p>
                    <div className="explanation">
                        <div className="explanationText">{gptExplanations[counter]}</div>
                    </div>
                </div>
            </div>
            <div className="buttons">
                <div style={{marginTop: 50}}>
                    <button onClick={() => counter > 0 ? setCounter(counter-1) : null}>
                        Vorheriges
                    </button>
                </div>
                <div style={{bottom: 0, marginBottom: 50}}>
                    <button onClick={() => counter < arrayLength ? setCounter(counter+1) : null}>
                        Nachfolgendes
                    </button>
                </div>
            </div>
        </div>
    )

}