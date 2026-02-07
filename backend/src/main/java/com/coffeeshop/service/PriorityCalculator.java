package com.coffeeshop.service;

import com.coffeeshop.model.Order;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class PriorityCalculator {

    private static final int MAX_WAIT_THRESHOLD_SECONDS = 600; // 10 minutes
    private static final int URGENCY_THRESHOLD_SECONDS = 480; // 8 minutes

    public void updatePriority(Order order) {
        long waitTimeSeconds = Duration.between(order.getArrivalTime(), LocalDateTime.now()).getSeconds();

        // 1. Wait Time Score (Weight: 40%)
        // Normalized: waitTime / 10 mins * 100 * 0.4
        double waitScore = (double) waitTimeSeconds / MAX_WAIT_THRESHOLD_SECONDS * 100 * 0.4;

        // 2. Order Complexity (Weight: 25%)
        // Prefer simpler orders for throughput. Max prep time for a single complex
        // drink is ~6 min.
        // Let's say max complexity is ~10 min for a large order.
        // Inverse: (10 - prepTime) / 10 * 100 * 0.25
        int totalPrepTime = order.getTotalPrepTime();
        double complexityScore = Math.max(0, (10 - totalPrepTime) / 10.0 * 100 * 0.25);

        // 3. Loyalty Status (Weight: 10%)
        double loyaltyScore = order.isLoyal() ? 10.0 : 0.0;

        // 4. Urgency (Weight: 25% + Emergency Boost)
        double urgencyScore = 0.0;
        if (waitTimeSeconds > URGENCY_THRESHOLD_SECONDS) {
            urgencyScore = 25.0 + 50.0; // standard weight + emergency boost
        } else {
            // Linear increase as it approaches 8 mins
            urgencyScore = (double) waitTimeSeconds / URGENCY_THRESHOLD_SECONDS * 25.0;
        }

        double totalScore = waitScore + complexityScore + loyaltyScore + urgencyScore;
        order.setPriorityScore(totalScore);

        // Determine Reason
        if (urgencyScore > 30) {
            order.setPriorityReason("⚠️ Urgent: Approaching Timeout!");
        } else if (loyaltyScore > 0 && totalScore > 50) {
            order.setPriorityReason("👑 Gold Member Priority");
        } else if (complexityScore > 20) {
            order.setPriorityReason("⚡ Quick Order Bonus");
        } else if (waitScore > 20) {
            order.setPriorityReason("clock Long Wait Time");
        } else {
            order.setPriorityReason("🔹 Initial Arrival / Standard");
        }
    }
}
