package com.scholix.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class GradeSyncWorker extends Worker {

    private static final String PREF_KEY = "latest_grades";
    private static final String CHANNEL_ID = "grade_sync_channel";

    public GradeSyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        SharedPreferences prefs = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        Gson gson = new Gson();

        // Load previously saved grades
        List<Grade> previousGrades = new ArrayList<>();
        String oldJson = prefs.getString(PREF_KEY, null);
        if (oldJson != null) {
            Type type = new TypeToken<ArrayList<Grade>>() {}.getType();
            previousGrades = gson.fromJson(oldJson, type);
        }

        // Fetch current grades from Webtop + Bar-Ilan
        List<Grade> newGrades = fetchGrades(context, prefs);
        if (newGrades == null) return Result.failure();

        // Merge logic: keep all old grades, add new ones that aren't already saved
        List<Grade> mergedGrades = new ArrayList<>(previousGrades);
        int newCount = 0;
        for (Grade g : newGrades) {
            if (!previousGrades.contains(g)) {
                mergedGrades.add(g);
                newCount++;
            }
        }

        // Only update storage and notify if there's something new
        if (newCount > 0) {
            prefs.edit().putString(PREF_KEY, gson.toJson(mergedGrades)).apply();
            showNotification(context, newCount);
        }

        // Always refresh the widget
        GradesWidget.forceWidgetUpdate(context);
        return Result.success();
    }


    private List<Grade> fetchGrades(Context context, SharedPreferences prefs) {
//        String cookies = prefs.getString("cookies", "");
//        String studentId = prefs.getString("student_id", "");
        String json = prefs.getString("accounts_list", null);
        if (json == null) return null;

        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<Account>>() {}.getType();
        List<Account> accounts = gson.fromJson(json, type);
        if (accounts == null || accounts.isEmpty()) return null;

        List<Grade> allGrades = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(accounts.size());

        for (Account account : accounts) {
            if ("Webtop".equals(account.getSource())) {
                new Thread(() -> {
                    try {
                        LoginManager loginManager = new LoginManager();
                        LoginManager.LoginResult result = loginManager.validateLogin(account.getUsername(), account.getPassword());

                        if (result.success) {
                            Object[] details = (Object[]) result.details;
                            @SuppressWarnings("unchecked")
                            List<String> cookieList = (List<String>) details[0];
                            String cookies = String.join("; ", cookieList);
                            String studentId = (String) details[1];

                            // ✅ Store cookies & studentId if needed (or skip)
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putString("cookies", cookies); // or cookies:<username> for multi-account
                            editor.putString("student_id", studentId);
                            editor.apply();

                            WebtopGradeFetcher fetcher = new WebtopGradeFetcher(cookies, studentId, account.getYear());
                            List<Grade> grades = fetcher.fetchGrades("b");

                            allGrades.addAll(grades);
                        } else {
                            Log.w("GradeSync", "Login failed for " + account.getUsername());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        latch.countDown();
                    }
                }).start();
            } else if ("Bar Ilan".equals(account.getSource())) {
                new BarilanGradeFetcher(context, account.getUsername(), account.getPassword(), account.getYear(), grades -> {
                    allGrades.addAll(grades);
                    latch.countDown();
                });
            }
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            return null;
        }

        return allGrades;
    }

    private void showNotification(Context context, int newCount) {
        String title = "📚 New Grades!";
        String message = newCount > 0
                ? "You got " + newCount + " new grade" + (newCount > 1 ? "s!" : "!")
                : "Grades updated";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Grade Updates", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_logo)
                .setAutoCancel(true);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) manager.notify(999, builder.build());
    }
}
