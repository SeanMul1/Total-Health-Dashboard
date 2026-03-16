package com.totalhealthdashboard.util;

/**
 * NormalizationEngine converts raw health metrics into dimensionless scores (0.0 to 1.0).
 */
public class NormalizationEngine {

    /**
     * Normalizes daily steps into an Activity Index.
     * Target: 10,000 steps = 1.0 score.
     */
    public static double normalizeActivity(int steps) {
        if (steps <= 0) return 0.0;
        double score = (double) steps / 10000.0;
        return Math.min(score, 1.0); // Cap at 1.0
    }

    /**
     * Normalizes sleep duration into a Sleep Quality Score.
     * Target: 8 hours (480 minutes) = 1.0 score.
     */
    public static double normalizeSleep(int minutesAsleep) {
        if (minutesAsleep <= 0) return 0.0;
        double score = (double) minutesAsleep / 480.0;
        return Math.min(score, 1.0);
    }

    /**
     * Normalizes nutritional data into a Nutrition Score.
     * This is a simplified example based on caloric balance or nutrient density.
     * For this mock, we'll use a range based on a target calorie intake.
     * Target: 2000 calories = 1.0 (with a penalty for over/under).
     */
    public static double normalizeNutrition(int calories) {
        if (calories <= 0) return 0.0;
        // Simple logic: 1.0 at 2000, decreasing as you move away
        double diff = Math.abs(2000 - calories);
        double score = 1.0 - (diff / 2000.0);
        return Math.max(0.0, score);
    }
}
