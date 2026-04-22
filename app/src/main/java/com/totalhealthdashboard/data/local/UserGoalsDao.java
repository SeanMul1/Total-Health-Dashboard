package com.totalhealthdashboard.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface UserGoalsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(UserGoals goals);

    @Query("SELECT * FROM user_goals WHERE userId = :userId")
    LiveData<UserGoals> getGoals(String userId);

    @Query("SELECT * FROM user_goals WHERE userId = :userId")
    UserGoals getGoalsSync(String userId);

    @Query("DELETE FROM user_goals WHERE userId = :userId")
    void deleteAllForUser(String userId);
}