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

    @Query("SELECT * FROM nutrition_entries WHERE userId = :userId " +
            "AND timestamp >= :from ORDER BY timestamp ASC")
    LiveData<List<NutritionEntry>> getEntriesFrom(String userId, long from);

    @Query("SELECT COALESCE(SUM(calories), 0) FROM nutrition_entries WHERE userId = :userId AND timestamp >= :startOfDay")
    int getTotalCaloriesTodaySync(String userId, long startOfDay);

    @Query("SELECT COALESCE(SUM(protein), 0) FROM nutrition_entries WHERE userId = :userId AND timestamp >= :startOfDay")
    double getTotalProteinTodaySync(String userId, long startOfDay);

    @Query("SELECT COALESCE(SUM(carbs), 0) FROM nutrition_entries WHERE userId = :userId AND timestamp >= :startOfDay")
    double getTotalCarbsTodaySync(String userId, long startOfDay);

    @Query("SELECT COALESCE(SUM(fat), 0) FROM nutrition_entries WHERE userId = :userId AND timestamp >= :startOfDay")
    double getTotalFatTodaySync(String userId, long startOfDay);

    @Query("DELETE FROM nutrition_entries WHERE userId = :userId")
    void deleteAllForUser(String userId);
}