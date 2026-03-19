package com.totalhealthdashboard.ui.diet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.totalhealthdashboard.R;
import com.totalhealthdashboard.data.local.NutritionEntry;
import com.totalhealthdashboard.data.models.NutritionData;
import com.totalhealthdashboard.repository.HealthRepository;
import java.util.Locale;

public class DietFragment extends Fragment {

    private HealthRepository repo;
    private NutritionData baseNutrition;
    private NutritionData adjustedNutrition;
    private double currentPortionGrams = 150.0;

    private EditText searchInput;
    private ProgressBar searchProgress;
    private CardView cardResult;
    private TextView tvFoodName, tvPortionLabel, tvCalories, tvProtein, tvCarbs, tvFat;
    private TextView tvTotalCalories, tvTotalMacros, tvEmptyLog, tvFrequentLabel;
    private LinearLayout foodLogContainer, customPortionContainer;
    private ChipGroup portionChips, frequentChips;
    private EditText etCustomGrams;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_diet, container, false);

        repo = HealthRepository.getInstance();
        repo.init(requireContext());

        initViews(view);
        observeData();

        return view;
    }

    private void initViews(View v) {
        searchInput            = v.findViewById(R.id.et_food_search);
        searchProgress         = v.findViewById(R.id.search_progress);
        cardResult             = v.findViewById(R.id.card_search_result);
        tvFoodName             = v.findViewById(R.id.tv_food_name);
        tvPortionLabel         = v.findViewById(R.id.tv_portion_label);
        tvCalories             = v.findViewById(R.id.tv_calories_big);
        tvProtein              = v.findViewById(R.id.tv_protein);
        tvCarbs                = v.findViewById(R.id.tv_carbs);
        tvFat                  = v.findViewById(R.id.tv_fat);
        tvTotalCalories        = v.findViewById(R.id.tv_total_calories);
        tvTotalMacros          = v.findViewById(R.id.tv_total_macros);
        tvEmptyLog             = v.findViewById(R.id.tv_empty_log);
        tvFrequentLabel        = v.findViewById(R.id.tv_frequent_label);
        foodLogContainer       = v.findViewById(R.id.food_log_container);
        portionChips           = v.findViewById(R.id.portion_chips);
        frequentChips          = v.findViewById(R.id.frequent_chips);
        customPortionContainer = v.findViewById(R.id.custom_portion_container);
        etCustomGrams          = v.findViewById(R.id.et_custom_grams);

        // Search button
        v.findViewById(R.id.btn_search).setOnClickListener(v2 -> triggerSearch());

        // Keyboard search action
        searchInput.setOnEditorActionListener((tv, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                triggerSearch();
                return true;
            }
            return false;
        });

        // Add to log — re-reads custom grams at save time
        v.findViewById(R.id.btn_add_to_log).setOnClickListener(v2 -> {
            if (adjustedNutrition == null) return;

            // If custom chip selected, re-read the input box at save time
            if (portionChips.getCheckedChipId() == R.id.chip_custom) {
                String customText = etCustomGrams.getText().toString().trim();
                if (!customText.isEmpty()) {
                    try {
                        currentPortionGrams = Double.parseDouble(customText);
                        updateAdjustedNutrition();
                    } catch (NumberFormatException ignored) {}
                }
            }

            repo.addFoodToLog(adjustedNutrition);
            cardResult.setVisibility(View.GONE);
            searchInput.setText("");
            Toast.makeText(getContext(), "Added to log ✓", Toast.LENGTH_SHORT).show();
        });

        // Portion selector — only fires after a food is selected
        portionChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;

            // Don't update if no food is selected yet
            if (baseNutrition == null) return;

            int id = checkedIds.get(0);
            if (id == R.id.chip_small) {
                currentPortionGrams = 75;
                customPortionContainer.setVisibility(View.GONE);
            } else if (id == R.id.chip_medium) {
                currentPortionGrams = 150;
                customPortionContainer.setVisibility(View.GONE);
            } else if (id == R.id.chip_large) {
                currentPortionGrams = 300;
                customPortionContainer.setVisibility(View.GONE);
            } else if (id == R.id.chip_custom) {
                customPortionContainer.setVisibility(View.VISIBLE);
                try {
                    currentPortionGrams = Double.parseDouble(
                            etCustomGrams.getText().toString());
                } catch (Exception e) {
                    currentPortionGrams = 100;
                    etCustomGrams.setText("100");
                }
            }
            updateAdjustedNutrition();
        });

        // Custom grams keyboard done action
        etCustomGrams.setOnEditorActionListener((tv, actionId, event) -> {
            try {
                currentPortionGrams = Double.parseDouble(tv.getText().toString());
                updateAdjustedNutrition();
            } catch (Exception ignored) {}
            return false;
        });
    }

    private void triggerSearch() {
        String query = searchInput.getText().toString().trim();
        if (query.length() < 2) {
            Toast.makeText(getContext(), "Type at least 2 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        // Hide keyboard
        InputMethodManager imm = (InputMethodManager)
                requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);

        searchProgress.setVisibility(View.VISIBLE);
        cardResult.setVisibility(View.GONE);

        repo.searchFood(query).observe(getViewLifecycleOwner(), data -> {
            searchProgress.setVisibility(View.GONE);
            if (data != null) {
                selectFood(data);
            } else {
                Toast.makeText(getContext(),
                        "Nothing found — try a different term",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Central method for selecting a food — used by search and frequent chips
    private void selectFood(NutritionData data) {
        baseNutrition = data;
        currentPortionGrams = 150;
        portionChips.check(R.id.chip_medium);
        customPortionContainer.setVisibility(View.GONE);
        updateAdjustedNutrition();
        tvFoodName.setText(data.getFoodName());
        cardResult.setVisibility(View.VISIBLE);
    }

    private void updateAdjustedNutrition() {
        if (baseNutrition == null) return;
        double factor = currentPortionGrams / 100.0;
        adjustedNutrition = new NutritionData(
                baseNutrition.getFoodName(),
                (int)(baseNutrition.getCalories() * factor),
                baseNutrition.getProtein() * factor,
                baseNutrition.getCarbs() * factor,
                baseNutrition.getFat() * factor,
                baseNutrition.getImageUrl()
        );
        tvCalories.setText(adjustedNutrition.getCalories() + " kcal");
        tvProtein.setText(String.format(Locale.getDefault(),
                "P: %.1fg", adjustedNutrition.getProtein()));
        tvCarbs.setText(String.format(Locale.getDefault(),
                "C: %.1fg", adjustedNutrition.getCarbs()));
        tvFat.setText(String.format(Locale.getDefault(),
                "F: %.1fg", adjustedNutrition.getFat()));
        tvPortionLabel.setText("per " + (int)currentPortionGrams + "g");
    }

    private void observeData() {
        repo.getTotalCalories().observe(getViewLifecycleOwner(), total ->
                tvTotalCalories.setText((total != null ? total : 0) + " kcal"));

        repo.getTotalProtein().observe(getViewLifecycleOwner(), p -> updateMacroSummary());
        repo.getTotalCarbs().observe(getViewLifecycleOwner(), c -> updateMacroSummary());
        repo.getTotalFat().observe(getViewLifecycleOwner(), f -> updateMacroSummary());

        repo.getNutritionEntriesForToday().observe(getViewLifecycleOwner(), entries -> {
            foodLogContainer.removeAllViews();
            if (entries == null || entries.isEmpty()) {
                tvEmptyLog.setVisibility(View.VISIBLE);
            } else {
                tvEmptyLog.setVisibility(View.GONE);
                for (NutritionEntry entry : entries) addLogRow(entry);
            }
        });

        repo.getFrequentFoods().observe(getViewLifecycleOwner(), foods -> {
            frequentChips.removeAllViews();
            if (foods != null && !foods.isEmpty()) {
                tvFrequentLabel.setVisibility(View.VISIBLE);
                for (NutritionData food : foods) {
                    Chip chip = new Chip(requireContext());
                    chip.setText(food.getFoodName());
                    chip.setOnClickListener(v -> selectFood(food));
                    frequentChips.addView(chip);
                }
            } else {
                tvFrequentLabel.setVisibility(View.GONE);
            }
        });
    }

    private void updateMacroSummary() {
        double p = repo.getTotalProtein().getValue() != null ? repo.getTotalProtein().getValue() : 0;
        double c = repo.getTotalCarbs().getValue() != null ? repo.getTotalCarbs().getValue() : 0;
        double f = repo.getTotalFat().getValue() != null ? repo.getTotalFat().getValue() : 0;
        tvTotalMacros.setText(String.format(Locale.getDefault(),
                "P: %.0fg  C: %.0fg  F: %.0fg", p, c, f));
    }

    private void addLogRow(NutritionEntry entry) {
        View row = LayoutInflater.from(getContext())
                .inflate(R.layout.item_food_log_row, foodLogContainer, false);
        ((TextView) row.findViewById(R.id.tv_log_food_name)).setText(entry.foodName);
        ((TextView) row.findViewById(R.id.tv_log_calories)).setText(entry.calories + " kcal");
        ((TextView) row.findViewById(R.id.tv_log_macros)).setText(
                String.format(Locale.getDefault(),
                        "P: %.0fg  C: %.0fg  F: %.0fg",
                        entry.protein, entry.carbs, entry.fat));
        row.findViewById(R.id.btn_remove_food).setOnClickListener(v -> {
            repo.removeFoodFromLog(entry);
            Toast.makeText(getContext(), entry.foodName + " removed", Toast.LENGTH_SHORT).show();
        });
        foodLogContainer.addView(row);
    }
}