package com.totalhealthdashboard.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.totalhealthdashboard.R;
import com.totalhealthdashboard.ui.dashboard.DashboardFragment;
import com.totalhealthdashboard.ui.physical.PhysicalFragment;
import com.totalhealthdashboard.ui.diet.DietFragment;
import com.totalhealthdashboard.ui.mental.MentalFragment;
import com.totalhealthdashboard.ui.journal.JournalFragment;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        // Load dashboard by default
        if (savedInstanceState == null) {
            loadFragment(new DashboardFragment());
        }

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
                fragment = new JournalFragment();
            }
            loadFragment(fragment);
            return true;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit();
    }
}