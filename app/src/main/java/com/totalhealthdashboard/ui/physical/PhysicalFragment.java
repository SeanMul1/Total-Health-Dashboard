package com.totalhealthdashboard.ui.physical;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.totalhealthdashboard.R;
import com.totalhealthdashboard.repository.HealthRepository;

public class PhysicalFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_physical, container, false);

        TextView tvSteps        = view.findViewById(R.id.tv_steps);
        TextView tvStepsPct     = view.findViewById(R.id.tv_steps_percent);
        TextView tvHeartRate    = view.findViewById(R.id.tv_heart_rate);
        TextView tvCalories     = view.findViewById(R.id.tv_calories_burned);
        TextView tvActive       = view.findViewById(R.id.tv_active_minutes);
        TextView tvSleep        = view.findViewById(R.id.tv_sleep);
        ProgressBar progressSteps = view.findViewById(R.id.progress_steps);

        // Load Fitbit synthetic data from repository
        HealthRepository.getInstance().getFitbitData()
            .observe(getViewLifecycleOwner(), data -> {
                if (data == null) return;

                // Steps + progress
                int steps = data.getSteps();
                int goalSteps = 10000;
                int pct = Math.min((steps * 100) / goalSteps, 100);
                tvSteps.setText(String.format("%,d", steps));
                progressSteps.setProgress(pct);
                tvStepsPct.setText(pct + "% of daily goal");

                // Other metrics
                tvHeartRate.setText(String.valueOf(data.getHeartRate()));
                tvCalories.setText(String.valueOf(data.getCaloriesBurned()));
                tvActive.setText(String.valueOf(data.getActiveMinutes()));
                tvSleep.setText(String.valueOf(data.getSleepHours()));
            });

        return view;
    }
}