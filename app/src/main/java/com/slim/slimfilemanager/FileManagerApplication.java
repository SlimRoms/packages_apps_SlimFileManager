package com.slim.slimfilemanager;

import android.app.Application;

import com.squareup.leakcanary.LeakCanary;

public class FileManagerApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        LeakCanary.install(this);
    }
}
