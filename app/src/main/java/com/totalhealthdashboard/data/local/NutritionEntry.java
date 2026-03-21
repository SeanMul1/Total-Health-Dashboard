package com.totalhealthdashboard.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "nutrition_entries")
public class NutritionEntry {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String userId;
    public String foodName;
    public int calories;
    public double protein;
    public double carbs;
    public double fat;
    public long timestamp;

    public NutritionEntry(String userId, String foodName, int calories,
                          double protein, double carbs, double fat, long timestamp) {
        this.userId    = userId;
        this.foodName  = foodName;
        this.calories  = calories;
        this.protein   = protein;
        this.carbs     = carbs;
        this.fat       = fat;
        this.timestamp = timestamp;
    }
}