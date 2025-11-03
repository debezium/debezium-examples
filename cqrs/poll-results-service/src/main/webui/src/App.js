import './App.css';
import React, { useEffect, useState } from "react";
import {
    BrowserRouter as Router,
    Link,
    Route,
    Routes
} from "react-router";
import VotesChart from "./VotesChart";

function App() {
    const API_URL = "http://127.0.0.1:8081/results/";

    const [polls, setData] = useState([]);
    const fetchPolls = async () => {
        try {
            const response = await fetch(API_URL);
            if (!response.ok) throw new Error("Failed to fetch data");
            const polls = await response.json();
            setData(polls);
        } catch (error) {
            console.error("Error fetching data:", error);
        }
    };

    // Polling
    useEffect(() => {
        fetchPolls(); // Initial fetch
        const interval = setInterval(fetchPolls, 5000); // Poll every 5s
        return () => clearInterval(interval);
    }, []);

    return (
        <div className="App">
            <Router>
                <div>
                    <h1>Polls</h1>
                    <nav>
                        <ul>
                            {polls.map(({id, question}) => (
                                <li key={id}>
                                    <Link to={`view/${id}`}>{question}</Link>
                                </li>
                            ))}
                        </ul>
                    </nav>
                    <Routes>
                        <Route
                            path="/view/:id"
                            element={<VotesChart/>}
                        />
                    </Routes>
                </div>
            </Router>
        </div>
    );
}

export default App;
