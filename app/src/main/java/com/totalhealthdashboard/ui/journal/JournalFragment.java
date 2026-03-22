package com.totalhealthdashboard.ui.journal;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
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
import java.util.Locale;

public class JournalFragment extends Fragment {

    private int currentMoodScore = 5;
    private HealthRepository repo;
    private LinearLayout journalListContainer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_journal, container, false);

        SeekBar seekBar   = view.findViewById(R.id.seekbar_mood);
        TextView tvEmoji  = view.findViewById(R.id.tv_mood_emoji);
        TextView tvLabel  = view.findViewById(R.id.tv_mood_label);
        TextView tvScore  = view.findViewById(R.id.tv_mood_score);
        EditText editText = view.findViewById(R.id.journal_input);
        Button saveButton = view.findViewById(R.id.btn_save_journal);
        journalListContainer = view.findViewById(R.id.journal_list_container);

        repo = HealthRepository.getInstance();
        repo.init(requireContext());

        // Mood slider
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentMoodScore = progress + 1;
                tvScore.setText(currentMoodScore + " / 10");
                tvEmoji.setText(getMoodEmoji(currentMoodScore));
                tvLabel.setText(getMoodLabel(currentMoodScore));
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        // Save entry
        saveButton.setOnClickListener(v -> {
            String entry = editText.getText().toString().trim();
            if (entry.isEmpty()) {
                Toast.makeText(getContext(),
                        "Add a note before saving", Toast.LENGTH_SHORT).show();
                return;
            }
            repo.saveJournalEntry(entry, currentMoodScore);
            Toast.makeText(getContext(),
                    "Entry saved " + getMoodEmoji(currentMoodScore),
                    Toast.LENGTH_SHORT).show();
            editText.setText("");
            seekBar.setProgress(4);
        });

        // Observe and display 5 most recent journal entries
        repo.getAllJournalEntries().observe(getViewLifecycleOwner(), entries -> {
            journalListContainer.removeAllViews();
            if (entries == null || entries.isEmpty()) return;
            for (JournalEntry entry : entries) {
                addJournalRow(entry);
            }
        });

        return view;
    }

    private void addJournalRow(JournalEntry entry) {
        // Card container
        android.widget.LinearLayout card = new android.widget.LinearLayout(requireContext());
        card.setOrientation(android.widget.LinearLayout.VERTICAL);
        card.setPadding(32, 24, 32, 24);

        android.widget.LinearLayout.LayoutParams cardParams =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, 12);
        card.setLayoutParams(cardParams);

        android.graphics.drawable.GradientDrawable bg =
                new android.graphics.drawable.GradientDrawable();
        bg.setColor(0xFFFFFFFF);
        bg.setCornerRadius(24f);
        bg.setStroke(1, 0xFFE0E0E0); // subtle grey border
        card.setBackground(bg);

        // Header row — date + mood + delete button
        android.widget.LinearLayout headerRow = new android.widget.LinearLayout(requireContext());
        headerRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        headerRow.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));

        // Date
        TextView tvDate = new TextView(requireContext());
        String date = new SimpleDateFormat("EEE dd MMM · h:mm a",
                Locale.getDefault()).format(new Date(entry.timestamp));
        tvDate.setText(date);
        tvDate.setTextSize(12);
        tvDate.setTextColor(0xFF9E9E9E);
        android.widget.LinearLayout.LayoutParams dateParams =
                new android.widget.LinearLayout.LayoutParams(0,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tvDate.setLayoutParams(dateParams);
        headerRow.addView(tvDate);

        // Mood emoji + score
        TextView tvMood = new TextView(requireContext());
        tvMood.setText(getMoodEmoji(entry.moodScore) + " " + entry.moodScore + "/10");
        tvMood.setTextSize(12);
        tvMood.setTextColor(0xFF9E9E9E);
        tvMood.setPadding(0, 0, 16, 0);
        headerRow.addView(tvMood);

        // Delete button
        TextView btnDelete = new TextView(requireContext());
        btnDelete.setText("🗑");
        btnDelete.setTextSize(16);
        btnDelete.setPadding(8, 0, 0, 0);
        btnDelete.setBackground(null);
        btnDelete.setOnClickListener(v -> {
            android.app.AlertDialog alertDialog = new android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Delete entry?")
                    .setMessage("This cannot be undone.")
                    .setPositiveButton("Delete", (d, w) -> {
                        repo.deleteJournalEntry(entry);
                        Toast.makeText(getContext(), "Entry deleted", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .create();
            alertDialog.show();
            alertDialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(0xFFF44336);
            alertDialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)
                    .setTextColor(0xFFFFFFFF);
        });
        headerRow.addView(btnDelete);
        card.addView(headerRow);

        // Entry content
        TextView tvContent = new TextView(requireContext());
        tvContent.setText(entry.content);
        tvContent.setTextSize(14);
        tvContent.setTextColor(0xFF1A1A1A);
        tvContent.setLineSpacing(0, 1.4f);
        android.widget.LinearLayout.LayoutParams contentParams =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        contentParams.setMargins(0, 8, 0, 0);
        tvContent.setLayoutParams(contentParams);
        card.addView(tvContent);

        journalListContainer.addView(card);
    }

    private String getMoodEmoji(int score) {
        if (score <= 2) return "😟";
        if (score <= 4) return "😔";
        if (score <= 6) return "😐";
        if (score <= 8) return "🙂";
        return "😄";
    }

    private String getMoodLabel(int score) {
        if (score <= 2) return "Really struggling";
        if (score <= 4) return "Not great";
        if (score <= 6) return "Neutral";
        if (score <= 8) return "Doing well";
        return "Feeling great";
    }
}