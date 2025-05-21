package com.scholix.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class GradeScheduler extends Service {

    private Handler handler;
    private Runnable syncRunnable;
    private static final int INTERVAL_MS = 10 * 1000;
    private static final String PREF_KEY = "latest_grades";
    private static final String CHANNEL_ID = "grade_channel";
    private static final int NOTIFY_ID = 123;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("GradeScheduler", "onStartCommand: called");
        startForeground(1, buildNotification());
        Log.d("GradeScheduler", "Foreground started");

        handler = new Handler();
        syncRunnable = () -> {
            Log.d("GradeScheduler", "Syncing grades...");
            fetchAndCompareGrades();
            handler.postDelayed(syncRunnable, INTERVAL_MS);
        };
        handler.post(syncRunnable);

        return START_STICKY;
    }

    private void fetchAndCompareGrades() {
        Context context = getApplicationContext();
        SharedPreferences prefs = context.getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        Gson gson = new Gson();

        // Load old grades
        List<Grade> previousGrades = new ArrayList<>();
        String oldJson = prefs.getString(PREF_KEY, null);
        if (oldJson != null) {
            Type type = new TypeToken<ArrayList<Grade>>() {}.getType();
            previousGrades = gson.fromJson(oldJson, type);
        }

        // Fetch current grades
        List<Grade> newGrades = fetchGrades(context, prefs);
        if (newGrades == null) return;

        // Compare
        boolean changed = !areGradeListsEqual(previousGrades, newGrades);

        if (changed) {
            // Save new list
            prefs.edit().putString(PREF_KEY, gson.toJson(newGrades)).apply();

            // Notify
            showNotification(context, newGrades.size() - previousGrades.size());
        }

        // Update widget regardless
        GradesWidget.forceWidgetUpdate(context);
    }

    private List<Grade> fetchGrades(Context context, SharedPreferences prefs) {
        String cookies = prefs.getString("cookies", "");
        String studentId = prefs.getString("student_id", "");
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
                    WebtopGradeFetcher fetcher = new WebtopGradeFetcher(cookies, studentId, account.getYear());
                    List<Grade> grades = fetcher.fetchGrades("b");
                    allGrades.addAll(grades);
                    latch.countDown();
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
                    CHANNEL_ID, "Grade Notifications", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_logo) // ✅ Your app logo here
                .setAutoCancel(true);

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) manager.notify(NOTIFY_ID, builder.build());
    }

    private boolean areGradeListsEqual(List<Grade> oldList, List<Grade> newList) {
        if (newList.size() != oldList.size()) return false;
        for (Grade grade : newList) {
            if (!oldList.contains(grade)) return false;
        }
        return true;
    }

    private Notification buildNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Grade Sync", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Grade sync is running")
                .setSmallIcon(R.drawable.ic_logo)
                .setOngoing(true)
                .build();
    }

    @Override
    public void onDestroy() {
        if (handler != null && syncRunnable != null) {
            handler.removeCallbacks(syncRunnable);
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
