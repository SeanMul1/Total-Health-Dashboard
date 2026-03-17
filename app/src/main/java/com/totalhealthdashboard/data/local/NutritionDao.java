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

    @Query("SELECT * FROM nutrition_entries WHERE timestamp >= :startOfDay ORDER BY timestamp DESC")
    LiveData<List<NutritionEntry>> getEntriesForToday(long startOfDay);

    @Query("SELECT SUM(calories) FROM nutrition_entries WHERE timestamp >= :startOfDay")
    LiveData<Integer> getTotalCaloriesToday(long startOfDay);
}