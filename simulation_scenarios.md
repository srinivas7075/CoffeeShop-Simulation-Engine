# Simulation Test Cases

This file defines the 10 automated test scenarios used in the "Peak Load Simulation". Each test case runs deterministically with a fixed seed to ensure reproducibility.

## General Config
*   **Time Window:** 7:00 AM - 10:00 AM (3 Hours)
*   **Baristas:** 3 Active
*   **Order Volume:** 200 - 300 Orders per test

## Scenarios

### Test 1: Standard Morning
*   **Orders:** 210
*   **Profile:** Balanced mix of Espressos, Lattes, and Cappuccinos. Normal arrival rate (~50s variance).
*   **Goal:** Verify baseline performance.

### Test 2: Espresso Rush
*   **Orders:** 300 (Max Load)
*   **Profile:** 80% Espresso/Americano (Short prep time: 2 mins).
*   **Goal:** High throughput testing. Even with high volume, baristas should clear queue fast due to simple drinks.

### Test 3: Specialty Wave
*   **Orders:** 230
*   **Profile:** 95% Specialty Coffees (Long prep time: 4-6 mins).
*   **Goal:** Stress test wait times. Expect higher avg wait times and potential backend SLA alerts.

### Test 4: Loyalty Flood
*   **Orders:** 240
*   **Profile:** 60% Gold Members.
*   **Goal:** Verify fairness logic. Ensure non-loyal customers don't get completely starved (Wait Time weighting should eventually boost them).

### Test 5: Stress Test
*   **Orders:** 250
*   **Profile:** Very fast arrival rate (20s intervals).
*   **Goal:** Force queue backup to trigger Manager Alerts and Emergency Priority adjustments.

### Test 6: Slow Start
*   **Orders:** 260
*   **Profile:** Slow arrivals 7:00-8:30, then massive spike 8:30-10:00.
*   **Goal:** Test system elasticity and recovery.

### Test 7: Lunch Rush
*   **Orders:** 270
*   **Profile:** Standard mix, but high urgency factors (simulated).
*   **Goal:** Verify Priority Score calculation under load.

### Test 8: Evening Chill
*   **Orders:** 280
*   **Profile:** Heavy on Tea/Decaf options (simulated by drink types).
*   **Goal:** General high-volume variance.

### Test 9: Utility Test
*   **Orders:** 290
*   **Profile:** Random assortment, edge case arrivals.
*   **Goal:** Stability check.

### Test 10: Grand Finale
*   **Orders:** 300
*   **Profile:** Everything at once. High Volume + High Complexity + High Loyalty.
*   **Goal:** The ultimate stress test. "SLA Violations" are expected here but should be minimized by the algorithm.
