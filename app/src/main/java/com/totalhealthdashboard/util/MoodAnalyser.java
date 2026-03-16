package com.totalhealthdashboard.util;

import java.util.Arrays;
import java.util.List;

public class MoodAnalyser {

    private static final List<String> POSITIVE_WORDS = Arrays.asList(
        "happy", "great", "good", "excited", "grateful", "thankful",
        "joy", "wonderful", "amazing", "fantastic", "love", "positive",
        "motivated", "energetic", "calm", "peaceful", "proud", "hope",
        "smile", "laugh", "relax", "refresh", "accomplish", "success"
    );

    private static final List<String> NEGATIVE_WORDS = Arrays.asList(
        "sad", "bad", "terrible", "awful", "angry", "frustrated",
        "anxious", "stress", "worry", "tired", "exhausted", "lonely",
        "depressed", "unhappy", "upset", "nervous", "fear", "hate",
        "horrible", "painful", "miserable", "difficult", "struggle"
    );

    /**
     * Analyses journal text and returns a mood score 1-10
     * This is the normalisation engine described in the technical plan
     */
    public static int analyseMood(String text) {
        if (text == null || text.isEmpty()) return 5;

        String lower = text.toLowerCase();
        String[] words = lower.split("\\s+");

        int positiveCount = 0;
        int negativeCount = 0;

        for (String word : words) {
            // Strip punctuation
            String clean = word.replaceAll("[^a-z]", "");
            if (POSITIVE_WORDS.contains(clean)) positiveCount++;
            if (NEGATIVE_WORDS.contains(clean)) negativeCount++;
        }

        int total = positiveCount + negativeCount;
        if (total == 0) return 5; // neutral

        // Normalise to 1-10 scale
        double ratio = (double) positiveCount / total;
        int score = (int) Math.round(ratio * 9) + 1;
        return Math.max(1, Math.min(10, score));
    }

    public static String getMoodLabel(float averageScore) {
        if (averageScore >= 8) return "😄 Feeling great this week";
        if (averageScore >= 6) return "🙂 Mostly positive";
        if (averageScore >= 4) return "😐 Mixed feelings";
        if (averageScore >= 2) return "😔 Tough week";
        return "😟 Struggling — consider talking to someone";
    }
}