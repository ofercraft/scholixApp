package com.scholix.app;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.LocaleList;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;
import android.widget.Button;
import android.widget.EditText;
import android.view.View;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private LoginManager loginManager = new LoginManager();
    public static void applyAppLocale(Context context, String langCode) {
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);
        Configuration config = context.getResources().getConfiguration();
        config.setLocale(locale);
        context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
//        Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
//        this.startActivity(intent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        1001  // requestCode
                );
            }
        }

        // Schedule grade fetch every 15 min
        PeriodicWorkRequest request = new PeriodicWorkRequest
                .Builder(GradeSyncWorker.class, 15, java.util.concurrent.TimeUnit.MINUTES)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "grade_sync", ExistingPeriodicWorkPolicy.REPLACE, request);

        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        String savedUsername = prefs.getString("username", null);
        String savedPassword = prefs.getString("password", null);

        if (savedUsername != null && savedPassword != null) {

            // Clear cookies ONLY when first launching the app (not when returning to MainActivity)
            if (isTaskRoot()) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.remove("cookies");
                editor.apply(); // apply changes
                autoLogin(savedUsername, savedPassword);
//                addAccount(savedUsername, savedPassword, "Main");
                Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                startActivity(intent);
                finish();


            }
        }
        else {

            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
        }
    }
    private void addAccount(String username, String password, String name) {
        Account newAccount = new Account(username, password, "Webtop", name);
        List<Account> accountList;
        Gson gson = new Gson();

        final String ACCOUNTS_KEY = "accounts_list";
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);

        String json = prefs.getString(ACCOUNTS_KEY, null);
        if (json != null) {
            Type type = new TypeToken<ArrayList<Account>>() {}.getType();
            accountList = gson.fromJson(json, type);
        } else {
            accountList = new ArrayList<>();
        }

        // ✅ Check for existing account with same username and source
        for (Account acc : accountList) {
            if (acc.getUsername().equals(username) && acc.getSource().equals("Webtop")) {
                return; // already exists, don't add
            }
        }

        accountList.add(newAccount);
        String json2 = gson.toJson(accountList);
        prefs.edit().putString(ACCOUNTS_KEY, json2).apply();
    }


    private void autoLogin(String username, String password) {
        new Thread(() -> {
            try {
                final LoginManager.LoginResult result = loginManager.validateLogin(username, password);
                Object[] details = (Object[]) result.details;
                @SuppressWarnings("unchecked")
                java.util.List<String> cookieList = (java.util.List<String>) details[0];
                String cookies = String.join("; ", cookieList);
                String studentId = (String) details[1];
                String info = details[2].toString();
                String classCode = (String) details[3];
                String institution = (String) details[4];
                String name = (String) details[5];

                // Save all info to SharedPreferences for persistent storage
                SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("cookies", cookies);
                editor.putString("student_id", studentId);
                editor.putString("info", info);
                editor.putString("class_code", classCode);
                editor.putString("institution", institution);
                editor.putString("name", name);
                editor.putString("username", username);
                editor.putString("password", password);
                editor.apply();
                runOnUiThread(() -> {
                    if (result.success) {
                        startActivity(new Intent(MainActivity.this, HomeActivity.class));
                        finish();
                    } else {
                        Toast.makeText(MainActivity.this, "Auto-login failed", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }















}