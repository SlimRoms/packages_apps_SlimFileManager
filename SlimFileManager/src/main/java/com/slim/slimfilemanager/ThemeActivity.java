package com.slim.slimfilemanager;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.slim.slimfilemanager.settings.SettingsProvider;

public class ThemeActivity extends AppCompatActivity {

    private int mCurrentTheme;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mCurrentTheme = SettingsProvider.getInt(this,
                SettingsProvider.THEME, R.style.AppTheme);

        setTheme(mCurrentTheme);

        super.onCreate(savedInstanceState);

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
