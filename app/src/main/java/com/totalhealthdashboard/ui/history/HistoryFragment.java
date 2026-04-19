package com.totalhealthdashboard.ui.history;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.totalhealthdashboard.R;
import com.totalhealthdashboard.data.local.NutritionEntry;
import com.totalhealthdashboard.data.local.PhysicalHistoryEntry;
import com.totalhealthdashboard.repository.HealthRepository;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HistoryFragment extends Fragment {

    private HealthRepository repo;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        repo = HealthRepository.getInstance();
        repo.init(requireContext());

        buildCalorieChart(view);
        buildPhysicalHistory(view);

        return view;
    }

    // ─── 7-day calorie chart ──────────────────────────────────────────────────

    private void buildCalorieChart(View view) {
        LinearLayout chartContainer = view.findViewById(R.id.chart_container);
        List<Long> last7Days = getLast7DayStarts();

        repo.getNutritionHistoryForDays(last7Days.get(0))
                .observe(getViewLifecycleOwner(), entries -> {
                    if (entries == null) return;

                    Map<Long, Integer> caloriesByDay = new LinkedHashMap<>();
                    for (Long day : last7Days) caloriesByDay.put(day, 0);

                    for (NutritionEntry e : entries) {
                        for (Long day : last7Days) {
                            if (e.timestamp >= day && e.timestamp < day + 86400000L) {
                                caloriesByDay.put(day,
                                        caloriesByDay.getOrDefault(day, 0) + e.calories);
                                break;
                            }
                        }
                    }

                    int maxCal = 1;
                    for (int cal : caloriesByDay.values())
                        if (cal > maxCal) maxCal = cal;

                    chartContainer.removeAllViews();
                    SimpleDateFormat sdf = new SimpleDateFormat("EEE", Locale.getDefault());

                    for (Map.Entry<Long, Integer> entry : caloriesByDay.entrySet()) {
                        int cal = entry.getValue();
                        String dayLabel = sdf.format(new Date(entry.getKey()));

                        LinearLayout col = new LinearLayout(requireContext());
                        col.setOrientation(LinearLayout.VERTICAL);
                        col.setGravity(android.view.Gravity.BOTTOM | android.view.Gravity.CENTER_HORIZONTAL);
                        LinearLayout.LayoutParams colParams =
                                new LinearLayout.LayoutParams(0,
                                        LinearLayout.LayoutParams.MATCH_PARENT, 1f);
                        colParams.setMargins(4, 0, 4, 0);
                        col.setLayoutParams(colParams);

                        TextView tvCal = new TextView(requireContext());
                        tvCal.setText(cal > 0 ? String.valueOf(cal) : "");
                        tvCal.setTextSize(9);
                        tvCal.setTextColor(0xFF9E9E9E);
                        tvCal.setGravity(android.view.Gravity.CENTER);
                        col.addView(tvCal);

                        View bar = new View(requireContext());
                        int barHeight = cal > 0 ? (int)(((float) cal / maxCal) * 150) : 4;
                        LinearLayout.LayoutParams barParams =
                                new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT, barHeight);
                        barParams.setMargins(8, 4, 8, 0);
                        android.graphics.drawable.GradientDrawable shape =
                                new android.graphics.drawable.GradientDrawable();
                        shape.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
                        shape.setCornerRadii(new float[]{8,8,8,8,0,0,0,0});
                        shape.setColor(cal > 0 ? 0xFF00B0B9 : 0xFFE0E0E0);
                        bar.setBackground(shape);
                        bar.setLayoutParams(barParams);
                        col.addView(bar);

                        TextView tvDay = new TextView(requireContext());
                        tvDay.setText(dayLabel);
                        tvDay.setTextSize(11);
                        tvDay.setTextColor(0xFF9E9E9E);
                        tvDay.setGravity(android.view.Gravity.CENTER);
                        LinearLayout.LayoutParams dayParams =
                                new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT);
                        dayParams.setMargins(0, 4, 0, 0);
                        tvDay.setLayoutParams(dayParams);
                        col.addView(tvDay);

                        chartContainer.addView(col);
                    }
                });
    }

    // ─── 7-day physical history ───────────────────────────────────────────────

    private void buildPhysicalHistory(View view) {
        LinearLayout historyContainer = view.findViewById(R.id.physical_history_container);

        repo.getPhysicalHistory().observe(getViewLifecycleOwner(), entries -> {
            historyContainer.removeAllViews();

            if (entries == null || entries.isEmpty()) {
                TextView empty = new TextView(requireContext());
                empty.setText("No history yet — data saves automatically each day after your first Fitbit sync");
                empty.setTextSize(13);
                empty.setTextColor(0xFF9E9E9E);
                empty.setPadding(0, 16, 0, 0);
                historyContainer.addView(empty);
                return;
            }

            SimpleDateFormat sdf = new SimpleDateFormat("EEE dd MMM", Locale.getDefault());

            for (PhysicalHistoryEntry e : entries) {

                // Use the score stored at snapshot time — not recalculated
                int score = e.overallScore;

                // ── Card ──
                androidx.cardview.widget.CardView card =
                        new androidx.cardview.widget.CardView(requireContext());
                androidx.cardview.widget.CardView.LayoutParams cardParams =
                        new androidx.cardview.widget.CardView.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                cardParams.setMargins(0, 0, 0, 12);
                card.setLayoutParams(cardParams);
                card.setRadius(16);
                card.setCardElevation(0);
                card.setCardBackgroundColor(0xFFFFFFFF);

                LinearLayout wrapper = new LinearLayout(requireContext());
                wrapper.setOrientation(LinearLayout.VERTICAL);
                card.addView(wrapper);

                // ── Header row — always visible, acts as toggle ──
                LinearLayout headerRow = new LinearLayout(requireContext());
                headerRow.setOrientation(LinearLayout.HORIZONTAL);
                headerRow.setPadding(48, 36, 48, 36);
                headerRow.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
                headerRow.setBackground(new android.graphics.drawable.ColorDrawable(
                        android.graphics.Color.TRANSPARENT));
                headerRow.setClickable(true);
                headerRow.setFocusable(true);

                TextView tvDate = new TextView(requireContext());
                tvDate.setText(sdf.format(new Date(e.date)));
                tvDate.setTextSize(15);
                tvDate.setTypeface(null, android.graphics.Typeface.BOLD);
                tvDate.setTextColor(0xFF1A1A1A);
                LinearLayout.LayoutParams dateParams =
                        new LinearLayout.LayoutParams(0,
                                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                tvDate.setLayoutParams(dateParams);
                headerRow.addView(tvDate);

                TextView tvScore = new TextView(requireContext());
                tvScore.setText(score + "/100");
                tvScore.setTextSize(16);
                tvScore.setTypeface(null, android.graphics.Typeface.BOLD);
                tvScore.setTextColor(getScoreColor(score));
                headerRow.addView(tvScore);

                TextView tvChevron = new TextView(requireContext());
                tvChevron.setText("  ❯");
                tvChevron.setTextSize(14);
                tvChevron.setTextColor(0xFF9E9E9E);
                headerRow.addView(tvChevron);

                wrapper.addView(headerRow);

                // ── Expandable content — hidden by default ──
                LinearLayout expandable = new LinearLayout(requireContext());
                expandable.setOrientation(LinearLayout.VERTICAL);
                expandable.setPadding(48, 0, 48, 36);
                expandable.setVisibility(View.GONE);
                wrapper.addView(expandable);

                headerRow.setOnClickListener(v -> {
                    if (expandable.getVisibility() == View.GONE) {
                        expandable.setVisibility(View.VISIBLE);
                        tvChevron.setRotation(90f);
                    } else {
                        expandable.setVisibility(View.GONE);
                        tvChevron.setRotation(0f);
                    }
                });

                // Divider
                View divider = new View(requireContext());
                divider.setBackgroundColor(0xFFF0F0F0);
                LinearLayout.LayoutParams divParams =
                        new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, 1);
                divParams.setMargins(0, 0, 0, 16);
                divider.setLayoutParams(divParams);
                expandable.addView(divider);

                // Row 1: steps, calories burned, active
                LinearLayout row1 = new LinearLayout(requireContext());
                row1.setOrientation(LinearLayout.HORIZONTAL);
                row1.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
                addMetricCell(row1, "👟", e.steps > 0
                        ? String.format("%,d", e.steps) : "—", "steps");
                addMetricCell(row1, "🔥", e.caloriesBurned > 0
                        ? String.valueOf(e.caloriesBurned) : "—", "kcal burned");
                addMetricCell(row1, "⏱️", e.activeMinutes > 0
                        ? e.activeMinutes + "m" : "—", "active");
                expandable.addView(row1);

                addSpacer(expandable, 12);

                // Row 2: sleep, sleep quality, stress
                LinearLayout row2 = new LinearLayout(requireContext());
                row2.setOrientation(LinearLayout.HORIZONTAL);
                row2.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
                addMetricCell(row2, "😴", e.sleepHours > 0
                        ? e.sleepHours + "h" : "—", "sleep");
                addMetricCell(row2, "⭐", e.sleepScore > 0
                        ? e.sleepScore + "/10" : "—", "quality");
                addMetricCell(row2, "🧘", e.stressScore > 0
                        ? e.stressScore + "/10" : "—", "stress level");
                expandable.addView(row2);

                // Row 3: calories consumed, mood, journal entries
                addSpacer(expandable, 12);
                LinearLayout row3mental = new LinearLayout(requireContext());
                row3mental.setOrientation(LinearLayout.HORIZONTAL);
                row3mental.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
                addMetricCell(row3mental, "🍽️", e.caloriesConsumed > 0
                        ? e.caloriesConsumed + " kcal" : "—", "consumed");
                addMetricCell(row3mental, "😊", e.moodScore > 0
                        ? String.format(Locale.getDefault(), "%.1f/10", e.moodScore) : "—", "avg mood");
                addMetricCell(row3mental, "📓", e.journalCount > 0
                        ? e.journalCount + " entries" : "—", "journal");
                expandable.addView(row3mental);

                // Row 4: heart rate + floors
                if (e.heartRate > 0 || e.floors > 0) {
                    addSpacer(expandable, 12);
                    LinearLayout row3 = new LinearLayout(requireContext());
                    row3.setOrientation(LinearLayout.HORIZONTAL);
                    row3.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT));
                    addMetricCell(row3, "❤️", e.heartRate > 0
                            ? e.heartRate + " bpm" : "—", "resting hr");
                    addMetricCell(row3, "🏢", e.floors > 0
                            ? String.valueOf(e.floors) : "—", "floors");
                    LinearLayout emptyCell = new LinearLayout(requireContext());
                    emptyCell.setLayoutParams(new LinearLayout.LayoutParams(0,
                            LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                    row3.addView(emptyCell);
                    expandable.addView(row3);
                }

                historyContainer.addView(card);
            }
        });
    }

    private int getScoreColor(int score) {
        if (score >= 75) return 0xFF4CAF50;
        if (score >= 50) return 0xFFFF9800;
        return 0xFFF44336;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void addMetricCell(LinearLayout parent, String emoji,
                               String value, String label) {
        LinearLayout col = new LinearLayout(requireContext());
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(android.view.Gravity.CENTER);
        col.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvEmoji = new TextView(requireContext());
        tvEmoji.setText(emoji);
        tvEmoji.setTextSize(18);
        tvEmoji.setGravity(android.view.Gravity.CENTER);
        col.addView(tvEmoji);

        TextView tvValue = new TextView(requireContext());
        tvValue.setText(value);
        tvValue.setTextSize(13);
        tvValue.setTypeface(null, android.graphics.Typeface.BOLD);
        tvValue.setTextColor(0xFF1A1A1A);
        tvValue.setGravity(android.view.Gravity.CENTER);
        col.addView(tvValue);

        TextView tvLabel = new TextView(requireContext());
        tvLabel.setText(label);
        tvLabel.setTextSize(10);
        tvLabel.setTextColor(0xFF9E9E9E);
        tvLabel.setGravity(android.view.Gravity.CENTER);
        col.addView(tvLabel);

        parent.addView(col);
    }

    private void addSpacer(LinearLayout parent, int heightDp) {
        View spacer = new View(requireContext());
        spacer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, heightDp));
        parent.addView(spacer);
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
}