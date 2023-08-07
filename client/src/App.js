import logo from './logo.svg';
import './App.css';
import Explanation from "./explanationComponent/Explanation";
import {
    BrowserRouter as Router,
    Route,
    Routes,
    R
} from "react-router-dom";

function App() {
  return (
    <div className="App">
        <Router>
            <Routes>
                <Route exact path="/" element={<Explanation />} />
            </Routes>
        </Router>
    </div>
  );
}

export default App;
