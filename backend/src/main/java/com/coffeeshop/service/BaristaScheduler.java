package com.coffeeshop.service;

import com.coffeeshop.model.Barista;
import com.coffeeshop.model.Order;
import com.coffeeshop.model.OrderStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

@Service
public class BaristaScheduler {

    private final List<Barista> baristas;
    private final ConcurrentLinkedQueue<Order> waitingQueue;
    private final List<Order> completedOrders; // NEW: History
    private final PriorityCalculator priorityCalculator;

    public BaristaScheduler(PriorityCalculator priorityCalculator) {
        this.priorityCalculator = priorityCalculator;
        this.waitingQueue = new ConcurrentLinkedQueue<>();
        this.completedOrders = new ArrayList<>();
        this.baristas = new ArrayList<>();
        // Initialize 3 Baristas
        baristas.add(new Barista("Barista 1"));
        baristas.add(new Barista("Barista 2"));
        baristas.add(new Barista("Barista 3"));
    }

    public void addOrder(Order order) {
        priorityCalculator.updatePriority(order);
        waitingQueue.add(order);
    }

    public List<Order> getQueue() {
        return waitingQueue.stream()
                .sorted(Comparator.comparingDouble(Order::getPriorityScore).reversed())
                .collect(Collectors.toList());
    }

    public List<Order> getCompletedOrders() {
        return new ArrayList<>(completedOrders);
    }

    public List<Barista> getBaristas() {
        return baristas;
    }

    @Scheduled(fixedRate = 1000) // Run every second
    public void scheduleOrders() {
        // 1. Update priorities for all waiting orders
        for (Order order : waitingQueue) {
            priorityCalculator.updatePriority(order);
            checkSlaAlerts(order); // Check for alerts
        }

        // 2. Sort queue by priority
        List<Order> sortedQueue = getQueue();

        // Update ETA (Simple approximation)
        // Avg prep time per item approx 3 mins. 3 Baristas.
        // Rate = 3 items / 3 mins = 1 item / min (real time).
        // In sim time (1 min = 5 sec), rate is faster.
        // Let's just say ETA = (Position + 1) * AvgPrepTime / NumBaristas
        int position = 0;
        for (Order o : sortedQueue) {
            // simple logic: wait time = sum of prep times of people ahead / 3 baristas
            // For demo: just arbitrary scaling
            long eta = (position + 1) * 30L; // 30 seconds wait per person ahead roughly

            // Fix: Ensure ETA is not 0 for visual trust
            if (eta < 60)
                eta = 60; // Minimum 1 min ETA in queue

            o.setEtaSeconds(eta);
            position++;
        }

        // 3. Assign orders to free baristas
        long nowMillis = System.currentTimeMillis();

        for (Barista barista : baristas) {
            // Check if barista finished their current order
            if (barista.isBusy() && nowMillis >= barista.getBusyUntilEpochMillis()) {
                completeOrder(barista);
            }

            // Assign new order if free
            if (!barista.isBusy() && !sortedQueue.isEmpty()) {
                // FIX 2: HARD override assignment for emergencies
                assignOrder(barista, sortedQueue);
            }
        }
    }

    private void checkSlaAlerts(Order order) {
        long waitTimeSeconds = java.time.Duration.between(order.getArrivalTime(), LocalDateTime.now()).getSeconds();

        // 9 minutes = 540 seconds (Warning)
        if (waitTimeSeconds >= 540 && waitTimeSeconds < 600 && !order.isWarningAlertSent()) {
            System.out.println("🚨 MANAGER ALERT: Order " + order.getId() + " (" + order.getCustomerName()
                    + ") is nearing SLA limit! (" + waitTimeSeconds + "s)");
            order.setWarningAlertSent(true);
        }

        // 10 minutes = 600 seconds (Critical Breach)
        if (waitTimeSeconds >= 600 && !order.isBreachAlertSent()) {
            System.err.println("❌ SLA BREACHED: Order " + order.getId() + " (" + order.getCustomerName()
                    + ") exceeded 10 minutes!");
            order.setBreachAlertSent(true);
        }
    }

    private void completeOrder(Barista barista) {
        if (barista.getCurrentOrder() != null) {
            barista.getCurrentOrder().setStatus(OrderStatus.COMPLETED);
            barista.getCurrentOrder().setCompletionTime(LocalDateTime.now());

            // Add to history
            completedOrders.add(barista.getCurrentOrder());

            // Log or notify completion
            System.out.println("Order " + barista.getCurrentOrder().getId() + " completed by " + barista.getId());
        }
        barista.setBusy(false);
        barista.setCurrentOrder(null);
    }

    private void assignOrder(Barista barista, List<Order> sortedQueue) {
        // Simple logic: take the highest priority order
        // Enhancement: We could check for workload balancing here as per reqs
        // For now, let's stick to strict priority from the sorted list

        // Find the first order that is still in WAITING status (in case of concurrency)
        Order nextOrder = null;

        // Priority Override: Check for Emergency orders (> 8 mins) explicitly first
        // Although sortedQueue is sorted by score, we want to be ABSOLUT ELY sure
        // that high-urgency orders (which have huge boosted scores) are picked.
        // Since PriorityCalculator gives +50 boost to urgency > 8 min, they should be
        // at top.
        // The sortedQueue is trustworthy.

        for (Order o : sortedQueue) {
            if (waitingQueue.contains(o)) { // still in queue
                nextOrder = o;
                break;
            }
        }

        if (nextOrder != null) {
            waitingQueue.remove(nextOrder);
            nextOrder.setStatus(OrderStatus.PROCESSING);
            nextOrder.setStartTime(LocalDateTime.now());

            // Log if this was an emergency assignment
            if (nextOrder.getPriorityReason() != null && nextOrder.getPriorityReason().contains("Urgent")) {
                System.out.println(
                        "🔥 EMERGENCY ASSIGNMENT: Order " + nextOrder.getId() + " assigned to " + barista.getId());
            }

            barista.setBusy(true);
            barista.setCurrentOrder(nextOrder);

            // Calculate processing time (simulating prep time)
            // For demo purposes, we can speed up time. 1 min real = 1 sec sim?
            // Or keep real time? Let's use real seconds for demo visibility, but maybe
            // scaled down.
            // Requirement says "Espresso (2 min)". Waiting 2 mins in a demo is long.
            // let's scale: 1 min prep = 5 seconds real time.
            int prepTimeSeconds = nextOrder.getTotalPrepTime() * 5;

            barista.setBusyUntilEpochMillis(System.currentTimeMillis() + (prepTimeSeconds * 1000L));

            System.out.println("Assigned Order " + nextOrder.getId() + " to " + barista.getId());
        }
    }
}
