package com.slim.slimfilemanager;

import android.app.Application;

import com.slim.slimfilemanager.settings.SettingsProvider;
import com.squareup.leakcanary.LeakCanary;

public class FileManagerApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        SettingsProvider.getInstance(getApplicationContext());
        LeakCanary.install(this);
    }

    public void getEventHandler() {

    }
}
