package com.slim.slimfilemanager.utils;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class RootUtils {

    public static ArrayList<String> listFiles(String path, boolean showHidden) {
        if (!isRooted()) return null;
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
        if (!isRooted()) return null;
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

    public static boolean copyFile(String old, String newDir) {
        if (!isRooted()) return false;
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
        if (!isRooted()) return false;
        try {
            boolean remounted = false;
            if (!new File(newDir).canWrite()) {
                remounted = true;
                remountSystem("rw");
            }
            runCommand("mv -fr " + old + " " + newDir);
            if (remounted) {
                remountSystem("ro");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean remountSystem(String mountType) {
        if (!isRooted()) return false;
        BufferedReader reader = runCommand("busybox mount -o remount," + mountType + " /system \n");
        try {
            if (reader != null) reader.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean deleteFile(String path) {
        if (!isRooted()) return false;
        try {
            boolean remounted = false;
            if (!new File(path).canWrite()) {
                remountSystem("rw");
                remounted = true;
            }

            if (new File(path).isDirectory()) {
                runCommand("rm -f -r \"" + path + "\"\n");
            } else {
                runCommand("rm -r \"" + path + "\"\n");
            }
            if (remounted) {
                remountSystem("ro");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean isRooted() {
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

}
