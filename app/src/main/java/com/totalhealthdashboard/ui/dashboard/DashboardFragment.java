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
import com.totalhealthdashboard.repository.HealthRepository;
import com.totalhealthdashboard.ui.LoginActivity;
import java.util.Locale;

public class DashboardFragment extends Fragment {

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

        // Physical card views
        TextView tvStepsDash  = view.findViewById(R.id.tv_dash_steps);
        TextView tvPhysScore  = view.findViewById(R.id.tv_dash_physical_score);

        // Diet card views
        TextView tvCalDash    = view.findViewById(R.id.tv_dash_calories);
        TextView tvDietScore  = view.findViewById(R.id.tv_dash_diet_score);

        // Mental card views
        TextView tvMoodDash    = view.findViewById(R.id.tv_dash_mood);
        TextView tvMentalScore = view.findViewById(R.id.tv_dash_mental_score);

        HealthRepository repo = HealthRepository.getInstance();
        repo.init(requireContext());

        // Physical — Fitbit synthetic data
        repo.getFitbitData().observe(getViewLifecycleOwner(), data -> {
            if (data == null) return;
            tvStepsDash.setText(String.format(Locale.getDefault(),
                    "%,d steps today", data.getSteps()));
            int score = Math.min((data.getSteps() * 100) / 10000, 100);
            tvPhysScore.setText(score + "/100");
            tvPhysScore.setTextColor(getScoreColour(score));
        });

        // Diet — real calories from database
        repo.getTotalCaloriesToday().observe(getViewLifecycleOwner(), total -> {
            if (total == null || total == 0) {
                tvCalDash.setText("Log a food to see data");
                tvDietScore.setText("—/100");
                return;
            }
            tvCalDash.setText(total + " kcal logged today");
            int score = Math.min((total * 100) / 2000, 100);
            tvDietScore.setText(score + "/100");
            tvDietScore.setTextColor(getScoreColour(score));
        });

        // Mental — journal mood average
        repo.getAverageMoodThisWeek().observe(getViewLifecycleOwner(), avg -> {
            if (avg == null || avg == 0) {
                tvMoodDash.setText("Write a journal entry");
                tvMentalScore.setText("—/100");
                return;
            }
            int score = Math.round(avg * 10);
            tvMoodDash.setText(String.format(Locale.getDefault(),
                    "Avg mood: %.1f / 10", avg));
            tvMentalScore.setText(score + "/100");
            tvMentalScore.setTextColor(getScoreColour(score));
        });

        // Card navigation
        BottomNavigationView nav = requireActivity().findViewById(R.id.bottom_navigation);
        view.findViewById(R.id.card_physical).setOnClickListener(v ->
                nav.setSelectedItemId(R.id.nav_physical));
        view.findViewById(R.id.card_diet).setOnClickListener(v ->
                nav.setSelectedItemId(R.id.nav_diet));
        view.findViewById(R.id.card_mental).setOnClickListener(v ->
                nav.setSelectedItemId(R.id.nav_mental));

        // Show "Set password" option for Google-only users
        TextView btnSetPassword = view.findViewById(R.id.btn_set_password);
        if (user != null) {
            boolean isGoogleOnly = user.getProviderData().stream()
                    .anyMatch(info -> info.getProviderId().equals("google.com"))
                    && user.getProviderData().stream()
                    .noneMatch(info -> info.getProviderId().equals("password"));

            if (isGoogleOnly) {
                btnSetPassword.setVisibility(View.VISIBLE);
            }
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

    private int getScoreColour(int score) {
        if (score >= 75) return 0xFF4CAF50;
        if (score >= 50) return 0xFFFF9800;
        return 0xFFF44336;
    }
}