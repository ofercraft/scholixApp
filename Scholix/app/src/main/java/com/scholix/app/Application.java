package com.scholix.app;

import android.content.Context;

public class Application extends android.app.Application {

    private static Application applicationInstance;

    public static synchronized Application getInstance() {
        return applicationInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        applicationInstance = this;
    }

    public void initAppLanguage(Context context) {
        LocaleUtils.initialize(context, LocaleUtils.getSelectedLanguageId());
    }
}
