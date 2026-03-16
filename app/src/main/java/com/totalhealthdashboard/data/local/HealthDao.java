package com.totalhealthdashboard.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.totalhealthdashboard.data.models.HealthMetric;
import com.totalhealthdashboard.data.models.JournalEntry;

import java.util.List;

@Dao
public interface HealthDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMetric(HealthMetric metric);

    @Query("SELECT * FROM health_metrics ORDER BY date DESC")
    LiveData<List<HealthMetric>> getAllMetrics();

    @Query("SELECT * FROM health_metrics WHERE date = :date LIMIT 1")
    HealthMetric getMetricByDate(String date);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertJournal(JournalEntry entry);

    @Query("SELECT * FROM journal_entries ORDER BY timestamp DESC")
    LiveData<List<JournalEntry>> getAllJournals();
}
