package com.totalhealthdashboard.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;


@Entity(tableName = "physical_entries")
public class PhysicalEntry {

    @PrimaryKey
    @NonNull
    public String userId = ""; // userId IS the primary key now

    public int steps;
    public double distanceKm;
    public int caloriesBurned;
    public int activeMinutes;
    public int heartRate;
    public double sleepHours;
    public int sleepScore;
    public int stressScore;
    public long timestamp;

    public PhysicalEntry() {}
}