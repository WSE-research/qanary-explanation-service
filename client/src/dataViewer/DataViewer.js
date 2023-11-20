import "./DataViewer.css";
import Box from '@mui/material/Box';
import InputLabel from '@mui/material/InputLabel';
import MenuItem from '@mui/material/MenuItem';
import FormControl from '@mui/material/FormControl';
import Select, { SelectChangeEvent } from '@mui/material/Select';
import TextField from '@mui/material/TextField';
import { useState } from 'react';

export default function DataViewer() {

    const annotationTypes:[] = ["Instance", "SpotInstance", "Relation", "QuestionTranslation", "AnswerSPARQL", "AnswerJSON"];
    const [shotCount, setShotCount] = useState(1);

    const handleChange = (event: SelectChangeEvent) => {
        setShotCount(event.target.value)
    }

    return(
    <div style={{width: 1600, marginLeft: 160, marginRight: 160, height: 360, display: "flex", marginTop: 10, backgroundColor: "lightgrey"}}>
        <div style={{width:600, display: "flex", justifyContent: "center", alignItems: "center"}}>
            <FormControl style={{width:300}}>
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
        </div>
        <div style={{width:1000, display: "flex"}}>
            <div style={{width: 500, display: "flex", flexDirection: "column", textAlign: "left", padding: 15}}>
                <h3>Test Data</h3>
                        <TextField
                          id="standard-select-currency"
                          select
                          label="Annotation-Type"
                          defaultValue="EUR"
                          helperText="Please select a Annotation type"
                          variant="standard"
                        >
                            {annotationTypes.map((annType) => (
                                <MenuItem key={annType} value={annType}>{"AnnotationOf" + annType}</MenuItem>
                            ))}
                        </TextField>
            </div>

            <div style={{width: 500, display: "flex", flexDirection: "column", textAlign: "left", padding: 15}}>
                <h3>Example Data</h3>
                {
                    [...Array(shotCount)].map((value: undefined, index: number) => (
                        <TextField
                          id="standard-select-currency"
                          select
                          label="Annotation-Type"
                          defaultValue="EUR"
                          helperText="Please select a Annotation type"
                          variant="standard"
                          style={{marginTop: 15}}
                        >
                            {annotationTypes.map((annType) => (
                                <MenuItem key={annType} value={annType}>{"AnnotationOf" + annType}</MenuItem>
                            ))}
                        </TextField>
                    ))
                }
            </div>
        </div>
    </div>
    );

}