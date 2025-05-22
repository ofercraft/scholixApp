package com.scholix.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Consumer;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AccountActivity extends BaseActivity {
    private LinearLayout itemContainer;
    private TextView label, value, todayTitle;
    private OkHttpClient client = UnsafeOkHttpClient.getUnsafeOkHttpClient();

    private MaterialButton btnLogout;
    private ImageView platformArrow;
    private SharedPreferences prefs;

    private BottomNavigationView bottomNavigation;
    protected int getLayoutResourceId() { return R.layout.activity_account; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutResourceId()); // Make sure this matches your XML file name



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


        // Bind TextViews
        TextView nameValue = findViewById(R.id.value_name);
        TextView phoneValue = findViewById(R.id.value_phone);
        TextView emailValue = findViewById(R.id.value_email);

        LinearLayout phoneGroup = (LinearLayout) findViewById(R.id.label_phone).getParent();
        LinearLayout emailGroup = (LinearLayout) findViewById(R.id.label_email).getParent();

        fetchAccountInfo(this,
                grades -> { /* handle grades if needed */ },
                info -> runOnUiThread(() -> {
                    String name = info[2];
                    String phone = info[0];
                    String email = info[1];

                    nameValue.setText(name);

                    if (phone.isEmpty()) {
                        phoneGroup.setVisibility(View.GONE);
                    } else {
                        phoneValue.setText(phone);
                        phoneGroup.setVisibility(View.VISIBLE);
                    }

                    if (email.isEmpty()) {
                        emailGroup.setVisibility(View.GONE);
                    } else {
                        emailValue.setText(email);
                        emailGroup.setVisibility(View.VISIBLE);
                    }
                })
        );


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


//
//
//
//
//        findViewById(R.id.btn_switch_language).setOnClickListener(v -> {
//            String current = LocaleHelper.getSavedLanguage(this);
//            String next = current.equals("he") ? "en" : "he";
//
//            LocaleHelper.setSavedLanguage(this, next);
//
//            Intent intent = getIntent();
//            finish();
//            startActivity(intent);
//            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
//        });


    }

    public void fetchAccountInfo(Context context, Consumer<List<Grade>> gradesCallback, Consumer<String[]> infoCallback) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            List<Grade> gradeList = new ArrayList<>();
            String[] returnValues = {"", "", ""}; // phone, email, name

            try {
                SharedPreferences prefs = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
                String savedCookies = prefs.getString("cookies", "");

                JSONObject requestBodyJson = new JSONObject();
                RequestBody body = RequestBody.create(
                        requestBodyJson.toString(),
                        MediaType.get("application/json; charset=utf-8")
                );

                Request request = new Request.Builder()
                        .url("https://webtopserver.smartschool.co.il/server/api/settings/GetUserProfile")
                        .addHeader("Cookie", savedCookies)
                        .post(body)
                        .build();

                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    JSONObject data = jsonResponse.getJSONObject("data");

                    String name = prefs.getString("name", ""); // or from prefs if more accurate
                    String phone = data.optString("cellphone", "");
                    String email = data.optString("email", "");
                    returnValues = new String[]{phone, email, name};

                    Log.d("AccountInfo", "Profile: " + data.toString());
                } else {
                    Log.e("AccountInfo", "Failed with code: " + response.code());
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            // Post results back to UI thread
            String[] finalReturnValues = returnValues;
            mainHandler.post(() -> {
                gradesCallback.accept(gradeList);
                infoCallback.accept(finalReturnValues);
            });
        });
    }


}