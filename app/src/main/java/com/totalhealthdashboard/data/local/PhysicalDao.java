package com.totalhealthdashboard.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface PhysicalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(PhysicalEntry entry);

    @Query("SELECT * FROM physical_entries WHERE id = 1 LIMIT 1")
    LiveData<PhysicalEntry> getPhysicalEntry();
}
