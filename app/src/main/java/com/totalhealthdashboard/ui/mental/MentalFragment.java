package com.totalhealthdashboard.ui.mental;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.totalhealthdashboard.R;
import com.totalhealthdashboard.data.local.JournalEntry;
import com.totalhealthdashboard.repository.HealthRepository;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MentalFragment extends Fragment {

    private HealthRepository repo;
    private LinearLayout journalListContainer;
    private List<JournalEntry> allEntries;
    private boolean showingAll = false;
    private static final int INITIAL_COUNT = 3;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mental, container, false);

        TextView tvMoodEmoji   = view.findViewById(R.id.tv_mood_emoji_big);
        TextView tvAvgScore    = view.findViewById(R.id.tv_avg_mood_score);
        TextView tvMoodLabel   = view.findViewById(R.id.tv_mood_label);
        TextView tvEntryCount  = view.findViewById(R.id.tv_entry_count);
        TextView tvMoodTrend   = view.findViewById(R.id.tv_mood_trend);
        TextView tvLatestEntry = view.findViewById(R.id.tv_latest_entry);
        TextView tvLatestDate  = view.findViewById(R.id.tv_latest_entry_date);
        TextView tvQuote       = view.findViewById(R.id.tv_quote);
        journalListContainer   = view.findViewById(R.id.journal_history_container);
        TextView btnViewAll    = view.findViewById(R.id.btn_view_all_journals);

        repo = HealthRepository.getInstance();
        repo.init(requireContext());

        repo.getEntryCountThisWeek().observe(getViewLifecycleOwner(), count -> {
            if (count == null) count = 0;
            tvEntryCount.setText(String.valueOf(count));
        });

        repo.getAverageMoodThisWeek().observe(getViewLifecycleOwner(), avg -> {
            if (avg == null || avg == 0) {
                tvAvgScore.setText("—");
                tvMoodLabel.setText("No entries yet this week");
                tvMoodEmoji.setText("😐");
                tvMoodTrend.setText("—");
                return;
            }
            tvAvgScore.setText(String.format(Locale.getDefault(), "%.1f", avg));
            tvMoodTrend.setText(String.format(Locale.getDefault(), "%.1f", avg));
            tvMoodLabel.setText(getMoodLabel(avg));
            tvMoodEmoji.setText(getMoodEmoji(avg));
        });

        repo.getLatestEntry().observe(getViewLifecycleOwner(), entry -> {
            if (entry == null) return;
            tvLatestEntry.setText(entry.content);
            String date = new SimpleDateFormat("EEE dd MMM · h:mm a",
                    Locale.getDefault()).format(new Date(entry.timestamp));
            tvLatestDate.setText(date + "  ·  Mood: " + entry.moodScore + "/10");
        });

        repo.getWellnessQuote().observe(getViewLifecycleOwner(), quote -> {
            if (quote != null && !quote.isEmpty()) {
                tvQuote.setText(quote);
            } else {
                tvQuote.setText("Could not load quote — check your connection");
            }
        });

        repo.getAllJournalEntries().observe(getViewLifecycleOwner(), entries -> {
            allEntries = entries;
            showingAll = false;
            renderJournalList(INITIAL_COUNT);
            if (entries != null && entries.size() > INITIAL_COUNT) {
                btnViewAll.setVisibility(View.VISIBLE);
                btnViewAll.setText("View all " + entries.size() + " entries ▼");
            } else {
                btnViewAll.setVisibility(View.GONE);
            }
        });

        btnViewAll.setOnClickListener(v -> {
            if (!showingAll) {
                showingAll = true;
                renderJournalList(allEntries.size());
                btnViewAll.setText("Show less ▲");
            } else {
                showingAll = false;
                renderJournalList(INITIAL_COUNT);
                btnViewAll.setText("View all " + allEntries.size() + " entries ▼");
            }
        });

        view.findViewById(R.id.btn_write_journal).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container,
                                new com.totalhealthdashboard.ui.journal.JournalFragment())
                        .addToBackStack(null)
                        .commit());

        return view;
    }

    private void renderJournalList(int count) {
        journalListContainer.removeAllViews();
        if (allEntries == null || allEntries.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setText("No journal entries yet — tap Write Entry to start");
            empty.setTextSize(13);
            empty.setTextColor(0xFF9E9E9E);
            empty.setPadding(0, 8, 0, 8);
            journalListContainer.addView(empty);
            return;
        }

        int limit = Math.min(count, allEntries.size());
        for (int i = 0; i < limit; i++) {
            addJournalCard(allEntries.get(i));
        }
    }

    private void addJournalCard(JournalEntry entry) {
        androidx.cardview.widget.CardView card =
                new androidx.cardview.widget.CardView(requireContext());
        androidx.cardview.widget.CardView.LayoutParams cardParams =
                new androidx.cardview.widget.CardView.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, 10);
        card.setLayoutParams(cardParams);
        card.setRadius(16);
        card.setCardElevation(0);
        card.setCardBackgroundColor(0xFFFFFFFF);

        LinearLayout wrapper = new LinearLayout(requireContext());
        wrapper.setOrientation(LinearLayout.VERTICAL);
        card.addView(wrapper);

        // Header — always visible
        LinearLayout header = new LinearLayout(requireContext());
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setPadding(40, 28, 40, 28);
        header.setClickable(true);
        header.setFocusable(true);
        header.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView tvDate = new TextView(requireContext());
        String date = new SimpleDateFormat("EEE dd MMM",
                Locale.getDefault()).format(new Date(entry.timestamp));
        tvDate.setText(date);
        tvDate.setTextSize(14);
        tvDate.setTypeface(null, android.graphics.Typeface.BOLD);
        tvDate.setTextColor(0xFF1A1A1A);
        LinearLayout.LayoutParams dateParams =
                new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tvDate.setLayoutParams(dateParams);
        header.addView(tvDate);

        TextView tvMood = new TextView(requireContext());
        tvMood.setText(getMoodEmoji(entry.moodScore) + " " + entry.moodScore + "/10");
        tvMood.setTextSize(13);
        tvMood.setTextColor(0xFF9E9E9E);
        tvMood.setPadding(0, 0, 12, 0);
        header.addView(tvMood);

        TextView tvChevron = new TextView(requireContext());
        tvChevron.setText("❯");
        tvChevron.setTextSize(13);
        tvChevron.setTextColor(0xFF9E9E9E);
        header.addView(tvChevron);

        wrapper.addView(header);

        // Expandable content
        LinearLayout expandable = new LinearLayout(requireContext());
        expandable.setOrientation(LinearLayout.VERTICAL);
        expandable.setPadding(40, 0, 40, 28);
        expandable.setVisibility(View.GONE);
        wrapper.addView(expandable);

        // Divider
        View divider = new View(requireContext());
        divider.setBackgroundColor(0xFFF0F0F0);
        LinearLayout.LayoutParams divParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1);
        divParams.setMargins(0, 0, 0, 12);
        divider.setLayoutParams(divParams);
        expandable.addView(divider);

        // Full content
        TextView tvContent = new TextView(requireContext());
        tvContent.setText(entry.content);
        tvContent.setTextSize(14);
        tvContent.setTextColor(0xFF1A1A1A);
        tvContent.setLineSpacing(0, 1.4f);
        expandable.addView(tvContent);

        // Time + delete row
        LinearLayout footerRow = new LinearLayout(requireContext());
        footerRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams footerParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        footerParams.setMargins(0, 12, 0, 0);
        footerRow.setLayoutParams(footerParams);

        TextView tvTime = new TextView(requireContext());
        String time = new SimpleDateFormat("h:mm a",
                Locale.getDefault()).format(new Date(entry.timestamp));
        tvTime.setText(time);
        tvTime.setTextSize(12);
        tvTime.setTextColor(0xFF9E9E9E);
        LinearLayout.LayoutParams timeParams =
                new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tvTime.setLayoutParams(timeParams);
        footerRow.addView(tvTime);

        TextView btnDelete = new TextView(requireContext());
        btnDelete.setText("🗑 Delete");
        btnDelete.setTextSize(12);
        btnDelete.setTextColor(0xFFF44336);
        btnDelete.setOnClickListener(v -> {
            android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Delete entry?")
                    .setMessage("This cannot be undone.")
                    .setPositiveButton("Delete", (d, w) -> {
                        repo.deleteJournalEntry(entry);
                        Toast.makeText(getContext(),
                                "Entry deleted", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .create();
            dialog.show();
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setTextColor(0xFFF44336);
            dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(0xFF9E9E9E);
        });
        footerRow.addView(btnDelete);
        expandable.addView(footerRow);

        // Toggle on tap
        header.setOnClickListener(v -> {
            if (expandable.getVisibility() == View.GONE) {
                expandable.setVisibility(View.VISIBLE);
                tvChevron.setRotation(90f);
            } else {
                expandable.setVisibility(View.GONE);
                tvChevron.setRotation(0f);
            }
        });

        journalListContainer.addView(card);
    }

    private String getMoodEmoji(int score) {
        if (score <= 2) return "😟";
        if (score <= 4) return "😔";
        if (score <= 6) return "😐";
        if (score <= 8) return "🙂";
        return "😄";
    }

    private String getMoodEmoji(float score) {
        return getMoodEmoji(Math.round(score));
    }

    private String getMoodLabel(float score) {
        if (score <= 2) return "Really struggling this week";
        if (score <= 4) return "Tough week overall";
        if (score <= 6) return "Mixed feelings this week";
        if (score <= 8) return "Doing well this week";
        return "Fantastic week!";
    }
}