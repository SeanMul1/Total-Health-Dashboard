package com.totalhealthdashboard.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
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
import com.totalhealthdashboard.data.local.UserGoals;
import com.totalhealthdashboard.data.models.FitbitData;
import com.totalhealthdashboard.repository.HealthRepository;
import com.totalhealthdashboard.ui.LoginActivity;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    private HealthRepository repo;

    // Cached latest values so we can recalculate score whenever any changes
    private FitbitData latestPhysical = null;
    private Integer latestCalories    = null;
    private Double  latestProtein     = null;
    private Double  latestCarbs       = null;
    private Double  latestFat         = null;
    private Float   latestMood        = null;
    private Integer latestJournalDays = null;
    private UserGoals latestGoals     = null;

    // Score views
    private TextView tvOverallScore, tvSubPhysical, tvSubDiet, tvSubMental;
    private TextView tvPhysScore, tvDietScore, tvMentalScore, tvGoalsScore;

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

        // Subtitle views
        TextView tvStepsDash = view.findViewById(R.id.tv_dash_steps);
        TextView tvCalDash   = view.findViewById(R.id.tv_dash_calories);
        TextView tvMoodDash  = view.findViewById(R.id.tv_dash_mood);
        TextView tvGoalsDash = view.findViewById(R.id.tv_dash_goals);

        repo = HealthRepository.getInstance();
        repo.init(requireContext());

        // Observe goals first — needed for score calculations
        repo.getUserGoals().observe(getViewLifecycleOwner(), goals -> {
            latestGoals = goals;
            recalculateScores();
        });

        // Physical data
        repo.getFitbitData().observe(getViewLifecycleOwner(), data -> {
            latestPhysical = data;
            if (data != null) {
                tvStepsDash.setText(data.getSteps() == 0
                        ? "Enter your physical data"
                        : String.format(Locale.getDefault(), "%,d steps today", data.getSteps()));
            }
            recalculateScores();
        });

        // Diet data
        repo.getTotalCaloriesToday().observe(getViewLifecycleOwner(), total -> {
            latestCalories = total;
            tvCalDash.setText((total == null || total == 0)
                    ? "Log a food to see data"
                    : total + " kcal logged today");
            recalculateScores();
        });

        repo.getTotalProtein().observe(getViewLifecycleOwner(), p -> {
            latestProtein = p;
            recalculateScores();
        });

        repo.getTotalCarbs().observe(getViewLifecycleOwner(), c -> {
            latestCarbs = c;
            recalculateScores();
        });

        repo.getTotalFat().observe(getViewLifecycleOwner(), f -> {
            latestFat = f;
            recalculateScores();
        });

        // Mental data
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

        // Goals card subtitle — show how many goals are being hit
        repo.getUserGoals().observe(getViewLifecycleOwner(), goals ->
                tvGoalsDash.setText(goals == null
                        ? "Set your goals"
                        : "Tap to view and edit your targets"));

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

        return view;
    }

    // ─── Score calculation ────────────────────────────────────────────────────

    private void recalculateScores() {
        // Use default goals if none saved yet
        UserGoals g = latestGoals != null ? latestGoals : new UserGoals();

        int physScore   = calcPhysicalScore(g);
        int dietScore   = calcDietScore(g);
        int mentalScore = calcMentalScore(g);

        // Overall = average of the three categories
        int overall = (physScore + dietScore + mentalScore) / 3;

        // Update overall hero card
        tvOverallScore.setText(String.valueOf(overall));
        tvOverallScore.setTextColor(getScoreColour(overall));

        // Update sub-scores in hero card
        tvSubPhysical.setText(physScore + "");
        tvSubDiet.setText(dietScore + "");
        tvSubMental.setText(mentalScore + "");

        // Update category card scores
        tvPhysScore.setText(physScore + "/100");
        tvPhysScore.setTextColor(getScoreColour(physScore));

        tvDietScore.setText(dietScore + "/100");
        tvDietScore.setTextColor(getScoreColour(dietScore));

        tvMentalScore.setText(mentalScore + "/100");
        tvMentalScore.setTextColor(getScoreColour(mentalScore));

        tvGoalsScore.setText(overall + "/100");
        tvGoalsScore.setTextColor(getScoreColour(overall));
    }

    private int calcPhysicalScore(UserGoals g) {
        if (latestPhysical == null) return 0;

        int total = 0;
        int count = 0;

        // Steps
        if (g.stepsEnabled && g.stepsGoal > 0) {
            total += Math.min((latestPhysical.getSteps() * 100) / g.stepsGoal, 100);
            count++;
        }
        // Active minutes
        if (g.activeMinutesEnabled && g.activeMinutesGoal > 0) {
            total += Math.min((latestPhysical.getActiveMinutes() * 100) / g.activeMinutesGoal, 100);
            count++;
        }
        // Sleep hours
        if (g.sleepHoursEnabled && g.sleepHoursGoal > 0) {
            total += (int) Math.min((latestPhysical.getSleepHours() / g.sleepHoursGoal) * 100, 100);
            count++;
        }
        // Sleep quality
        if (g.sleepScoreEnabled && g.sleepScoreGoal > 0) {
            total += Math.min((latestPhysical.getSleepScore() * 100) / g.sleepScoreGoal, 100);
            count++;
        }
        // Heart rate — lower is better, score is 100 if at or below goal
        if (g.heartRateEnabled && g.heartRateGoal > 0 && latestPhysical.getHeartRate() > 0) {
            int hrScore = latestPhysical.getHeartRate() <= g.heartRateGoal ? 100
                    : Math.max(0, 100 - (latestPhysical.getHeartRate() - g.heartRateGoal) * 5);
            total += hrScore;
            count++;
        }

        return count == 0 ? 0 : total / count;
    }

    private int calcDietScore(UserGoals g) {
        int total = 0;
        int count = 0;

        int cal   = latestCalories  != null ? latestCalories  : 0;
        double p  = latestProtein   != null ? latestProtein   : 0;
        double c  = latestCarbs     != null ? latestCarbs     : 0;
        double f  = latestFat       != null ? latestFat       : 0;

        // Calories — score 100 when within 10% of goal, penalise over/under
        if (g.caloriesEnabled && g.caloriesGoal > 0) {
            double ratio = (double) cal / g.caloriesGoal;
            int calScore;
            if (ratio >= 0.9 && ratio <= 1.1) calScore = 100;
            else if (ratio < 0.9) calScore = (int)(ratio / 0.9 * 100);
            else calScore = Math.max(0, (int)(100 - (ratio - 1.1) * 200));
            total += calScore;
            count++;
        }
        // Protein
        if (g.proteinEnabled && g.proteinGoal > 0) {
            total += (int) Math.min((p / g.proteinGoal) * 100, 100);
            count++;
        }
        // Carbs
        if (g.carbsEnabled && g.carbsGoal > 0) {
            double ratio = c / g.carbsGoal;
            int carbScore = ratio >= 0.9 && ratio <= 1.1 ? 100
                    : ratio < 0.9 ? (int)(ratio / 0.9 * 100)
                    : Math.max(0, (int)(100 - (ratio - 1.1) * 200));
            total += carbScore;
            count++;
        }
        // Fat
        if (g.fatEnabled && g.fatGoal > 0) {
            double ratio = f / g.fatGoal;
            int fatScore = ratio >= 0.9 && ratio <= 1.1 ? 100
                    : ratio < 0.9 ? (int)(ratio / 0.9 * 100)
                    : Math.max(0, (int)(100 - (ratio - 1.1) * 200));
            total += fatScore;
            count++;
        }

        return count == 0 ? 0 : total / count;
    }

    private int calcMentalScore(UserGoals g) {
        int total = 0;
        int count = 0;

        // Mood score
        if (g.moodEnabled && g.moodGoal > 0 && latestMood != null && latestMood > 0) {
            total += (int) Math.min((latestMood / g.moodGoal) * 100, 100);
            count++;
        }
        // Journal days
        if (g.journalDaysEnabled && g.journalDaysGoal > 0) {
            int days = latestJournalDays != null ? latestJournalDays : 0;
            total += Math.min((days * 100) / g.journalDaysGoal, 100);
            count++;
        }

        return count == 0 ? 0 : total / count;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private int getScoreColour(int score) {
        if (score >= 75) return 0xFF4CAF50;
        if (score >= 50) return 0xFFFF9800;
        return 0xFFF44336;
    }

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