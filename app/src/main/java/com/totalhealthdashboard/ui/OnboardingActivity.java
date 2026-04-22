package com.totalhealthdashboard.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.totalhealthdashboard.R;
import com.totalhealthdashboard.data.local.UserGoals;
import com.totalhealthdashboard.repository.HealthRepository;

public class OnboardingActivity extends AppCompatActivity {

    private EditText etSteps, etActive, etSleep, etFloors;
    private EditText etCalories, etProtein, etCarbs, etFat;
    private EditText etMood, etJournal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        HealthRepository repo = HealthRepository.getInstance();
        repo.init(this);

        etSteps    = findViewById(R.id.et_steps);
        etActive   = findViewById(R.id.et_active);
        etSleep    = findViewById(R.id.et_sleep);
        etFloors   = findViewById(R.id.et_floors);
        etCalories = findViewById(R.id.et_calories);
        etProtein  = findViewById(R.id.et_protein);
        etCarbs    = findViewById(R.id.et_carbs);
        etFat      = findViewById(R.id.et_fat);
        etMood     = findViewById(R.id.et_mood);
        etJournal  = findViewById(R.id.et_journal);

        findViewById(R.id.btn_save_goals).setOnClickListener(v -> {
            if (!validateAndSave(repo)) return;
            goToMain();
        });

        findViewById(R.id.btn_skip_onboarding).setOnClickListener(v -> {
            UserGoals defaults = new UserGoals();
            repo.saveGoals(defaults);
            goToMain();
        });
    }

    private boolean validateAndSave(HealthRepository repo) {
        try {
            int steps    = parseField(etSteps,    1,   100000, "Steps");
            int active   = parseField(etActive,   1,   600,    "Active minutes");
            int sleep    = parseField(etSleep,    1,   14,     "Sleep hours");
            int floors   = parseField(etFloors,   1,   500,    "Floors");
            int calories = parseField(etCalories, 1,   15000,  "Calories");
            int protein  = parseField(etProtein,  1,   500,    "Protein");
            int carbs    = parseField(etCarbs,    1,   1500,   "Carbs");
            int fat      = parseField(etFat,      1,   500,    "Fat");
            int mood     = parseField(etMood,     1,   10,     "Mood");
            int journal  = parseField(etJournal,  1,   7,      "Journal days");

            if (steps < 0 || active < 0 || sleep < 0 || floors < 0
                    || calories < 0 || protein < 0 || carbs < 0
                    || fat < 0 || mood < 0 || journal < 0) return false;

            UserGoals goals = new UserGoals();
            goals.stepsGoal         = steps;
            goals.activeMinutesGoal = active;
            goals.sleepHoursGoal    = sleep;
            goals.floorsGoal        = floors;
            goals.caloriesGoal      = calories;
            goals.proteinGoal       = protein;
            goals.carbsGoal         = carbs;
            goals.fatGoal           = fat;
            goals.moodGoal          = mood;
            goals.journalDaysGoal   = journal;
            repo.saveGoals(goals);
            return true;

        } catch (Exception e) {
            Toast.makeText(this, "Please check all fields", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private int parseField(EditText et, int min, int max, String label) {
        try {
            String raw = et.getText().toString().trim();
            if (raw.isEmpty()) {
                et.setError(label + " is required");
                return -1;
            }
            int val = Integer.parseInt(raw);
            if (val < min || val > max) {
                et.setError(label + " must be " + min + "–" + max);
                return -1;
            }
            return val;
        } catch (NumberFormatException e) {
            et.setError("Enter a valid number");
            return -1;
        }
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}