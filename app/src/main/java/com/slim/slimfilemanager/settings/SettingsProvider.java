/*
 * Copyright (C) 2015 The SlimRoms Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.slim.slimfilemanager.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;

public class SettingsProvider {

    public static final String KEY_ENABLE_ROOT = "enable_root";
    public static final String THEME = "app_theme";
    public static final String SORT_MODE = "sort_mode";

    public static SettingsProvider instance;
    public Context mContext;

    public SettingsProvider(Context context) {
        mContext = context;
    }

    public static SettingsProvider getInstance(Context context) {
        if (instance == null) {
            instance = new SettingsProvider(context);
        }
        return instance;
    }

    public SharedPreferences get() {
        return PreferenceManager.getDefaultSharedPreferences(mContext);
    }

    public SharedPreferences.Editor put() {
        return get().edit();
    }

    public String getString(String key, String defValue) {
        return get().getString(key, defValue);
    }

    public boolean getBoolean(String key, boolean defValue) {
        return get().getBoolean(key, defValue);
    }

    public int getInt(String key, int defValue) {
        int i;
        try {
            i = get().getInt(key, defValue);
        } catch (Exception e) {
            i = Integer.parseInt(get().getString(key, Integer.toString(defValue)));
        }
        return i;
    }

    public void putInt(String key, int value) {
        put().putInt(key, value).commit();
    }

    public ArrayList<String> getListString(String key, ArrayList<String> def) {
        ArrayList<String> array = new ArrayList<>(
                Arrays.asList(TextUtils.split(get().getString(key, ""), "‚‗‚")));
        if (array.isEmpty()) {
            array.addAll(def);
        }
        return array;
    }

    public void putListString(String key, ArrayList<String> stringList) {
        String[] myStringList = stringList.toArray(new String[stringList.size()]);
        put().putString(key, TextUtils.join("‚‗‚", myStringList)).apply();
    }
}