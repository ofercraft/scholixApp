package com.scholix.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

public class AccountActivity extends AppCompatActivity {
    private LinearLayout itemContainer;
    private TextView label, value, todayTitle;
    private MaterialButton btnLogout;
    private BottomNavigationView bottomNavigation;
    @Override protected int getLayoutResourceId() { return R.layout.activity_account; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account); // Make sure this matches your XML file name
//
//        // Initialize views
//        itemContainer = findViewById(R.id.item_container);
//        label = findViewById(R.id.label);
//        value = findViewById(R.id.value);
//        todayTitle = findViewById(R.id.today_title);
//        btnLogout = findViewById(R.id.btn_logout);
//        bottomNavigation = findViewById(R.id.bottom_navigation);
//
//        // Set up click for the row (platforms)
//        itemContainer.setOnClickListener(v -> {
//            // Navigate to platforms page (replace PlatformsActivity with your actual class)
//            Intent intent = new Intent(AccountActivity.this, PlatformsActivity.class);
//            startActivity(intent);
//        });
//
//        // Set up logout button
//        btnLogout.setOnClickListener(v -> {
//            Toast.makeText(AccountActivity.this, "Logged out", Toast.LENGTH_SHORT).show();
//            // Add actual logout logic here if needed
//
//            // Example: return to login activity
//            // Intent intent = new Intent(AccountActivity.this, LoginActivity.class);
//            // startActivity(intent);
//            // finish();
//        });
//
//        // Bottom navigation item selection
//        bottomNavigation.setOnItemSelectedListener(item -> {
//            switch (item.getItemId()) {
//                case R.id.menu_home:
//                    startActivity(new Intent(AccountActivity.this, HomeActivity.class));
//                    return true;
//                case R.id.menu_classes:
//                    startActivity(new Intent(AccountActivity.this, ClassesActivity.class));
//                    return true;
//                case R.id.menu_account:
//                    // Already on this screen
//                    return true;
//                default:
//                    return false;
//            }
//        });
//
//        // Mark the current menu item as selected
//        bottomNavigation.setSelectedItemId(R.id.menu_account);
    }
}