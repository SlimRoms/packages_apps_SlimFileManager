package com.slim.slimfilemanager;

import android.app.Activity;
import android.os.Bundle;

import com.slim.slimfilemanager.settings.SettingsProvider;

public class ThemeActivity extends Activity {

    private int mCurrentTheme;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCurrentTheme = SettingsProvider.getInt(this,
                SettingsProvider.THEME, R.style.AppTheme);

        setTheme(mCurrentTheme);
    }

    @Override
    public void onResume() {
        super.onResume();

        int newTheme = SettingsProvider.getInt(this, SettingsProvider.THEME,
                R.style.AppTheme);

        if (mCurrentTheme != newTheme) {
            recreate();
        }
    }
}
