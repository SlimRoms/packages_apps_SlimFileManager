package com.slim.slimfilemanager.widget;

import android.content.Context;
import android.support.v4.widget.DrawerLayout;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;

public class CustomDrawerLayout extends DrawerLayout {

    private ArrayList<DrawerListener> mListeners = new ArrayList<>();

    public CustomDrawerLayout(Context context) {
        super(context);
        addListener(mBaseListener);
    }

    public CustomDrawerLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        addListener(mBaseListener);
    }

    public CustomDrawerLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setDrawerListener(mBaseListener);
    }

    public void addListener(DrawerListener listener) {
        if (mListeners.contains(listener)) {
            return;
        }
        mListeners.add(listener);
    }

    public void removeListener(DrawerListener listener) {
        if (mListeners.contains(listener)) {
            mListeners.remove(listener);
        }
    }

    private DrawerListener mBaseListener = new DrawerListener() {
        @Override
        public void onDrawerSlide(View drawerView, float slideOffset) {
            if (mListeners.size() > 0) {
                for (DrawerListener listener : mListeners) {
                    listener.onDrawerSlide(drawerView, slideOffset);
                }
            }
        }

        @Override
        public void onDrawerOpened(View drawerView) {
            if (mListeners.size() > 0) {
                for (DrawerListener listener : mListeners) {
                    listener.onDrawerOpened(drawerView);
                }
            }
        }

        @Override
        public void onDrawerClosed(View drawerView) {
            if (mListeners.size() > 0) {
                for (DrawerListener listener : mListeners) {
                    listener.onDrawerClosed(drawerView);
                }
            }
        }

        @Override
        public void onDrawerStateChanged(int newState) {
            if (mListeners.size() > 0) {
                for (DrawerListener listener : mListeners) {
                    listener.onDrawerStateChanged(newState);
                }
            }
        }
    };
}
