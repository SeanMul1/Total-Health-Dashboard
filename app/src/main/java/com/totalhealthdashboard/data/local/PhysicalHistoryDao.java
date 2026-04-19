package com.totalhealthdashboard.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface PhysicalHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(PhysicalHistoryEntry entry);

    // Get last 7 days of physical history for a user
    @Query("SELECT * FROM physical_history WHERE userId = :userId " +
            "ORDER BY date DESC LIMIT 7")
    LiveData<List<PhysicalHistoryEntry>> getLast7Days(String userId);

    // Check if an entry already exists for today
    @Query("SELECT * FROM physical_history WHERE userId = :userId " +
            "AND date >= :startOfDay LIMIT 1")
    PhysicalHistoryEntry getEntryForDay(String userId, long startOfDay);
}