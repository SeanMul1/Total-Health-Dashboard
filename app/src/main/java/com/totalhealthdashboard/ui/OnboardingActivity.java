package com.totalhealthdashboard.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.totalhealthdashboard.R;
import com.totalhealthdashboard.data.local.UserGoals;
import com.totalhealthdashboard.repository.HealthRepository;
import java.util.Locale;

public class OnboardingActivity extends AppCompatActivity {

    private final int[] steps = {10000};
    private final int[] activeMinutes = {30};
    private final int[] sleepHours = {8};
    private final int[] calories = {2000};
    private final int[] protein = {50};
    private final int[] mood = {7};
    private final int[] journalDays = {5};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        HealthRepository repo = HealthRepository.getInstance();
        repo.init(this);

        TextView tvStepsVal   = findViewById(R.id.tv_steps_val);
        TextView tvActiveVal  = findViewById(R.id.tv_active_val);
        TextView tvSleepVal   = findViewById(R.id.tv_sleep_val);
        TextView tvCalVal     = findViewById(R.id.tv_calories_val);
        TextView tvProtVal    = findViewById(R.id.tv_protein_val);
        TextView tvMoodVal    = findViewById(R.id.tv_mood_val);
        TextView tvJournalVal = findViewById(R.id.tv_journal_val);

        // Steps slider — range 2000–20000
        SeekBar sbSteps = findViewById(R.id.sb_steps);
        sbSteps.setMax(18000);
        sbSteps.setProgress(8000); // default 10000
        tvStepsVal.setText("10,000");
        sbSteps.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            public void onProgressChanged(SeekBar s, int p, boolean u) {
                steps[0] = p + 2000;
                tvStepsVal.setText(String.format(Locale.getDefault(), "%,d", steps[0]));
            }
        });

        // Active minutes — range 10–120
        SeekBar sbActive = findViewById(R.id.sb_active);
        sbActive.setMax(110);
        sbActive.setProgress(20);
        tvActiveVal.setText("30 min");
        sbActive.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            public void onProgressChanged(SeekBar s, int p, boolean u) {
                activeMinutes[0] = p + 10;
                tvActiveVal.setText(activeMinutes[0] + " min");
            }
        });

        // Sleep — range 4–12
        SeekBar sbSleep = findViewById(R.id.sb_sleep);
        sbSleep.setMax(8);
        sbSleep.setProgress(4);
        tvSleepVal.setText("8.0 hrs");
        sbSleep.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            public void onProgressChanged(SeekBar s, int p, boolean u) {
                sleepHours[0] = p + 4;
                tvSleepVal.setText(sleepHours[0] + ".0 hrs");
            }
        });

        // Calories — range 1000–4000
        SeekBar sbCal = findViewById(R.id.sb_calories);
        sbCal.setMax(3000);
        sbCal.setProgress(1000);
        tvCalVal.setText("2,000 kcal");
        sbCal.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            public void onProgressChanged(SeekBar s, int p, boolean u) {
                calories[0] = p + 1000;
                tvCalVal.setText(String.format(Locale.getDefault(), "%,d kcal", calories[0]));
            }
        });

        // Protein — range 10–200
        SeekBar sbProt = findViewById(R.id.sb_protein);
        sbProt.setMax(190);
        sbProt.setProgress(40);
        tvProtVal.setText("50g");
        sbProt.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            public void onProgressChanged(SeekBar s, int p, boolean u) {
                protein[0] = p + 10;
                tvProtVal.setText(protein[0] + "g");
            }
        });

        // Mood — range 1–10
        SeekBar sbMood = findViewById(R.id.sb_mood);
        sbMood.setMax(9);
        sbMood.setProgress(6);
        tvMoodVal.setText("7/10");
        sbMood.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            public void onProgressChanged(SeekBar s, int p, boolean u) {
                mood[0] = p + 1;
                tvMoodVal.setText(mood[0] + "/10");
            }
        });

        // Journal days — range 1–7
        SeekBar sbJournal = findViewById(R.id.sb_journal);
        sbJournal.setMax(6);
        sbJournal.setProgress(4);
        tvJournalVal.setText("5 days");
        sbJournal.setOnSeekBarChangeListener(new SimpleSeekBarListener() {
            public void onProgressChanged(SeekBar s, int p, boolean u) {
                journalDays[0] = p + 1;
                tvJournalVal.setText(journalDays[0] + " day" + (journalDays[0] == 1 ? "" : "s"));
            }
        });

        // Save and go to main
        findViewById(R.id.btn_save_goals).setOnClickListener(v -> {
            saveAndContinue(repo);
        });

        // Skip — use defaults
        findViewById(R.id.btn_skip_onboarding).setOnClickListener(v -> {
            UserGoals defaults = new UserGoals();
            repo.saveGoals(defaults);
            goToMain();
        });
    }

    private void saveAndContinue(HealthRepository repo) {
        UserGoals goals = new UserGoals();
        goals.stepsGoal         = steps[0];
        goals.activeMinutesGoal = activeMinutes[0];
        goals.sleepHoursGoal    = sleepHours[0];
        goals.caloriesGoal      = calories[0];
        goals.proteinGoal       = protein[0];
        goals.moodGoal          = mood[0];
        goals.journalDaysGoal   = journalDays[0];
        repo.saveGoals(goals);
        goToMain();
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    // Simple helper to avoid implementing all 3 methods every time
    abstract static class SimpleSeekBarListener implements SeekBar.OnSeekBarChangeListener {
        public void onStartTrackingTouch(SeekBar s) {}
        public void onStopTrackingTouch(SeekBar s) {}
    }
}