package com.slim.slimfilemanager.utils;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class RootUtils {

    protected final static Pattern sEscape = Pattern.compile("([\"\'`\\\\])");

    public static ArrayList<String> listFiles(String path, boolean showHidden) {
        if (!isRootAvailable()) return null;
        ArrayList<String> mDirContent = new ArrayList<>();
        BufferedReader in;

        try {
            in = runCommand("ls -a " + "\"" + path + "\"\n");

            if (in == null) return null;

            String line;
            while ((line = in.readLine()) != null) {
                if (!showHidden) {
                    if (line.charAt(0) != '.')
                        mDirContent.add(path + "/" + line);
                } else {
                    mDirContent.add(path + "/" + line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return mDirContent;
    }

    public static BufferedReader runCommand(String cmd) {
        if (!isRootAvailable()) return null;
        BufferedReader reader;
        try {
            Process process = Runtime.getRuntime().exec("su");
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

    public static BufferedReader runCommandError(String cmd) {
        if (!isRootAvailable()) return null;
        BufferedReader reader;
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(
                    process.getOutputStream());
            os.writeBytes(cmd + "\n");
            os.writeBytes("exit\n");
            reader = new BufferedReader(new InputStreamReader(
                    process.getErrorStream()));
            os.flush();
            return reader;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean copyFile(String old, String newDir) {
        if (!isRootAvailable()) return false;
        try {
            boolean remounted = false;
            if (!new File(newDir).canWrite()) {
                remounted = true;
                remountSystem("rw");
            }
            runCommand("cp -fr " + old + " " + newDir);
            if (remounted) {
                remountSystem("ro");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean moveFile(String old, String newDir) {
        if (!isRootAvailable()) return false;
        try {
            remountSystem("rw");
            runCommand("mv -f " + old + " " + newDir);
            remountSystem("ro");
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return new File(newDir).exists();
    }

    public static boolean remountSystem(String mountType) {
        if (!isRootAvailable()) return false;
        BufferedReader reader = runCommand("mount -o remount," + mountType + " /system \n");
        try {
            if (reader != null) reader.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean deleteFile(String path) {
        if (!isRootAvailable()) return false;
        try {
            remountSystem("rw");

            if (new File(path).isDirectory()) {
                runCommand("rm -rf '" + path + "'\n");
            } else {
                runCommand("rm -rf '" + path + "'\n");
            }

            remountSystem("ro");
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return !new File(path).exists();
    }

    public static boolean createFile(File file) {
        if (!isRootAvailable()) return false;
        remountSystem("rw");
        runCommand("touch " + file.getAbsolutePath());
        remountSystem("ro");
        return true;
    }

    public static boolean createFolder(File folder) {
        if (!isRootAvailable()) return false;
        remountSystem("rw");
        runCommand("mkdir " + folder.getPath());
        remountSystem("ro");
        return true;
    }

    /*public static boolean exists(File file) {
        try {
            BufferedReader reader = runCommandError("test -d " + file.getPath());
            return reader != null && reader.read() == 0;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }*/

    public static boolean isRootAvailable() {
        return findBinary("su");
    }



    public static boolean findBinary(String binaryName) {
        boolean found = false;
        String[] places = {"/sbin/", "/system/bin/", "/system/xbin/", "/data/local/xbin/",
                "/data/local/bin/", "/system/sd/xbin/", "/system/bin/failsafe/", "/data/local/"};
        for (String where : places) {
            if ( new File( where + binaryName ).exists() ) {
                found = true;
                break;
            }
        }
        return found;
    }

    public static boolean isSuEnabled() {
        int value = 0;
        try {
            value = Integer.valueOf(Utils.getProperty("persist.sys.root_access"));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return (value == 1 || value == 3);
    }

    public static void writeFile(Context context, String content, File file, String encoding) {
        if (!isRootAvailable()) return;
        String redirect = ">";
        String[] input = content.trim().split("\n");
        remountSystem("rw");
        for (String line : input) {
            String l = sEscape.matcher(line).replaceAll("\\\\$1");
            runCommand("echo '" + l + "' " + redirect + " '" + file.getAbsolutePath() + "' ");
            redirect = ">>";
        }
    }

    public static String readFile(Uri uri) {
        String r = "";
        try {
            BufferedReader br = runCommand("cat " + uri.getPath() + "\n");
            if (br == null) return null;
            String line;
            while ((line = br.readLine()) != null) {
                r += line;
            }
        } catch (IOException e) {
            // ignore
        }
        return r;
    }
}
