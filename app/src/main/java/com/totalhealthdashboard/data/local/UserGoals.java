package com.totalhealthdashboard.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "user_goals")
public class UserGoals {

    @PrimaryKey
    @NonNull
    public String userId = ""; // userId IS the primary key now

    // Physical goals
    public int stepsGoal = 10000;
    public boolean stepsEnabled = true;
    public double distanceGoal = 8.0;
    public boolean distanceEnabled = true;
    public int caloriesBurnedGoal = 500;
    public boolean caloriesBurnedEnabled = true;
    public int activeMinutesGoal = 30;
    public boolean activeMinutesEnabled = true;
    public int heartRateGoal = 70;
    public boolean heartRateEnabled = true;
    public double sleepHoursGoal = 8.0;
    public boolean sleepHoursEnabled = true;
    public int sleepScoreGoal = 8;
    public boolean sleepScoreEnabled = true;
    public int stressGoal = 7;
    public boolean stressEnabled = true;

    // Diet goals
    public int caloriesGoal = 2000;
    public boolean caloriesEnabled = true;
    public int proteinGoal = 50;
    public boolean proteinEnabled = true;
    public int carbsGoal = 250;
    public boolean carbsEnabled = true;
    public int fatGoal = 70;
    public boolean fatEnabled = true;

    // Mental goals
    public int moodGoal = 7;
    public boolean moodEnabled = true;
    public int journalDaysGoal = 5;
    public boolean journalDaysEnabled = true;

    public UserGoals() {}
}