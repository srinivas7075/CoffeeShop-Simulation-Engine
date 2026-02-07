package com.coffeeshop.model;

public enum DrinkType {
    COLD_BREW(1, 120),
    ESPRESSO(2, 150),
    AMERICANO(2, 140),
    CAPPUCCINO(4, 180),
    LATTE(4, 200),
    SPECIALTY_MOCHA(6, 250);

    private final int prepTimeMinutes;
    private final int priceInRupees;

    DrinkType(int prepTimeMinutes, int priceInRupees) {
        this.prepTimeMinutes = prepTimeMinutes;
        this.priceInRupees = priceInRupees;
    }

    public int getPrepTimeMinutes() {
        return prepTimeMinutes;
    }

    public int getPriceInRupees() {
        return priceInRupees;
    }
}
