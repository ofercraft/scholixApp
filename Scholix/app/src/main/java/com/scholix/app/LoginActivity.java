package com.scholix.app;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings;
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

public class LoginActivity extends AppCompatActivity {

    private EditText usernameEditText, passwordEditText;
    private Button loginButton;
    private LoginManager loginManager = new LoginManager();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
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

        // Bind UI elements
        usernameEditText = findViewById(R.id.username);
        passwordEditText = findViewById(R.id.password);
        loginButton = findViewById(R.id.login_button);

        // Set login button listener
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String username = usernameEditText.getText().toString().trim();
                final String password = passwordEditText.getText().toString().trim();

                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Enter both username and password", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Perform network login on a background thread
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            final LoginManager.LoginResult result = loginManager.validateLogin(username, password);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (result.success) {
                                        // Retrieve details from LoginManager result
                                        Object[] details = (Object[]) result.details;
                                        @SuppressWarnings("unchecked")
                                        java.util.List<String> cookieList = (java.util.List<String>) details[0];
                                        String cookies = String.join("; ", cookieList);
                                        String studentId = (String) details[1];
                                        String info = details[2].toString();
                                        String classCode = (String) details[3];
                                        String institution = (String) details[4];
                                        String name = (String) details[5];
                                        addAccount(username, password);
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

//                                        Toast.makeText(LoginActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();

                                        // Navigate to HomeActivity
                                        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        Toast.makeText(LoginActivity.this, "Login Failed: " + result.message, Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(LoginActivity.this, "Login error: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                }).start();
            }
        });
    }
    private void addAccount(String username, String password) {
        Account newAccount = new Account(username, password, "Webtop");
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
}