package com.scholix.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

public class AccountActivity extends BaseActivity {
    private LinearLayout itemContainer;
    private TextView label, value, todayTitle;
    private MaterialButton btnLogout;
    private ImageView platformArrow;

    private BottomNavigationView bottomNavigation;
    protected int getLayoutResourceId() { return R.layout.activity_account; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account); // Make sure this matches your XML file name



        // Bottom navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        setupBottomNavigation(bottomNav);
//
//        platformArrow = findViewById(R.id.platform_arrow);
//
//        platformArrow.setOnClickListener(v -> {
//            // Handle click on the arrow
//            Toast.makeText(AccountActivity.this, "Platform arrow clicked", Toast.LENGTH_SHORT).show();
//
//            // Example: navigate to platform screen
//            Intent intent = new Intent(AccountActivity.this, SettingsActivity.class);
//            startActivity(intent);
//        });



        findViewById(R.id.platforms_container).setOnClickListener(v -> {
            Intent intent = new Intent(AccountActivity.this, PlatformsActivity.class);
            startActivity(intent);
        });


        MaterialButton logoutButton = findViewById(R.id.btn_logout);
        logoutButton.setOnClickListener(v -> {
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.removeAllCookies(new ValueCallback<Boolean>() {
                @Override
                public void onReceiveValue(Boolean value) {
                    cookieManager.flush(); // Ensure it's saved

                    getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
                            .edit()
                            .clear()
                            .apply();
                    Intent intent = new Intent(AccountActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear back stack
                    startActivity(intent);
                }
            });
        });


//        itemContainer.setOnClickListener(v -> {
//            // Navigate to platforms page (replace PlatformsActivity with your actual class)
//            Intent intent = new Intent(AccountActivity.this, SettingsActivity.class);
//            startActivity(intent);
//        });
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