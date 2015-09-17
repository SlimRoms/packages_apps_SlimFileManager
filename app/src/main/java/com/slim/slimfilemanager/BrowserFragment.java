package com.slim.slimfilemanager;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.slim.slimfilemanager.multichoice.MultiChoiceViewHolder;
import com.slim.slimfilemanager.multichoice.MultiSelector;
import com.slim.slimfilemanager.settings.SettingsProvider;
import com.slim.slimfilemanager.utils.BackgroundUtils;
import com.slim.slimfilemanager.utils.FileUtils;
import com.slim.slimfilemanager.utils.FragmentLifecycle;
import com.slim.slimfilemanager.utils.IconCache;
import com.slim.slimfilemanager.utils.MimeUtils;
import com.slim.slimfilemanager.utils.PasteTask.SelectedFiles;
import com.slim.slimfilemanager.utils.RootUtils;
import com.slim.slimfilemanager.utils.SortUtils;
import com.slim.slimfilemanager.utils.Utils;
import com.slim.slimfilemanager.widget.DividerItemDecoration;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class BrowserFragment extends Fragment implements View.OnClickListener,
        FragmentLifecycle, SearchView.OnQueryTextListener {

    private static final int MENU_COPY = 1001;
    private static final int MENU_CUT = 1002;
    private static final int MENU_DELETE = 1003;
    private static final int MENU_PERMISSIONS = 1004;
    private static final int MENU_RENAME = 1005;
    private static final int MENU_SHARE = 1006;
    private static final int MENU_ZIP = 1007;

    public static final int ACTION_ADD_FOLDER = 10001;
    public static final int ACTION_ADD_FILE = 10002;

    private static final String ARG_PATH = "path";

    private String mCurrentPath;

    private Context mContext;
    private FileManager mActivity;

    private MultiSelector mMultiSelector = new MultiSelector();

    private ActionMode mActionMode;
    private TextView mPath;
    private SearchView mSearchView;
    private ProgressBar mProgress;
    private RecyclerView mRecyclerView;
    private ViewAdapter mAdapter;
    private ArrayList<Item> mFiles = new ArrayList<>();

    private boolean mExitOnBack = false;
    private boolean mSearching = false;

    public class Item {
        public String name;
        public String path;
    }

    public static BrowserFragment newInstance(String path) {
        BrowserFragment fragment = new BrowserFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PATH, path);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = getActivity();
        mActivity = (FileManager) getActivity();
        setHasOptionsMenu(true);
    }

    @Override
    public void onPauseFragment() {
        mExitOnBack = false;
        if (mSearchView != null && !mSearchView.isIconified()) {
            mSearchView.setIconified(true);
        }
    }

    @Override
    public void onResumeFragment() {
        /*if (mPasteButton == null) return;
        if (!SelectedFiles.isEmpty()) {
            mPasteButton.setVisibility(View.VISIBLE);
        } else {
            mPasteButton.setVisibility(View.GONE);
        }*/
    }

    public void onPreferencesChanged() {
        sortFiles();
        if (mAdapter != null) mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (getView() == null) return;
        getView().setFocusableInTouchMode(true);
        getView().requestFocus();
        getView().setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                return event.getAction() == KeyEvent.ACTION_UP
                        && keyCode == KeyEvent.KEYCODE_BACK && onBackPressed();
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_browser, container, false);

        String defaultDir = null;
        Bundle extras = getArguments();

        if (extras != null) {
            defaultDir = extras.getString(ARG_PATH);
        }
        if (TextUtils.isEmpty(defaultDir)) {
            defaultDir = Environment.getExternalStorageDirectory().getPath();
        }
        if (TextUtils.isEmpty(defaultDir)) {
            defaultDir = "/";
        }
        mCurrentPath = defaultDir;

        mPath = (TextView) rootView.findViewById(R.id.path);

        mAdapter = new ViewAdapter();

        mProgress = (ProgressBar) rootView.findViewById(R.id.progress);
        mProgress.setIndeterminate(true);
        mProgress.setVisibility(View.GONE);

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.list);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(mContext);
        mRecyclerView.setHasFixedSize(true);
        DividerItemDecoration decor = new DividerItemDecoration(mContext, null);
        decor.setShowFirstDivider(false);
        decor.setShowLastDivider(false);
        mRecyclerView.addItemDecoration(decor);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setAdapter(mAdapter);

        filesChanged(mCurrentPath);

        return rootView;
    }

    public boolean onBackPressed() {
        Log.d("TEST", mCurrentPath);
        if (mSearchView != null && !mSearchView.isIconified()) {
            mSearchView.setIconified(true);
            return true;
        }
        if (!mCurrentPath.equals("/")) {
            File file = new File(mCurrentPath);
            onClickFile(file.getParent());
            mRecyclerView.scrollToPosition(mAdapter.indexOf(file.getName()));
            mExitOnBack = false;
        } else if (mCurrentPath.equals("/")) {
            if (mExitOnBack) {
                mActivity.finish();
            } else {
                Toast.makeText(mContext, getString(R.string.back_confirm),
                        Toast.LENGTH_SHORT).show();
                mExitOnBack = true;
            }
        } else {
            mExitOnBack = false;
        }
        return true;
    }

    ActionMode.Callback mMultiSelect = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            menu.add(0, MENU_COPY, 0, R.string.copy).setIcon(R.drawable.copy)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            menu.add(0, MENU_CUT, 0, R.string.move).setIcon(R.drawable.cut)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            menu.add(0, MENU_DELETE, 0, R.string.move).setIcon(R.drawable.delete)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            menu.add(0, MENU_PERMISSIONS, 0, R.string.permissions)
                    .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER);
            menu.add(0, MENU_RENAME, 0, R.string.rename)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
            menu.add(0, MENU_SHARE, 0, R.string.share).setIcon(R.drawable.share)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            menu.add(0, MENU_ZIP, 0, R.string.zip).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
            mActionMode = mode;
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            menu.findItem(MENU_PERMISSIONS).setVisible(
                    mMultiSelector.getSelectedPositions().size() == 1);
            menu.findItem(MENU_RENAME).setVisible(
                    mMultiSelector.getSelectedPositions().size() == 1);
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

            SelectedFiles.clearAll();
            for (int i = 0; i < mFiles.size(); i++) {
                if (mMultiSelector.isSelected(i)) {
                    SelectedFiles.addFile(mFiles.get(i).path);
                }
            }

            if (mMultiSelector.getSelectedPositions().size() > 0) {
                int id = item.getItemId();
                switch (id) {
                    case MENU_CUT:
                    case MENU_COPY:
                        if (id == MENU_CUT) mActivity.setMove(true);
                        mActivity.showPaste(true);
                        break;
                    case MENU_DELETE:
                        showDialog(MENU_DELETE);
                        mode.finish();
                        break;
                    case MENU_PERMISSIONS:
                        showDialog(MENU_PERMISSIONS);
                        break;
                    case MENU_RENAME:
                        showDialog(MENU_RENAME);
                        break;
                    case MENU_SHARE:
                        handleShareFile();
                        break;
                    case MENU_ZIP:
                        new BackgroundUtils(mContext, null, BackgroundUtils.ZIP_FILE).execute();
                }
            }
            mMultiSelector.clearSelections();
            mMultiSelector.setSelectable(false);
            mode.finish();
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mMultiSelector.clearSelections();
            mMultiSelector.setSelectable(false);
        }
    };

    public void setSearching(boolean searching) {
        mSearching = searching;
    }

    @Override
    public void onClick(View v) {
        /*if (v == mPasteButton) {
            new PasteTask(mContext, mMove, mCurrentPath);
            filesChanged(mCurrentPath);
            mMove = false;
            //mActionMenu.setVisibility(View.VISIBLE);
            mPasteButton.setVisibility(View.GONE);
        } else*/ if (v.getTag() == ACTION_ADD_FOLDER) {
            showDialog(ACTION_ADD_FOLDER);
        }
    }

    public void handleShareFile() {
        ArrayList<Uri> uris = new ArrayList<>();
        for (String f : SelectedFiles.getFiles()) {
            File file = new File(f);
            if (file.exists()) {
                if (!file.isDirectory()) {
                    uris.add(Uri.fromFile(file));
                }
            }
        }
        SelectedFiles.clearAll();

        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (uris.size() == 1) {
            intent.setAction(Intent.ACTION_SEND);
            intent.setType(MimeUtils.getMimeType(new File(uris.get(0).getPath())));
            intent.putExtra(Intent.EXTRA_STREAM, uris.get(0));
        } else {
            intent.setAction(Intent.ACTION_SEND_MULTIPLE);
            intent.setType(MimeUtils.ALL_MIME_TYPES);
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        }
        if (mActionMode != null) mActionMode.finish();
        mActivity.startActivity(Intent.createChooser(intent, getString(R.string.share)));
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, MenuInflater inflater) {

        mSearchView = (SearchView) menu.findItem(R.id.search).getActionView();
        mSearchView.setIconifiedByDefault(true);
        mSearchView.setOnQueryTextListener(this);
        mSearchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSearching(true);
                mActivity.closeDrawesrs();
                mFiles.clear();
                mAdapter.notifyDataSetChanged();
            }
        });
        mSearchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                mProgress.setVisibility(View.GONE);
                setSearching(false);
                filesChanged(mCurrentPath);
                return false;
            }
        });

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.search:
                setSearching(true);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if (mSearchView != null && !mSearchView.isIconified()) {
            FileUtils.setQuery(newText);
            mFiles.clear();
            mAdapter.notifyDataSetChanged();
            if (!TextUtils.isEmpty(newText)) {
                new SearchTask().execute(newText);
            }
        }
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {

        return false;
    }

    private void onClickFile(String file) {
        if (TextUtils.isEmpty(file)) {
            return;
        }
        File f = new File(file);
        if (f.exists()) {
            if (f.isDirectory()) {
                filesChanged(file);
            } else {
                if (file.endsWith("zip")) {
                    try {
                        onClickFile(new BackgroundUtils(mContext, file,
                                BackgroundUtils.UNZIP_FILE).execute().get());
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                } else {
                    Utils.onClickFile(mContext, file);
                }
            }
        }
    }

    public class UpdateListRunnable implements Runnable {

        File file;

        @Override
        public void run() {
            if (file.exists()) {
                Item item = new Item();
                item.path = file.getPath();
                item.name = file.getName();
                mFiles.add(item);
                mAdapter.notifyItemInserted(mFiles.indexOf(item));
            }
        }

        public UpdateListRunnable init(String path) {
            file = new File(path);
            return this;

        }
    }

    UpdateListRunnable mUpdateListRunnable = new UpdateListRunnable();

    protected void filesChanged(String file) {
        if (mExitOnBack) mExitOnBack = false;
        File newPath = new File(file);
        if (!newPath.exists()) {
            return;
        }
        if (!newPath.canRead() && !RootUtils.isRootAvailable()) {
            Toast.makeText(mContext, "Root is required to view folder.", Toast.LENGTH_SHORT).show();
        }
        List<String> files = Utils.listFiles(file);
        if (files == null) {
            return;
        }
        mPath.setText(new File(file).getAbsolutePath());
        mCurrentPath = file;
        if (!mFiles.isEmpty()) mFiles.clear();
        mAdapter.notifyDataSetChanged();
        for (String s : files) {
            Item item = new Item();
            item.name = new File(s).getName();
            item.path = s;
            mFiles.add(item);
            sortFiles();
            mAdapter.notifyItemInserted(mFiles.indexOf(item));
        }
        mRecyclerView.scrollToPosition(0);
    }

    public String getCurrentPath() {
        return mCurrentPath;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case MENU_CUT:
            case MENU_COPY:
            case MENU_DELETE:
                showDialog(MENU_DELETE);
                return true;
            case MENU_PERMISSIONS:
                showDialog(MENU_PERMISSIONS);
                return true;
        }
        return false;
    }

    public class ViewAdapter extends RecyclerView.Adapter<BrowserViewHolder> {

        @Override
        public BrowserViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View v = inflater.inflate(R.layout.item, parent, false);
            BrowserViewHolder vh = new BrowserViewHolder(v);
            vh.icon = (ImageView) v.findViewById(R.id.image);
            vh.title = (TextView) v.findViewById(R.id.title);
            vh.date = (TextView) v.findViewById(R.id.date);
            vh.info = (TextView) v.findViewById(R.id.info);
            return vh;
        }

        @Override
        public void onBindViewHolder(final BrowserViewHolder holder, final int position) {
            holder.title.setText(mFiles.get(position).name);
            IconCache.getIconForFile(mContext, mFiles.get(position).path, holder.icon);

            DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT,
                    DateFormat.SHORT, Locale.getDefault());
            File file = new File(mFiles.get(position).path);
            if (file.isFile()) {
                holder.info.setText(Utils.displaySize(file.length()));
            } else {
                int num = 0;
                String[] files = file.list();
                if (files != null) {
                    num = files.length;
                }
                holder.info.setText(String.valueOf(num));
            }
            if (mSearching) {
                holder.date.setText(mFiles.get(position).path);
            } else {
                holder.date.setText(df.format(file.lastModified()));
            }
        }

        @Override
        public int getItemCount() {
            return mFiles.size();
        }

        public int indexOf(String name) {
            for (int i = 0; i < getItemCount(); i++) {
                if (mFiles.get(i).name.equalsIgnoreCase(name)) {
                    return i;
                }
            }
            return 0;
        }
    }

    public void removeFile(String file) {
        int id = -1;
        for (int i = 0; i < mFiles.size(); i++) {
            if (mFiles.get(i).path.equals(file)) {
                id = i;
                break;
            }
        }
        if (id == -1) return;
        mFiles.remove(id);
        mAdapter.notifyItemRemoved(id);
    }

    public void addFile(String file) {
        Item item = new Item();
        item.path = file;
        item.name = new File(file).getName();
        mFiles.add(item);
        sortFiles();
        mAdapter.notifyItemInserted(mFiles.indexOf(item));
    }

    private void sortFiles() {
        SortUtils.sort(mFiles);
    }

    public void showDialog(int id) {

        DialogFragment newFragment =
                MyAlertDialogFragment.newInstance(id);
        newFragment.setTargetFragment(this, 0);
        newFragment.show(getFragmentManager(), "dialog " + id);
    }

    public static class MyAlertDialogFragment extends DialogFragment {

        public static MyAlertDialogFragment newInstance(int id) {
            MyAlertDialogFragment frag = new MyAlertDialogFragment();
            Bundle args = new Bundle();
            args.putInt("id", id);
            frag.setArguments(args);
            return frag;
        }

        BrowserFragment getOwner() {
            return (BrowserFragment) getTargetFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final int id = getArguments().getInt("id");
            switch (id) {
                case MENU_DELETE:
                    final AlertDialog.Builder b = new AlertDialog.Builder(getOwner().mContext);
                    if (SelectedFiles.getFiles().size() == 1) {
                        b.setTitle(SelectedFiles.getFiles().get(0));
                    } else {
                        b.setTitle(R.string.delete_dialog_title);
                    }
                    b.setMessage(R.string.delete_dialog_message);
                    b.setPositiveButton(R.string.delete,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    for (String file : SelectedFiles.getFiles()) {
                                        if (FileUtils.deleteFile(file)) {
                                            getOwner().removeFile(file);
                                        } else {
                                            Toast.makeText(getOwner().mContext,
                                                    "Failed to delete file: "
                                                            + new File(file).getName(),
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                    SelectedFiles.clearAll();
                                    dialog.dismiss();
                                }
                            });
                    b.setNegativeButton(R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    return b.create();
                case MENU_PERMISSIONS:
                    String path = SelectedFiles.getFiles().get(0);
                    if (TextUtils.isEmpty(path)) return null;
                    PermissionsDialog dialog = new PermissionsDialog(
                            getOwner().mContext, path);
                    return dialog.getDialog();
                case ACTION_ADD_FILE:
                case ACTION_ADD_FOLDER:
                case MENU_RENAME:
                    final AlertDialog.Builder builder = new AlertDialog.Builder(getOwner().mContext);
                    View view = View.inflate(getOwner().mContext, R.layout.add_folder, null);
                    final EditText folderName = (EditText) view.findViewById(R.id.folder_name);
                    if (id == ACTION_ADD_FOLDER) {
                        builder.setTitle(R.string.create_folder);
                        folderName.setHint(R.string.folder_name_hint);
                    } else if (id == ACTION_ADD_FILE){
                        builder.setTitle(R.string.create_file);
                        folderName.setHint(R.string.file_name_hint);
                    } else {
                        builder.setTitle(R.string.rename);
                        folderName.setText(SelectedFiles.getFiles().get(0));
                    }
                    builder.setView(view);
                    View.OnClickListener listener = new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (v.getId() == R.id.cancel) {
                                dismiss();
                            } else if (v.getId() == R.id.create) {
                                boolean dismiss = false;
                                File newFolder = new File(getOwner().getCurrentPath()
                                        + File.separator
                                        + folderName.getText().toString());
                                if (newFolder.exists()) {
                                    if (id == ACTION_ADD_FILE) {
                                        Toast.makeText(getActivity(), R.string.file_exists,
                                                Toast.LENGTH_SHORT).show();
                                    } else if (id == ACTION_ADD_FOLDER) {
                                        Toast.makeText(getActivity(), R.string.folder_exists,
                                                Toast.LENGTH_SHORT).show();
                                    }
                                    return;
                                }
                                if (id == ACTION_ADD_FOLDER) {
                                    if (!newFolder.exists()) {
                                        if (!newFolder.mkdirs()) {
                                            if (SettingsProvider.getInstance(null).getBoolean(
                                                    SettingsProvider.KEY_ENABLE_ROOT, false)
                                                    && RootUtils.isRootAvailable()) {
                                                if (!RootUtils.createFolder(newFolder)) {
                                                    Toast.makeText(getOwner().getActivity(),
                                                            R.string.unable_to_create_folder,
                                                            Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        }
                                    }
                                } else if (id == ACTION_ADD_FILE) {
                                    try {
                                        if (!newFolder.exists()) {
                                            if (!newFolder.createNewFile()) {
                                                if (!RootUtils.createFile(newFolder)) {
                                                    Toast.makeText(getOwner().mContext,
                                                            R.string.unable_to_create_file,
                                                            Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        }
                                        dismiss = true;
                                    } catch (IOException e) {
                                        dismiss = false;
                                    }
                                }
                                if (dismiss) {
                                    getOwner().addFile(newFolder.getPath());
                                    dismiss();
                                }
                            }
                        }
                    };
                    view.findViewById(R.id.cancel).setOnClickListener(listener);
                    view.findViewById(R.id.create).setOnClickListener(listener);
                    return builder.create();
            }
            return null;
        }
    }

    /*@Override
    public final void setUserVisibleHint(boolean isVisibleToUser) {
        final boolean needUpdate = isVisibleToUser != getUserVisibleHint();
        super.setUserVisibleHint(isVisibleToUser);
        if (needUpdate) {
            if (isVisibleToUser) {
                // onVisible()
                //mActivity.setCurrentlyDisplayedFragment(this);
            }
            //onInvisible()
        }
    }*/

    public class SearchTask extends AsyncTask<String, Integer, Void> {

            String mQuery;
            String mDir;

            protected void onPreExecute() {
                mProgress.setVisibility(View.VISIBLE);
                mFiles.clear();
                mAdapter.notifyDataSetChanged();
            }

            protected Void doInBackground(String... s) {
                mQuery = s[0];
                mDir = mCurrentPath;
                FileUtils.searchForFile(mDir, mQuery, mUpdateListRunnable, mActivity);
                return null;
            }

            protected void onPostExecute(Void v) {
                mProgress.setVisibility(View.GONE);
            }
    }

    public class BrowserViewHolder extends MultiChoiceViewHolder
            implements View.OnClickListener, View.OnLongClickListener {

        public View main;
        public TextView title;
        public TextView date;
        public TextView info;
        public ImageView icon;

        public BrowserViewHolder(View v) {
            super(v, mMultiSelector);
            main = v;
            main.setOnClickListener(this);
            main.setOnLongClickListener(this);
            main.setLongClickable(true);
        }

        public void onClick(View view) {
            boolean b = mMultiSelector.tapSelection(this);
            if (mMultiSelector.getSelectedPositions().size() == 0) {
                mMultiSelector.setSelectable(false);
                if (mActionMode != null) mActionMode.finish();
            }
            if (!b) {
                onClickFile(mFiles.get(getAdapterPosition()).path);
            }
            if (mActionMode != null && !b) {
                mActionMode.finish();
            }
        }

        public boolean onLongClick(View view) {
            mActivity.startActionMode(mMultiSelect);
            mMultiSelector.setSelectable(true);
            mMultiSelector.setSelected(this, true);
            return true;
        }
    }
}