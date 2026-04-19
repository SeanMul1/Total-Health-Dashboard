package com.totalhealthdashboard.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "physical_history")
public class PhysicalHistoryEntry {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String userId;
    public long date;          // start of day timestamp — used as the day key
    public int steps;
    public double distanceKm;
    public int caloriesBurned;
    public int activeMinutes;
    public int floors;
    public int heartRate;
    public double sleepHours;
    public int sleepScore;
    public int stressScore;
    public long timestamp;
    public int dailyScore;
    public int overallScore;
    public int caloriesConsumed;
    public float moodScore;
    public int journalCount;

    public PhysicalHistoryEntry() {}
}