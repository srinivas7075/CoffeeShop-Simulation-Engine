import axios from 'axios';

const API_URL = 'http://localhost:8080/api';

export const placeOrder = (order) => axios.post(`${API_URL}/orders`, order);
export const getQueue = () => axios.get(`${API_URL}/queue`);
export const getBaristas = () => axios.get(`${API_URL}/baristas`);
