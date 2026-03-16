package com.totalhealthdashboard.ui.diet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
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
import com.totalhealthdashboard.R;
import com.totalhealthdashboard.data.models.NutritionData;
import com.totalhealthdashboard.repository.HealthRepository;
import java.util.ArrayList;
import java.util.List;

public class DietFragment extends Fragment {

    // Running daily totals
    private int totalCalories = 0;
    private double totalProtein = 0;
    private double totalCarbs = 0;
    private double totalFat = 0;

    private final List<NutritionData> foodLog = new ArrayList<>();

    // Current search result
    private NutritionData currentResult = null;

    // Views
    private CardView cardResult;
    private TextView tvFoodName, tvCaloriesBig, tvProtein, tvCarbs, tvFat;
    private TextView tvTotalCalories, tvTotalProtein, tvTotalCarbs, tvTotalFat;
    private TextView tvEmptyLog;
    private LinearLayout foodLogContainer;
    private ProgressBar progressBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_diet, container, false);

        // Wire up views
        EditText searchInput    = view.findViewById(R.id.et_food_search);
        Button btnSearch        = view.findViewById(R.id.btn_search);
        Button btnAddToLog      = view.findViewById(R.id.btn_add_to_log);
        cardResult              = view.findViewById(R.id.card_search_result);
        tvFoodName              = view.findViewById(R.id.tv_food_name);
        tvCaloriesBig           = view.findViewById(R.id.tv_calories_big);
        tvProtein               = view.findViewById(R.id.tv_protein);
        tvCarbs                 = view.findViewById(R.id.tv_carbs);
        tvFat                   = view.findViewById(R.id.tv_fat);
        tvTotalCalories         = view.findViewById(R.id.tv_total_calories);
        tvTotalProtein          = view.findViewById(R.id.tv_total_protein);
        tvTotalCarbs            = view.findViewById(R.id.tv_total_carbs);
        tvTotalFat              = view.findViewById(R.id.tv_total_fat);
        tvEmptyLog              = view.findViewById(R.id.tv_empty_log);
        foodLogContainer        = view.findViewById(R.id.food_log_container);
        progressBar             = view.findViewById(R.id.progress_bar);

        HealthRepository repo = HealthRepository.getInstance();
        repo.init(requireContext());

        // Search on button click
        btnSearch.setOnClickListener(v -> {
            String query = searchInput.getText().toString().trim();
            if (query.isEmpty()) {
                Toast.makeText(getContext(), "Enter a food to search", Toast.LENGTH_SHORT).show();
                return;
            }
            performSearch(query, repo);
        });

        // Search on keyboard done
        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = searchInput.getText().toString().trim();
                if (!query.isEmpty()) performSearch(query, repo);
                return true;
            }
            return false;
        });

        // Add current result to daily log
        btnAddToLog.setOnClickListener(v -> {
            if (currentResult != null) {
                addToLog(currentResult);
                cardResult.setVisibility(View.GONE);
                currentResult = null;
                searchInput.setText("");
            }
        });

        return view;
    }

    private void performSearch(String query, HealthRepository repo) {
        progressBar.setVisibility(View.VISIBLE);
        cardResult.setVisibility(View.GONE);

        repo.searchFood(query).observe(getViewLifecycleOwner(), data -> {
            progressBar.setVisibility(View.GONE);
            if (data != null) {
                currentResult = data;
                showSearchResult(data);
            } else {
                Toast.makeText(getContext(),
                    "No results found — try another search", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showSearchResult(NutritionData data) {
        tvFoodName.setText(capitalize(data.getFoodName()));
        tvCaloriesBig.setText(String.valueOf(data.getCalories()));
        tvProtein.setText(String.format("%.1fg", data.getProtein()));
        tvCarbs.setText(String.format("%.1fg", data.getCarbs()));
        tvFat.setText(String.format("%.1fg", data.getFat()));
        cardResult.setVisibility(View.VISIBLE);
    }

    private void addToLog(NutritionData data) {
        foodLog.add(data);
        totalCalories += data.getCalories();
        totalProtein  += data.getProtein();
        totalCarbs    += data.getCarbs();
        totalFat      += data.getFat();

        updateSummary();
        addFoodRowToLog(data);
        tvEmptyLog.setVisibility(View.GONE);

        Toast.makeText(getContext(),
            capitalize(data.getFoodName()) + " added to log",
            Toast.LENGTH_SHORT).show();
    }

    private void updateSummary() {
        tvTotalCalories.setText(totalCalories + " kcal");
        tvTotalProtein.setText(String.format("%.1fg", totalProtein));
        tvTotalCarbs.setText(String.format("%.1fg", totalCarbs));
        tvTotalFat.setText(String.format("%.1fg", totalFat));
    }

    private void addFoodRowToLog(NutritionData data) {
        View row = LayoutInflater.from(getContext())
            .inflate(R.layout.item_food_log_row, foodLogContainer, false);

        ((TextView) row.findViewById(R.id.tv_log_food_name))
            .setText(capitalize(data.getFoodName()));
        ((TextView) row.findViewById(R.id.tv_log_calories))
            .setText(data.getCalories() + " kcal");
        ((TextView) row.findViewById(R.id.tv_log_macros))
            .setText(String.format("P: %.0fg  C: %.0fg  F: %.0fg",
                data.getProtein(), data.getCarbs(), data.getFat()));

        foodLogContainer.addView(row);
    }

    private String capitalize(String text) {
        if (text == null || text.isEmpty()) return text;
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }
}