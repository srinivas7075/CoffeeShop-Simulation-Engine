import React, { useState } from 'react';
import OrderForm from './components/OrderForm';
import Dashboard from './components/Dashboard';
import Statistics from './components/Statistics';

function App() {
  const [view, setView] = useState('dashboard'); // 'dashboard' or 'stats'

  return (
    <div className="min-h-screen bg-coffee-50 p-8">
      <header className="mb-8 text-center">
        <h1 className="text-4xl font-extrabold text-coffee-900 tracking-tight">Bean & Brew Smart Queue</h1>
        <p className="text-coffee-700 mt-2">Intelligent Order Management System</p>

        <div className="mt-6 flex justify-center gap-4">
          <button
            onClick={() => setView('dashboard')}
            className={`px-4 py-2 rounded-full font-bold transition-all ${view === 'dashboard' ? 'bg-coffee-800 text-white shadow-lg' : 'bg-white text-coffee-800 hover:bg-gray-100'}`}
          >
            ☕ Live Dashboard
          </button>
          <button
            onClick={() => setView('stats')}
            className={`px-4 py-2 rounded-full font-bold transition-all ${view === 'stats' ? 'bg-coffee-800 text-white shadow-lg' : 'bg-white text-coffee-800 hover:bg-gray-100'}`}
          >
            📊 Simulation Stats
          </button>
        </div>
      </header>

      <main className="max-w-6xl mx-auto">
        {view === 'dashboard' ? (
          <>
            <OrderForm onOrderPlaced={() => { }} />
            <Dashboard />
          </>
        ) : (
          <Statistics />
        )}
      </main>
    </div>
  );
}

export default App;
