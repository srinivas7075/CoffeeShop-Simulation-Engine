package com.coffeeshop.model;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class Order {
    private String id;
    private String customerName;
    private List<DrinkType> drinks;
    private LocalDateTime arrivalTime;
    private OrderStatus status;
    private double priorityScore;
    private boolean isLoyal;
    private LocalDateTime startTime; // When a barista starts working on it
    private LocalDateTime completionTime; // When it's done
    private long etaSeconds; // Estimated time to service
    private String priorityReason; // Explanation for priority (e.g. "Urgent", "Quick Order")
    private boolean warningAlertSent = false;
    private boolean breachAlertSent = false;

    public Order(String customerName, List<DrinkType> drinks, boolean isLoyal) {
        this.id = UUID.randomUUID().toString();
        this.customerName = customerName;
        this.drinks = drinks;
        this.isLoyal = isLoyal;
        this.arrivalTime = LocalDateTime.now();
        this.status = OrderStatus.WAITING;
        this.priorityScore = 0.0;
    }

    public boolean isWarningAlertSent() {
        return warningAlertSent;
    }

    public void setWarningAlertSent(boolean warningAlertSent) {
        this.warningAlertSent = warningAlertSent;
    }

    public boolean isBreachAlertSent() {
        return breachAlertSent;
    }

    public void setBreachAlertSent(boolean breachAlertSent) {
        this.breachAlertSent = breachAlertSent;
    }

    public int getTotalPrepTime() {
        return drinks.stream().mapToInt(DrinkType::getPrepTimeMinutes).sum();
    }
}
