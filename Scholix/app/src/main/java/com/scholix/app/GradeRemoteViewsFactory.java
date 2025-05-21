package com.scholix.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Looper;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class GradeRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private final Context context;
    private final Intent intent;
    private List<Grade> gradeList = new ArrayList<>();

    public GradeRemoteViewsFactory(Context context, Intent intent) {
        this.context = context;
        this.intent = intent;
    }

    @Override
    public void onCreate() {
        fetchGradesNow();  // Initial load
    }

    @Override
    public void onDataSetChanged() {
        fetchGradesNow();  // Reload on update
    }

    private void fetchGradesNow() {
        gradeList.clear();

        SharedPreferences prefs = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE);
        String savedCookies = prefs.getString("cookies", "");
        String studentId = prefs.getString("student_id", "");
        String json = prefs.getString("accounts_list", null);

        if (json == null) return;

        Type type = new TypeToken<ArrayList<Account>>() {}.getType();
        List<Account> accounts = new Gson().fromJson(json, type);

        if (accounts == null || accounts.isEmpty()) return;

        CountDownLatch latch = new CountDownLatch(accounts.size());
        List<Grade> allGrades = Collections.synchronizedList(new ArrayList<>());

        for (Account account : accounts) {
            if ("Webtop".equals(account.getSource())) {
                new Thread(() -> {
                    WebtopGradeFetcher fetcher = new WebtopGradeFetcher(savedCookies, studentId, account.getYear());
                    List<Grade> grades = fetcher.fetchGrades("b");
                    Collections.reverse(grades);
                    allGrades.addAll(grades);
                    latch.countDown();
                }).start();
            } else if ("Bar Ilan".equals(account.getSource())) {
                BarilanGradeFetcher fetcher = new BarilanGradeFetcher(context, account.getUsername(), account.getPassword(), account.getYear(), grades -> {
                    allGrades.addAll(grades);
                    latch.countDown();
                });
            }
        }

        try {
            latch.await();  // Wait for all fetchers to complete
            gradeList.addAll(allGrades);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        gradeList.add(new Grade("","",""));
        gradeList.add(new Grade("","",""));
        gradeList.add(new Grade("","",""));

    }

    @Override
    public int getCount() {
        return gradeList.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        Grade grade = gradeList.get(position);

        // Split course name: first word = subject, rest = title
        String subject = grade.getSubject();
        String titleSmall = grade.getName();


        RemoteViews rv = new RemoteViews(context.getPackageName(),
                R.layout.widget_grade_item);

        rv.setTextViewText(R.id.text_grade,    String.valueOf(grade.getGrade()));
        rv.setTextViewText(R.id.text_subject,  subject);
        rv.setTextViewText(R.id.text_title_small, titleSmall);

        return rv;
    }



    @Override public RemoteViews getLoadingView() { return null; }
    @Override public int getViewTypeCount() { return 1; }
    @Override public long getItemId(int position) { return position; }
    @Override public boolean hasStableIds() { return true; }
    @Override public void onDestroy() {}
}
