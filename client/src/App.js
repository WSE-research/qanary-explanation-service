import '@fontsource/roboto/300.css';
import '@fontsource/roboto/400.css';
import '@fontsource/roboto/500.css';
import '@fontsource/roboto/700.css';
import './App.css';
import {
    BrowserRouter as Router,
    Route,
    Routes,
    R
} from "react-router-dom";
import DatasetViewer from './datasetViewer/DatasetViewer';
import DataViewer from './dataViewer/DataViewer';

function App() {
  return (
    <div className="App">
        <Router>
            <Routes>
                <Route exact path="/" element={<DataViewer />} />
                <Route exact path="dataviewer" element={<DatasetViewer />} />
            </Routes>
        </Router>
    </div>
  );
}

export default App;
