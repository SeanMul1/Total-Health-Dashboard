package com.totalhealthdashboard.ui.goals;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.totalhealthdashboard.R;
import com.totalhealthdashboard.data.local.UserGoals;
import com.totalhealthdashboard.repository.HealthRepository;
import com.totalhealthdashboard.ui.journal.JournalFragment;
import java.util.Locale;

public class GoalsFragment extends Fragment {

    private HealthRepository repo;
    private UserGoals currentGoals;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_goals, container, false);

        repo = HealthRepository.getInstance();
        repo.init(requireContext());

        // Section expand/collapse toggles
        setupExpandToggle(view, R.id.btn_expand_physical, R.id.container_physical);
        setupExpandToggle(view, R.id.btn_expand_diet, R.id.container_diet);
        setupExpandToggle(view, R.id.btn_expand_mental, R.id.container_mental);

        // Observe and display goals
        repo.getUserGoals().observe(getViewLifecycleOwner(), goals -> {
            if (goals == null) goals = new UserGoals();
            currentGoals = goals;
            bindGoals(view, goals);
        });

        // Observe live progress data
        repo.getFitbitData().observe(getViewLifecycleOwner(), data -> {
            if (data == null || currentGoals == null) return;
            TextView tvSteps  = view.findViewById(R.id.tv_progress_steps);
            TextView tvActive = view.findViewById(R.id.tv_progress_active);
            TextView tvSleep  = view.findViewById(R.id.tv_progress_sleep);
            tvSteps.setText("Today: " + String.format(Locale.getDefault(),
                    "%,d", data.getSteps()) + " steps");
            tvActive.setText("Today: " + data.getActiveMinutes() + " min");
            tvSleep.setText("Last night: " + data.getSleepHours() + " hrs");
        });

        repo.getTotalCaloriesToday().observe(getViewLifecycleOwner(), cal -> {
            TextView tv = view.findViewById(R.id.tv_progress_calories);
            tv.setText("Today: " + (cal != null ? cal : 0) + " kcal");
        });

        repo.getTotalProtein().observe(getViewLifecycleOwner(), prot -> {
            TextView tv = view.findViewById(R.id.tv_progress_protein);
            tv.setText(String.format(Locale.getDefault(),
                    "Today: %.0fg", prot != null ? prot : 0));
        });

        repo.getAverageMoodThisWeek().observe(getViewLifecycleOwner(), avg -> {
            TextView tv = view.findViewById(R.id.tv_progress_mood);
            if (avg == null || avg == 0) {
                tv.setText("This week avg: —");
            } else {
                tv.setText(String.format(Locale.getDefault(),
                        "This week avg: %.1f/10", avg));
            }
        });

        repo.getEntryCountThisWeek().observe(getViewLifecycleOwner(), count -> {
            TextView tv = view.findViewById(R.id.tv_progress_journal);
            tv.setText("This week: " + (count != null ? count : 0) + " entries");
        });

        return view;
    }

    private void bindGoals(View view, UserGoals goals) {
        // Physical
        bindGoalRow(view,
                R.id.tv_goal_steps, R.id.sw_steps, R.id.btn_edit_steps,
                "Goal: " + String.format(Locale.getDefault(), "%,d", goals.stepsGoal) + " steps",
                goals.stepsEnabled,
                (newVal, enabled) -> {
                    currentGoals.stepsGoal    = newVal;
                    currentGoals.stepsEnabled = enabled;
                    repo.saveGoals(currentGoals);
                }, goals.stepsGoal, "steps");

        bindGoalRow(view,
                R.id.tv_goal_active, R.id.sw_active, R.id.btn_edit_active,
                "Goal: " + goals.activeMinutesGoal + " min",
                goals.activeMinutesEnabled,
                (newVal, enabled) -> {
                    currentGoals.activeMinutesGoal    = newVal;
                    currentGoals.activeMinutesEnabled = enabled;
                    repo.saveGoals(currentGoals);
                }, goals.activeMinutesGoal, "min");

        bindGoalRow(view,
                R.id.tv_goal_sleep, R.id.sw_sleep, R.id.btn_edit_sleep,
                "Goal: " + goals.sleepHoursGoal + " hrs",
                goals.sleepHoursEnabled,
                (newVal, enabled) -> {
                    currentGoals.sleepHoursGoal    = newVal;
                    currentGoals.sleepHoursEnabled = enabled;
                    repo.saveGoals(currentGoals);
                }, (int) goals.sleepHoursGoal, "hrs");

        // Diet
        bindGoalRow(view,
                R.id.tv_goal_calories, R.id.sw_calories, R.id.btn_edit_calories,
                "Goal: " + String.format(Locale.getDefault(), "%,d", goals.caloriesGoal) + " kcal",
                goals.caloriesEnabled,
                (newVal, enabled) -> {
                    currentGoals.caloriesGoal    = newVal;
                    currentGoals.caloriesEnabled = enabled;
                    repo.saveGoals(currentGoals);
                }, goals.caloriesGoal, "kcal");

        bindGoalRow(view,
                R.id.tv_goal_protein, R.id.sw_protein, R.id.btn_edit_protein,
                "Goal: " + goals.proteinGoal + "g",
                goals.proteinEnabled,
                (newVal, enabled) -> {
                    currentGoals.proteinGoal    = newVal;
                    currentGoals.proteinEnabled = enabled;
                    repo.saveGoals(currentGoals);
                }, goals.proteinGoal, "g");

        // Mental
        bindGoalRow(view,
                R.id.tv_goal_mood, R.id.sw_mood, R.id.btn_edit_mood,
                "Goal: " + goals.moodGoal + "/10",
                goals.moodEnabled,
                (newVal, enabled) -> {
                    currentGoals.moodGoal    = newVal;
                    currentGoals.moodEnabled = enabled;
                    repo.saveGoals(currentGoals);
                }, goals.moodGoal, "/10");

        bindGoalRow(view,
                R.id.tv_goal_journal, R.id.sw_journal_days, R.id.btn_edit_journal_days,
                "Goal: " + goals.journalDaysGoal + " days",
                goals.journalDaysEnabled,
                (newVal, enabled) -> {
                    currentGoals.journalDaysGoal    = newVal;
                    currentGoals.journalDaysEnabled = enabled;
                    repo.saveGoals(currentGoals);
                }, goals.journalDaysGoal, "days");
    }

    private void bindGoalRow(View root, int tvGoalId, int swId, int btnEditId,
                             String goalText, boolean enabled,
                             GoalUpdateCallback callback,
                             int currentValue, String unit) {
        TextView tvGoal  = root.findViewById(tvGoalId);
        Switch sw        = root.findViewById(swId);
        TextView btnEdit = root.findViewById(btnEditId);

        tvGoal.setText(goalText);
        sw.setChecked(enabled);

        // Toggle enabled/disabled
        sw.setOnCheckedChangeListener((btn, isChecked) ->
                callback.onUpdate(currentValue, isChecked));

        // Edit goal value
        btnEdit.setOnClickListener(v ->
                showEditDialog(goalText, currentValue, unit, callback, sw.isChecked()));
    }

    private void showEditDialog(String title, int currentValue, String unit,
                                GoalUpdateCallback callback, boolean currentEnabled) {
        android.app.AlertDialog.Builder builder =
                new android.app.AlertDialog.Builder(requireContext());

        android.widget.LinearLayout layout = new android.widget.LinearLayout(requireContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(64, 48, 64, 32);
        layout.setBackgroundColor(0xFFFAFAFA);

        android.widget.TextView tvTitle = new android.widget.TextView(requireContext());
        tvTitle.setText("Update Goal");
        tvTitle.setTextSize(20);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setTextColor(0xFF1A1A1A);
        tvTitle.setPadding(0, 0, 0, 16);
        layout.addView(tvTitle);

        final EditText etValue = new EditText(requireContext());
        etValue.setText(String.valueOf(currentValue));
        etValue.setTextSize(16);
        etValue.setTextColor(0xFF1A1A1A);
        etValue.setPadding(24, 20, 24, 20);
        etValue.setBackgroundColor(0xFFF0F0F0);
        etValue.setSingleLine(true);
        etValue.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etValue.setHint("Enter value in " + unit);

        android.widget.LinearLayout.LayoutParams inputParams =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        inputParams.setMargins(0, 0, 0, 24);
        etValue.setLayoutParams(inputParams);
        layout.addView(etValue);

        android.widget.Button btnSave = new android.widget.Button(requireContext());
        btnSave.setText("Save →");
        btnSave.setTextSize(16);
        btnSave.setTypeface(null, android.graphics.Typeface.BOLD);
        btnSave.setTextColor(0xFFFFFFFF);
        btnSave.setBackgroundColor(0xFF1A1A1A);
        btnSave.setPadding(0, 32, 0, 32);
        android.widget.LinearLayout.LayoutParams btnParams =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        btnSave.setLayoutParams(btnParams);
        layout.addView(btnSave);

        builder.setView(layout);
        android.app.AlertDialog dialog = builder.create();

        btnSave.setOnClickListener(v -> {
            try {
                int newVal = Integer.parseInt(etValue.getText().toString().trim());
                callback.onUpdate(newVal, currentEnabled);
                dialog.dismiss();
                Toast.makeText(getContext(), "Goal updated ✓", Toast.LENGTH_SHORT).show();
            } catch (NumberFormatException e) {
                etValue.setError("Enter a valid number");
            }
        });

        dialog.show();
    }

    private void setupExpandToggle(View root, int btnId, int containerId) {
        View btn       = root.findViewById(btnId);
        View container = root.findViewById(containerId);
        btn.setOnClickListener(v -> {
            if (container.getVisibility() == View.VISIBLE) {
                container.setVisibility(View.GONE);
                ((TextView) btn).setText("▶");
            } else {
                container.setVisibility(View.VISIBLE);
                ((TextView) btn).setText("▼");
            }
        });
    }

    interface GoalUpdateCallback {
        void onUpdate(int newValue, boolean enabled);
    }
}