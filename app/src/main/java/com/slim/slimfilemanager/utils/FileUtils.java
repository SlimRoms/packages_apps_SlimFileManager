package com.slim.slimfilemanager.utils;

import android.app.Activity;
import android.text.TextUtils;
import android.util.Log;

import com.slim.slimfilemanager.BrowserFragment;
import com.slim.slimfilemanager.settings.SettingsProvider;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class FileUtils {

    private static final int BUFFER = 4096;

    public static boolean copyFile(String f, String fol) {
        File file = new File(f);
        File folder = new File(fol);
        byte[] data = new byte[BUFFER];
        int read;

        if (file.canRead() && folder.isDirectory()
                && folder.canWrite()) {
            if (file.isFile()) {
                String file_name = file.getName();
                File cp_file = new File(folder.getPath() + File.separator + file_name);

                try {
                    BufferedOutputStream o_stream = new BufferedOutputStream(
                            new FileOutputStream(cp_file));
                    BufferedInputStream i_stream = new BufferedInputStream(
                            new FileInputStream(file));

                    while ((read = i_stream.read(data, 0, BUFFER)) != -1)
                        o_stream.write(data, 0, read);

                    o_stream.flush();
                    i_stream.close();
                    o_stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (file.isDirectory()) {
                String files[] = file.list();
                String dir = folder.getPath() + file.getName();

                if (!new File(dir).mkdir())
                    return false;

                for (String fil : files) copyFile(f + "/" + fil, dir);
            }
        } else if (SettingsProvider.getInstance(null)
                .getBoolean(SettingsProvider.KEY_ENABLE_ROOT, false)) {
            return RootUtils.copyFile(f, fol);
        }
        return false;
    }

    public static boolean moveFile(String f, String fol) {
        File file = new File(f);
        File folder = new File(fol);

        if (file.canWrite() && folder.isDirectory()
                && folder.canWrite()) {
            if (file.isFile()) {
                String file_name = file.getName();
                File cp_file = new File(folder.getPath() + File.separator + file_name);
                if (!file.renameTo(cp_file)) return false;
            } else if (file.isDirectory()) {
                String files[] = file.list();
                String dir = folder.getPath() + file.getName();

                if (!new File(dir).mkdir())
                    return false;

                for (String fil : files) if (!moveFile(f + "/" + fil, dir)) return false;
            }
        } else if (SettingsProvider.getInstance(null)
                .getBoolean(SettingsProvider.KEY_ENABLE_ROOT, false)
                && RootUtils.isRootAvailable()) {
            return RootUtils.moveFile(f, fol);
        }
        return false;
    }

    public static boolean deleteFile(String path) {
        File target = new File(path);

        if (target.isFile() && target.canWrite()) {
            if (target.delete()) return true;
        } else {
            if (target.isDirectory() && target.canRead()) {
                String[] file_list = target.list();

                if (file_list != null && file_list.length == 0) {
                    if (target.delete()) return true;
                } else if (file_list != null && file_list.length > 0) {

                    for (String aFile_list : file_list) {
                        File temp_f = new File(target.getAbsolutePath() + "/"
                                + aFile_list);

                        if (temp_f.isDirectory())
                            return deleteFile(temp_f.getAbsolutePath());
                        else if (temp_f.isFile()) {
                            if (temp_f.delete()) return true;
                        }
                    }
                }

                if (target.exists())
                    if (target.delete()) return true;
            }
            if (SettingsProvider.getInstance(null)
                    .getBoolean(SettingsProvider.KEY_ENABLE_ROOT, false)
                    && RootUtils.isRootAvailable()) {
                return RootUtils.deleteFile(path);
            }
        }
        return false;
    }

    public static String[] getFileProperties(File file) {
        BufferedReader in;
        String[] info = null;
        String line;

        try {
            if (SettingsProvider.getInstance(null)
                    .getBoolean(SettingsProvider.KEY_ENABLE_ROOT, false)) {
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

    public static boolean changeGroupOwner(File file, String owner, String group) {
        try {
            boolean useRoot = false;
            if (!file.canWrite() && SettingsProvider.getInstance(null)
                    .getBoolean(SettingsProvider.KEY_ENABLE_ROOT, false)
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

    public static boolean applyPermissions(File file, Permissions permissions) {
        try {
            boolean useSu = false;
            if (!file.canWrite() && SettingsProvider.getInstance(null)
                    .getBoolean(SettingsProvider.KEY_ENABLE_ROOT, false)
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

    public static String getExtension(File f) {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');

        if (i > 0 && i < s.length() - 1) {
            ext = s.substring(i + 1).toLowerCase();
        }
        return ext;
    }

    public static String removeExtension(String s) {

        String separator = System.getProperty("file.separator");
        String filename;

        // Remove the path upto the filename.
        int lastSeparatorIndex = s.lastIndexOf(separator);
        if (lastSeparatorIndex == -1) {
            filename = s;
        } else {
            filename = s.substring(lastSeparatorIndex + 1);
        }

        // Remove the extension.
        int extensionIndex = filename.lastIndexOf(".");
        if (extensionIndex == -1)
            return filename;

        return filename.substring(0, extensionIndex);
    }
}
