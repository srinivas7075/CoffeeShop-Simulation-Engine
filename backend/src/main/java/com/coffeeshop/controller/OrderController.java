package com.coffeeshop.controller;

import com.coffeeshop.model.Barista;
import com.coffeeshop.model.DrinkType;
import com.coffeeshop.model.Order;
import com.coffeeshop.service.BaristaScheduler;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Allow frontend to access
public class OrderController {

    private final BaristaScheduler baristaScheduler;

    public OrderController(BaristaScheduler baristaScheduler) {
        this.baristaScheduler = baristaScheduler;
    }

    @PostMapping("/orders")
    public Order placeOrder(@RequestBody Map<String, Object> payload) {
        String customerName = (String) payload.get("customerName");
        boolean isLoyal = (boolean) payload.getOrDefault("isLoyal", false);
        List<String> drinkNames = (List<String>) payload.get("drinks");

        List<DrinkType> drinks = drinkNames.stream()
                .map(DrinkType::valueOf)
                .toList();

        Order order = new Order(customerName, drinks, isLoyal);
        baristaScheduler.addOrder(order);
        return order;
    }

    @GetMapping("/queue")
    public List<Order> getQueue() {
        return baristaScheduler.getQueue();
    }

    @GetMapping("/baristas")
    public List<Barista> getBaristas() {
        return baristaScheduler.getBaristas();
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        List<Order> completed = baristaScheduler.getCompletedOrders();
        List<Order> active = baristaScheduler.getQueue();
        int ordersServed = completed.size();

        // Calculate stats even if ordersServed is 0 (because we might have active
        // violations)

        double avgWaitSeconds = completed.stream()
                .mapToLong(o -> Duration.between(o.getArrivalTime(), o.getCompletionTime()).getSeconds())
                .average()
                .orElse(0.0);

        long maxWaitSecondsCompleted = completed.stream()
                .mapToLong(o -> Duration.between(o.getArrivalTime(), o.getCompletionTime()).getSeconds())
                .max()
                .orElse(0);

        // Check active max wait too (to show worst case currently in lobby)
        long maxWaitSecondsActive = active.stream()
                .mapToLong(o -> Duration.between(o.getArrivalTime(), LocalDateTime.now()).getSeconds())
                .max()
                .orElse(0);

        long maxWaitSeconds = Math.max(maxWaitSecondsCompleted, maxWaitSecondsActive);

        // SLA Violations: Completed (>10min) + Active (>10min)
        long slaViolationsCompleted = completed.stream()
                .mapToLong(o -> Duration.between(o.getArrivalTime(), o.getCompletionTime()).getSeconds())
                .filter(seconds -> seconds > 600)
                .count();

        long slaViolationsActive = active.stream()
                .mapToLong(o -> Duration.between(o.getArrivalTime(), LocalDateTime.now()).getSeconds())
                .filter(seconds -> seconds > 600)
                .count();

        // Convert to minutes for display
        String avgWaitStr = String.format("%.1f min", avgWaitSeconds / 60.0);
        String maxWaitStr = String.format("%.1f min", maxWaitSeconds / 60.0);

        return Map.of(
                "avgWaitTime", avgWaitStr,
                "ordersServed", ordersServed,
                "maxWaitTime", maxWaitStr,
                "slaViolations", slaViolationsCompleted + slaViolationsActive);
    }
}
