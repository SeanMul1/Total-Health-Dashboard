package com.totalhealthdashboard.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;
import androidx.room.Delete;

@Dao
public interface JournalDao {

    @Insert
    void insert(JournalEntry entry);

    @Query("SELECT * FROM journal_entries WHERE userId = :userId ORDER BY timestamp DESC")
    LiveData<List<JournalEntry>> getAllEntries(String userId);

    @Query("SELECT COUNT(*) FROM journal_entries WHERE userId = :userId AND timestamp > :weekAgo")
    LiveData<Integer> getEntryCountThisWeek(String userId, long weekAgo);

    @Query("SELECT AVG(moodScore) FROM journal_entries WHERE userId = :userId AND timestamp > :weekAgo")
    LiveData<Float> getAverageMoodThisWeek(String userId, long weekAgo);

    @Query("SELECT * FROM journal_entries WHERE userId = :userId ORDER BY timestamp DESC LIMIT 1")
    LiveData<JournalEntry> getLatestEntry(String userId);

    @Delete
    void delete(JournalEntry entry);

    @Query("SELECT * FROM journal_entries WHERE userId = :userId ORDER BY timestamp DESC LIMIT :limit")
    LiveData<List<JournalEntry>> getRecentEntries(String userId, int limit);
}