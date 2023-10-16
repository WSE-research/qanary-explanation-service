import logo from './logo.svg';
import './App.css';
import Explanation from "./explanationComponent/Explanation";
import {
    BrowserRouter as Router,
    Route,
    Routes,
    R
} from "react-router-dom";
import DatasetViewer from './datasetViewer/DatasetViewer';

function App() {
  return (
    <div className="App">
        <Router>
            <Routes>
                <Route exact path="/" element={<Explanation />} />
                <Route exact path="dataviewer" element={<DatasetViewer />} />
            </Routes>
        </Router>
    </div>
  );
}

export default App;
