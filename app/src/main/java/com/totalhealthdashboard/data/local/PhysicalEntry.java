package com.totalhealthdashboard.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "physical_entries")
public class PhysicalEntry {

    @PrimaryKey
    public int id = 1;

    // Activity
    public int steps;
    public double distanceKm;
    public int caloriesBurned;
    public int activeMinutes;
   // public int floorsClimbed;

    // Heart
    public int heartRate;
   // public int fatBurnMinutes;
   // public int cardioMinutes;
   // public int peakMinutes;
   // public int vo2Max;

    // Sleep
    public double sleepHours;
    public int sleepScore;
   // public int sleepDeepMinutes;
   // public int sleepRemMinutes;
   // public int sleepLightMinutes;
   // public int sleepAwakeMinutes;

    // Wellness
  //  public int spo2;
    public int stressScore;
  //  public int breathingRate;
   // public double skinTempVariation;
   // public int hrv;

    public long timestamp;

    public PhysicalEntry() {}
}
