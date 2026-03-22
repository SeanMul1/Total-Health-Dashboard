package com.totalhealthdashboard.data.models;

import com.totalhealthdashboard.data.local.PhysicalEntry;

public class FitbitData {

    // Activity
    private int steps;
    private double distanceKm;
    private int caloriesBurned;
    private int activeMinutes;
    // private int floorsClimbed;

    // Heart
    private int heartRate;
    // private int fatBurnMinutes;
    // private int cardioMinutes;
    // private int peakMinutes;
    // private int vo2Max;

    // Sleep
    private double sleepHours;
    private int sleepScore;
    // private int sleepDeepMinutes;
    // private int sleepRemMinutes;
    // private int sleepLightMinutes;
    // private int sleepAwakeMinutes;

    // Wellness
    // private int spo2;
    private int stressScore;
    // private int breathingRate;
    // private double skinTempVariation;
    // private int hrv;

    // Default synthetic constructor
    public FitbitData() {
        this.steps = 0;
        this.distanceKm = 0;
        this.caloriesBurned = 0;
        this.activeMinutes = 0;
        // this.floorsClimbed = 12;
        this.heartRate = 0;
        // this.fatBurnMinutes = 25;
        // this.cardioMinutes = 10;
        // this.peakMinutes = 3;
        // this.vo2Max = 42;
        this.sleepHours = 0;
        this.sleepScore = 0;
        // this.sleepDeepMinutes = 62;
        // this.sleepRemMinutes = 94;
        // this.sleepLightMinutes = 212;
        // this.sleepAwakeMinutes = 18;
        // this.spo2 = 97;
        this.stressScore = 0;
        // this.breathingRate = 15;
        // this.skinTempVariation = 0.2;
        // this.hrv = 45;
    }

    // Constructor from manual PhysicalEntry
    public FitbitData(PhysicalEntry e) {
        this.steps = e.steps;
        this.distanceKm = e.distanceKm;
        this.caloriesBurned = e.caloriesBurned;
        this.activeMinutes = e.activeMinutes;
        // this.floorsClimbed = e.floorsClimbed;
        this.heartRate = e.heartRate;
        // this.fatBurnMinutes = e.fatBurnMinutes;
        // this.cardioMinutes = e.cardioMinutes;
        // this.peakMinutes = e.peakMinutes;
        // this.vo2Max = e.vo2Max;
        this.sleepHours = e.sleepHours;
        this.sleepScore = e.sleepScore;
        // this.sleepDeepMinutes = e.sleepDeepMinutes;
        // this.sleepRemMinutes = e.sleepRemMinutes;
        // this.sleepLightMinutes = e.sleepLightMinutes;
        // this.sleepAwakeMinutes = e.sleepAwakeMinutes;
        // this.spo2 = e.spo2;
        this.stressScore = e.stressScore;
        // this.breathingRate = e.breathingRate;
        // this.skinTempVariation = e.skinTempVariation;
        // this.hrv = e.hrv;
    }

    public int getSteps() { return steps; }
    public double getDistanceKm() { return distanceKm; }
    public int getCaloriesBurned() { return caloriesBurned; }
    public int getActiveMinutes() { return activeMinutes; }
    // public int getFloorsClimbed() { return floorsClimbed; }
    public int getHeartRate() { return heartRate; }
    // public int getFatBurnMinutes() { return fatBurnMinutes; }
    // public int getCardioMinutes() { return cardioMinutes; }
    // public int getPeakMinutes() { return peakMinutes; }
    // public int getVo2Max() { return vo2Max; }
    public double getSleepHours() { return sleepHours; }
    public int getSleepScore() { return sleepScore; }
    // public int getSleepDeepMinutes() { return sleepDeepMinutes; }
    // public int getSleepRemMinutes() { return sleepRemMinutes; }
    // public int getSleepLightMinutes() { return sleepLightMinutes; }
    // public int getSleepAwakeMinutes() { return sleepAwakeMinutes; }
    // public int getSpo2() { return spo2; }
    public int getStressScore() { return stressScore; }
    // public int getBreathingRate() { return breathingRate; }
    // public double getSkinTempVariation() { return skinTempVariation; }
    // public int getHrv() { return hrv; }
}