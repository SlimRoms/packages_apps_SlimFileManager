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
    public static final int TAR_FILE = 1001004;
    public static final int TAR_COMPRESS = 1001005;

    public static final String EXTRACTED_LOCATION = Environment.getExternalStorageDirectory()
            + File.separator + "Slim" + File.separator + "Extracted";
    public static final String ARCHIVE_LOCATION = Environment.getExternalStorageDirectory()
            + File.separator + "Slim" + File.separator + "Archived";

    static {
        if (!new File(EXTRACTED_LOCATION).exists()) {
            new File(EXTRACTED_LOCATION).mkdirs();
        }
        if (!new File(ARCHIVE_LOCATION).exists()) {
            new File(ARCHIVE_LOCATION).mkdirs();
        }
    }

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
            case TAR_FILE:
            case TAR_COMPRESS:
                mDialog = ProgressDialog.show(mContext, "Tarring", "Please wait...", true, false);
                break;
        }
    }

    protected String doInBackground(Void... v) {
        switch (mId) {
            case UNZIP_FILE:
                return ArchiveUtils.extractZipFiles(mFile, EXTRACTED_LOCATION);
            case ZIP_FILE:
                return ArchiveUtils.createZipFile(mFile,
                        PasteTask.SelectedFiles.getFiles());
            case UNTAR_FILE:
                return ArchiveUtils.unTar(mContext, mFile, EXTRACTED_LOCATION);
            case TAR_FILE:
                return ArchiveUtils.createTar(mFile, PasteTask.SelectedFiles.getFiles());
            case TAR_COMPRESS:
                return ArchiveUtils.createTarGZ(mFile, PasteTask.SelectedFiles.getFiles());

        }
        return null;
    }

    protected void onPostExecute(String v) {
        mDialog.dismiss();

    }
}
