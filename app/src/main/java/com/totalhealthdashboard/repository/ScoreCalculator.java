package com.totalhealthdashboard.repository;

import com.totalhealthdashboard.data.local.UserGoals;
import com.totalhealthdashboard.data.models.FitbitData;

public class ScoreCalculator {

    // ─── Physical ─────────────────────────────────────────────────────────────

    public static int calcPhysicalScore(FitbitData data, UserGoals g) {
        if (data == null) return 0;
        int total = 0;
        int count = 0;

        if (g.stepsEnabled && g.stepsGoal > 0 && data.getSteps() > 0) {
            total += threeZoneScore(data.getSteps(), g.stepsGoal);
            count++;
        }
        if (g.activeMinutesEnabled && g.activeMinutesGoal > 0 && data.getActiveMinutes() > 0) {
            total += threeZoneScore(data.getActiveMinutes(), g.activeMinutesGoal);
            count++;
        }
        if (g.sleepHoursEnabled && g.sleepHoursGoal > 0 && data.getSleepHours() > 0) {
            total += threeZoneScoreDouble(data.getSleepHours(), g.sleepHoursGoal);
            count++;
        }
        if (g.sleepScoreEnabled && g.sleepScoreGoal > 0 && data.getSleepScore() > 0) {
            total += threeZoneScore(data.getSleepScore(), g.sleepScoreGoal);
            count++;
        }
        if (g.heartRateEnabled && g.heartRateGoal > 0 && data.getHeartRate() > 0) {
            total += heartRateScore(data.getHeartRate(), g.heartRateGoal);
            count++;
        }
        if (g.floorsEnabled && g.floorsGoal > 0 && data.getFloors() > 0) {
            total += threeZoneScore(data.getFloors(), g.floorsGoal);
            count++;
        }
        if (g.stressEnabled && data.getStressScore() > 0) {
            total += threeZoneScore(data.getStressScore(), 10);
            count++;
        }

        return count == 0 ? 0 : total / count;
    }

    public static int calcPhysicalScoreFromEntry(
            int steps, int activeMinutes, double sleepHours,
            int sleepScore, int heartRate, int floors,
            int stressScore, UserGoals g) {

        int total = 0;
        int count = 0;

        if (g.stepsEnabled && g.stepsGoal > 0 && steps > 0) {
            total += threeZoneScore(steps, g.stepsGoal);
            count++;
        }
        if (g.activeMinutesEnabled && g.activeMinutesGoal > 0 && activeMinutes > 0) {
            total += threeZoneScore(activeMinutes, g.activeMinutesGoal);
            count++;
        }
        if (g.sleepHoursEnabled && g.sleepHoursGoal > 0 && sleepHours > 0) {
            total += threeZoneScoreDouble(sleepHours, g.sleepHoursGoal);
            count++;
        }
        if (g.sleepScoreEnabled && g.sleepScoreGoal > 0 && sleepScore > 0) {
            total += threeZoneScore(sleepScore, g.sleepScoreGoal);
            count++;
        }
        if (g.heartRateEnabled && g.heartRateGoal > 0 && heartRate > 0) {
            total += heartRateScore(heartRate, g.heartRateGoal);
            count++;
        }
        if (g.floorsEnabled && g.floorsGoal > 0 && floors > 0) {
            total += threeZoneScore(floors, g.floorsGoal);
            count++;
        }
        if (g.stressEnabled && stressScore > 0) {
            total += threeZoneScore(stressScore, 10);
            count++;
        }

        return count == 0 ? 0 : total / count;
    }

    // ─── Diet ─────────────────────────────────────────────────────────────────

    public static int calcDietScore(int calories, double protein,
                                    double carbs, double fat, UserGoals g) {
        int total = 0;
        int count = 0;

        if (g.caloriesEnabled && g.caloriesGoal > 0 && calories > 0) {
            total += sweetSpotScore(calories, g.caloriesGoal);
            count++;
        }
        if (g.proteinEnabled && g.proteinGoal > 0 && protein > 0) {
            total += proteinScore(protein, g.proteinGoal);
            count++;
        }
        if (g.carbsEnabled && g.carbsGoal > 0 && carbs > 0) {
            total += sweetSpotScore((int) carbs, g.carbsGoal);
            count++;
        }
        if (g.fatEnabled && g.fatGoal > 0 && fat > 0) {
            total += sweetSpotScore((int) fat, g.fatGoal);
            count++;
        }

        return count == 0 ? 0 : total / count;
    }

