package com.totalhealthdashboard.data.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "health_metrics")
public class HealthMetric {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String date; // YYYY-MM-DD
    private double activityIndex;
    private double nutritionScore;
    private double sleepScore;
    private long lastSynced;

    public HealthMetric(String date, double activityIndex, double nutritionScore, double sleepScore, long lastSynced) {
        this.date = date;
        this.activityIndex = activityIndex;
        this.nutritionScore = nutritionScore;
        this.sleepScore = sleepScore;
        this.lastSynced = lastSynced;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public double getActivityIndex() { return activityIndex; }
    public void setActivityIndex(double activityIndex) { this.activityIndex = activityIndex; }
    public double getNutritionScore() { return nutritionScore; }
    public void setNutritionScore(double nutritionScore) { this.nutritionScore = nutritionScore; }
    public double getSleepScore() { return sleepScore; }
    public void setSleepScore(double sleepScore) { this.sleepScore = sleepScore; }
    public long getLastSynced() { return lastSynced; }
    public void setLastSynced(long lastSynced) { this.lastSynced = lastSynced; }
}
