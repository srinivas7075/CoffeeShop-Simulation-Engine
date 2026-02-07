# Coffee Shop Simulation Engine ☕

A full-stack discrete event simulation engine modeling peak coffee shop traffic. This system implements complex priority scheduling algorithms, dynamic barista load balancing, and real-time SLA monitoring to optimize order throughput under high concurrency.

## 🚀 Features

*   **Discrete Event Simulation**: Time-stepped simulation engine modeling order arrivals, prep times, and resource availability.
*   **Smart Priority Scheduling**: Custom weighted algorithm calculating priority based on:
    *   Dynamic Wait Time (Urgency increases as SLA breach approaches).
    *   Drink Complexity (Prep time impact).
    *   Loyalty Status (Gold members get boosts).
*   **Multi-Barista Concurrency**: Simulates 3 baristas working in parallel with non-blocking assignment logic.
*   **Real-Time Analytics**: React frontend visualizing wait times, SLA violations, and bottleneck detection.
*   **Data Export**: Automatic CSV generation for simulation datasets and result logs for analysis.

## 🛠️ Tech Stack

*   **Backend**: Java Spring Boot (REST API, Simulation Logic)
*   **Frontend**: React.js (Vite, Tailwind CSS, Axios)
*   **Data Structure**: Priority Queues, HashMaps, Linked Lists

## 🏗️ Architecture

### Controller (`SimulationController.java`)
Exposes REST endpoints to trigger simulations and retrieve history.

### Service (`SimulationService.java`)
The core engine. Handles:
*   Order Generation (Randomized distribution profiles).
*   Event Loop (Processing arrivals and completions).
*   Resource Allocation (Assigning free baristas).

### Model (`PriorityCalculator.java`)
The "Brain" of the operation. Calculates a normalized score (0-100) for every order to determine processing sequence.

## 📦 Installation & Run

### Backend
1.  Navigate to `backend`:
    ```bash
    cd backend
    ```
2.  Run the Spring Boot application:
    ```bash
    ./mvnw spring-boot:run
    ```
    *Server starts on port 8080.*

### Frontend
1.  Navigate to `frontend`:
    ```bash
    cd frontend
    ```
2.  Install dependencies:
    ```bash
    npm install
    ```
3.  Start the development server:
    ```bash
    npm run dev
    ```
    *Client starts on port 5173 (or 3000).*

## 📊 Usage

1.  Open the Frontend URL.
2.  Select a **Test Scenario** (e.g., "Espresso Rush", "Morning Chill").
3.  Click **Run Analysis**.
4.  View the generated usage report and "Drink Breakdown" in the backend console.
5.  Check `simulation_results.csv` in the project root for raw data.

## 🧪 Simulation Scenarios

The system includes 10 pre-configured test cases ranging from 200 to 300 orders, simulating different traffic patterns:
*   **Espresso Rush**: High volume of quick drinks.
*   **Complex Wave**: Operations bottleneck test with multi-step beverages.
*   **Loyalty Flood**: Priority logic stress test.

## 👨‍💻 Author
**Srinivas** - [GitHub](https://github.com/srinivas7075)
