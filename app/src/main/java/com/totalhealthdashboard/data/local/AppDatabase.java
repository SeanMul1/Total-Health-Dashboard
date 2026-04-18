package com.totalhealthdashboard.data.local;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(
        entities = {
                JournalEntry.class,
                NutritionEntry.class,
                PhysicalEntry.class,
                UserGoals.class
        },
        version = 9,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

    public abstract JournalDao journalDao();
    public abstract NutritionDao nutritionDao();
    public abstract PhysicalDao physicalDao();
    public abstract UserGoalsDao userGoalsDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    AppDatabase.class,
                    "health_database"
            ).fallbackToDestructiveMigration().build();
        }
        return instance;
    }
}