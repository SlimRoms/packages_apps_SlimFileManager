package com.slim.slimfilemanager.settings;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;

import com.slim.slimfilemanager.R;
import com.slim.slimfilemanager.utils.RootUtils;
import com.slim.utils.Constant;

public class SettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);

        if (!RootUtils.isRootAvailable()) {
            getPreferenceScreen().removePreference(
                    findPreference(SettingsProvider.KEY_ENABLE_ROOT));
        } else {
            findPreference(SettingsProvider.KEY_ENABLE_ROOT)
                    .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            if (!((SwitchPreference) preference).isChecked()) {
                                RootUtils.runCommand("");
                            }
                            return true;
                        }
                    });
        }

        String[] entries = new String[]{
                getActivity().getString(R.string.light),
                getActivity().getString(R.string.dark)
        };
        String[] values = new String[]{
                Integer.toString(R.style.AppTheme),
                Integer.toString(R.style.AppTheme_Dark)
        };

        String value = Integer.toString(SettingsProvider.getInt(getActivity(),
                SettingsProvider.THEME, R.style.AppTheme));

        ListPreference theme = (ListPreference) findPreference("key_theme");
        theme.setEntries(entries);
        theme.setEntryValues(values);
        theme.setValue(value);
        theme.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                SettingsProvider.putInt(getActivity(), SettingsProvider.THEME,
                        Integer.parseInt((String) newValue));
                ((SettingsActivity) getActivity()).onUpdateTheme();
                return true;
            }
        });

        value = SettingsProvider.getString(getActivity(), SettingsProvider.EDITOR_ENCODING,
                Constant.DEFAULT_ENCODING);

        ListPreference encoding = (ListPreference) findPreference(SettingsProvider.EDITOR_ENCODING);
        encoding.setEntries(Constant.ENCODINGS);
        encoding.setEntryValues(Constant.ENCODINGS);
        encoding.setValue(value);
    }
}
