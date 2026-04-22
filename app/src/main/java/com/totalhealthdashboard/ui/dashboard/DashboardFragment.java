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
import com.totalhealthdashboard.repository.ScoreCalculator;
import com.totalhealthdashboard.ui.LoginActivity;
import com.totalhealthdashboard.ui.MainActivity;
import com.totalhealthdashboard.ui.history.HistoryFragment;
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

        repo.getUserGoals().observe(getViewLifecycleOwner(), goals -> {
            latestGoals = goals;
            recalculateScores();
        });

        repo.getFitbitData().observe(getViewLifecycleOwner(), data -> {
            latestPhysical = data;
            if (data != null) {
                tvStepsDash.setText(data.getSteps() == 0
                        ? "Enter your physical data"
                        : String.format(Locale.getDefault(), "%,d steps today", data.getSteps()));
            }
            recalculateScores();
        });

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

        view.findViewById(R.id.btn_logout).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(requireContext(), LoginActivity.class));
            requireActivity().finish();
        });

        return view;
    }

    // ─── Score calculation ────────────────────────────────────────────────────

    private void recalculateScores() {
        UserGoals g = latestGoals != null ? latestGoals : new UserGoals();

        int physScore   = ScoreCalculator.calcPhysicalScore(latestPhysical, g);
        int dietScore   = ScoreCalculator.calcDietScore(
                latestCalories  != null ? latestCalories  : 0,
                latestProtein   != null ? latestProtein   : 0,
                latestCarbs     != null ? latestCarbs     : 0,
                latestFat       != null ? latestFat       : 0, g);
        int mentalScore = ScoreCalculator.calcMentalScore(
                latestMood          != null ? latestMood          : 0,
                latestJournalDays   != null ? latestJournalDays   : 0, g);

        int overall = ScoreCalculator.calcOverallScore(physScore, dietScore, mentalScore);

        // Overall hero card
        tvOverallScore.setText(String.valueOf(overall));
        tvOverallScore.setTextColor(getScoreColour(overall));

        // Sub-scores in hero card
        tvSubPhysical.setText(physScore  < 0 ? "—" : String.valueOf(physScore));
        tvSubDiet.setText(dietScore      < 0 ? "—" : String.valueOf(dietScore));
        tvSubMental.setText(mentalScore  < 0 ? "—" : String.valueOf(mentalScore));

        // Category card scores
        tvPhysScore.setText(physScore    < 0 ? "N/A" : physScore   + "/100");
        tvPhysScore.setTextColor(physScore < 0 ? 0xFF9E9E9E : getScoreColour(physScore));

        tvDietScore.setText(dietScore    < 0 ? "N/A" : dietScore   + "/100");
        tvDietScore.setTextColor(dietScore < 0 ? 0xFF9E9E9E : getScoreColour(dietScore));

        tvMentalScore.setText(mentalScore < 0 ? "N/A" : mentalScore + "/100");
        tvMentalScore.setTextColor(mentalScore < 0 ? 0xFF9E9E9E : getScoreColour(mentalScore));

        tvGoalsScore.setText(overall + "/100");
        tvGoalsScore.setTextColor(getScoreColour(overall));
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