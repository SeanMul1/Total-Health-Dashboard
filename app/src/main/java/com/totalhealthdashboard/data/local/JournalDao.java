package com.totalhealthdashboard.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface JournalDao {

    @Insert
    void insert(JournalEntry entry);

    @Query("SELECT * FROM journal_entries ORDER BY timestamp DESC")
    LiveData<List<JournalEntry>> getAllEntries();

    @Query("SELECT COUNT(*) FROM journal_entries WHERE timestamp > :weekAgo")
    LiveData<Integer> getEntryCountThisWeek(long weekAgo);

    @Query("SELECT AVG(moodScore) FROM journal_entries WHERE timestamp > :weekAgo")
    LiveData<Float> getAverageMoodThisWeek(long weekAgo);

    @Query("SELECT * FROM journal_entries ORDER BY timestamp DESC LIMIT 1")
    LiveData<JournalEntry> getLatestEntry();
}