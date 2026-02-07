import React, { useState } from 'react';
import { placeOrder } from '../services/api';

const OrderForm = ({ onOrderPlaced }) => {
    const [customerName, setCustomerName] = useState('');
    const [drink, setDrink] = useState('COLD_BREW');
    const [isLoyal, setIsLoyal] = useState(false);
    const [loading, setLoading] = useState(false);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        try {
            await placeOrder({
                customerName,
                drinks: [drink],
                isLoyal
            });
            setCustomerName('');
            setDrink('COLD_BREW');
            setIsLoyal(false);
            onOrderPlaced(); // Refresh parent
        } catch (error) {
            console.error("Failed to place order:", error);
            alert("Failed to place order. Ensure backend is running.");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="bg-white p-6 rounded-lg shadow-md mb-8">
            <h2 className="text-xl font-bold mb-4 text-coffee-800">Place New Order</h2>
            <form onSubmit={handleSubmit} className="flex flex-col gap-4 md:flex-row md:items-end">
                <div className="flex-1">
                    <label className="block text-sm font-medium text-gray-700">Customer Name</label>
                    <input
                        type="text"
                        required
                        value={customerName}
                        placeholder="e.g. Alice"
                        onChange={(e) => setCustomerName(e.target.value)}
                        className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-coffee-500 focus:ring-coffee-500 sm:text-sm p-2 border"
                    />
                </div>
                <div className="flex-1">
                    <label className="block text-sm font-medium text-gray-700">Drink</label>
                    <select
                        value={drink}
                        onChange={(e) => setDrink(e.target.value)}
                        className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-coffee-500 focus:ring-coffee-500 sm:text-sm p-2 border"
                    >
                        <option value="COLD_BREW">Cold Brew (1 min)</option>
                        <option value="ESPRESSO">Espresso (2 min)</option>
                        <option value="AMERICANO">Americano (2 min)</option>
                        <option value="CAPPUCCINO">Cappuccino (4 min)</option>
                        <option value="LATTE">Latte (4 min)</option>
                        <option value="SPECIALTY_MOCHA">Mocha (6 min)</option>
                    </select>
                </div>
                <div className="flex items-center mb-2">
                    <input
                        type="checkbox"
                        id="loyalty"
                        checked={isLoyal}
                        onChange={(e) => setIsLoyal(e.target.checked)}
                        className="h-4 w-4 text-coffee-600 focus:ring-coffee-500 border-gray-300 rounded"
                    />
                    <label htmlFor="loyalty" className="ml-2 block text-sm text-gray-900">
                        Loyalty Member (Gold)
                    </label>
                </div>
                <button
                    type="submit"
                    disabled={loading}
                    className="bg-coffee-600 text-white px-4 py-2 rounded-md hover:bg-coffee-700 disabled:opacity-50"
                >
                    {loading ? 'Placing...' : 'Submit Order'}
                </button>
            </form>
        </div>
    );
};

export default OrderForm;
