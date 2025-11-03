import React, {useEffect, useState} from "react";
import {useParams} from "react-router";
import {BarChart, Bar, XAxis, YAxis, Tooltip, CartesianGrid, ResponsiveContainer} from "recharts";

export default function VotesChart() {
    const {id} = useParams();
    console.log("Poll ID is " + id);
    const API_URL = "http://127.0.0.1:8081/results/" + id;

    const [data, setData] = useState([]);
    const fetchResult = async () => {
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
        fetchResult(); // Initial fetch
        const interval = setInterval(fetchResult, 5000); // Poll every 5s
        return () => clearInterval(interval);
    }, [id]);


    return (
        <div className="w-full h-[500px] p-4 flex flex-col items-center">
            <h2 className="text-2xl font-bold mb-4">Live Voting Results</h2>
            <ResponsiveContainer width="30%" height={500}>
                <BarChart data={data} margin={{top: 20, right: 30, left: 20, bottom: 5}}>
                    <CartesianGrid strokeDasharray="3 3"/>
                    <XAxis dataKey="option"/>
                    <YAxis allowDecimals={false}/>
                    <Tooltip/>
                    <Bar dataKey="votes" fill="#4F46E5" radius={[8, 8, 0, 0]}/>
                </BarChart>
            </ResponsiveContainer>
        </div>
    );
}
