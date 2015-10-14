package com.slim.slimfilemanager.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.slim.slimfilemanager.R;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class PasteTask implements View.OnClickListener {

    Context mContext;
    ArrayList<String> mFiles = new ArrayList<>();
    boolean mMove;
    String mLocation;

    String mCurrent;

    AlertDialog mDialog;

    HashMap<String, String> mExistingFiles = new HashMap<>();
    ArrayList<String> mProcess = new ArrayList<>();

    public PasteTask(Context context,
                     boolean shouldDelete, String location) {
        mContext = context;
        mMove = shouldDelete;
        mLocation = location;

        mFiles.addAll(SelectedFiles.getFiles());

        for (int i = 0; i < mFiles.size(); i++) {
            File file = new File(mFiles.get(i));
            if (file.exists()) {
                File newFile = new File(mLocation + File.separator + file.getName());
                if (newFile.exists()) {
                    mExistingFiles.put(newFile.getPath(), file.getPath());
                } else {
                    mProcess.add(file.getPath());
                }
            }
        }
        processFiles();
    }

    private void processFiles() {
        boolean failed = false;
        if (mExistingFiles.isEmpty()) {
            if (mProcess.isEmpty()) return;
            for (String path : mProcess) {
                if (!TextUtils.isEmpty(path)) {
                    if (mMove) {
                        failed = !FileUtils.moveFile(mContext, path, mLocation);
                    } else {
                        failed = !FileUtils.copyFile(mContext, path, mLocation);
                    }
                }
            }
            if (failed) {
                Toast.makeText(mContext, "Failed.", Toast.LENGTH_SHORT).show();
            }
        } else {
            String key = mExistingFiles.keySet().iterator().next();
            mCurrent = mExistingFiles.get(key);
            mExistingFiles.remove(key);
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            View view = View.inflate(mContext, R.layout.file_exists_dialog, null);
            view.findViewById(R.id.skip).setOnClickListener(this);
            view.findViewById(R.id.skip_all).setOnClickListener(this);
            view.findViewById(R.id.replace).setOnClickListener(this);
            view.findViewById(R.id.replace_all).setOnClickListener(this);
            view.findViewById(R.id.cancel).setOnClickListener(this);

            ((TextView) view.findViewById(R.id.source)).setText(mCurrent);
            ((TextView) view.findViewById(R.id.destination)).setText(key);

            builder.setView(view);
            mDialog = builder.create();
            mDialog.show();
        }
    }

    @Override
    public void onClick(View view) {
        if (mDialog != null) mDialog.dismiss();

        if (view.getId() == R.id.skip_all) {
            mExistingFiles.clear();
        } else if (view.getId() == R.id.replace) {
            mProcess.add(mCurrent);
        } else if (view.getId() == R.id.replace_all) {
            mProcess.add(mCurrent);
            for (String file : mExistingFiles.keySet()) {
                mProcess.add(mExistingFiles.get(file));
            }
            mExistingFiles.clear();
        } else if (view.getId() == R.id.cancel) {
            mExistingFiles.clear();
            mProcess.clear();
        }
        processFiles();
    }

    public static final class SelectedFiles {
        private static final ArrayList<String> files = new ArrayList<>();

        public static void addFile(String file) {
            files.add(file);
        }

        public static void clearAll() {
            files.clear();
        }

        public static boolean isEmpty() {
            return files.isEmpty();
        }

        public static ArrayList<String> getFiles() {
            return files;
        }
    }
}
