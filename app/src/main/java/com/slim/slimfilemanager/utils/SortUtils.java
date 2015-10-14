package com.slim.slimfilemanager.utils;

import android.content.Context;
import android.text.TextUtils;

import com.slim.slimfilemanager.BrowserFragment;
import com.slim.slimfilemanager.settings.SettingsProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class SortUtils {

    public static final String SORT_MODE_NAME = "sort_mode_name";
    public static final String SORT_MODE_SIZE = "sort_mode_size";
    public static final String SORT_MODE_TYPE = "sort_mode_type";

    public static void sort(Context context, ArrayList<BrowserFragment.Item> files) {
        String sortMode = SettingsProvider.getString(context,
                SettingsProvider.SORT_MODE, SORT_MODE_NAME);
        switch (sortMode) {
            case SORT_MODE_SIZE:
                Collections.sort(files, getSizeComparator());
                return;
            case SORT_MODE_TYPE:
                Collections.sort(files, getTypeComparator());
                return;
        }
        Collections.sort(files, getNameComparator());
    }

    private static Comparator<BrowserFragment.Item> getNameComparator() {
        return new Comparator<BrowserFragment.Item>() {
            @Override
            public int compare(BrowserFragment.Item lhs, BrowserFragment.Item rhs) {
                return lhs.name.toLowerCase().compareTo(rhs.name.toLowerCase());
            }
        };
    }

    private static Comparator<BrowserFragment.Item> getSizeComparator() {
        return new Comparator<BrowserFragment.Item>() {
            @Override
            public int compare(BrowserFragment.Item lhs, BrowserFragment.Item rhs) {
                File a = new File(lhs.path);
                File b = new File(rhs.path);

                if (a.isDirectory() && b.isDirectory()) {
                    int al = 0, bl = 0;
                    String[] aList = a.list();
                    String[] bList = b.list();

                    if (aList != null) {
                        al = aList.length;
                    }

                    if (bList != null) {
                        bl = bList.length;
                    }

                    if (al == bl) {
                        return a.getName().toLowerCase().compareTo(a.getName().toLowerCase());
                    }

                    if (al < bl) {
                        return -1;
                    }
                    return 1;
                }

                if (a.isDirectory()) {
                    return -1;
                }

                if (b.isDirectory()) {
                    return 1;
                }

                final long len_a = a.length();
                final long len_b = b.length();

                if (len_a == len_b) {
                    return lhs.name.toLowerCase().compareTo(rhs.name.toLowerCase());
                }

                if (len_a < len_b) {
                    return -1;
                }

                return 1;
            }
        };
    }

    private static Comparator<BrowserFragment.Item> getTypeComparator() {
        return new Comparator<BrowserFragment.Item>() {
            @Override
            public int compare(BrowserFragment.Item lhs, BrowserFragment.Item rhs) {
                File a = new File(lhs.path);
                File b = new File(rhs.path);

                if (a.isDirectory() && b.isDirectory()) {
                    return lhs.name.toLowerCase().compareTo(rhs.name.toLowerCase());
                }

                if (a.isDirectory()) {
                    return -1;
                }

                if (b.isDirectory()) {
                    return 1;
                }

                final String ext_a = FileUtils.getExtension(a);
                final String ext_b = FileUtils.getExtension(b);

                if (TextUtils.isEmpty(ext_a) && TextUtils.isEmpty(ext_b)) {
                    return lhs.name.toLowerCase().compareTo(rhs.name.toLowerCase());
                }

                if (TextUtils.isEmpty(ext_a)) {
                    return -1;
                }

                if (TextUtils.isEmpty(ext_b)) {
                    return 1;
                }

                final int res = ext_a.compareTo(ext_b);
                if (res == 0) {
                    return lhs.name.toLowerCase().compareTo(rhs.name.toLowerCase());
                }
                return res;
            }
        };
    }
}