    // ─── Mental ───────────────────────────────────────────────────────────────

    public static int calcMentalScore(float moodAvg, int journalCount, UserGoals g) {
        int total = 0;
        int count = 0;

        if (g.moodEnabled && g.moodGoal > 0 && moodAvg > 0) {
            total += moodScore(moodAvg, g.moodGoal);
            count++;
        }
        if (g.journalDaysEnabled && g.journalDaysGoal > 0) {
            total += Math.min((journalCount * 100) / g.journalDaysGoal, 100);
            count++;
        }

        return count == 0 ? 0 : total / count;
    }

    // ─── Scoring algorithms ───────────────────────────────────────────────────

    /**
     * Three zone scoring for metrics where more is generally better:
     * 0-100% of goal  → linear 0-100
     * 100-120% goal   → bonus up to 110
     * >120% goal      → gradual decline back down
     */
    private static int threeZoneScore(int actual, int goal) {
        double ratio = (double) actual / goal;
        if (ratio <= 1.0) {
            return (int)(ratio * 100);
        } else if (ratio <= 1.2) {
            // Bonus zone — scales from 100 to 110
            return (int)(100 + (ratio - 1.0) / 0.2 * 10);
        } else {
            // Overdoing it — decline from 110 back down
            // At 2x goal score is around 70, at 4x goal score is around 30
            return (int) Math.max(0, 110 - (ratio - 1.2) * 50);
        }
    }

    private static int threeZoneScoreDouble(double actual, double goal) {
        double ratio = actual / goal;
        if (ratio <= 1.0) {
            return (int)(ratio * 100);
        } else if (ratio <= 1.2) {
            return (int)(100 + (ratio - 1.0) / 0.2 * 10);
        } else {
            return (int) Math.max(0, 110 - (ratio - 1.2) * 50);
        }
    }

    /**
     * Heart rate scoring — lower is better but with safe floor:
     * Below 40bpm     → penalised (dangerously low)
     * 40bpm to goal   → 100 (good zone)
     * Above goal      → loses 5 points per bpm over
     */
    private static int heartRateScore(int actual, int goal) {
        if (actual < 40) {
            // Dangerously low — penalise
            return Math.max(0, (int)(actual / 40.0 * 80));
        } else if (actual <= goal) {
            // At or below goal — full score
            return 100;
        } else {
            // Above goal — lose 5 points per bpm over
            return Math.max(0, 100 - (actual - goal) * 5);
        }
    }

    /**
     * Sweet spot scoring for calories, carbs, fat:
     * 90-110% of goal → 100
     * Below 90%       → linear penalty
     * Above 110%      → aggressive penalty
     */
    private static int sweetSpotScore(int actual, int goal) {
        double ratio = (double) actual / goal;
        if (ratio >= 0.9 && ratio <= 1.1) return 100;
        if (ratio < 0.9) return (int)(ratio / 0.9 * 100);
        return Math.max(0, (int)(100 - (ratio - 1.1) * 250));
    }

    /**
     * Protein scoring — more is okay up to a point:
     * 0-100% of goal  → linear 0-100
     * 100-130%        → stays at 100
     * Above 130%      → gradual decline
     */
    private static int proteinScore(double actual, int goal) {
        double ratio = actual / goal;
        if (ratio <= 1.0) return (int)(ratio * 100);
        if (ratio <= 1.3) return 100;
        return (int) Math.max(0, 100 - (ratio - 1.3) * 100);
    }

    /**
     * Mood scoring — above goal gives bonus, no penalty for feeling great:
     * 0-100% of goal  → linear 0-100
     * Above goal      → bonus up to 120
     */
    private static int moodScore(float actual, int goal) {
        double ratio = actual / goal;
        if (ratio <= 1.0) return (int)(ratio * 100);
        return Math.min(120, (int)(100 + (ratio - 1.0) * 100));
    }

    // ─── Overall ──────────────────────────────────────────────────────────────

    public static int calcOverallScore(int physScore, int dietScore, int mentalScore) {
        return (physScore + dietScore + mentalScore) / 3;
    }
}