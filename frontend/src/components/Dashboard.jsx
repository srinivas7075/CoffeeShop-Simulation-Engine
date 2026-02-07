import React, { useEffect, useState } from 'react';
import axios from 'axios';
import { getQueue, getBaristas } from '../services/api';

const Dashboard = () => {
    const [queue, setQueue] = useState([]);
    const [baristas, setBaristas] = useState([]);
    const [stats, setStats] = useState({ avgWaitTime: '0.0 min', ordersServed: 0, maxWaitTime: '0.0 min', slaViolations: 0 });

    const fetchData = async () => {
        try {
            const [queueRes, baristasRes, statsRes] = await Promise.all([
                getQueue(),
                getBaristas(),
                axios.get('http://localhost:8080/api/stats')
            ]);
            setQueue(queueRes.data);
            setBaristas(baristasRes.data);
            setStats(statsRes.data);
        } catch (error) {
            console.error("Error fetching dashboard data:", error);
        }
    };

    useEffect(() => {
        fetchData();
        const interval = setInterval(fetchData, 1000); // Polling faster for countdowns
        return () => clearInterval(interval);
    }, []);

    const getRowColor = (order) => {
        if (order.priorityReason?.includes('Urgent') || order.priorityScore > 80) return 'bg-red-50 border-red-200';
        if (order.etaSeconds > 300) return 'bg-yellow-50 border-yellow-200';
        return 'bg-white border-gray-200';
    };

    const getRemainingTime = (barista) => {
        if (!barista.busy) return null;
        const now = Date.now();
        const end = barista.busyUntilEpochMillis;
        return Math.max(0, Math.ceil((end - now) / 1000));
    };

    const getPriorityLabel = (score) => {
        if (score > 80) return <span className="bg-red-100 text-red-800 text-xs px-2 py-0.5 rounded font-bold">HIGH ({score.toFixed(0)})</span>;
        if (score > 50) return <span className="bg-yellow-100 text-yellow-800 text-xs px-2 py-0.5 rounded font-bold">MED ({score.toFixed(0)})</span>;
        return <span className="bg-green-100 text-green-800 text-xs px-2 py-0.5 rounded font-bold">LOW ({score.toFixed(0)})</span>;
    };

    return (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 mb-8">
            {/* Queue Section */}
            <div className="lg:col-span-2 bg-white p-6 rounded-lg shadow-md">
                <h2 className="text-xl font-bold mb-4 text-coffee-800 flex justify-between items-center">
                    <span>Smart Waiting Queue ({queue.length})</span>
                    <span className="text-sm font-normal text-gray-500">Sorted by Dynamic Priority</span>
                </h2>

                <div className="overflow-x-auto">
                    <table className="w-full text-left border-collapse">
                        <thead>
                            <tr className="bg-gray-100 text-gray-600 text-sm uppercase">
                                <th className="p-3">Customer</th>
                                <th className="p-3">Drink</th>
                                <th className="p-3">ETA</th>
                                <th className="p-3">Priority</th>
                                <th className="p-3">Reason</th>
                            </tr>
                        </thead>
                        <tbody className="text-sm">
                            {queue.length === 0 ? (
                                <tr><td colSpan="5" className="p-4 text-center text-gray-400">No orders in queue.</td></tr>
                            ) : (
                                queue.map((order) => (
                                    <tr key={order.id} className={`border-b border-l-4 ${getRowColor(order)} transition-colors`}>
                                        <td className="p-3 font-semibold">
                                            {order.customerName}
                                            {order.loyal && <span className="ml-2 text-xs bg-yellow-100 text-yellow-800 px-1.5 py-0.5 rounded">Gold</span>}
                                        </td>
                                        <td className="p-3">{order.drinks.join(', ')}</td>
                                        <td className="p-3 font-mono">
                                            {order.etaSeconds < 60 ? '< 1 min' : `${Math.ceil(order.etaSeconds / 60)} min`}
                                        </td>
                                        <td className="p-3">
                                            {getPriorityLabel(order.priorityScore)}
                                        </td>
                                        <td className="p-3">
                                            {order.priorityReason && (
                                                <span className={`text-xs px-2 py-1 rounded font-medium ${order.priorityReason.includes('Urgent') ? 'bg-red-100 text-red-800' :
                                                    order.priorityReason.includes('Quick') ? 'bg-green-100 text-green-800' :
                                                        'bg-blue-50 text-blue-700'
                                                    }`}>
                                                    {order.priorityReason}
                                                </span>
                                            )}
                                        </td>
                                    </tr>
                                ))
                            )}
                        </tbody>
                    </table>
                </div>
            </div>

            <div className="space-y-8">
                {/* Barista Section */}
                <div className="bg-white p-6 rounded-lg shadow-md">
                    <h2 className="text-xl font-bold mb-4 text-coffee-800">Barista Status</h2>
                    <div className="grid grid-cols-1 gap-4">
                        {baristas.map((barista) => {
                            const remaining = getRemainingTime(barista);
                            return (
                                <div key={barista.id} className={`p-4 rounded-md border-l-4 shadow-sm ${barista.busy ? 'border-red-500 bg-red-50' : 'border-green-500 bg-green-50'}`}>
                                    <div className="flex justify-between items-center mb-2">
                                        <h3 className="font-bold text-lg text-gray-800">{barista.id}</h3>
                                        <span className={`px-2 py-0.5 rounded text-xs font-bold uppercase ${barista.busy ? 'bg-red-200 text-red-800' : 'bg-green-200 text-green-800'}`}>
                                            {barista.busy ? 'BUSY' : 'IDLE'}
                                        </span>
                                    </div>
                                    {barista.busy && barista.currentOrder ? (
                                        <div className="text-sm">
                                            <div className="flex justify-between text-gray-700 mb-1">
                                                <span>Making:</span>
                                                <span className="font-bold">{barista.currentOrder.drinks[0]}</span>
                                            </div>
                                            <div className="flex justify-between items-center">
                                                <span className="text-gray-500 text-xs">For: {barista.currentOrder.customerName}</span>
                                                <span className="font-mono text-lg font-bold text-coffee-700">{remaining}s</span>
                                            </div>
                                            <div className="w-full bg-gray-200 rounded-full h-2.5 mt-2 relative overflow-hidden">
                                                <div className="bg-coffee-600 h-2.5 rounded-full animate-progress" style={{ width: '100%' }}></div>
                                                <div className="absolute top-0 left-0 w-full h-full text-[8px] text-white text-center flex items-center justify-center font-bold tracking-wider">
                                                    PROCESSING
                                                </div>
                                            </div>
                                        </div>
                                    ) : (
                                        <div className="text-sm text-gray-500 italic">Waiting for next order...</div>
                                    )}
                                </div>
                            );
                        })}
                    </div>
                </div>

                {/* Analytics Panel */}
                <div className="bg-coffee-900 text-white p-6 rounded-lg shadow-md">
                    <h2 className="text-xl font-bold mb-4 border-b border-coffee-700 pb-2">Live Analytics</h2>
                    <div className="grid grid-cols-2 gap-4 text-center mb-4">
                        <div className="p-3 bg-coffee-800 rounded-lg">
                            <div className="text-2xl font-bold text-green-400">{stats.avgWaitTime}</div>
                            <div className="text-xs text-coffee-200 uppercase tracking-wider mt-1">Avg Wait</div>
                        </div>
                        <div className="p-3 bg-coffee-800 rounded-lg">
                            <div className="text-2xl font-bold text-yellow-400">{stats.ordersServed}</div>
                            <div className="text-xs text-coffee-200 uppercase tracking-wider mt-1">Orders Served</div>
                        </div>
                    </div>
                    <div className="grid grid-cols-2 gap-4 text-center">
                        <div className="p-3 bg-coffee-800 rounded-lg">
                            <div className="text-2xl font-bold text-red-400">{stats.maxWaitTime}</div>
                            <div className="text-xs text-coffee-200 uppercase tracking-wider mt-1">Max Wait</div>
                        </div>
                        <div className="p-3 bg-coffee-800 rounded-lg border border-red-900">
                            <div className="text-2xl font-bold text-orange-500">{stats.slaViolations || 0}</div>
                            <div className="text-xs text-coffee-200 uppercase tracking-wider mt-1">SLA Violations</div>
                        </div>
                    </div>
                    <div className="mt-4 text-center">
                        <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-green-900 text-green-300 text-xs font-medium border border-green-700">
                            <span className="w-1.5 h-1.5 rounded-full bg-green-400 animate-pulse"></span>
                            System Healthy
                        </span>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default Dashboard;
