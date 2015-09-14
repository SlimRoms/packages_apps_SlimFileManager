package com.slim.slimfilemanager.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;


public class BackgroundUtils extends AsyncTask<Void, Void, String> {

    public static final int UNZIP_FILE = 1001001;
    public static final int ZIP_FILE = 1001002;

    Context mContext;
    int mId;
    ProgressDialog mDialog;
    String mFile;

    public BackgroundUtils(Context context, String file, int id) {
        mContext = context;
        mFile = file;
        mId = id;
    }

    protected void onPreExecute() {
        switch (mId) {
            case UNZIP_FILE:
                mDialog = ProgressDialog.show(mContext, "Unzipping", "Please wait...", true, false);
                break;
            case ZIP_FILE:
                mDialog = ProgressDialog.show(mContext, "Zipping", "Please wait...", true, false);
        }
    }

    protected String doInBackground(Void... v) {
        switch (mId) {
            case UNZIP_FILE:
                String location = Environment.getExternalStorageDirectory() + "/Slim/Extracted";
                return ArchiveUtils.extractZipFiles(mFile, location);
            case ZIP_FILE:
                String loc = Environment.getExternalStorageDirectory() + "/Slim/Archives";
                return ArchiveUtils.createZipFile(loc, PasteTask.SelectedFiles.getFiles());
        }
        return null;
    }

    protected void onPostExecute(String v) {
        mDialog.dismiss();

    }
}
