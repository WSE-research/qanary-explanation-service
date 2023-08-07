import { useEffect, useState } from "react";
import "./Explanation.css";

export default function Explanation() {

    const [explanation, setExplanation] = useState([]);

    return (
        <div>
            <h3>Eingabe GraphID: </h3>
            <input placeholder={"GraphID"} id={"graphid"}/>
            <button onClick={() => {
                const graphID = document.getElementById("graphid").value;
                if(graphID) {
                    fetch("http://localhost:4000/explanation?graphID=" + graphID)
                      //  .then(response=> response.text().then(textValue => setExplanation(textValue)))
                        .then(response => response.json())
                        .then(data => setExplanation(data));
                }
            }}>Erklär's mir!</button>
            <div>
                { explanation.length > 0 ? (
                    <div class="explanationWrapper">
                        <p>
                        Die Komponente DBpedia-Spotlight-NED hat zu deiner GraphID folgende Entitäten, Konfidenzen und URIs gefunden:
                        </p>
                            <table>
                            <thead>
                            <tr>
                                <th>Entität</th>
                                <th>Konfidenz</th>
                                <th>DBpedia-URI</th>
                            </tr>
                            </thead>
                            <tbody>
                                {
                                    explanation.map(item => (
                                        <tr>
                                            <td>{item.entity}</td>
                                            <td>{(item.score.value*100).toPrecision(6)} %</td>
                                            <td>{item.body.value}</td>
                                        </tr>
                                    ))
                                }
                            </tbody>
                        </table>
                    </div>
                ) : null
                }
            </div>
        </div>
    )
}