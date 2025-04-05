package com.scholix.app;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends BaseActivity {

    private RecyclerView accountsRecyclerView;
    private FloatingActionButton addAccountFab;
    private AccountAdapter accountAdapter;
    private SharedPreferences prefs;
    private List<Account> accountList;
    private Gson gson = new Gson();

    private static final String ACCOUNTS_KEY = "accounts_list";
    private static final String[] sources = {"Classroom", "Bar Ilan", "Webtop"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);

        accountsRecyclerView = findViewById(R.id.accounts_recycler_view);

        TopSnappingLinearLayoutManager layoutManager = new TopSnappingLinearLayoutManager(this);
        accountsRecyclerView.setLayoutManager(layoutManager);
        accountsRecyclerView.setClipToPadding(false);
        accountsRecyclerView.setPadding(0, 0, 0, 500);

        loadAccounts();

        accountAdapter = new AccountAdapter(accountList, position -> {
            accountList.remove(position);
            saveAccounts();
            accountAdapter.notifyDataSetChanged();
            Toast.makeText(this, "Account Deleted", Toast.LENGTH_SHORT).show();
        }, () -> {
            saveAccounts();
            Toast.makeText(this, "Account Saved", Toast.LENGTH_SHORT).show();
        });

        accountsRecyclerView.setAdapter(accountAdapter);

        addAccountFab = findViewById(R.id.add_account_fab);
        addAccountFab.setOnClickListener(v -> showAddAccountDialog());

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        setupBottomNavigation(bottomNavigationView);
    }

    private void loadAccounts() {
        String json = prefs.getString(ACCOUNTS_KEY, null);
        if (json != null) {
            Type type = new TypeToken<ArrayList<Account>>() {}.getType();
            accountList = gson.fromJson(json, type);
        } else {
            accountList = new ArrayList<>();
        }
    }

    private void saveAccounts() {
        String json = gson.toJson(accountList);
        System.out.println(json);
        System.out.println(json);
        System.out.println(json);
        System.out.println(json);
        System.out.println(json);
        System.out.println(json);
        System.out.println(json);








        prefs.edit().putString(ACCOUNTS_KEY, json).apply();
    }

    private void showAddAccountDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_account, null);
        EditText usernameInput = dialogView.findViewById(R.id.dialog_username);
        EditText passwordInput = dialogView.findViewById(R.id.dialog_password);
        Spinner sourceSpinner = dialogView.findViewById(R.id.dialog_source_spinner);
        Spinner yearSpinner = dialogView.findViewById(R.id.dialog_year_spinner);

        // Setup source spinner
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, sources);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sourceSpinner.setAdapter(spinnerAdapter);

        // Setup year spinner
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new String[]{"1", "2", "3"});
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        yearSpinner.setAdapter(yearAdapter);

        // Show year spinner only if Bar Ilan selected
        sourceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedSource = sourceSpinner.getSelectedItem().toString();
                if (selectedSource.equals("Bar Ilan")) {
                    yearSpinner.setVisibility(View.VISIBLE);
                } else {
                    yearSpinner.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Add Account")
                .setView(dialogView)
                .setPositiveButton("Add", null)
                .setNegativeButton("Cancel", null)
                .create();
        dialog.getWindow().setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.rounded_menu));

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String username = usernameInput.getText().toString().trim();
                String password = passwordInput.getText().toString().trim();
                String selectedSource = sourceSpinner.getSelectedItem().toString();

                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (selectedSource.equals("Webtop")) {
                    // Validate Webtop login
                    new Thread(() -> {
                        try {
                            LoginManager loginManager = new LoginManager();
                            LoginManager.LoginResult result = loginManager.validateLogin(username, password);

                            runOnUiThread(() -> {
                                if (result.success) {
                                    Account newAccount = new Account(username, password, selectedSource);
                                    accountList.add(newAccount);
                                    saveAccounts();
                                    accountAdapter.notifyDataSetChanged();
                                    Toast.makeText(this, "Webtop Account Added", Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                } else {
                                    Toast.makeText(this, "Webtop Login Failed: " + result.message, Toast.LENGTH_SHORT).show();
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                            runOnUiThread(() -> Toast.makeText(this, "Login Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        }
                    }).start();
                } else {
                    // Bar Ilan or Classroom
                    Account newAccount = new Account(username, password, selectedSource);
                    if (selectedSource.equals("Bar Ilan")) {
                        int selectedYear = Integer.parseInt(yearSpinner.getSelectedItem().toString());
                        newAccount.setYear(selectedYear);
                    }
                    accountList.add(newAccount);
                    saveAccounts();
                    accountAdapter.notifyDataSetChanged();
                    Toast.makeText(this, "Account Added", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }
            });
        });

        dialog.show();
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_settings;
    }
}
