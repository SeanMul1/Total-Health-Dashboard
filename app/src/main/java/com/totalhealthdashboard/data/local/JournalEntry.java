package com.totalhealthdashboard.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "journal_entries")
public class JournalEntry {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String content;
    public long timestamp;
    public int moodScore; // calculated on save

    public JournalEntry(String content, long timestamp, int moodScore) {
        this.content = content;
        this.timestamp = timestamp;
        this.moodScore = moodScore;
    }
}