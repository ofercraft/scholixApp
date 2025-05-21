package com.scholix.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class GradesWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int widgetId : appWidgetIds) {
            System.out.println("hi there");
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_grades);

            Intent serviceIntent = new Intent(context, GradeWidgetService.class);
            views.setRemoteAdapter(R.id.widget_grade_list, serviceIntent);

            // Set last updated time
            String formattedTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
            views.setTextViewText(R.id.widget_title, "ציונים" + " " + formattedTime);

            // Click to open GradesActivity
            Intent openIntent = new Intent(context, GradesActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, openIntent, PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widget_title, pendingIntent);

            appWidgetManager.updateAppWidget(widgetId, views);
        }

    }

    private static void updateSingleWidget(Context context, AppWidgetManager appWidgetManager, int widgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_grades);

        // ⏰ Set formatted time
        String formattedTime = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                .format(new java.util.Date());
        views.setTextViewText(R.id.widget_title, "ציונים" + " " + formattedTime);

        // 🧩 Set adapter for ListView
        Intent serviceIntent = new Intent(context, GradeWidgetService.class);
        views.setRemoteAdapter(R.id.widget_grade_list, serviceIntent);

        // 🟢 Click to open GradesActivity
        Intent openAppIntent = new Intent(context, GradesActivity.class);
        PendingIntent openPendingIntent = PendingIntent.getActivity(context, 0, openAppIntent, PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_title, openPendingIntent);

        // 🚀 Push update
        appWidgetManager.updateAppWidget(widgetId, views);
    }


    // ✅ CALL THIS TO FORCE UPDATE ANYWHERE IN YOUR APP
    public static void forceWidgetUpdate(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName componentName = new ComponentName(context, GradesWidget.class);
        int[] widgetIds = appWidgetManager.getAppWidgetIds(componentName);

        for (int widgetId : widgetIds) {
            System.out.println("1");
            updateSingleWidget(context, appWidgetManager, widgetId);
        }
    }
}
