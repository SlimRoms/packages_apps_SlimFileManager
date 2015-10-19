package com.slim.slimfilemanager.utils;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.slim.slimfilemanager.settings.SettingsProvider;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class FileUtil {

    public static boolean copyFile(Context context, String f, String fol) {
        File file = new File(f);
        File folder = new File(fol);

        if (file.exists()) {
            if (!folder.exists()) {
                if (!mkdir(context, folder)) return false;
            }
            try {
                if (file.isDirectory()) {
                    FileUtils.copyDirectoryToDirectory(file, folder);
                } else if (file.isFile()) {
                    FileUtils.copyFileToDirectory(file, folder);
                }
                return true;
            } catch (IOException e) {
                return SettingsProvider.getBoolean(context, SettingsProvider.KEY_ENABLE_ROOT, false)
                        && RootUtils.isRootAvailable() && RootUtils.copyFile(f, fol);
            }
        } else {
            return false;
        }
    }

    public static boolean moveFile(Context context, String source, String destination) {
        if (TextUtils.isEmpty(source) || TextUtils.isEmpty(destination)) {
            return false;
        }
        File file = new File(source);
        File folder = new File(destination);

        try {
            FileUtils.moveFileToDirectory(file, folder, true);
            return true;
        } catch (IOException e) {
            return SettingsProvider.getBoolean(context, SettingsProvider.KEY_ENABLE_ROOT, false)
                && RootUtils.isRootAvailable() && RootUtils.moveFile(source, destination);
        }
    }

    public static boolean deleteFile(Context context, String path) {
        try {
            FileUtils.forceDelete(new File(path));
            return true;
        } catch (IOException e) {
            return SettingsProvider.getBoolean(context, SettingsProvider.KEY_ENABLE_ROOT, false)
                    && RootUtils.isRootAvailable() && RootUtils.deleteFile(path);
        }
    }

    public static String[] getFileProperties(Context context, File file) {
        BufferedReader in;
        String[] info = null;
        String line;

        try {
            if (SettingsProvider.getBoolean(context, SettingsProvider.KEY_ENABLE_ROOT, false)) {
                in = RootUtils.runCommand("ls -l " + file.getAbsolutePath());
            } else {
                in = runCommand("ls -l " + file.getAbsolutePath());
            }
            if (in == null) return null;

            while ((line = in.readLine()) != null) {
                info = getAttrs(line);
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return info;
    }

    private static String[] getAttrs(String string) {
        if (string.length() < 44) {
            throw new IllegalArgumentException("Bad ls -l output: " + string);
        }
        final char[] chars = string.toCharArray();

        final String[] results = new String[11];
        int ind = 0;
        final StringBuilder current = new StringBuilder();

        Loop:
        for (int i = 0; i < chars.length; i++) {
            switch (chars[i]) {
                case ' ':
                case '\t':
                    if (current.length() != 0) {
                        results[ind] = current.toString();
                        ind++;
                        current.setLength(0);
                        if (ind == 10) {
                            results[ind] = string.substring(i).trim();
                            break Loop;
                        }
                    }
                    break;

                default:
                    current.append(chars[i]);
                    break;
            }
        }

        return results;
    }

    public static BufferedReader runCommand(String cmd) {
        BufferedReader reader;
        try {
            Process process = Runtime.getRuntime().exec("sh");
            DataOutputStream os = new DataOutputStream(
                    process.getOutputStream());
            os.writeBytes(cmd + "\n");
            os.writeBytes("exit\n");
            reader = new BufferedReader(new InputStreamReader(
                    process.getInputStream()));
            String err = (new BufferedReader(new InputStreamReader(
                    process.getErrorStream()))).readLine();
            os.flush();

            if (process.waitFor() != 0 || (!"".equals(err) && null != err)) {
                Log.e("Root Error, cmd: " + cmd, err);
                return null;
            }
            return reader;
        } catch (IOException|InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean changeGroupOwner(Context context, File file, String owner, String group) {
        try {
            boolean useRoot = false;
            if (!file.canWrite() && SettingsProvider.getBoolean(context,
                    SettingsProvider.KEY_ENABLE_ROOT, false)
                    && RootUtils.isRootAvailable()) {
                useRoot = true;
                RootUtils.remountSystem("rw");
            }

            if (useRoot) {
                RootUtils.runCommand("chown " + owner + "." + group + " "
                        + file.getAbsolutePath());
            } else {
                runCommand("chown " + owner + "." + group + " "
                        + file.getAbsolutePath());
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public static boolean applyPermissions(Context context, File file, Permissions permissions) {
        try {
            boolean useSu = false;
            if (!file.canWrite() && SettingsProvider.getBoolean(context,
                    SettingsProvider.KEY_ENABLE_ROOT, false)
                    && RootUtils.isRootAvailable()) {
                useSu = true;
                RootUtils.remountSystem("rw");
            }

            if (useSu) {
                RootUtils.runCommand("chmod " + permissions.getOctalPermissions() + " "
                        + file.getAbsolutePath());
            } else {
                runCommand("chmod " + permissions.getOctalPermissions() + " "
                        + file.getAbsolutePath());
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public static boolean mkdir (Context context, File dir) {
        return dir.mkdirs() || (SettingsProvider.getBoolean(context,
                SettingsProvider.KEY_ENABLE_ROOT, false) && RootUtils.isRootAvailable()
                && RootUtils.createFolder(dir));
    }

    public static String getExtension(File f) {
        return FilenameUtils.getExtension(f.getName());
    }

    public static String removeExtension(String s) {
        return FilenameUtils.removeExtension(new File(s).getName());
    }

    public static void writeFile(Context context, String content, File file, String encoding) {
        if (file.canWrite()) {
            try {
                FileUtils.write(file, content, encoding);
            } catch (IOException e) {
                // ignore
            }
        } else if (SettingsProvider.getBoolean(context, SettingsProvider.KEY_ENABLE_ROOT, false)){
            RootUtils.writeFile(file, content);
        }
    }

    public static void renameFile(Context context, File oldFile, File newFile) {
        try {
            FileUtils.moveFile(oldFile, newFile);
        } catch (IOException e) {
            if (SettingsProvider.getBoolean(context, SettingsProvider.KEY_ENABLE_ROOT, false)) {
                RootUtils.renameFile(oldFile, newFile);
            }
        }
    }
}
