package com.slim.slimfilemanager;

import android.app.Activity;
import android.content.Intent;
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
            updateTheme();
        }
    }

    protected void updateTheme() {
        final Bundle outState = new Bundle();
        onSaveInstanceState(outState);
        final Intent intent = new Intent(this, getClass());
        intent.putExtras(outState);
        finish();
        overridePendingTransition(0, 0);
        startActivity(intent);
    }
}
