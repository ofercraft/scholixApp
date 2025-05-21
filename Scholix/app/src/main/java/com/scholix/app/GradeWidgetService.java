package com.scholix.app;

import android.content.Intent;
import android.widget.RemoteViewsService;

public class GradeWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new GradeRemoteViewsFactory(getApplicationContext(), intent);
    }
}
