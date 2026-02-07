package com.coffeeshop.model;

import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class TestCase {
    private String id;
    private int testNumber;
    private int totalOrders;
    private double avgWaitTime; // minutes
    private double maxWaitTime; // minutes
    private int barista1Count;
    private int barista2Count;
    private int barista3Count;
    private int slaViolations;
    private List<Order> orders;

    public TestCase(int testNumber, int totalOrders) {
        this.id = UUID.randomUUID().toString();
        this.testNumber = testNumber;
        this.totalOrders = totalOrders;
    }
}
