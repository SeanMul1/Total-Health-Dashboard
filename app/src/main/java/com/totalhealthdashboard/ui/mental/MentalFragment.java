package com.totalhealthdashboard.ui.mental;

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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MentalFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mental, container, false);

        TextView tvMoodEmoji    = view.findViewById(R.id.tv_mood_emoji_big);
        TextView tvAvgScore     = view.findViewById(R.id.tv_avg_mood_score);
        TextView tvMoodLabel    = view.findViewById(R.id.tv_mood_label);
        TextView tvEntryCount   = view.findViewById(R.id.tv_entry_count);
        TextView tvMoodTrend    = view.findViewById(R.id.tv_mood_trend);
        TextView tvLatestEntry  = view.findViewById(R.id.tv_latest_entry);
        TextView tvLatestDate   = view.findViewById(R.id.tv_latest_entry_date);
        TextView tvQuote        = view.findViewById(R.id.tv_quote);

        HealthRepository repo = HealthRepository.getInstance();
        repo.init(requireContext());

        // Journal entry count this week
        repo.getEntryCountThisWeek().observe(getViewLifecycleOwner(), count -> {
            if (count == null) count = 0;
            tvEntryCount.setText(String.valueOf(count));
        });

        // Average mood score this week
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

        // Latest journal entry
        repo.getLatestEntry().observe(getViewLifecycleOwner(), entry -> {
            if (entry == null) return;
            tvLatestEntry.setText(entry.content);
            String date = new SimpleDateFormat("EEE dd MMM · h:mm a",
                Locale.getDefault()).format(new Date(entry.timestamp));
            tvLatestDate.setText(date + "  ·  Mood: " + entry.moodScore + "/10");
        });

        // ZenQuotes live API call
        repo.getWellnessQuote().observe(getViewLifecycleOwner(), quote -> {
            if (quote != null) tvQuote.setText(quote);
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

    private String getMoodEmoji(float score) {
        if (score <= 2) return "😟";
        if (score <= 4) return "😔";
        if (score <= 6) return "😐";
        if (score <= 8) return "🙂";
        return "😄";
    }

    private String getMoodLabel(float score) {
        if (score <= 2) return "Really struggling this week";
        if (score <= 4) return "Tough week overall";
        if (score <= 6) return "Mixed feelings this week";
        if (score <= 8) return "Doing well this week";
        return "Fantastic week!";
    }
}