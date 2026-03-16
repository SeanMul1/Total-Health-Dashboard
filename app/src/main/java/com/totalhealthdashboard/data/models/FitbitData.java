package com.totalhealthdashboard.data.models;

public class FitbitData {
    private int steps;
    private int heartRate;
    private int caloriesBurned;
    private int activeMinutes;
    private double sleepHours;

    public FitbitData(int steps, int heartRate, int caloriesBurned, int activeMinutes, double sleepHours) {
        this.steps = steps;
        this.heartRate = heartRate;
        this.caloriesBurned = caloriesBurned;
        this.activeMinutes = activeMinutes;
        this.sleepHours = sleepHours;
    }

    public int getSteps() { return steps; }
    public int getHeartRate() { return heartRate; }
    public int getCaloriesBurned() { return caloriesBurned; }
    public int getActiveMinutes() { return activeMinutes; }
    public double getSleepHours() { return sleepHours; }
}