/*
    Open Manager, an open source file manager for the Android system
    Copyright (C) 2009, 2010, 2011  Joe Berria <nexesdevelopment@gmail.com>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.slim.filemanager;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.ArrayList;
import java.io.File;
import java.lang.RuntimeException;

public class ThumbnailCreator extends Thread {
    private int mWidth;
    private int mHeight;
    private SoftReference<Bitmap> mThumb;
    private static HashMap<String, Bitmap> mCacheMap = null;
    private ArrayList<String> mFiles;
    private String mDir;
    private Handler mHandler;
    private boolean mStop = false;

    private final String TAG = "SFM-ThumbnailCreator";

    public ThumbnailCreator(int width, int height) {
        mHeight = height;
        mWidth = width;

        if(mCacheMap == null)
            mCacheMap = new HashMap<String, Bitmap>();
    }

    public Bitmap isBitmapCached(String name) {
        return mCacheMap.get(name);
    }

    public void setCancelThumbnails(boolean stop) {
        mStop = stop;
    }

    public void createNewThumbnail(ArrayList<String> files,  String dir,  Handler handler) {
        this.mFiles = files;
        this.mDir = dir;
        this.mHandler = handler;
    }

    // Returns the next power of two.
    // Returns the input if it is already power of 2.
    // Throws IllegalArgumentException if the input is <= 0 or
    // the answer overflows.
    public static int nextPowerOf2(int n) {
        if (n <= 0 || n > (1 << 30)) throw new IllegalArgumentException("n is invalid: " + n);
        n -= 1;
        n |= n >> 16;
        n |= n >> 8;
        n |= n >> 4;
        n |= n >> 2;
        n |= n >> 1;
        return n + 1;
    }

    public int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            // Calculate ratios of height and width to requested height and width
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);

            // Choose the smallest ratio as inSampleSize value, this will guarantee
            // a final image with both dimensions larger than or equal to the
            // requested height and width.
            inSampleSize = Math.min(heightRatio, widthRatio);

            // Finally round up the sample size to a power of 2 or multiple
            // of 8 because BitmapFactory only honors sample size this way.
            // For example, BitmapFactory downsamples an image by 2 even though the
            // request is 3. So we round up the sample size to avoid OOM.
            inSampleSize = inSampleSize <= 8 ? nextPowerOf2(inSampleSize) : (inSampleSize + 7) / 8 * 8;
        }

        return inSampleSize;
    }

    public Bitmap decodeSampledBitmapFromFile(String imagePath, int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imagePath, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set (NOTE: won't work on GIF files!)
        options.inJustDecodeBounds = false;
        options.inPurgeable = true;
        return BitmapFactory.decodeFile(imagePath, options);
    }

    private Bitmap.Config getConfig(Bitmap bitmap) {
        Bitmap.Config config = bitmap.getConfig();
        if (config == null) {
            config = Bitmap.Config.ARGB_8888;
        }
        return config;
    }

    public Bitmap resizeAndCropCenter(Bitmap bitmap, int size, boolean recycle) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        if (w == size && h == size) return bitmap;

        // scale the image so that the shorter side equals to the target;
        // the longer side will be center-cropped.
        float scale = (float) size / Math.min(w,  h);

        Bitmap target = Bitmap.createBitmap(size, size, getConfig(bitmap));
        int width = Math.round(scale * bitmap.getWidth());
        int height = Math.round(scale * bitmap.getHeight());
        Canvas canvas = new Canvas(target);
        canvas.translate((size - width) / 2f, (size - height) / 2f);
        canvas.scale(scale, scale);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
        canvas.drawBitmap(bitmap, 0, 0, paint);
        if (recycle) bitmap.recycle();
        return target;
    }

    @Override
    public void run() {
        String TAG = "ThumbnailCreator";

        int len = mFiles.size();

        for (int i = 0; i < len; i++) {
            if (mStop) {
                mStop = false;
                mFiles = null;
                return;
            }
            final File file = new File(mDir + "/" + mFiles.get(i));

            if (isImageFile(file.getName())) {
                try {
                    mThumb = new SoftReference<Bitmap>(resizeAndCropCenter(decodeSampledBitmapFromFile(file.getPath(), mWidth, mHeight),52,true));
                } catch (RuntimeException e) {
                    Log.e(TAG, "Error creating thumbnail in " + file.getPath());
                }

                if (mThumb != null) {
                    mCacheMap.put(file.getPath(), mThumb.get());
                }

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mThumb != null) {
                            Message msg = mHandler.obtainMessage();
                            msg.obj = (Bitmap)mThumb.get();
                            msg.sendToTarget();
                        }
                    }
                });
            }
        }
    }

    private boolean isImageFile(String file) {
        String ext = file.substring(file.lastIndexOf(".") + 1);

        if (ext.equalsIgnoreCase("png") || ext.equalsIgnoreCase("jpg") ||
            ext.equalsIgnoreCase("jpeg")|| ext.equalsIgnoreCase("gif") ||
            ext.equalsIgnoreCase("tiff")|| ext.equalsIgnoreCase("tif"))
            return true;

        return false;
    }
}
