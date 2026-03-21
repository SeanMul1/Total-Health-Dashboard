package com.totalhealthdashboard.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.totalhealthdashboard.R;
import com.totalhealthdashboard.ui.dashboard.DashboardFragment;
import com.totalhealthdashboard.ui.diet.DietFragment;
import com.totalhealthdashboard.ui.goals.GoalsFragment;
import com.totalhealthdashboard.ui.mental.MentalFragment;
import com.totalhealthdashboard.ui.physical.PhysicalFragment;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private long backPressedTime = 0;
    private BottomNavigationView bottomNav;
    private TextView btnBackArrow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav    = findViewById(R.id.bottom_navigation);
        btnBackArrow = findViewById(R.id.btn_back_arrow_main);

        // Set today's date
        TextView tvDate = findViewById(R.id.tv_top_date);
        String date = new SimpleDateFormat("EEE, dd MMM", Locale.getDefault())
                .format(new Date());
        tvDate.setText(date);

        btnBackArrow.setOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed());

        loadFragment(new DashboardFragment(), false);
        updateHeaderVisibility(R.id.nav_dashboard);

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment;
            int id = item.getItemId();
            if (id == R.id.nav_dashboard) {
                fragment = new DashboardFragment();
            } else if (id == R.id.nav_physical) {
                fragment = new PhysicalFragment();
            } else if (id == R.id.nav_diet) {
                fragment = new DietFragment();
            } else if (id == R.id.nav_mental) {
                fragment = new MentalFragment();
            } else {
                fragment = new GoalsFragment();
            }
            loadFragment(fragment, false);
            updateHeaderVisibility(id);
            return true;
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();
                    return;
                }
                int selectedId = bottomNav.getSelectedItemId();
                if (selectedId != R.id.nav_dashboard) {
                    bottomNav.setSelectedItemId(R.id.nav_dashboard);
                    return;
                }
                if (backPressedTime + 2000 > System.currentTimeMillis()) {
                    finish();
                } else {
                    backPressedTime = System.currentTimeMillis();
                    Toast.makeText(MainActivity.this,
                            "Press back again to exit", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateHeaderVisibility(int selectedId) {
        if (selectedId == R.id.nav_dashboard) {
            btnBackArrow.setVisibility(View.GONE);
        } else {
            btnBackArrow.setVisibility(View.VISIBLE);
        }
    }

    public void loadFragment(Fragment fragment, boolean addToBackStack) {
        androidx.fragment.app.FragmentTransaction transaction =
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, fragment);
        if (addToBackStack) transaction.addToBackStack(null);
        transaction.commit();
    }
}