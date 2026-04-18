package com.totalhealthdashboard.ui.physical;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
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
    private FitbitAuthManager authManager;
    private Handler autoRefreshHandler;
    private Runnable autoRefreshRunnable;
    private static final long REFRESH_INTERVAL_MS = 120_000;

    private TextView tvSteps, tvDistance, tvCalories, tvActive,
            tvHR, tvFloors, tvSleep,
            tvSleepScore, tvStress, tvDataSource;
    private Button btnFitbit;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_physical, container, false);

        repo        = HealthRepository.getInstance();
        repo.init(requireContext());
        authManager = new FitbitAuthManager(requireContext());

        tvSteps      = view.findViewById(R.id.tv_steps);
        tvDistance   = view.findViewById(R.id.tv_distance);
        tvCalories   = view.findViewById(R.id.tv_calories_burned);
        tvActive     = view.findViewById(R.id.tv_active_minutes);
        tvHR         = view.findViewById(R.id.tv_heart_rate);
        tvFloors     = view.findViewById(R.id.tv_floors);
        tvSleep      = view.findViewById(R.id.tv_sleep);
        tvSleepScore = view.findViewById(R.id.tv_sleep_score);
        tvStress     = view.findViewById(R.id.tv_stress);
        tvDataSource = view.findViewById(R.id.tv_data_source);
        btnFitbit    = view.findViewById(R.id.btn_fitbit_sync);

        repo.getFitbitData().observe(getViewLifecycleOwner(), data -> {
            if (data == null) return;
            updateUI(data);
        });

        repo.getPhysicalEntry().observe(getViewLifecycleOwner(), entry -> {
            if (entry != null) {
                if (authManager.isConnected()) {
                    tvDataSource.setText("  ✓ FITBIT SYNCED  ");
                    tvDataSource.setTextColor(0xFF00B0B9);
                } else {
                    tvDataSource.setText("  ✓ MANUALLY ENTERED DATA  ");
                    tvDataSource.setTextColor(0xFF4CAF50);
                }
            } else {
                tvDataSource.setText("  NO DATA ENTERED YET  ");
                tvDataSource.setTextColor(0xFF9E9E9E);
            }
        });

        // Fitbit button
        btnFitbit.setText(authManager.isConnected() ? "Sync Fitbit Data" : "Connect Fitbit");
        btnFitbit.setOnClickListener(v -> {
            if (authManager.isConnected()) {
                android.app.AlertDialog alertDialog =
                        new android.app.AlertDialog.Builder(requireContext())
                                .setTitle("Fitbit Connected")
                                .setMessage("Tap Sync to refresh your data or Disconnect to unlink.")
                                .setPositiveButton("Sync Now", (d, w) -> {
                                    if (authManager.isTokenExpired()) {
                                        Intent intent = new Intent(Intent.ACTION_VIEW,
                                                android.net.Uri.parse(authManager.getAuthUrl()));
                                        startActivity(intent);
                                    } else {
                                        String token = authManager.getAccessToken();
                                        if (token != null) {
                                            Toast.makeText(getContext(), "Syncing...", Toast.LENGTH_SHORT).show();
                                            repo.syncFitbitInBackground(token, () ->
                                                    requireActivity().runOnUiThread(() ->
                                                            Toast.makeText(getContext(), "Synced!", Toast.LENGTH_SHORT).show()
                                                    )
                                            );
                                        }
                                    }
                                })
                                .setNegativeButton("Disconnect", (d, w) -> {
                                    authManager.disconnect();
                                    stopAutoRefresh();
                                    btnFitbit.setText("Connect Fitbit");
                                    tvDataSource.setText("  NO DATA ENTERED YET  ");
                                    tvDataSource.setTextColor(0xFF9E9E9E);
                                    Toast.makeText(getContext(),
                                            "Fitbit disconnected", Toast.LENGTH_SHORT).show();
                                })
                                .setNeutralButton("Cancel", null)
                                .create();
                alertDialog.show();
                alertDialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
                        .setTextColor(0xFF00B0B9);
                alertDialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)
                        .setTextColor(0xFFF44336);
                alertDialog.getButton(android.app.AlertDialog.BUTTON_NEUTRAL)
                        .setTextColor(0xFF9E9E9E);
            } else {
                Intent intent = new Intent(Intent.ACTION_VIEW,
                        android.net.Uri.parse(authManager.getAuthUrl()));
                startActivity(intent);
            }
        });

        // ── Health Connect — commented out pending registration fix ──────────
        // healthConnectLauncher = registerForActivityResult(
        //         androidx.health.connect.client.PermissionController
        //                 .createRequestPermissionResultContract(),
        //         granted -> {
        //             if (granted.containsAll(HealthConnectHelper.PERMISSIONS)) {
        //                 Toast.makeText(getContext(),
        //                         "Health Connect connected! Reading data...",
        //                         Toast.LENGTH_SHORT).show();
        //                 HealthConnectHelper.readData(requireContext(),
        //                         new HealthConnectHelper.Callback() {
        //                             @Override
        //                             public void onData(int steps, double distanceKm, int calories,
        //                                                int heartRate, double sleepHours, int floors) {
        //                                 saveHealthConnectData(steps, distanceKm, calories,
        //                                         heartRate, sleepHours, floors);
        //                             }
        //                             @Override
        //                             public void onError(String message) {
        //                                 requireActivity().runOnUiThread(() ->
        //                                         Toast.makeText(getContext(),
        //                                                 "Error: " + message, Toast.LENGTH_SHORT).show());
        //                             }
        //                             @Override public void onPermissionRequired() {}
        //                         });
        //             } else {
        //                 Toast.makeText(getContext(),
        //                         "Some permissions were denied", Toast.LENGTH_SHORT).show();
        //             }
        //         }
        // );
        //
        // view.findViewById(R.id.btn_health_connect).setOnClickListener(v -> {
        //     if (!HealthConnectHelper.isAvailable(requireContext())) {
        //         Toast.makeText(getContext(),
        //                 "Health Connect not available on this device",
        //                 Toast.LENGTH_SHORT).show();
        //         return;
        //     }
        //     HealthConnectHelper.checkPermissionsAndRead(requireContext(),
        //             new HealthConnectHelper.Callback() {
        //                 @Override
        //                 public void onData(int steps, double distanceKm, int calories,
        //                                    int heartRate, double sleepHours, int floors) {
        //                     saveHealthConnectData(steps, distanceKm, calories,
        //                             heartRate, sleepHours, floors);
        //                 }
        //                 @Override
        //                 public void onError(String message) {
        //                     requireActivity().runOnUiThread(() ->
        //                             Toast.makeText(getContext(),
        //                                     "Error: " + message, Toast.LENGTH_SHORT).show());
        //                 }
        //                 @Override
        //                 public void onPermissionRequired() {
        //                     healthConnectLauncher.launch(HealthConnectHelper.PERMISSIONS);
        //                 }
        //             });
        // });
        // ── End Health Connect ───────────────────────────────────────────────

        // Manual entry button
        view.findViewById(R.id.btn_manual_entry)
                .setOnClickListener(v -> showManualEntryDialog());

        return view;
    }

    // ─── Health Connect save — commented out ──────────────────────────────────
    // private void saveHealthConnectData(int steps, double distanceKm, int calories,
    //                                    int heartRate, double sleepHours, int floors) {
    //     repo.getPhysicalEntry().observe(getViewLifecycleOwner(), existing -> {
    //         PhysicalEntry entry      = new PhysicalEntry();
    //         entry.steps              = steps;
    //         entry.distanceKm         = distanceKm;
    //         entry.caloriesBurned     = calories;
    //         entry.heartRate          = heartRate;
    //         entry.sleepHours         = sleepHours;
    //         entry.floors             = floors;
    //         entry.activeMinutes      = existing != null ? existing.activeMinutes : 0;
    //         entry.sleepScore         = existing != null ? existing.sleepScore : 0;
    //         entry.stressScore        = existing != null ? existing.stressScore : 0;
    //         entry.overrideSteps      = existing != null && existing.overrideSteps;
    //         entry.overrideDistance   = existing != null && existing.overrideDistance;
    //         entry.overrideCalories   = existing != null && existing.overrideCalories;
    //         entry.overrideHeartRate  = existing != null && existing.overrideHeartRate;
    //         entry.overrideSleepHours = existing != null && existing.overrideSleepHours;
    //         entry.timestamp          = System.currentTimeMillis();
    //         repo.saveManualPhysicalData(entry);
    //         requireActivity().runOnUiThread(() -> {
    //             tvDataSource.setText("  ✓ HEALTH CONNECT SYNCED  ");
    //             tvDataSource.setTextColor(0xFF4CAF50);
    //             Toast.makeText(getContext(),
    //                     "Health Connect data synced!", Toast.LENGTH_SHORT).show();
    //         });
    //     });
    // }

    // ─── Auto-refresh ─────────────────────────────────────────────────────────

    @Override
    public void onResume() {
        super.onResume();
        if (authManager.isConnected()) {
            startAutoRefresh();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopAutoRefresh();
    }

    private void startAutoRefresh() {
        autoRefreshHandler  = new Handler(Looper.getMainLooper());
        autoRefreshRunnable = new Runnable() {
            @Override
            public void run() {
                triggerBackgroundSync();
                autoRefreshHandler.postDelayed(this, REFRESH_INTERVAL_MS);
            }
        };
        triggerBackgroundSync();
        autoRefreshHandler.postDelayed(autoRefreshRunnable, REFRESH_INTERVAL_MS);
    }

    private void stopAutoRefresh() {
        if (autoRefreshHandler != null && autoRefreshRunnable != null) {
            autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
        }
    }

    private void triggerBackgroundSync() {
        String token = authManager.getAccessToken();
        if (token == null) return;
        if (authManager.isTokenExpired()) {
            android.util.Log.d("FITBIT", "Token expired — skipping background sync");
            return;
        }
        repo.syncFitbitInBackground(token, null);
    }

    // ─── UI update ────────────────────────────────────────────────────────────

    private void updateUI(com.totalhealthdashboard.data.models.FitbitData data) {
        tvSteps.setText(data.getSteps() == 0 ? "—"
                : String.format("%,d", data.getSteps()));
        tvDistance.setText(data.getDistanceKm() == 0 ? "—"
                : String.format(Locale.getDefault(), "%.1f", data.getDistanceKm()));
        tvCalories.setText(data.getCaloriesBurned() == 0 ? "—"
                : String.valueOf(data.getCaloriesBurned()));
        tvActive.setText(data.getActiveMinutes() == 0 ? "—"
                : String.valueOf(data.getActiveMinutes()));
        tvFloors.setText(data.getFloors() == 0 ? "—"
                : String.valueOf(data.getFloors()));
        tvHR.setText(data.getHeartRate() == 0 ? "—"
                : data.getHeartRate() + " bpm");
        tvSleep.setText(data.getSleepHours() == 0 ? "—"
                : String.valueOf(data.getSleepHours()));
        tvSleepScore.setText(data.getSleepScore() == 0 ? "—"
                : String.valueOf(data.getSleepScore()));

        int stress = data.getStressScore();
        if (stress == 0) {
            tvStress.setText("—");
            tvStress.setTextColor(0xFF9E9E9E);
        } else {
            tvStress.setText(stress + "/10");
            if (stress >= 7) tvStress.setTextColor(0xFF4CAF50);
            else if (stress >= 4) tvStress.setTextColor(0xFFFF9800);
            else tvStress.setTextColor(0xFFF44336);
        }
    }

    // ─── Manual entry dialog ──────────────────────────────────────────────────

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
                "Toggle 'Override Fitbit' to lock a value and stop auto-sync overwriting it");

        final boolean[] overrideSteps  = {false};
        final boolean[] overrideDist   = {false};
        final boolean[] overrideCal    = {false};
        final boolean[] overrideActive = {false};
        final boolean[] overrideHR     = {false};
        final boolean[] overrideSleep  = {false};

        final Switch[] swSteps  = {null};
        final Switch[] swDist   = {null};
        final Switch[] swCal    = {null};
        final Switch[] swActive = {null};
        final Switch[] swHR     = {null};
        final Switch[] swSleep  = {null};

        addSectionHeader(layout, "ACTIVITY");
        EditText etSteps  = addFieldWithToggle(layout, "Steps",
                "e.g. 7500  ·  ~1,300 steps per 10 min walk", false, overrideSteps, swSteps);
        EditText etDist   = addFieldWithToggle(layout, "Distance (km)",
                "e.g. 5.4  ·  ~0.8km per 1,000 steps", true, overrideDist, swDist);
        EditText etCal    = addFieldWithToggle(layout, "Calories Burned",
                "e.g. 420  ·  light day ~300, active ~600+", false, overrideCal, swCal);
        EditText etActive = addFieldWithToggle(layout, "Active Minutes",
                "e.g. 45  ·  WHO recommends 30 min/day", false, overrideActive, swActive);

        addSectionHeader(layout, "HEART");
        EditText etHR = addFieldWithToggle(layout, "Resting Heart Rate (bpm)",
                "e.g. 72  ·  normal: 60-100 bpm", false, overrideHR, swHR);

        addSectionHeader(layout, "SLEEP");
        EditText etSleepH = addFieldWithToggle(layout, "Hours Slept",
                "e.g. 7.5  ·  recommended: 7-9 hours", true, overrideSleep, swSleep);

        addSectionHeader(layout, "WELLNESS");
        android.widget.TextView tvSleepLabel = new android.widget.TextView(requireContext());
        tvSleepLabel.setText("Sleep Quality (1-10)");
        tvSleepLabel.setTextSize(13);
        tvSleepLabel.setTextColor(0xFF666666);
        tvSleepLabel.setPadding(0, 0, 0, 4);
        layout.addView(tvSleepLabel);

        final int[] sleepScore = {7};
        android.widget.TextView tvSleepVal = new android.widget.TextView(requireContext());
        tvSleepVal.setText("7 / 10  -  Good");
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
                tvSleepVal.setText(sleepScore[0] + " / 10  -  " + getSleepLabel(sleepScore[0]));
            }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });
        layout.addView(sbSleep);

        android.widget.TextView tvStressLabel = new android.widget.TextView(requireContext());
        tvStressLabel.setText("Stress Level (1 = very stressed, 10 = very calm)");
        tvStressLabel.setTextSize(13);
        tvStressLabel.setTextColor(0xFF666666);
        tvStressLabel.setPadding(0, 0, 0, 4);
        layout.addView(tvStressLabel);

        final int[] stressScore = {5};
        android.widget.TextView tvStressVal = new android.widget.TextView(requireContext());
        tvStressVal.setText("5 / 10  -  Moderate");
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
                tvStressVal.setText(stressScore[0] + " / 10  -  " + getStressLabel(stressScore[0]));
            }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });
        layout.addView(sbStress);

        repo.getPhysicalEntry().observe(getViewLifecycleOwner(), entry -> {
            if (entry != null) {
                etSteps.setText(entry.steps == 0 ? "" : String.valueOf(entry.steps));
                etDist.setText(entry.distanceKm == 0 ? "" : String.valueOf(entry.distanceKm));
                etCal.setText(entry.caloriesBurned == 0 ? "" : String.valueOf(entry.caloriesBurned));
                etActive.setText(entry.activeMinutes == 0 ? "" : String.valueOf(entry.activeMinutes));
                etHR.setText(entry.heartRate == 0 ? "" : String.valueOf(entry.heartRate));
                etSleepH.setText(entry.sleepHours == 0 ? "" : String.valueOf(entry.sleepHours));
                if (entry.sleepScore > 0) sbSleep.setProgress(entry.sleepScore - 1);
                if (entry.stressScore > 0) sbStress.setProgress(entry.stressScore - 1);

                overrideSteps[0]  = entry.overrideSteps;
                overrideDist[0]   = entry.overrideDistance;
                overrideCal[0]    = entry.overrideCalories;
                overrideActive[0] = entry.overrideActiveMinutes;
                overrideHR[0]     = entry.overrideHeartRate;
                overrideSleep[0]  = entry.overrideSleepHours;

                if (swSteps[0]  != null) updateSwitch(swSteps[0],  entry.overrideSteps);
                if (swDist[0]   != null) updateSwitch(swDist[0],   entry.overrideDistance);
                if (swCal[0]    != null) updateSwitch(swCal[0],    entry.overrideCalories);
                if (swActive[0] != null) updateSwitch(swActive[0], entry.overrideActiveMinutes);
                if (swHR[0]     != null) updateSwitch(swHR[0],     entry.overrideHeartRate);
                if (swSleep[0]  != null) updateSwitch(swSleep[0],  entry.overrideSleepHours);
            }
        });

        Button btnSave = new Button(requireContext());
        btnSave.setText("Save Data");
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
            PhysicalEntry entry   = new PhysicalEntry();
            entry.steps           = parseInt(etSteps, 0);
            entry.distanceKm      = parseDouble(etDist, 0);
            entry.caloriesBurned  = parseInt(etCal, 0);
            entry.activeMinutes   = parseInt(etActive, 0);
            entry.heartRate       = parseInt(etHR, 0);
            entry.sleepHours      = parseDouble(etSleepH, 0);
            entry.sleepScore      = sleepScore[0];
            entry.stressScore     = stressScore[0];
            entry.timestamp       = System.currentTimeMillis();

            entry.overrideSteps         = overrideSteps[0];
            entry.overrideDistance      = overrideDist[0];
            entry.overrideCalories      = overrideCal[0];
            entry.overrideActiveMinutes = overrideActive[0];
            entry.overrideHeartRate     = overrideHR[0];
            entry.overrideSleepHours    = overrideSleep[0];

            repo.saveManualPhysicalData(entry);
            dialog.dismiss();
            Toast.makeText(getContext(), "Data saved", Toast.LENGTH_SHORT).show();

            boolean anyOverrideTurnedOff = !overrideSteps[0] || !overrideDist[0]
                    || !overrideCal[0] || !overrideActive[0]
                    || !overrideHR[0] || !overrideSleep[0];

            if (anyOverrideTurnedOff && authManager.isConnected()
                    && !authManager.isTokenExpired()) {
                String token = authManager.getAccessToken();
                if (token != null) {
                    repo.syncFitbitInBackground(token, null);
                }
            }
        });

        dialog.show();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void updateSwitch(Switch sw, boolean checked) {
        sw.setChecked(checked);
        sw.setTextColor(checked ? 0xFF00B0B9 : 0xFF9E9E9E);
        sw.setText(checked ? "Fitbit locked out" : "Override Fitbit");
    }

    private EditText addFieldWithToggle(android.widget.LinearLayout parent,
                                        String label, String hint,
                                        boolean isDecimal, boolean[] overrideFlag,
                                        Switch[] switchRef) {
        android.widget.LinearLayout headerRow = new android.widget.LinearLayout(requireContext());
        headerRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        headerRow.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));

        android.widget.TextView tv = new android.widget.TextView(requireContext());
        tv.setText(label);
        tv.setTextSize(13);
        tv.setTextColor(0xFF666666);
        tv.setPadding(0, 0, 0, 4);
        android.widget.LinearLayout.LayoutParams tvParams =
                new android.widget.LinearLayout.LayoutParams(0,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tv.setLayoutParams(tvParams);
        headerRow.addView(tv);

        Switch toggle = new Switch(requireContext());
        toggle.setTextSize(10);
        toggle.setChecked(overrideFlag[0]);
        toggle.setText(overrideFlag[0] ? "Fitbit locked out" : "Override Fitbit");
        toggle.setTextColor(overrideFlag[0] ? 0xFF00B0B9 : 0xFF9E9E9E);
        toggle.setOnCheckedChangeListener((btn, checked) -> {
            overrideFlag[0] = checked;
            toggle.setTextColor(checked ? 0xFF00B0B9 : 0xFF9E9E9E);
            toggle.setText(checked ? "Fitbit locked out" : "Override Fitbit");
        });
        switchRef[0] = toggle;
        headerRow.addView(toggle);
        parent.addView(headerRow);

        EditText et = new EditText(requireContext());
        et.setHint(hint);
        et.setTextSize(14);
        et.setTextColor(0xFF1A1A1A);
        et.setHintTextColor(0xFFBDBDBD);
        et.setPadding(20, 16, 20, 16);
        et.setBackgroundColor(0xFFF0F0F0);
        et.setSingleLine(true);
        et.setInputType(isDecimal
                ? android.text.InputType.TYPE_CLASS_NUMBER
                | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
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

    private void addTitle(android.widget.LinearLayout parent,
                          String title, String subtitle) {
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
                ? android.text.InputType.TYPE_CLASS_NUMBER
                | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
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