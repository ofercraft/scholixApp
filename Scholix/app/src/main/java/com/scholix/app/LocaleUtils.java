package com.scholix.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.preference.PreferenceManager;

import java.util.Locale;

public class LocaleUtils {

    public static final String ENGLISH = "en";
    public static final String HEBREW = "he";

    public static void initialize(Context context, String defaultLanguage) {
        setLocale(context, defaultLanguage);
    }

    public static boolean setLocale(Context context, String language) {
        return updateResources(context, language);
    }

    private static boolean updateResources(Context context, String language) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale);
        Configuration config = context.getResources().getConfiguration();
        config.setLocale(locale);
        context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
        return true;
    }

    public static void setSelectedLanguageId(String id) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(Application.getInstance().getApplicationContext());
        prefs.edit().putString("app_language_id", id).apply();
    }

    public static String getSelectedLanguageId() {
        return PreferenceManager.getDefaultSharedPreferences(Application.getInstance().getApplicationContext())
                .getString("app_language_id", ENGLISH);
    }
}
