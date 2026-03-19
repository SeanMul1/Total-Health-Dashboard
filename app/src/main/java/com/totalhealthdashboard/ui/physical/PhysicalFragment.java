package com.totalhealthdashboard.ui.physical;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.totalhealthdashboard.R;
import com.totalhealthdashboard.data.local.PhysicalEntry;
import com.totalhealthdashboard.repository.HealthRepository;
import java.util.Locale;

public class PhysicalFragment extends Fragment {

    private HealthRepository repo;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_physical, container, false);

        repo = HealthRepository.getInstance();
        repo.init(requireContext());

        // Active views
        TextView tvSteps      = view.findViewById(R.id.tv_steps);
        TextView tvStepsPct   = view.findViewById(R.id.tv_steps_percent);
        TextView tvDistance   = view.findViewById(R.id.tv_distance);
        TextView tvCalories   = view.findViewById(R.id.tv_calories_burned);
        TextView tvActive     = view.findViewById(R.id.tv_active_minutes);
        TextView tvHR         = view.findViewById(R.id.tv_heart_rate);
        TextView tvSleep      = view.findViewById(R.id.tv_sleep);
        TextView tvSleepScore = view.findViewById(R.id.tv_sleep_score);
        TextView tvStress     = view.findViewById(R.id.tv_stress);
        TextView tvDataSource = view.findViewById(R.id.tv_data_source);
        ProgressBar progress  = view.findViewById(R.id.progress_steps);

        // FUTURE: uncomment when fields added back to PhysicalEntry and XML
        // TextView tvFloors     = view.findViewById(R.id.tv_floors);
        // TextView tvFatBurn    = view.findViewById(R.id.tv_fat_burn);
        // TextView tvCardio     = view.findViewById(R.id.tv_cardio);
        // TextView tvPeak       = view.findViewById(R.id.tv_peak);
        // TextView tvVo2        = view.findViewById(R.id.tv_vo2max);
        // TextView tvDeep       = view.findViewById(R.id.tv_sleep_deep);
        // TextView tvRem        = view.findViewById(R.id.tv_sleep_rem);
        // TextView tvLight      = view.findViewById(R.id.tv_sleep_light);
        // TextView tvAwake      = view.findViewById(R.id.tv_sleep_awake);
        // TextView tvSpo2       = view.findViewById(R.id.tv_spo2);
        // TextView tvHrv        = view.findViewById(R.id.tv_hrv);
        // TextView tvBreathing  = view.findViewById(R.id.tv_breathing);
        // TextView tvSkinTemp   = view.findViewById(R.id.tv_skin_temp);

        repo.getFitbitData().observe(getViewLifecycleOwner(), data -> {
            if (data == null) return;

            // Activity
            int steps = data.getSteps();
            int pct = Math.min((steps * 100) / 10000, 100);
            tvSteps.setText(String.format("%,d", steps));
            progress.setProgress(pct);
            tvStepsPct.setText(pct + "% of daily goal");
            tvDistance.setText(String.format(Locale.getDefault(), "%.1f km", data.getDistanceKm()));
            tvCalories.setText(String.valueOf(data.getCaloriesBurned()));
            tvActive.setText(String.valueOf(data.getActiveMinutes()));
            // FUTURE: tvFloors.setText(String.valueOf(data.getFloorsClimbed()));

            // Heart
            tvHR.setText(data.getHeartRate() + " bpm");
            // FUTURE: tvFatBurn.setText(String.valueOf(data.getFatBurnMinutes()));
            // FUTURE: tvCardio.setText(String.valueOf(data.getCardioMinutes()));
            // FUTURE: tvPeak.setText(String.valueOf(data.getPeakMinutes()));
            // FUTURE: tvVo2.setText(String.valueOf(data.getVo2Max()));

            // Sleep
            tvSleep.setText(String.valueOf(data.getSleepHours()));
            tvSleepScore.setText(String.valueOf(data.getSleepScore()));
            // FUTURE: int sleepMins = (int)(data.getSleepHours() * 60);
            // FUTURE: tvSleep.setText(sleepMins / 60 + "h " + sleepMins % 60 + "m");
            // FUTURE: tvDeep.setText(String.valueOf(data.getSleepDeepMinutes()));
            // FUTURE: tvRem.setText(String.valueOf(data.getSleepRemMinutes()));
            // FUTURE: tvLight.setText(String.valueOf(data.getSleepLightMinutes()));
            // FUTURE: tvAwake.setText(String.valueOf(data.getSleepAwakeMinutes()));

            // Wellness
            int stress = data.getStressScore();
            tvStress.setText(stress + "/10");
            if (stress >= 7) tvStress.setTextColor(0xFF4CAF50);
            else if (stress >= 4) tvStress.setTextColor(0xFFFF9800);
            else tvStress.setTextColor(0xFFF44336);
            // FUTURE: tvSpo2.setText(String.valueOf(data.getSpo2()));
            // FUTURE: tvHrv.setText(String.valueOf(data.getHrv()));
            // FUTURE: tvBreathing.setText(String.valueOf(data.getBreathingRate()));
            // FUTURE: tvSkinTemp.setText((data.getSkinTempVariation() >= 0 ? "+" : "") +
            //         String.format(Locale.getDefault(), "%.1f", data.getSkinTempVariation()));
        });

        repo.getPhysicalEntry().observe(getViewLifecycleOwner(), entry -> {
            if (entry != null) {
                tvDataSource.setText("  ✓ MANUALLY ENTERED DATA  ");
                tvDataSource.setTextColor(0xFF4CAF50);
            } else {
                tvDataSource.setText("  FITBIT SYNTHETIC DATA — DEMO MODE  ");
                tvDataSource.setTextColor(0xFF00B0B9);
            }
        });

        view.findViewById(R.id.btn_manual_entry)
                .setOnClickListener(v -> showManualEntryDialog());

        return view;
    }

    private void showManualEntryDialog() {
        android.app.AlertDialog.Builder builder =
                new android.app.AlertDialog.Builder(requireContext());

        android.widget.ScrollView scroll = new android.widget.ScrollView(requireContext());
        android.widget.LinearLayout layout = new android.widget.LinearLayout(requireContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(64, 48, 64, 32);
        layout.setBackgroundColor(0xFFFAFAFA);
        scroll.addView(layout);

        addTitle(layout, "Enter Today's Data",
                "Use the guides to estimate if you don't have a tracker");

        // Active fields
        addSectionHeader(layout, "ACTIVITY");
        EditText etSteps  = addField(layout, "🚶 Steps", "e.g. 7500  ·  ~1,300 steps per 10 min walk", false);
        EditText etDist   = addField(layout, "📍 Distance (km)", "e.g. 5.4  ·  ~0.8km per 1,000 steps", true);
        EditText etCal    = addField(layout, "🔥 Calories Burned", "e.g. 420  ·  light day ~300, active ~600+", false);
        EditText etActive = addField(layout, "⏱️ Active Minutes", "e.g. 45  ·  WHO recommends 30 min/day", false);

        // FUTURE: floors field — uncomment when floorsClimbed added to PhysicalEntry
        // EditText etFloors = addField(layout, "🏢 Floors Climbed", "e.g. 12  ·  each floor ~3 metres", false);

        addSectionHeader(layout, "HEART");
        EditText etHR = addField(layout, "❤️ Resting Heart Rate (bpm)", "e.g. 72  ·  normal: 60–100 bpm", false);

        // FUTURE: HR zone fields — uncomment when added to PhysicalEntry
        // EditText etFatBurn = addField(layout, "🟡 Fat Burn Zone (min)", "e.g. 25  ·  light-moderate exercise", false);
        // EditText etCardio  = addField(layout, "🔴 Cardio Zone (min)", "e.g. 10  ·  vigorous exercise", false);
        // EditText etPeak    = addField(layout, "🟣 Peak Zone (min)", "e.g. 3  ·  max effort exercise", false);
        // EditText etVo2     = addField(layout, "💨 VO2 Max (ml/kg/min)", "e.g. 42  ·  average adult: 35–50", false);

        addSectionHeader(layout, "SLEEP");
        EditText etSleepH = addField(layout, "😴 Hours Slept", "e.g. 7.5  ·  recommended: 7–9 hours", true);

        // FUTURE: detailed sleep fields — uncomment when added to PhysicalEntry
        // EditText etSleepSc = addField(layout, "📊 Sleep Score (0-100)", "e.g. 78  ·  good sleep: 80+", false);
        // EditText etDeep    = addField(layout, "🔵 Deep Sleep (min)", "e.g. 62  ·  aim for 60–90 min", false);
        // EditText etRem     = addField(layout, "🟣 REM Sleep (min)", "e.g. 94  ·  aim for 90–120 min", false);
        // EditText etLight   = addField(layout, "💙 Light Sleep (min)", "e.g. 212  ·  largest sleep stage", false);
        // EditText etAwake   = addField(layout, "⚪ Awake (min)", "e.g. 18  ·  normal: under 30 min", false);

        // Sleep quality slider (1-10)
        addSectionHeader(layout, "WELLNESS");
        android.widget.TextView tvSleepLabel = new android.widget.TextView(requireContext());
        tvSleepLabel.setText("⭐ Sleep Quality (1–10)");
        tvSleepLabel.setTextSize(13);
        tvSleepLabel.setTextColor(0xFF666666);
        tvSleepLabel.setPadding(0, 0, 0, 4);
        layout.addView(tvSleepLabel);

        final int[] sleepScore = {7};
        android.widget.TextView tvSleepVal = new android.widget.TextView(requireContext());
        tvSleepVal.setText("7 / 10  —  Good");
        tvSleepVal.setTextSize(14);
        tvSleepVal.setTypeface(null, android.graphics.Typeface.BOLD);
        tvSleepVal.setTextColor(0xFF2196F3);
        tvSleepVal.setPadding(0, 0, 0, 8);
        layout.addView(tvSleepVal);

        SeekBar sbSleep = new SeekBar(requireContext());
        sbSleep.setMax(9);
        sbSleep.setProgress(6);
        sbSleep.setProgressTintList(android.content.res.ColorStateList.valueOf(0xFF2196F3));
        sbSleep.setThumbTintList(android.content.res.ColorStateList.valueOf(0xFF2196F3));
        android.widget.LinearLayout.LayoutParams sbParams =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        sbParams.setMargins(0, 0, 0, 24);
        sbSleep.setLayoutParams(sbParams);
        sbSleep.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean u) {
                sleepScore[0] = p + 1;
                tvSleepVal.setText(sleepScore[0] + " / 10  —  " + getSleepLabel(sleepScore[0]));
            }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });
        layout.addView(sbSleep);

        // Stress slider (1-10)
        android.widget.TextView tvStressLabel = new android.widget.TextView(requireContext());
        tvStressLabel.setText("🧘 Stress Level (1 = very stressed · 10 = very calm)");
        tvStressLabel.setTextSize(13);
        tvStressLabel.setTextColor(0xFF666666);
        tvStressLabel.setPadding(0, 0, 0, 4);
        layout.addView(tvStressLabel);

        final int[] stressScore = {5};
        android.widget.TextView tvStressVal = new android.widget.TextView(requireContext());
        tvStressVal.setText("5 / 10  —  Moderate");
        tvStressVal.setTextSize(14);
        tvStressVal.setTypeface(null, android.graphics.Typeface.BOLD);
        tvStressVal.setTextColor(0xFF4CAF50);
        tvStressVal.setPadding(0, 0, 0, 8);
        layout.addView(tvStressVal);

        SeekBar sbStress = new SeekBar(requireContext());
        sbStress.setMax(9);
        sbStress.setProgress(4);
        sbStress.setProgressTintList(android.content.res.ColorStateList.valueOf(0xFF4CAF50));
        sbStress.setThumbTintList(android.content.res.ColorStateList.valueOf(0xFF4CAF50));
        android.widget.LinearLayout.LayoutParams sbStressParams =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        sbStressParams.setMargins(0, 0, 0, 24);
        sbStress.setLayoutParams(sbStressParams);
        sbStress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean u) {
                stressScore[0] = p + 1;
                tvStressVal.setText(stressScore[0] + " / 10  —  " + getStressLabel(stressScore[0]));
            }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });
        layout.addView(sbStress);

        // FUTURE: wellness fields — uncomment when added to PhysicalEntry
        // EditText etSpo2     = addField(layout, "🫁 SpO2 — Blood Oxygen (%)", "e.g. 97  ·  normal: 95–100%", false);
        // EditText etHrv      = addField(layout, "💓 HRV (ms)", "e.g. 45  ·  higher = better recovery", false);
        // EditText etBreath   = addField(layout, "🌬️ Breathing Rate (br/min)", "e.g. 15  ·  normal: 12–20 br/min", false);
        // EditText etSkinTemp = addField(layout, "🌡️ Skin Temp Variation (°C)", "e.g. 0.2  ·  normal: -0.5 to +0.5", true);

        // Pre-fill with saved values
        repo.getPhysicalEntry().observe(getViewLifecycleOwner(), entry -> {
            if (entry != null) {
                etSteps.setText(String.valueOf(entry.steps));
                etDist.setText(String.valueOf(entry.distanceKm));
                etCal.setText(String.valueOf(entry.caloriesBurned));
                etActive.setText(String.valueOf(entry.activeMinutes));
                etHR.setText(String.valueOf(entry.heartRate));
                etSleepH.setText(String.valueOf(entry.sleepHours));
                sbSleep.setProgress(entry.sleepScore - 1);
                sbStress.setProgress(entry.stressScore - 1);
                // FUTURE: etFloors.setText(String.valueOf(entry.floorsClimbed));
                // FUTURE: etFatBurn.setText(String.valueOf(entry.fatBurnMinutes));
                // FUTURE: etCardio.setText(String.valueOf(entry.cardioMinutes));
                // FUTURE: etPeak.setText(String.valueOf(entry.peakMinutes));
                // FUTURE: etVo2.setText(String.valueOf(entry.vo2Max));
                // FUTURE: etSleepSc.setText(String.valueOf(entry.sleepScore));
                // FUTURE: etDeep.setText(String.valueOf(entry.sleepDeepMinutes));
                // FUTURE: etRem.setText(String.valueOf(entry.sleepRemMinutes));
                // FUTURE: etLight.setText(String.valueOf(entry.sleepLightMinutes));
                // FUTURE: etAwake.setText(String.valueOf(entry.sleepAwakeMinutes));
                // FUTURE: etSpo2.setText(String.valueOf(entry.spo2));
                // FUTURE: etHrv.setText(String.valueOf(entry.hrv));
                // FUTURE: etBreath.setText(String.valueOf(entry.breathingRate));
                // FUTURE: etSkinTemp.setText(String.valueOf(entry.skinTempVariation));
            }
        });

        // Save button
        Button btnSave = new Button(requireContext());
        btnSave.setText("Save Data →");
        btnSave.setTextSize(16);
        btnSave.setTypeface(null, android.graphics.Typeface.BOLD);
        btnSave.setTextColor(0xFFFFFFFF);
        btnSave.setBackgroundColor(0xFF1A1A1A);
        btnSave.setPadding(0, 32, 0, 32);
        android.widget.LinearLayout.LayoutParams btnParams =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        btnParams.setMargins(0, 16, 0, 0);
        btnSave.setLayoutParams(btnParams);
        layout.addView(btnSave);

        builder.setView(scroll);
        builder.setCancelable(true);
        android.app.AlertDialog dialog = builder.create();

        btnSave.setOnClickListener(v -> {
            PhysicalEntry entry = new PhysicalEntry();
            entry.steps          = parseInt(etSteps, 0);
            entry.distanceKm     = parseDouble(etDist, 0);
            entry.caloriesBurned = parseInt(etCal, 0);
            entry.activeMinutes  = parseInt(etActive, 0);
            entry.heartRate      = parseInt(etHR, 0);
            entry.sleepHours     = parseDouble(etSleepH, 0);
            entry.sleepScore     = sleepScore[0];
            entry.stressScore    = stressScore[0];
            entry.timestamp      = System.currentTimeMillis();

            // FUTURE: uncomment when fields added back to PhysicalEntry
            // entry.floorsClimbed   = parseInt(etFloors, 0);
            // entry.fatBurnMinutes  = parseInt(etFatBurn, 0);
            // entry.cardioMinutes   = parseInt(etCardio, 0);
            // entry.peakMinutes     = parseInt(etPeak, 0);
            // entry.vo2Max          = parseInt(etVo2, 0);
            // entry.sleepDeepMinutes   = parseInt(etDeep, 0);
            // entry.sleepRemMinutes    = parseInt(etRem, 0);
            // entry.sleepLightMinutes  = parseInt(etLight, 0);
            // entry.sleepAwakeMinutes  = parseInt(etAwake, 0);
            // entry.spo2            = parseInt(etSpo2, 0);
            // entry.hrv             = parseInt(etHrv, 0);
            // entry.breathingRate   = parseInt(etBreath, 0);
            // entry.skinTempVariation = parseDouble(etSkinTemp, 0);

            repo.saveManualPhysicalData(entry);
            dialog.dismiss();
            Toast.makeText(getContext(), "Data saved ✓", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    private void addTitle(android.widget.LinearLayout parent, String title, String subtitle) {
        android.widget.TextView tv = new android.widget.TextView(requireContext());
        tv.setText(title);
        tv.setTextSize(22);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setTextColor(0xFF1A1A1A);
        tv.setPadding(0, 0, 0, 4);
        parent.addView(tv);

        android.widget.TextView ts = new android.widget.TextView(requireContext());
        ts.setText(subtitle);
        ts.setTextSize(13);
        ts.setTextColor(0xFF9E9E9E);
        ts.setPadding(0, 0, 0, 20);
        parent.addView(ts);
    }

    private void addSectionHeader(android.widget.LinearLayout parent, String label) {
        android.widget.TextView tv = new android.widget.TextView(requireContext());
        tv.setText(label);
        tv.setTextSize(11);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setTextColor(0xFF9E9E9E);
        tv.setLetterSpacing(0.1f);
        tv.setPadding(0, 16, 0, 8);
        parent.addView(tv);
    }

    private EditText addField(android.widget.LinearLayout parent,
                              String label, String hint, boolean isDecimal) {
        android.widget.TextView tv = new android.widget.TextView(requireContext());
        tv.setText(label);
        tv.setTextSize(13);
        tv.setTextColor(0xFF666666);
        tv.setPadding(0, 0, 0, 4);
        parent.addView(tv);

        EditText et = new EditText(requireContext());
        et.setHint(hint);
        et.setTextSize(14);
        et.setTextColor(0xFF1A1A1A);
        et.setHintTextColor(0xFFBDBDBD);
        et.setPadding(20, 16, 20, 16);
        et.setBackgroundColor(0xFFF0F0F0);
        et.setSingleLine(true);
        et.setInputType(isDecimal
                ? android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
                : android.text.InputType.TYPE_CLASS_NUMBER);

        android.widget.LinearLayout.LayoutParams p =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 0, 0, 12);
        et.setLayoutParams(p);
        parent.addView(et);
        return et;
    }

    private String getSleepLabel(int score) {
        if (score <= 3) return "Poor";
        if (score <= 5) return "Fair";
        if (score <= 7) return "Good";
        if (score <= 9) return "Very good";
        return "Excellent";
    }

    private String getStressLabel(int score) {
        if (score <= 2) return "Very stressed";
        if (score <= 4) return "Stressed";
        if (score <= 6) return "Moderate";
        if (score <= 8) return "Calm";
        return "Very calm";
    }

    private int parseInt(EditText et, int fallback) {
        try { return Integer.parseInt(et.getText().toString().trim()); }
        catch (Exception e) { return fallback; }
    }

    private double parseDouble(EditText et, double fallback) {
        try { return Double.parseDouble(et.getText().toString().trim()); }
        catch (Exception e) { return fallback; }
    }
}