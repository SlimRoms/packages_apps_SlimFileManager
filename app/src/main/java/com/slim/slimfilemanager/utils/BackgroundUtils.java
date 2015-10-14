package com.slim.slimfilemanager.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;

import java.io.File;


public class BackgroundUtils extends AsyncTask<Void, Void, String> {

    public static final int UNZIP_FILE = 1001001;
    public static final int ZIP_FILE = 1001002;

    public static final int UNTAR_FILE = 1001003;

    public static final String EXTRACTED_LOCATION = Environment.getExternalStorageDirectory()
            + File.separator + "Slim" + File.separator + "Extracted";

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
                break;
            case UNTAR_FILE:
                mDialog = ProgressDialog.show(mContext, "Untarring", "Please wait...", true, false);
                break;
        }
    }

    protected String doInBackground(Void... v) {
        final String location = Environment.getExternalStorageDirectory() + "/Slim/Extracted";
        switch (mId) {
            case UNZIP_FILE:
                return ArchiveUtils.extractZipFiles(mFile, location);
            case ZIP_FILE:
                String loc = Environment.getExternalStorageDirectory() + "/Slim/Archives";
                return ArchiveUtils.createZipFile(loc, PasteTask.SelectedFiles.getFiles());
            case UNTAR_FILE:
                return ArchiveUtils.unTar(mContext, mFile, location);

        }
        return null;
    }

    protected void onPostExecute(String v) {
        mDialog.dismiss();

    }
}
