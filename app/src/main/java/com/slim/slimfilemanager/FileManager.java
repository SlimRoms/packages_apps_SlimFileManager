package com.slim.slimfilemanager;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Environment;
import android.os.Bundle;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.text.TextUtils;
import android.util.StateSet;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.slim.slimfilemanager.settings.SettingsActivity;
import com.slim.slimfilemanager.settings.SettingsProvider;
import com.slim.slimfilemanager.utils.FragmentLifecycle;
import com.slim.slimfilemanager.utils.IconCache;
import com.slim.slimfilemanager.utils.PasteTask;
import com.slim.slimfilemanager.widget.PageIndicator;
import com.slim.slimfilemanager.widget.TabPageIndicator;

public class FileManager extends ThemeActivity implements View.OnClickListener {

    private SectionsPagerAdapter mSectionsPagerAdapter;

    private BrowserFragment mFragment;

    private ViewPager mViewPager;
    private ListView mDrawer;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerAdapter mDrawerAdapter;
    private FloatingActionsMenu mActionMenu;
    private FloatingActionButton mPasteButton;

    int mCurrentPosition;
    boolean mMove;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SettingsProvider.get(this)
                .registerOnSharedPreferenceChangeListener(mPreferenceListener);

        setContentView(R.layout.file_manager);

        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setHomeButtonEnabled(true);
        }

        // setup drawer
        mDrawer = (ListView) findViewById(R.id.drawer);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.string.drawer_open, R.string.drawer_close);

        mDrawerLayout.setDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                if (Float.toString(slideOffset).contains("0.1")) {
                    mDrawerAdapter.notifyDataSetChanged();
                    mDrawerAdapter.notifyDataSetInvalidated();
                    mDrawerLayout.invalidate();
                }
                mDrawerToggle.onDrawerSlide(drawerView, slideOffset);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                mDrawerToggle.onDrawerOpened(drawerView);
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                mDrawerToggle.onDrawerClosed(drawerView);
            }

            @Override
            public void onDrawerStateChanged(int newState) {
                mDrawerToggle.onDrawerStateChanged(newState);
            }
        });
        mDrawerToggle.syncState();
        mDrawerAdapter = new DrawerAdapter(this);
        mDrawer.setAdapter(mDrawerAdapter);
        mDrawerAdapter.addItem(getString(R.string.root_title), "/");
        mDrawerAdapter.addItem(getString(R.string.sdcard_title),
                Environment.getExternalStorageDirectory().getPath());
        mDrawerAdapter.addItem(getString(R.string.downloads_title),
                Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS).getPath());
        mDrawerAdapter.addItem(getString(R.string.dcim_title),
                Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DCIM).getPath());
        getExternalSDCard();
        mDrawer.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mFragment.filesChanged(mDrawerAdapter.getPath(position));
                //mSectionsPagerAdapter.addTab(mDrawerAdapter.getPath(position));
                mDrawerLayout.closeDrawers();
            }
        });

        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageScrolled(int position,
                                       float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                FragmentLifecycle fragmentToShow = (FragmentLifecycle)
                        mSectionsPagerAdapter.getItem(position);
                fragmentToShow.onResumeFragment();

                if (mActionMenu != null) {
                    mActionMenu.collapse();
                }

                setCurrentlyDisplayedFragment(
                        (BrowserFragment) mSectionsPagerAdapter.getItem(position));

                FragmentLifecycle fragmentToHide = (FragmentLifecycle)
                        mSectionsPagerAdapter.getItem(mCurrentPosition);
                if (fragmentToHide != null) fragmentToHide.onPauseFragment();

                mCurrentPosition = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        mViewPager.setCurrentItem(SettingsProvider.getInt(this, "current_tab", 0));

        mActionMenu = (FloatingActionsMenu) findViewById(R.id.float_button);
        buildActionButtons();

        mPasteButton = (FloatingActionButton) findViewById(R.id.paste);
        mPasteButton.setIcon(R.drawable.paste);
        mPasteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new PasteTask(FileManager.this, mMove, mFragment.getCurrentPath());
                for (TabItem item : mSectionsPagerAdapter.getItems()) {
                    item.fragment.filesChanged(item.fragment.getCurrentPath());
                }
                //mFragment.filesChanged(mFragment.getCurrentPath());
                setMove(false);
                showPaste(false);
            }
        });
        mPasteButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                PasteTask.SelectedFiles.clearAll();
                setMove(false);
                showPaste(false);
                return true;
            }
        });

        PageIndicator indicator = (PageIndicator) findViewById(R.id.indicator);
        indicator.setViewPager(mViewPager);

        TabPageIndicator tabPageIndicator = (TabPageIndicator) findViewById(R.id.tab_indicator);
        tabPageIndicator.setViewPager(mViewPager);

        if (savedInstanceState == null) {
            mDrawerLayout.openDrawer(mDrawer);
            mDrawerToggle.syncState();
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        setCurrentlyDisplayedFragment((BrowserFragment) mSectionsPagerAdapter.getItem(
                SettingsProvider.getInt(this, "current_tab", 0)));
    }

    private void buildActionButtons() {
        mActionMenu.addButton(getButton(R.drawable.add_folder,
                R.string.create_folder, BrowserFragment.ACTION_ADD_FOLDER));
        mActionMenu.addButton(getButton(R.drawable.add_file,
                R.string.create_file, BrowserFragment.ACTION_ADD_FILE));
    }

    private FloatingActionButton getButton(int icon, int title, int tag) {
        FloatingActionButton button = new FloatingActionButton(this);
        button.setColorNormalResId(R.color.accent);
        button.setColorPressedResId(R.color.accent_dark);
        button.setIcon(icon);
        button.setTitle(getString(title));
        button.setTag(tag);
        button.setOnClickListener(this);
        return button;
    }

    public void onClick(View v) {
        if (v.getTag() == BrowserFragment.ACTION_ADD_FILE) {
            mFragment.showDialog(BrowserFragment.ACTION_ADD_FILE);
            mActionMenu.collapseImmediately();
        } else if (v.getTag() == BrowserFragment.ACTION_ADD_FOLDER) {
            mFragment.showDialog(BrowserFragment.ACTION_ADD_FOLDER);
            mActionMenu.collapseImmediately();
        }
    }

    public void setMove(boolean move) {
        mMove = move;
    }

    public void showPaste(boolean show) {
        if (show) {
            mPasteButton.setVisibility(View.VISIBLE);
            mActionMenu.setVisibility(View.GONE);
        } else {
            mActionMenu.setVisibility(View.VISIBLE);
            mPasteButton.setVisibility(View.GONE);
        }
    }

    public void getExternalSDCard() {
        final String rawSecondaryStoragesStr = System.getenv("SECONDARY_STORAGE");
        if (TextUtils.isEmpty(rawSecondaryStoragesStr)) return;
        String[] sec = rawSecondaryStoragesStr.split(File.pathSeparator);
        if (sec.length > 0) {
            for (String s : sec) {
                if (s.toLowerCase().contains("usb")) {
                    if (new File(s).exists() && new File(s).isDirectory()
                            && new File(s).list().length > 0){
                        mDrawerAdapter.addItem("USB OTG", s);
                    }
                } else if (s.toLowerCase().contains("sdcard1")) {
                    if (new File(s).exists() && new File(s).isDirectory()
                            && new File(s).list().length > 0) {
                        mDrawerAdapter.addItem("External SD", s);
                    }
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_file_manager, menu);
        return true;
    }

    public void setCurrentlyDisplayedFragment(final BrowserFragment fragment) {
        mFragment = fragment;
    }

    public void closeDrawesrs() {
        mDrawerLayout.closeDrawers();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch (id) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.add_tab:
                mSectionsPagerAdapter.addTab(Environment.getExternalStorageDirectory().getPath());
                return true;
            case R.id.close_tab:
                mSectionsPagerAdapter.removeCurrentTab();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private class TabItem {
        BrowserFragment fragment;
        String path;

        private TabItem(BrowserFragment f, String p) {
            fragment = f;
            path = p;
        }
    }

    public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

        ArrayList<TabItem> mItems = new ArrayList<>();
        ArrayList<String> mTabs = new ArrayList<>();

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
            addDefault();
            mTabs = SettingsProvider.getListString(FileManager.this, "tabs", mTabs);
            for (String tab : mTabs) {
                mItems.add(new TabItem(
                        BrowserFragment.newInstance(tab), tab));
            }
        }

        public void addTab(String path) {
            mItems.add(new TabItem(
                    BrowserFragment.newInstance(path), path));
            notifyDataSetChanged();
            mViewPager.setCurrentItem(getCount());
        }

        public void removeCurrentTab() {
            int id = mCurrentPosition;
            mViewPager.setCurrentItem(id - 1, true);
            mItems.get(id).fragment.onDestroy();
            mItems.remove(mItems.get(id));
            notifyDataSetChanged();
        }

        public void addDefault() {
            mTabs.add(Environment.getExternalStorageDirectory().getAbsolutePath());
            mTabs.add("/");
        }

        @Override
        public Fragment getItem(int position) {
            //setCurrentlyDisplayedFragment(mItems.get(position).fragment);
            return mItems.get(position).fragment;
        }

        public ArrayList<TabItem> getItems() {
            return mItems;
        }

        @Override
        public int getCount() {
            return mItems.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (!TextUtils.isEmpty(mItems.get(position).fragment.getCurrentPath())) {
                File file = new File(mItems.get(position).fragment.getCurrentPath());
                if (file.exists())
                    return file.getName();
            }
            return "";
        }

        @Override
        public int getItemPosition(Object object) {
            return PagerAdapter.POSITION_NONE;
        }
    }

    @SuppressWarnings("deprecation")
    public class DrawerAdapter extends BaseAdapter {

        public class DrawerItem {
            String title;
            String path;
        }

        public class ViewHolder {

            View view;
            TextView title;
            ImageView plus;

            public ViewHolder(View v) {
                title = (TextView) v.findViewById(R.id.title);
                plus = (ImageView) v.findViewById(R.id.add_tab);
                v.setTag(this);
                view = v;
            }
        }

        ArrayList<DrawerItem> mItems = new ArrayList<>();
        Context mContext;

        public DrawerAdapter(Context context) {
            mContext = context;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder holder;

            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) mContext
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.item_drawer, parent, false);
                holder = new ViewHolder(convertView);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.view.setBackground(getAccentStateDrawable(mContext));

            holder.title.setText(mItems.get(position).title);
            if (getPath(position).equalsIgnoreCase(
                    ((BrowserFragment) mSectionsPagerAdapter.getItem(
                            mViewPager.getCurrentItem())).getCurrentPath())) {
                holder.view.setActivated(true);
            } else {
                holder.view.setActivated(false);
            }
            holder.view.jumpDrawablesToCurrentState();
            updatePlus(holder.plus);
            holder.plus.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mSectionsPagerAdapter.addTab(getPath(position));
                    mDrawerLayout.closeDrawer(mDrawer);
                }
            });
            return convertView;
        }

        private void updatePlus(ImageView view) {
            Drawable d = view.getDrawable().mutate();
            d.setColorFilter(getTextColor(), PorterDuff.Mode.MULTIPLY);
            view.setImageDrawable(d);
        }

        public void addItem(String title, String path) {
            DrawerItem item = new DrawerItem();
            item.title = title;
            item.path = path;
            mItems.add(item);
            notifyDataSetChanged();
        }

        @Override
        public String getItem(int i) {
            return mItems.get(i).title;
        }

        public String getPath(int i) {
            return mItems.get(i).path;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public int getCount() {
            return mItems.size();
        }

        private int getTextColor() {
            if (SettingsProvider.getInt(FileManager.this,
                    SettingsProvider.THEME, R.style.AppTheme) == R.style.AppTheme) {
                return getResources().getColor(R.color.primary_text);
            } else {
                return getResources().getColor(R.color.primary_text_dark);
            }
        }

        private Drawable getAccentStateDrawable(Context context) {
            Drawable colorDrawable = new ColorDrawable(getAccentColor(context));

            StateListDrawable stateListDrawable = new StateListDrawable();
            stateListDrawable.addState(new int[]{android.R.attr.state_activated}, colorDrawable);
            stateListDrawable.addState(StateSet.WILD_CARD, null);

            return stateListDrawable;
        }

        @SuppressWarnings("deprecation")
        private int getAccentColor(Context context) {
            int c = context.getResources().getColor(R.color.accent);
            return Color.argb(99, Color.red(c), Color.green(c), Color.blue(c));
        }
    }

    @Override
    public void onTrimMemory(int level) {
        if (level >= Activity.TRIM_MEMORY_MODERATE) {
            IconCache.clearCache();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        SettingsProvider.get(this)
                .unregisterOnSharedPreferenceChangeListener(mPreferenceListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        ArrayList<String> arrayList = new ArrayList<>();
        for (TabItem item : mSectionsPagerAdapter.getItems()) {
            String path = item.fragment.getCurrentPath();
            if (!TextUtils.isEmpty(path)) {
                arrayList.add(item.fragment.getCurrentPath());
            }
        }
        if (!arrayList.isEmpty()) {
            SettingsProvider.putListString(this, "tabs", arrayList);
        }
        SettingsProvider.putInt(this, "current_tab", mViewPager.getCurrentItem());
    }

    SharedPreferences.OnSharedPreferenceChangeListener mPreferenceListener
            = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            for (TabItem tabItem : mSectionsPagerAdapter.getItems()) {
                tabItem.fragment.onPreferencesChanged();
            }
        }
    };
}