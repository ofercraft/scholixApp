package com.scholix.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class GradesActivity extends BaseActivity {

    private RecyclerView gradesRecyclerView;
    private GradeAdapter gradeAdapter;
    private List<Grade> gradeList;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grades);

        Log.d("NAV_DEBUG", "GradesActivity started");

        gradesRecyclerView = findViewById(R.id.grades_recycler_view);
        gradesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        gradeList = new ArrayList<>();
        gradeAdapter = new GradeAdapter(gradeList);
        gradesRecyclerView.setAdapter(gradeAdapter);

        // Setup account menu
        ImageButton accountButton = findViewById(R.id.account_button);
        if (accountButton != null) {
            accountButton.setOnClickListener(this::showAccountPopup);
        }

        prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);

        // Load accounts list
        String json = prefs.getString("accounts_list", null);
        List<Account> savedAccounts = new ArrayList<>();

        if (json != null) {
            Gson gson = new Gson();
            Type type = new TypeToken<ArrayList<Account>>() {}.getType();
            savedAccounts = gson.fromJson(json, type);
        }

        // Read global cookies and studentId for now (for Webtop accounts, until individual cookies saved)
        String savedCookies = prefs.getString("cookies", "");
        String studentId = prefs.getString("student_id", "");

        // Loop through all accounts
        for (Account account : savedAccounts) {
            if (account.getSource().equals("Webtop")) {
                // Fetch Webtop grades, pass year from Account
                WebtopGradeFetcher webtopFetcher = new WebtopGradeFetcher(savedCookies, studentId, account.getYear());
                new Thread(() -> {
                    List<Grade> webtopGrades = webtopFetcher.fetchGrades("b");
                    runOnUiThread(() -> {
                        gradeList.addAll(webtopGrades);
                        gradeAdapter.notifyDataSetChanged();
                    });
                }).start();
            } else if (account.getSource().equals("Bar Ilan")) {
                // Fetch Bar Ilan grades, year passed to BarilanGradeFetcher
                BarilanGradeFetcher barilanFetcher = new BarilanGradeFetcher(this, account.getUsername(), account.getPassword(), account.getYear(), barilanGrades -> runOnUiThread(() -> {
                    gradeList.addAll(barilanGrades);
                    gradeAdapter.notifyDataSetChanged();
                }));
            }
        }

        // Bottom Navigation
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        setupBottomNavigation(bottomNavigationView);
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_grades;
    }

    // Shows the account popup menu
    private void showAccountPopup(View anchor) {
        PopupMenu popupMenu = new PopupMenu(GradesActivity.this, anchor);
        popupMenu.getMenuInflater().inflate(R.menu.popup_menu, popupMenu.getMenu());

        // Force show icons
        try {
            Field[] fields = popupMenu.getClass().getDeclaredFields();
            for (Field field : fields) {
                if ("mPopup".equals(field.getName())) {
                    field.setAccessible(true);
                    Object menuPopupHelper = field.get(popupMenu);
                    Class<?> classPopupHelper = Class.forName(menuPopupHelper.getClass().getName());
                    Method setForceIcons = classPopupHelper.getMethod("setForceShowIcon", boolean.class);
                    setForceIcons.invoke(menuPopupHelper, true);
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Menu actions
        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.menu_username:
                    Toast.makeText(GradesActivity.this, "Username clicked", Toast.LENGTH_SHORT).show();
                    return true;
                case R.id.menu_settings:
                    startActivity(new Intent(GradesActivity.this, SettingsActivity.class));
                    Toast.makeText(GradesActivity.this, "Settings clicked", Toast.LENGTH_SHORT).show();
                    return true;
                case R.id.menu_logout:
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.clear();
                    editor.apply();
                    Toast.makeText(GradesActivity.this, "Logged out", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(GradesActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                    return true;
                default:
                    return false;
            }
        });
        popupMenu.show();
    }
}
