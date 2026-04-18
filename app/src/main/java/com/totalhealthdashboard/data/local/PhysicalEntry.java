package com.totalhealthdashboard.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "physical_entries")
public class PhysicalEntry {

    @PrimaryKey
    @NonNull
    public String userId = "";

    public int steps;
    public double distanceKm;
    public int caloriesBurned;
    public int activeMinutes;
    public int heartRate;
    public double sleepHours;
    public int sleepScore;
    public int stressScore;
    public long timestamp;
    public int currentHeartRate;
    public int floors;

    // Manual override flags — when true, Fitbit sync won't overwrite this field
    public boolean overrideSteps;
    public boolean overrideDistance;
    public boolean overrideCalories;
    public boolean overrideActiveMinutes;
    public boolean overrideHeartRate;
    public boolean overrideSleepHours;

    public PhysicalEntry() {}
}