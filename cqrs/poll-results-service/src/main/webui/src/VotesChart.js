import React, { useEffect, useState } from "react";
import { BarChart, Bar, XAxis, YAxis, Tooltip, CartesianGrid, ResponsiveContainer } from "recharts";

export default function VotesChart() {
    // REST service endpoint with hardcoded poll ID (too bad - a homework for you:-)
    const API_URL = "http://127.0.0.1:8081/results/1";

    const [data, setData] = useState([]);
    const fetchData = async () => {
	try {
	    const response = await fetch(API_URL);
	    if (!response.ok) throw new Error("Failed to fetch data");
	    const result = await response.json();
	    setData(result);
	} catch (error) {
	    console.error("Error fetching data:", error);
	}
    };

    // Polling
    useEffect(() => {
	fetchData(); // Initial fetch
	const interval = setInterval(fetchData, 5000); // Poll every 5s
	return () => clearInterval(interval);
    }, []);

    
    return (
	<div className="w-full h-[500px] p-4 flex flex-col items-center">
	    <h2 className="text-2xl font-bold mb-4">Live Voting Results</h2>
	    <ResponsiveContainer width="30%" height={500}>
		<BarChart data={data} margin={{ top: 20, right: 30, left: 20, bottom: 5 }}>
		    <CartesianGrid strokeDasharray="3 3" />
		    <XAxis dataKey="option" />
		    <YAxis allowDecimals={false} />
		    <Tooltip />
		    <Bar dataKey="votes" fill="#4F46E5" radius={[8, 8, 0, 0]} />
		</BarChart>
	    </ResponsiveContainer>
	</div>
    );
}
