package com.totalhealthdashboard.ui.journal;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.totalhealthdashboard.R;
import com.totalhealthdashboard.repository.HealthRepository;

public class JournalFragment extends Fragment {

    private int currentMoodScore = 5;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_journal, container, false);

        SeekBar seekBar     = view.findViewById(R.id.seekbar_mood);
        TextView tvEmoji    = view.findViewById(R.id.tv_mood_emoji);
        TextView tvLabel    = view.findViewById(R.id.tv_mood_label);
        TextView tvScore    = view.findViewById(R.id.tv_mood_score);
        EditText editText   = view.findViewById(R.id.journal_input);
        Button saveButton   = view.findViewById(R.id.btn_save_journal);

        HealthRepository repo = HealthRepository.getInstance();
        repo.init(requireContext());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentMoodScore = progress + 1; // 1–10
                tvScore.setText(currentMoodScore + " / 10");
                tvEmoji.setText(getMoodEmoji(currentMoodScore));
                tvLabel.setText(getMoodLabel(currentMoodScore));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        saveButton.setOnClickListener(v -> {
            String entry = editText.getText().toString().trim();
            if (entry.isEmpty()) {
                Toast.makeText(getContext(),
                    "Add a note before saving", Toast.LENGTH_SHORT).show();
                return;
            }
            repo.saveJournalEntry(entry, currentMoodScore);
            Toast.makeText(getContext(),
                "Entry saved  " + getMoodEmoji(currentMoodScore),
                Toast.LENGTH_SHORT).show();
            editText.setText("");
            seekBar.setProgress(4);
        });

        return view;
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