package com.coffeeshop.service;

import com.coffeeshop.model.DrinkType;
import com.coffeeshop.model.Order;
import com.coffeeshop.model.TestCase;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SimulationService {

    private final PriorityCalculator priorityCalculator;
    private final List<TestCase> history = new ArrayList<>();

    public SimulationService(PriorityCalculator priorityCalculator) {
        this.priorityCalculator = priorityCalculator;
    }

    @jakarta.annotation.PostConstruct
    public void init() {
        // Generate the "Master Dataset" CSV on startup
        String filename = "simulation_dataset_inputs_" + System.currentTimeMillis() + ".csv";
        try (java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(filename))) {
            writer.println("Test_ID,Customer_Name,Drink_Type,Arrival_Time,Is_Loyal_Member");

            for (int i = 1; i <= 10; i++) {
                int count = 200 + (i * 10);
                if (i == 2)
                    count = 300;

                List<Order> orders = generateTestCaseOrders(i, count, i * 12345);
                // Sort by arrival
                orders.sort(Comparator.comparing(Order::getArrivalTime));

                for (Order o : orders) {
                    writer.printf("%d,%s,%s,%s,%b%n",
                            i,
                            o.getCustomerName(),
                            o.getDrinks().get(0),
                            o.getArrivalTime().toLocalTime(),
                            o.isLoyal());
                }
            }
            System.out.println("Dataset generated: simulation_dataset_inputs.csv");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public TestCase runSimulation(int testId) {
        int seed = testId * 12345; // Fixed seed for reproducibility
        int orderCount = 200 + (testId * 10); // Varies 210 - 300
        if (testId == 2)
            orderCount = 300; // Espresso Rush Max

        TestCase testCase = new TestCase(testId, orderCount);
        List<Order> allOrders = generateTestCaseOrders(testId, orderCount, seed);
        testCase.setOrders(allOrders); // Save full list

        // Simulation State
        long[] baristaAvailableAt = new long[3]; // 0=B1, 1=B2, 2=B3 (epoch seconds)
        // Start time: 7:00 AM = 0 relative seconds
        // End time: 10:00 AM = 10800 relative seconds

        long currentTime = 0;

        // Output lists
        List<Order> completedOrders = new ArrayList<>();
        int b1Count = 0, b2Count = 0, b3Count = 0;

        // Queue processing
        // Unlike previous simple loop, we must respect ARRIVAL times.
        // We simulate a clock ticking forward.

        PriorityQueue<Order> waitingQueue = new PriorityQueue<>((o1, o2) -> {
            // This comparator will be re-evaluated dynamically
            return Double.compare(o2.getPriorityScore(), o1.getPriorityScore());
        });

        List<Order> incomingOrders = new ArrayList<>(allOrders);
        incomingOrders.sort(Comparator.comparing(Order::getArrivalTime)); // Ensure chronological

        int processedCount = 0;

        // Discrete Event Simulation Loop
        // We jump to the next "Interesting Event": either an Order Arrival OR a Barista
        // Becoming Free

        while (processedCount < orderCount) {
            // 1. Find next arrival time
            long nextArrivalTime = incomingOrders.isEmpty() ? Long.MAX_VALUE
                    : toRelativeSeconds(incomingOrders.get(0).getArrivalTime());

            // 2. Find next barista free time
            long earliestBaristaFree = Long.MAX_VALUE;
            int freeBaristaIdx = -1;
            for (int i = 0; i < 3; i++) {
                if (baristaAvailableAt[i] < earliestBaristaFree) {
                    earliestBaristaFree = baristaAvailableAt[i];
                    freeBaristaIdx = i;
                }
            }

            // 3. Determine simulation clock advancement
            // If queue is empty, we MUST jump to next arrival
            // If queue has items, we can assign if a barista is free "now" or in future

            // If all baristas differ, we pick the earliest event.
            // But strict logic: We can't assign until arrival.

            long previousTime = currentTime;

            if (waitingQueue.isEmpty() && !incomingOrders.isEmpty()) {
                currentTime = Math.max(currentTime, nextArrivalTime);
            } else if (!waitingQueue.isEmpty()) {
                // If baristas are busy until future, jump there
                // BUT, if a new order arrives BEFORE barista is free, we must process that
                // arrival first
                // because it might have higher priority!

                if (!incomingOrders.isEmpty() && nextArrivalTime < earliestBaristaFree) {
                    currentTime = nextArrivalTime;
                } else {
                    currentTime = Math.max(currentTime, earliestBaristaFree);
                }
            }

            // 4. Process Arrivals up to currentTime
            while (!incomingOrders.isEmpty()
                    && toRelativeSeconds(incomingOrders.get(0).getArrivalTime()) <= currentTime) {
                waitingQueue.add(incomingOrders.remove(0));
            }

            // 5. Update Priorities for Waiting Queue based on new currentTime
            // (Score depends on wait time = currentTime - arrivalTime)
            List<Order> temp = new ArrayList<>(waitingQueue);
            waitingQueue.clear();
            for (Order o : temp) {
                o.setPriorityScore(calculateSimScore(o, currentTime - toRelativeSeconds(o.getArrivalTime())));
                setReason(o, currentTime - toRelativeSeconds(o.getArrivalTime()));
            }
            waitingQueue.addAll(temp);

            // 6. Assign if Barista is Free
            // Find truly free barista at currentTime
            int bestBarista = -1;
            for (int i = 0; i < 3; i++) {
                if (baristaAvailableAt[i] <= currentTime) {
                    bestBarista = i;
                    break; // Take first available
                }
            }

            if (bestBarista != -1 && !waitingQueue.isEmpty()) {
                Order nextOrder = waitingQueue.poll();

                // Assign
                long start = currentTime;
                long prep = nextOrder.getTotalPrepTime() * 60L; // min to sec
                long end = start + prep;

                baristaAvailableAt[bestBarista] = end;

                // Update Order Stats
                // We carry original arrival time.
                // Set completion time relative to base for display
                LocalDateTime base = nextOrder.getArrivalTime()
                        .minusSeconds(toRelativeSeconds(nextOrder.getArrivalTime())); // 7:00 AM
                nextOrder.setCompletionTime(base.plusSeconds(end));
                // Priority Reason is already set by the update loop above

                if (bestBarista == 0)
                    b1Count++;
                else if (bestBarista == 1)
                    b2Count++;
                else
                    b3Count++;

                completedOrders.add(nextOrder);
                processedCount++;
            } else {
                // If we couldn't assign (all busy), and no new orders coming immediately,
                // we force jump time to next event (earliest barista free)
                // This prevents infinite loop if currentTime < earliestBaristaFree
                if (bestBarista == -1 && !incomingOrders.isEmpty() && nextArrivalTime > currentTime) {
                    // Next interesting thing is either a barista freeing up OR an order arriving
                    long nextEvent = Math.min(nextArrivalTime, earliestBaristaFree);
                    currentTime = nextEvent;
                } else if (bestBarista == -1 && incomingOrders.isEmpty()) {
                    currentTime = earliestBaristaFree;
                }
            }
        }

        // Finalize counts
        // Sort orders by Arrival Time for "Time Wise" display in logs
        completedOrders.sort(Comparator.comparing(Order::getArrivalTime));

        testCase.setOrders(completedOrders);
        testCase.setBarista1Count(b1Count);
        testCase.setBarista2Count(b2Count);
        testCase.setBarista3Count(b3Count);

        // Calculate Stats
        double totalWait = completedOrders.stream()
                .mapToLong(o -> Duration.between(o.getArrivalTime(), o.getCompletionTime()).getSeconds()).sum();
        testCase.setAvgWaitTime((totalWait / orderCount) / 60.0);

        long maxWait = completedOrders.stream()
                .mapToLong(o -> Duration.between(o.getArrivalTime(), o.getCompletionTime()).getSeconds()).max()
                .orElse(0);
        testCase.setMaxWaitTime(maxWait / 60.0);

        long slas = completedOrders.stream()
                .mapToLong(o -> Duration.between(o.getArrivalTime(), o.getCompletionTime()).getSeconds())
                .filter(s -> s > 600).count();
        testCase.setSlaViolations((int) slas);

        // Store in history map effectively
        history.removeIf(t -> t.getTestNumber() == testId); // replace old run of same test
        history.add(testCase);
        history.sort(Comparator.comparingInt(TestCase::getTestNumber));

        exportToCsv(testCase);

        // --- CONSOLE REPORT FOR INVIGILATOR ---
        System.out.println("\n============ SIMULATION REPORT (Test #" + testId + ") ============");
        System.out.println("Total Orders Processed: " + orderCount);

        Map<String, Long> drinkCounts = completedOrders.stream()
                .flatMap(o -> o.getDrinks().stream())
                .map(Enum::name)
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));

        System.out.println("--- Drink Breakdown ---");
        drinkCounts.forEach((drink, count) -> System.out.printf("%-15s: %d%n", drink, count));
        System.out.println("======================================================\n");

        return testCase;
    }

    private void exportToCsv(TestCase testCase) {
        String filename = "simulation_results_" + System.currentTimeMillis() + ".csv";
        try (java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(filename))) {
            writer.println("Test ID, Customer, Drink, Arrival Time, Wait Time (min), Priority Score, Reason");
            for (Order o : testCase.getOrders()) {
                long waitSeconds = java.time.Duration.between(o.getArrivalTime(), o.getCompletionTime()).getSeconds();
                writer.printf("%d, %s, %s, %s, %.1f, %.1f, %s%n",
                        testCase.getTestNumber(),
                        o.getCustomerName(),
                        o.getDrinks().get(0), // Assume 1 item
                        o.getArrivalTime().toLocalTime(),
                        waitSeconds / 60.0,
                        o.getPriorityScore(),
                        o.getPriorityReason());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private long toRelativeSeconds(LocalDateTime time) {
        // Assume all times are today 7:00 AM based.
        // We construct them that way.
        LocalDateTime start = time.toLocalDate().atTime(7, 0);
        return Duration.between(start, time).getSeconds();
    }

    private List<Order> generateTestCaseOrders(int testId, int n, int seed) {
        Random rand = new Random(seed);
        List<Order> list = new ArrayList<>();
        LocalDateTime baseTime = LocalDateTime.now().toLocalDate().atTime(7, 0); // 7:00 AM today

        DrinkType[] types = DrinkType.values();

        // Distribution Profiles
        double espressoChance = 0.2;
        double loyaltyChance = 0.15;
        double arrivalRateMean = 40; // seconds between orders (Faster for 300 orders in 3 hours)

        if (testId == 2) {
            espressoChance = 0.8;
        } // Espresso Rush
        if (testId == 3) {
            espressoChance = 0.05;
        } // Complex / Slow
        if (testId == 4) {
            loyaltyChance = 0.6;
        } // Loyalty Flood
        if (testId == 5) {
            arrivalRateMean = 20;
        } // High Load (Breach likely)

        long currentOffsetSeconds = 0;

        for (int i = 0; i < n; i++) {
            // Drink
            // Drink Selection based on Requirement Frequencies
            // Cold Brew 25%, Espresso 20%, Americano 15%, Cappuccino 20%, Latte 12%,
            // Specialty 8%
            double r = rand.nextDouble() * 100;
            DrinkType drink;
            if (r < 25)
                drink = DrinkType.COLD_BREW;
            else if (r < 45)
                drink = DrinkType.ESPRESSO; // 25 + 20
            else if (r < 60)
                drink = DrinkType.AMERICANO; // 45 + 15
            else if (r < 80)
                drink = DrinkType.CAPPUCCINO; // 60 + 20
            else if (r < 92)
                drink = DrinkType.LATTE; // 80 + 12
            else
                drink = DrinkType.SPECIALTY_MOCHA; // Remaining 8%

            // Loyalty
            boolean isLoyal = rand.nextDouble() < loyaltyChance;

            // Arrival Time
            // Arrival: Poisson distribution (lambda = 1.4 customers/minute)
            // Time between arrivals = Exponential distribution with mean 1/lambda
            // Mean inter-arrival time = 1 / 1.4 min = ~43 seconds
            // Formula: -ln(U) / lambda
            double lambda = 1.4 / 60.0; // customers per second
            double u = rand.nextDouble();
            int interval = (int) (-Math.log(1.0 - u) / lambda);

            // Allow override for stress tests
            if (testId == 5)
                interval = (int) (interval * 0.5); // Rush

            // ADDED: Leisure Time / Lulls
            // 8% chance of a gap between 2 to 5 minutes
            if (rand.nextDouble() < 0.08) {
                interval += 120 + rand.nextInt(180);
            }

            currentOffsetSeconds += interval;
            if (currentOffsetSeconds > 10800)
                currentOffsetSeconds = 10800; // Cap at 10 AM

            LocalDateTime arrival = baseTime.plusSeconds(currentOffsetSeconds);

            String name = "Test" + testId + "-Cust" + (i + 1);
            Order o = new Order(name, List.of(drink), isLoyal);
            o.setArrivalTime(arrival);
            o.setId(String.valueOf(1000 * testId + i)); // ID: 1001, 1002...

            list.add(o);
        }
        return list;
    }

    private double calculateSimScore(Order order, long waitTimeSeconds) {
        // Mirrored logic from PriorityCalculator
        double waitScore = (double) waitTimeSeconds / 600.0 * 100 * 0.4;
        double complexityScore = Math.max(0, (10 - order.getTotalPrepTime()) / 10.0 * 100 * 0.25);
        double loyaltyScore = order.isLoyal() ? 10.0 : 0.0;

        double urgencyScore = 0.0;
        if (waitTimeSeconds > 480) { // 8 mins
            urgencyScore = 25.0 + 50.0;
        } else {
            urgencyScore = (double) waitTimeSeconds / 480.0 * 25.0;
        }
        return waitScore + complexityScore + loyaltyScore + urgencyScore;
    }

    private void setReason(Order order, long waitTimeSeconds) {
        if (waitTimeSeconds > 480)
            order.setPriorityReason("⚠️ Urgent: Approaching Timeout!");
        else if (order.isLoyal())
            order.setPriorityReason("👑 Gold Member Priority");
        else if (order.getTotalPrepTime() <= 2)
            order.setPriorityReason("⚡ Quick Order Bonus");
        else
            order.setPriorityReason("Standard Queue");
    }

    public List<TestCase> getHistory() {
        return history;
    }
}
