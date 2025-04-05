package com.scholix.app;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class AuthenticatorService extends Service {
    private Authenticator mAuthenticator;

    @Override
    public void onCreate() {
        // Initialize your authenticator with the service's context
        mAuthenticator = new Authenticator(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }
}
