package com.slim.slimfilemanager.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.widget.ImageView;

import com.slim.slimfilemanager.R;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IconCache {

    private static ConcurrentHashMap<String, Object> mCache;

    private static ImageHandler mHandler;

    private static ExecutorService mExecutor;

    static {
        mCache = new ConcurrentHashMap<>();
        mHandler = new ImageHandler();
        mExecutor = Executors.newFixedThreadPool(6);
    }

    public static void getIconForFile(Context context, String file, ImageView view) {
        view.setImageBitmap(null);
        view.setImageDrawable(null);
        if (mCache.containsKey(file)) {
            setImage(view, mCache.get(file));
            return;
        }
        queueImage(context, new File(file), view);
    }

    public static Object getImage(Context context, File file) {
        boolean isPicture = MimeUtils.isPicture(file);
        boolean isVideo = MimeUtils.isVideo(file);
        boolean isApp = MimeUtils.isApp(file);

        Object object;
        int width = (int) context.getResources().getDimension(R.dimen.item_height);

        String path = file.getPath();

        if (isPicture) {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;

            BitmapFactory.decodeFile(path, o);
            o.inJustDecodeBounds = false;

            if (o.outWidth != -1 && o.outHeight != -1) {
                final int originalSize = (o.outHeight > o.outWidth) ? o.outWidth
                        : o.outHeight;
                o.inSampleSize = originalSize / width;
            }

            object = BitmapFactory.decodeFile(path, o);
        } else if (isVideo) {
            object = ThumbnailUtils.createVideoThumbnail(path,
                    MediaStore.Video.Thumbnails.MICRO_KIND);
        } else if (isApp) {
            PackageManager pm = context.getPackageManager();
            PackageInfo packageInfo = pm.getPackageArchiveInfo(path,
                    PackageManager.GET_ACTIVITIES);

            if (packageInfo != null) {

                final ApplicationInfo appInfo = packageInfo.applicationInfo;
                appInfo.sourceDir = path;
                appInfo.publicSourceDir = path;

                return pm.getDrawable(appInfo.packageName, appInfo.icon, appInfo);
            } else {
                return context.getDrawable(
                        android.R.drawable.sym_def_app_icon);
            }
        } else if (file.isDirectory()) {
            if (file.list() != null && file.list().length > 0) {
                object = context.getDrawable(R.drawable.folder);
            } else {
                object = context.getDrawable(R.drawable.empty_folder);
            }
        } else if (MimeUtils.isTextFile(file)) {
            object = context.getDrawable(R.drawable.text);
        } else {
            object = context.getDrawable(R.drawable.file);
        }
        return object;
    }

    private static class ImageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Bundle bundle = msg.getData();
            if (bundle != null) {
                String key = bundle.getString("key");
                if (!TextUtils.isEmpty(key)) {
                    if (msg.obj != null) {
                        setImage((ImageView) msg.obj, mCache.get(key));
                    }
                }
            }
        }
    }

    public static void queueImage(final Context context, final File file, final ImageView view) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                final Object o = getImage(context, file);
                mCache.put(file.getPath(), o);
                Message message = Message.obtain();
                Bundle data = new Bundle();
                data.putString("key", file.getPath());
                message.setData(data);
                message.obj = view;
                mHandler.sendMessage(message);
            }
        });
    }

    public static void setImage(ImageView view, Object o) {
        if (o instanceof Drawable) {
            view.setImageDrawable((Drawable) o);
        } else if (o instanceof Bitmap) {
            view.setImageBitmap((Bitmap) o);
        }
    }

    public static void clearCache() {
        mCache.clear();
    }
}
