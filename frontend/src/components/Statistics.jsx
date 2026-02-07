import React, { useState, useEffect } from 'react';
import axios from 'axios';

const Statistics = () => {
    const [history, setHistory] = useState([]);
    const [loading, setLoading] = useState(false);
    const [expandedTestId, setExpandedTestId] = useState(null);

    // Initial load: fetch history
    //connects the backend to get past results(Get request)
    const fetchHistory = async () => {
        try {
            const res = await axios.get('http://localhost:8080/api/simulation/history');
            setHistory(res.data);
        } catch (err) {
            console.error(err);
        }
    };

    useEffect(() => {
        fetchHistory();
    }, []);
//post request
// tells backend to start specific test
    const runTest = async (id) => {
        setLoading(true);
        try {
            await axios.post(`http://localhost:8080/api/simulation/run?testId=${id}`);
            await fetchHistory();
        } catch (err) {
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    const toggleExpand = (id) => {
        setExpandedTestId(expandedTestId === id ? null : id);
    };

    // Helper to sort tests 1-10
    const sortedHistory = [...history].sort((a, b) => a.testNumber - b.testNumber);

    const testScenarios = [
        { id: 1, name: "Standard Morning (210 orders)", desc: "Balanced mix, standard flow." },
        { id: 2, name: "Espresso Rush (300 orders)", desc: "High volume, short prep times." },
        { id: 3, name: "Specialty Wave (230 orders)", desc: "Complex drinks, slower prep." },
        { id: 4, name: "Loyalty Flood (240 orders)", desc: "50% Gold members. Fairness test." },
        { id: 5, name: "Stress Test (250 orders)", desc: "Fast arrival, high SLA risk." },
        { id: 6, name: "Slow Start (260 orders)", desc: "Sparse then busy." },
        { id: 7, name: "Lunch Rush (270 orders)", desc: "Standard mix, high urgency." },
        { id: 8, name: "Evening Chill (280 orders)", desc: "Decaf/Tea heavy." },
        { id: 9, name: "Utility Test (290 orders)", desc: "Edge case combos." },
        { id: 10, name: "Grand Finale (300 orders)", desc: "Max load, everything." },
    ];

    const getTestResult = (id) => sortedHistory.find(h => h.testNumber === id);

    return (
        <div className="p-6">
            <h2 className="text-3xl font-extrabold text-coffee-800 mb-6 flex items-center gap-2">
                <span>📊</span> Peak Load Simulation
                <span className="text-sm font-normal text-gray-500 ml-4 py-1 px-3 bg-gray-100 rounded-full">Automated 7:00 AM - 10:00 AM Scenarios</span>
            </h2>

            <div className="bg-white rounded-lg shadow overflow-hidden">
                <table className="w-full text-left">
                    <thead className="bg-coffee-900 text-white uppercase text-sm font-bold tracking-wider">
                        <tr>
                            <th className="p-4">Test Case</th>
                            <th className="p-4">Avg Wait</th>
                            <th className="p-4">Max Wait</th>
                            <th className="p-4">Staff Load (B1/B2/B3)</th>
                            <th className="p-4">SLA Alerts</th>
                            <th className="p-4 text-center">Action</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-gray-200">
                        {testScenarios.map((scenario) => {
                            const result = getTestResult(scenario.id);
                            const isExpanded = expandedTestId === scenario.id;

                            return (
                                <React.Fragment key={scenario.id}>
                                    <tr className={`hover:bg-blue-50 transition-colors ${result ? 'bg-white' : 'bg-gray-50'}`}>
                                        <td className="p-4">
                                            <div className="font-bold text-coffee-900">#{scenario.id}: {scenario.name}</div>
                                            <div className="text-xs text-gray-500 mt-1">{scenario.desc}</div>
                                        </td>

                                        {result ? (
                                            <>
                                                <td className="p-4 font-mono font-bold text-blue-700">{result.avgWaitTime.toFixed(1)} min</td>
                                                <td className="p-4 font-mono font-bold text-red-700">{result.maxWaitTime.toFixed(1)} min</td>
                                                <td className="p-4 font-mono text-sm">
                                                    {result.barista1Count} / {result.barista2Count} / {result.barista3Count}
                                                </td>
                                                <td className="p-4">
                                                    {result.slaViolations > 0 ? (
                                                        <span className="bg-red-100 text-red-800 px-2 py-1 rounded text-xs font-bold border border-red-200">
                                                            ⚠ {result.slaViolations} Violations
                                                        </span>
                                                    ) : (
                                                        <span className="text-green-600 font-bold text-xs flex items-center gap-1">
                                                            <span className="text-lg">✔</span> Perfect
                                                        </span>
                                                    )}
                                                </td>
                                                <td className="p-4 text-center">
                                                    <button
                                                        onClick={() => toggleExpand(scenario.id)}
                                                        className="text-coffee-600 hover:text-coffee-800 text-sm font-bold mr-4 underline"
                                                    >
                                                        {isExpanded ? 'Hide' : 'Details'}
                                                    </button>
                                                    <button
                                                        onClick={() => runTest(scenario.id)}
                                                        disabled={loading}
                                                        className="text-xs bg-gray-200 hover:bg-gray-300 px-3 py-1 rounded text-gray-700 font-semibold"
                                                    >
                                                        ↻ Rerun
                                                    </button>
                                                </td>
                                            </>
                                        ) : (
                                            <>
                                                <td colSpan="4" className="p-4 text-center text-gray-400 italic">
                                                    Not run yet
                                                </td>
                                                <td className="p-4 text-center">
                                                    <button
                                                        onClick={() => runTest(scenario.id)}
                                                        disabled={loading}
                                                        className="bg-coffee-600 hover:bg-coffee-700 text-white px-4 py-1.5 rounded-full text-sm font-bold shadow-sm transition-transform active:scale-95"
                                                    >
                                                        ▶ Run Analysis
                                                    </button>
                                                </td>
                                            </>
                                        )}
                                    </tr>

                                    {/* EXPANDED DETAILS */}
                                    {isExpanded && result && (
                                        <tr className="bg-gray-50 animate-fadeIn">
                                            <td colSpan="6" className="p-4 border-t border-b border-gray-200">
                                                <div className="mb-2 font-bold text-coffee-800 text-sm uppercase tracking-wide">
                                                    Execution Log (120 Rows)
                                                </div>
                                                <div className="max-h-96 overflow-y-auto border rounded bg-white shadow-inner">
                                                    <table className="w-full text-sm table-fixed">
                                                        <thead className="bg-gray-100 text-gray-500 text-xs uppercase sticky top-0">
                                                            <tr>
                                                                <th className="text-left p-2 w-20">Time</th>
                                                                <th className="text-left p-2">Customer</th>
                                                                <th className="text-left p-2">Drink</th>
                                                                <th className="text-left p-2 w-24">Wait</th>
                                                                <th className="text-left p-2 w-24">Priority</th>
                                                                <th className="text-left p-2">Reason</th>
                                                            </tr>
                                                        </thead>
                                                        <tbody className="divide-y divide-gray-100">
                                                            {result.orders.map((o, idx) => {
                                                                const arrival = new Date(o.arrivalTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
                                                                const waitMins = ((new Date(o.completionTime) - new Date(o.arrivalTime)) / 60000).toFixed(1);
                                                                return (
                                                                    <tr key={idx} className="hover:bg-blue-50">
                                                                        <td className="p-2 font-mono text-gray-500">{arrival}</td>
                                                                        <td className="p-2 font-medium">{o.customerName}</td>
                                                                        <td className="p-2 text-gray-600">{o.drinks[0]}</td>
                                                                        <td className={`p-2 font-mono font-bold ${waitMins > 10 ? 'text-red-600' : 'text-blue-600'}`}>
                                                                            {waitMins} m
                                                                        </td>
                                                                        <td className="p-2">
                                                                            <span className={`px-2 py-0.5 rounded text-xs font-bold ${o.priorityScore > 80 ? 'bg-red-100 text-red-800' : 'bg-gray-100 text-gray-700'}`}>
                                                                                {o.priorityScore.toFixed(0)}
                                                                            </span>
                                                                        </td>
                                                                        <td className="p-2 text-xs text-gray-500">{o.priorityReason}</td>
                                                                    </tr>
                                                                )
                                                            })}
                                                        </tbody>
                                                    </table>
                                                </div>
                                            </td>
                                        </tr>
                                    )}
                                </React.Fragment>
                            );
                        })}
                    </tbody>
                </table>
            </div>
        </div>
    );
};

export default Statistics;
