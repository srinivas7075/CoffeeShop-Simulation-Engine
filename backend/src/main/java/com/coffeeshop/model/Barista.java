package com.coffeeshop.model;

import lombok.Data;

@Data
public class Barista {
    private String id; // e.g., "Barista 1"
    private boolean isBusy;
    private Order currentOrder;
    private long busyUntilEpochMillis; // When they will be free

    public Barista(String id) {
        this.id = id;
        this.isBusy = false;
        this.currentOrder = null;
        this.busyUntilEpochMillis = 0;
    }
}
