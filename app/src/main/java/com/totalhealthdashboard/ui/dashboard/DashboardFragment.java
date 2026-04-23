package com.totalhealthdashboard.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.totalhealthdashboard.R;
import com.totalhealthdashboard.data.local.NutritionEntry;
import com.totalhealthdashboard.data.local.PhysicalHistoryEntry;
import com.totalhealthdashboard.data.local.UserGoals;
import com.totalhealthdashboard.data.models.FitbitData;
import com.totalhealthdashboard.repository.HealthRepository;
import com.totalhealthdashboard.repository.ScoreCalculator;
import com.totalhealthdashboard.ui.LoginActivity;
import com.totalhealthdashboard.ui.MainActivity;
import com.totalhealthdashboard.ui.history.HistoryFragment;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    private HealthRepository repo;

    private FitbitData latestPhysical = null;
    private Integer latestCalories    = null;
    private Double  latestProtein     = null;
    private Double  latestCarbs       = null;
    private Double  latestFat         = null;
    private Float   latestMood        = null;
    private Integer latestJournalDays = null;
    private UserGoals latestGoals     = null;

    private TextView tvOverallScore, tvSubPhysical, tvSubDiet, tvSubMental;
    private TextView tvPhysScore, tvDietScore, tvMentalScore, tvGoalsScore;

    private LinearLayout metricSelectorRow;
    private LinearLayout dashboardChartContainer;
    private List<PhysicalHistoryEntry> physicalHistory;
    private List<NutritionEntry> nutritionHistory;
    private String selectedMetric = "Steps";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        // Welcome user
        TextView tvWelcome = view.findViewById(R.id.tv_welcome_user);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String name = user.getDisplayName();
            if (name != null && !name.isEmpty()) {
                tvWelcome.setText(name.split(" ")[0] + " 👋");
            } else {
                tvWelcome.setText("Welcome 👋");
            }
        }

        // Wire score views
        tvOverallScore = view.findViewById(R.id.tv_overall_score);
        tvSubPhysical  = view.findViewById(R.id.tv_sub_physical);
        tvSubDiet      = view.findViewById(R.id.tv_sub_diet);
        tvSubMental    = view.findViewById(R.id.tv_sub_mental);
        tvPhysScore    = view.findViewById(R.id.tv_dash_physical_score);
        tvDietScore    = view.findViewById(R.id.tv_dash_diet_score);
        tvMentalScore  = view.findViewById(R.id.tv_dash_mental_score);
        tvGoalsScore   = view.findViewById(R.id.tv_dash_goals_score);

        TextView tvStepsDash = view.findViewById(R.id.tv_dash_steps);
        TextView tvCalDash   = view.findViewById(R.id.tv_dash_calories);
        TextView tvMoodDash  = view.findViewById(R.id.tv_dash_mood);
        TextView tvGoalsDash = view.findViewById(R.id.tv_dash_goals);

        repo = HealthRepository.getInstance();
        repo.init(requireContext());

        metricSelectorRow       = view.findViewById(R.id.metric_selector_row);
        dashboardChartContainer = view.findViewById(R.id.dashboard_chart_container);

        // Build metric selector buttons
        String[] metrics = {"Steps", "Cal Burned", "Sleep", "Active", "Floors",
                "Heart Rate", "Cal Intake", "Protein", "Carbs", "Fat", "Mood"};
        for (String metric : metrics) {
            TextView btn = new TextView(requireContext());
            btn.setText(metric);
            btn.setTextSize(12);
            btn.setPadding(28, 16, 28, 16);
            btn.setTypeface(null, android.graphics.Typeface.BOLD);
            btn.setBackground(buildSelectorBackground(metric.equals(selectedMetric)));
            btn.setTextColor(metric.equals(selectedMetric) ? 0xFFFFFFFF : 0xFF9E9E9E);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            p.setMargins(0, 0, 8, 0);
            btn.setLayoutParams(p);
            btn.setOnClickListener(v -> {
                selectedMetric = metric;
                updateSelectorButtons();
                buildDashboardChart();
            });
            metricSelectorRow.addView(btn);
        }

        // Observe physical history
        repo.getPhysicalHistory().observe(getViewLifecycleOwner(), entries -> {
            physicalHistory = entries;
            buildDashboardChart();
        });

        // Observe nutrition history
        List<Long> last7 = getLast7DayStarts();
        repo.getNutritionHistoryForDays(last7.get(0)).observe(getViewLifecycleOwner(), entries -> {
            nutritionHistory = entries;
            buildDashboardChart();
        });

        // Goals
        repo.getUserGoals().observe(getViewLifecycleOwner(), goals -> {
            latestGoals = goals;
            recalculateScores();
            tvGoalsDash.setText(goals == null
                    ? "Set your goals"
                    : "Tap to view and edit your targets");
        });

        // Physical
        repo.getFitbitData().observe(getViewLifecycleOwner(), data -> {
            latestPhysical = data;
            if (data != null) {
                tvStepsDash.setText(data.getSteps() == 0
                        ? "Enter your physical data"
                        : String.format(Locale.getDefault(), "%,d steps today", data.getSteps()));
            }
            recalculateScores();
        });

        // Diet
        repo.getTotalCaloriesToday().observe(getViewLifecycleOwner(), total -> {
            latestCalories = total;
            tvCalDash.setText((total == null || total == 0)
                    ? "Log a food to see data"
                    : total + " kcal logged today");
            recalculateScores();
        });

        repo.getTotalProteinToday().observe(getViewLifecycleOwner(), p -> {
            latestProtein = p;
            recalculateScores();
        });

        repo.getTotalCarbsToday().observe(getViewLifecycleOwner(), c -> {
            latestCarbs = c;
            recalculateScores();
        });

        repo.getTotalFatToday().observe(getViewLifecycleOwner(), f -> {
            latestFat = f;
            recalculateScores();
        });

        // Mental
        repo.getAverageMoodThisWeek().observe(getViewLifecycleOwner(), avg -> {
            latestMood = avg;
            if (avg == null || avg == 0) {
                tvMoodDash.setText("Write a journal entry");
            } else {
                tvMoodDash.setText(String.format(Locale.getDefault(),
                        "Avg mood: %.1f / 10", avg));
            }
            recalculateScores();
        });

        repo.getEntryCountThisWeek().observe(getViewLifecycleOwner(), count -> {
            latestJournalDays = count;
            recalculateScores();
        });

        // Card navigation
        BottomNavigationView nav = requireActivity().findViewById(R.id.bottom_navigation);
        view.findViewById(R.id.card_physical).setOnClickListener(v ->
                nav.setSelectedItemId(R.id.nav_physical));
        view.findViewById(R.id.card_diet).setOnClickListener(v ->
                nav.setSelectedItemId(R.id.nav_diet));
        view.findViewById(R.id.card_mental).setOnClickListener(v ->
                nav.setSelectedItemId(R.id.nav_mental));
        view.findViewById(R.id.card_goals).setOnClickListener(v ->
                nav.setSelectedItemId(R.id.nav_goals));
        view.findViewById(R.id.btn_view_history).setOnClickListener(v ->
                ((MainActivity) requireActivity())
                        .loadFragment(new HistoryFragment(), true));

        // Set password option for Google-only users
        TextView btnSetPassword = view.findViewById(R.id.btn_set_password);
        if (user != null) {
            boolean isGoogleOnly = user.getProviderData().stream()
                    .anyMatch(info -> info.getProviderId().equals("google.com"))
                    && user.getProviderData().stream()
                    .noneMatch(info -> info.getProviderId().equals("password"));
            if (isGoogleOnly) btnSetPassword.setVisibility(View.VISIBLE);
        }
        btnSetPassword.setOnClickListener(v -> showSetPasswordDialog());

        // Logout
        view.findViewById(R.id.btn_logout).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(requireContext(), LoginActivity.class));
            requireActivity().finish();
        });

        // Delete account
        view.findViewById(R.id.btn_delete_account).setOnClickListener(v ->
                showDeleteAccountDialog());

        return view;
    }

    // ─── Score calculation ────────────────────────────────────────────────────

    private void recalculateScores() {
        UserGoals g = latestGoals != null ? latestGoals : new UserGoals();

        int physScore   = ScoreCalculator.calcPhysicalScore(latestPhysical, g);
        int dietScore   = ScoreCalculator.calcDietScore(
                latestCalories != null ? latestCalories : 0,
                latestProtein  != null ? latestProtein  : 0,
                latestCarbs    != null ? latestCarbs    : 0,
                latestFat      != null ? latestFat      : 0, g);
        int mentalScore = ScoreCalculator.calcMentalScore(
                latestMood         != null ? latestMood         : 0,
                latestJournalDays  != null ? latestJournalDays  : 0, g);

        int overall = ScoreCalculator.calcOverallScore(physScore, dietScore, mentalScore);

        tvOverallScore.setText(String.valueOf(overall));
        tvOverallScore.setTextColor(getScoreColour(overall));

        tvSubPhysical.setText(physScore  < 0 ? "—" : String.valueOf(physScore));
        tvSubDiet.setText(dietScore      < 0 ? "—" : String.valueOf(dietScore));
        tvSubMental.setText(mentalScore  < 0 ? "—" : String.valueOf(mentalScore));

        tvPhysScore.setText(physScore    < 0 ? "N/A" : physScore   + "/100");
        tvPhysScore.setTextColor(physScore   < 0 ? 0xFF9E9E9E : getScoreColour(physScore));

        tvDietScore.setText(dietScore    < 0 ? "N/A" : dietScore   + "/100");
        tvDietScore.setTextColor(dietScore   < 0 ? 0xFF9E9E9E : getScoreColour(dietScore));

        tvMentalScore.setText(mentalScore < 0 ? "N/A" : mentalScore + "/100");
        tvMentalScore.setTextColor(mentalScore < 0 ? 0xFF9E9E9E : getScoreColour(mentalScore));

        tvGoalsScore.setText(overall + "/100");
        tvGoalsScore.setTextColor(getScoreColour(overall));
    }

    // ─── Graph ────────────────────────────────────────────────────────────────

    private void updateSelectorButtons() {
        for (int i = 0; i < metricSelectorRow.getChildCount(); i++) {
            TextView btn = (TextView) metricSelectorRow.getChildAt(i);
            boolean selected = btn.getText().toString().equals(selectedMetric);
            btn.setBackground(buildSelectorBackground(selected));
            btn.setTextColor(selected ? 0xFFFFFFFF : 0xFF9E9E9E);
        }
    }

    private android.graphics.drawable.GradientDrawable buildSelectorBackground(boolean selected) {
        android.graphics.drawable.GradientDrawable bg =
                new android.graphics.drawable.GradientDrawable();
        bg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        bg.setCornerRadius(40f);
        bg.setColor(selected ? 0xFF00B0B9 : 0xFFF5F5F5);
        return bg;
    }

    private void buildDashboardChart() {
        if (dashboardChartContainer == null) return;
        dashboardChartContainer.removeAllViews();

        List<Long> last7Days = getLast7DayStarts();
        float[] values = new float[7];

        if ("Mood".equals(selectedMetric)) {
            // Must run off main thread
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            new android.os.AsyncTask<Void, Void, float[]>() {
                @Override
                protected float[] doInBackground(Void... v) {
                    float[] result = new float[7];
                    for (int i = 0; i < 7; i++) {
                        long dayStart = last7Days.get(i);
                        long dayEnd   = dayStart + 86400000L;
                        result[i] = repo.getDailyMoodSync(uid, dayStart, dayEnd);
                    }
                    return result;
                }
                @Override
                protected void onPostExecute(float[] moodValues) {
                    if (getContext() == null) return;
                    renderChart(moodValues, last7Days);
                }
            }.execute();
            return;

        } else if ("Cal Intake".equals(selectedMetric) || "Protein".equals(selectedMetric)
                || "Carbs".equals(selectedMetric) || "Fat".equals(selectedMetric)) {
            for (int i = 0; i < 7; i++) {
                long dayStart = last7Days.get(i);
                long dayEnd   = dayStart + 86400000L;
                float total = 0;
                if (nutritionHistory != null) {
                    for (NutritionEntry e : nutritionHistory) {
                        if (e.timestamp >= dayStart && e.timestamp < dayEnd) {
                            switch (selectedMetric) {
                                case "Cal Intake": total += e.calories; break;
                                case "Protein":    total += e.protein;  break;
                                case "Carbs":      total += e.carbs;    break;
                                case "Fat":        total += e.fat;      break;
                            }
                        }
                    }
                }
                values[i] = total;
            }
        } else {
            // Physical history metrics
            if (physicalHistory == null || physicalHistory.isEmpty()) {
                showEmptyChart();
                return;
            }
            for (int i = 0; i < 7; i++) {
                long dayStart = last7Days.get(i);
                values[i] = 0;
                for (PhysicalHistoryEntry e : physicalHistory) {
                    if (e.date == dayStart) {
                        switch (selectedMetric) {
                            case "Steps":       values[i] = e.steps;           break;
                            case "Cal Burned":  values[i] = e.caloriesBurned;  break;
                            case "Sleep":       values[i] = (float) e.sleepHours; break;
                            case "Active":      values[i] = e.activeMinutes;   break;
                            case "Floors":      values[i] = e.floors;          break;
                            case "Heart Rate":  values[i] = e.heartRate;       break;
                        }
                        break;
                    }
                }
            }
        }

        renderChart(values, last7Days);
    }

    private void renderChart(float[] values, List<Long> last7Days) {
        if (dashboardChartContainer == null) return;
        dashboardChartContainer.removeAllViews();

        float maxVal = 1f;
        for (float v : values) if (v > maxVal) maxVal = v;

        java.text.SimpleDateFormat sdf =
                new java.text.SimpleDateFormat("EEE", Locale.getDefault());

        for (int i = 0; i < 7; i++) {
            float val = values[i];
            String dayLabel = sdf.format(new java.util.Date(last7Days.get(i)));

            LinearLayout col = new LinearLayout(requireContext());
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(android.view.Gravity.BOTTOM | android.view.Gravity.CENTER_HORIZONTAL);
            LinearLayout.LayoutParams colParams =
                    new LinearLayout.LayoutParams(0,
                            LinearLayout.LayoutParams.MATCH_PARENT, 1f);
            colParams.setMargins(4, 0, 4, 0);
            col.setLayoutParams(colParams);

            TextView tvVal = new TextView(requireContext());
            tvVal.setText(val > 0 ? formatValue(val) : "");
            tvVal.setTextSize(8);
            tvVal.setTextColor(0xFF9E9E9E);
            tvVal.setGravity(android.view.Gravity.CENTER);
            col.addView(tvVal);

            View bar = new View(requireContext());
            int barHeight = val > 0 ? (int)((val / maxVal) * 140) : 4;
            LinearLayout.LayoutParams barParams =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, barHeight);
            barParams.setMargins(6, 4, 6, 0);
            android.graphics.drawable.GradientDrawable shape =
                    new android.graphics.drawable.GradientDrawable();
            shape.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            shape.setCornerRadii(new float[]{8,8,8,8,0,0,0,0});
            shape.setColor(val > 0 ? 0xFF00B0B9 : 0xFFE0E0E0);
            bar.setBackground(shape);
            bar.setLayoutParams(barParams);
            col.addView(bar);

            TextView tvDay = new TextView(requireContext());
            tvDay.setText(dayLabel);
            tvDay.setTextSize(10);
            tvDay.setTextColor(0xFF9E9E9E);
            tvDay.setGravity(android.view.Gravity.CENTER);
            LinearLayout.LayoutParams dayParams =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
            dayParams.setMargins(0, 4, 0, 0);
            tvDay.setLayoutParams(dayParams);
            col.addView(tvDay);

            dashboardChartContainer.addView(col);
        }
    }

    private void showEmptyChart() {
        TextView tv = new TextView(requireContext());
        tv.setText("No data yet — sync Fitbit to see your trends");
        tv.setTextSize(12);
        tv.setTextColor(0xFF9E9E9E);
        tv.setGravity(android.view.Gravity.CENTER);
        tv.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));
        dashboardChartContainer.addView(tv);
    }

    private String formatValue(float val) {
        if (val >= 1000) return String.format(Locale.getDefault(), "%.0fk", val / 1000);
        if (val == (int) val) return String.valueOf((int) val);
        return String.format(Locale.getDefault(), "%.1f", val);
    }

    private List<Long> getLast7DayStarts() {
        List<Long> days = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        for (int i = 6; i >= 0; i--) {
            Calendar day = (Calendar) cal.clone();
            day.add(Calendar.DAY_OF_YEAR, -i);
            day.set(Calendar.HOUR_OF_DAY, 0);
            day.set(Calendar.MINUTE, 0);
            day.set(Calendar.SECOND, 0);
            day.set(Calendar.MILLISECOND, 0);
            days.add(day.getTimeInMillis());
        }
        return days;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private int getScoreColour(int score) {
        if (score >= 75) return 0xFF4CAF50;
        if (score >= 50) return 0xFFFF9800;
        return 0xFFF44336;
    }

    // ─── Delete account ───────────────────────────────────────────────────────

    private void showDeleteAccountDialog() {
        android.app.AlertDialog step1 = new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete Account")
                .setMessage("This will permanently delete your account and all your health data " +
                        "including journals, food logs and history. This cannot be undone.")
                .setPositiveButton("Continue", (d, w) -> showDeleteConfirmInput())
                .setNegativeButton("Cancel", null)
                .create();
        step1.show();
        step1.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setTextColor(0xFFF44336);
        step1.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(0xFF9E9E9E);
    }

    private void showDeleteConfirmInput() {
        android.widget.LinearLayout layout = new android.widget.LinearLayout(requireContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(64, 48, 64, 32);

        android.widget.TextView tvMsg = new android.widget.TextView(requireContext());
        tvMsg.setText("Type DELETE in capitals to confirm permanently deleting your account");
        tvMsg.setTextSize(14);
        tvMsg.setTextColor(0xFFF44336);
        tvMsg.setPadding(0, 0, 0, 16);
        layout.addView(tvMsg);

        android.widget.EditText etConfirm = new android.widget.EditText(requireContext());
        etConfirm.setHint("DELETE");
        etConfirm.setTextSize(16);
        etConfirm.setPadding(24, 20, 24, 20);
        etConfirm.setBackgroundColor(0xFFF0F0F0);
        etConfirm.setSingleLine(true);
        etConfirm.setTextColor(0xFF1A1A1A);
        etConfirm.setHintTextColor(0xFFBDBDBD);
        etConfirm.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(etConfirm);

        android.app.AlertDialog step2 = new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Are you sure?")
                .setView(layout)
                .setPositiveButton("Delete Forever", (d, w) -> {
                    if (!"DELETE".equals(etConfirm.getText().toString().trim())) {
                        Toast.makeText(getContext(),
                                "Type DELETE exactly to confirm",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    repo.deleteAccount(
                            () -> requireActivity().runOnUiThread(() -> {
                                Toast.makeText(getContext(),
                                        "Account deleted", Toast.LENGTH_SHORT).show();
                                FirebaseAuth.getInstance().signOut();
                                startActivity(new Intent(
                                        requireContext(), LoginActivity.class));
                                requireActivity().finish();
                            }),
                            error -> requireActivity().runOnUiThread(() ->
                                    Toast.makeText(getContext(),
                                            "Failed: " + error +
                                                    " — try logging out and back in first",
                                            Toast.LENGTH_LONG).show())
                    );
                })
                .setNegativeButton("Cancel", null)
                .create();
        step2.show();
        step2.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setTextColor(0xFFF44336);
        step2.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(0xFF9E9E9E);
    }

    // ─── Set password ─────────────────────────────────────────────────────────

    private void showSetPasswordDialog() {
        android.app.AlertDialog.Builder builder =
                new android.app.AlertDialog.Builder(requireContext());

        android.widget.LinearLayout layout = new android.widget.LinearLayout(requireContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(64, 48, 64, 32);
        layout.setBackgroundColor(0xFFFAFAFA);

        android.widget.TextView tvTitle = new android.widget.TextView(requireContext());
        tvTitle.setText("Set a password");
        tvTitle.setTextSize(22);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setTextColor(0xFF1A1A1A);
        tvTitle.setPadding(0, 0, 0, 8);
        layout.addView(tvTitle);

        android.widget.TextView tvSubtitle = new android.widget.TextView(requireContext());
        tvSubtitle.setText("This lets you log in with email and password in future");
        tvSubtitle.setTextSize(14);
        tvSubtitle.setTextColor(0xFF9E9E9E);
        tvSubtitle.setPadding(0, 0, 0, 24);
        layout.addView(tvSubtitle);

        final EditText etPassword = new EditText(requireContext());
        etPassword.setHint("New password (min 6 characters)");
        etPassword.setTextSize(16);
        etPassword.setTextColor(0xFF1A1A1A);
        etPassword.setHintTextColor(0xFFBDBDBD);
        etPassword.setPadding(32, 24, 32, 24);
        etPassword.setBackgroundColor(0xFFF0F0F0);
        etPassword.setSingleLine(true);
        etPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);

        android.widget.LinearLayout.LayoutParams inputParams =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        inputParams.setMargins(0, 0, 0, 24);
        etPassword.setLayoutParams(inputParams);
        layout.addView(etPassword);

        Button btnSave = new Button(requireContext());
        btnSave.setText("Set Password →");
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
        builder.setCancelable(true);
        android.app.AlertDialog dialog = builder.create();

        btnSave.setOnClickListener(v -> {
            String password = etPassword.getText().toString().trim();
            if (password.length() < 6) {
                etPassword.setError("Must be at least 6 characters");
                return;
            }

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null || currentUser.getEmail() == null) return;

            currentUser.linkWithCredential(
                            EmailAuthProvider.getCredential(currentUser.getEmail(), password))
                    .addOnSuccessListener(r -> {
                        dialog.dismiss();
                        Toast.makeText(getContext(),
                                "Password set! You can now log in with email too ✓",
                                Toast.LENGTH_LONG).show();
                        requireView().findViewById(R.id.btn_set_password)
                                .setVisibility(View.GONE);
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(),
                                    "Failed: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show());
        });

        dialog.show();
    }
}