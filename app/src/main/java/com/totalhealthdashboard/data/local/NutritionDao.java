package com.totalhealthdashboard.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface NutritionDao {

    @Insert
    void insert(NutritionEntry entry);

    @Delete
    void delete(NutritionEntry entry);

    @Query("SELECT * FROM nutrition_entries WHERE userId = :userId AND timestamp >= :startOfDay ORDER BY timestamp DESC")
    LiveData<List<NutritionEntry>> getEntriesForToday(String userId, long startOfDay);

    @Query("SELECT SUM(calories) FROM nutrition_entries WHERE userId = :userId AND timestamp >= :startOfDay")
    LiveData<Integer> getTotalCaloriesToday(String userId, long startOfDay);

    @Query("SELECT SUM(protein) FROM nutrition_entries WHERE userId = :userId AND timestamp >= :startOfDay")
    LiveData<Double> getTotalProteinToday(String userId, long startOfDay);

    @Query("SELECT SUM(carbs) FROM nutrition_entries WHERE userId = :userId AND timestamp >= :startOfDay")
    LiveData<Double> getTotalCarbsToday(String userId, long startOfDay);

    @Query("SELECT SUM(fat) FROM nutrition_entries WHERE userId = :userId AND timestamp >= :startOfDay")
    LiveData<Double> getTotalFatToday(String userId, long startOfDay);
}