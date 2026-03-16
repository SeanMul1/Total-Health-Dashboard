package com.totalhealthdashboard.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.totalhealthdashboard.R;
import com.totalhealthdashboard.repository.HealthRepository;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        // Physical card views
        TextView tvStepsDash    = view.findViewById(R.id.tv_dash_steps);
        TextView tvPhysScore    = view.findViewById(R.id.tv_dash_physical_score);

        // Diet card views
        TextView tvCalDash      = view.findViewById(R.id.tv_dash_calories);
        TextView tvDietScore    = view.findViewById(R.id.tv_dash_diet_score);

        // Mental card views
        TextView tvMoodDash     = view.findViewById(R.id.tv_dash_mood);
        TextView tvMentalScore  = view.findViewById(R.id.tv_dash_mental_score);

        HealthRepository repo = HealthRepository.getInstance();
        repo.init(requireContext());

        // Physical — Fitbit synthetic data
        repo.getFitbitData().observe(getViewLifecycleOwner(), data -> {
            if (data == null) return;
            tvStepsDash.setText(String.format(Locale.getDefault(), "%,d steps today", data.getSteps()));
            int score = Math.min((data.getSteps() * 100) / 10000, 100);
            tvPhysScore.setText(score + "/100");
            tvPhysScore.setTextColor(getScoreColour(score));
        });

        // Mental — journal mood average
        repo.getAverageMoodThisWeek().observe(getViewLifecycleOwner(), avg -> {
            if (avg == null || avg == 0) {
                tvMoodDash.setText("No entries yet this week");
                tvMentalScore.setText("—/100");
                return;
            }
            int score = Math.round(avg * 10);
            tvMoodDash.setText(String.format(Locale.getDefault(), "Avg mood: %.1f / 10", avg));
            tvMentalScore.setText(score + "/100");
            tvMentalScore.setTextColor(getScoreColour(score));
        });

        // Navigation
        BottomNavigationView nav = requireActivity().findViewById(R.id.bottom_navigation);

        view.findViewById(R.id.card_physical).setOnClickListener(v ->
            nav.setSelectedItemId(R.id.nav_physical));
        view.findViewById(R.id.card_diet).setOnClickListener(v ->
            nav.setSelectedItemId(R.id.nav_diet));
        view.findViewById(R.id.card_mental).setOnClickListener(v ->
            nav.setSelectedItemId(R.id.nav_mental));

        return view;
    }

    private int getScoreColour(int score) {
        if (score >= 75) return 0xFF4CAF50; // green
        if (score >= 50) return 0xFFFF9800; // orange
        return 0xFFF44336;                  // red
    }
}