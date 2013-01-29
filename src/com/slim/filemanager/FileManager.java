/*
    Open Manager For Tablets, an open source file manager for the Android system
    Copyright (C) 2011  Joe Berria <nexesdevelopment@gmail.com>

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Locale;
import java.util.Stack;
import java.io.File;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import android.os.PatternMatcher;
import android.util.Log;
import com.stericson.RootTools.Command;
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.Shell;
/**
 * This class is completely modular, which is to say that it has
 * no reference to the any GUI activity. This class could be taken
 * and placed into in other java (not just Android) project and work.
 * <br>
 * <br>
 * This class handles all file and folder operations on the system.
 * This class dictates how files and folders are copied/pasted, (un)zipped
 * renamed and searched. The EventHandler class will generally call these
 * methods and have them performed in a background thread. Threading is not
 * done in this class.
 *
 * @author Joe Berria
 *
 */
public class FileManager {
    private static final int BUFFER =         2048;
    private static final int SORT_NONE =     0;
    private static final int SORT_ALPHA =     1;
    private static final int SORT_TYPE =     2;
    private static final int SORT_SIZE =     3;
    private static final int SORT_FF =     4;

    private boolean mShowHiddenFiles = false;
    private boolean mRootMode = false;
    private int mSortType = SORT_ALPHA;
    private long mDirSize = 0;
    private Stack<String> mPathStack;
    private ArrayList<String> mDirContent;
    private boolean isDir = false;

    private final String TAG = "SFM-FileManager";
    /**
     * Constructs an object of the class
     * <br>
     * this class uses a stack to handle the navigation of directories.
     */
    public FileManager() {
        mDirContent = new ArrayList<String>();
        mPathStack = new Stack<String>();

        mPathStack.push("/");
        mPathStack.push(mPathStack.peek() + "sdcard");
    }

    /**
     * This will return a string of the current directory path
     * @return the current directory
     */
    public String getCurrentDir() {
        return mPathStack.peek();
    }

    /**
     * This will return a string of the current home path.
     * @return    the home directory
     */
    public ArrayList<String> setHomeDir(String name) {
        //This will eventually be placed as a settings item
        mPathStack.clear();
        mPathStack.push("/");
        mPathStack.push(name);

        return populate_list();
    }

    /**
     * This will determine if hidden files and folders will be visible to the
     * user.
     * @param choice    true if user is veiwing hidden files, false otherwise
     */
    public void setShowHiddenFiles(boolean choice) {
        mShowHiddenFiles = choice;
    }

    /**
     * This will determine if root mode is active
     * @param choice    true if user is in root mode
     */
    public void setRootMode(boolean choice) {
        mRootMode = choice;
    }

    /**
     * This will determine if root mode is active
     * @param choice    true if user is in root mode
     */
    public boolean getRootMode() {
        return mRootMode;
    }

    /**
     *
     * @param type
     */
    public void setSortType(int type) {
        mSortType = type;
    }

    /**
     * This will return a string that represents the path of the previous path
     * @return    returns the previous path
     */
    public ArrayList<String> getPreviousDir() {
        int size = mPathStack.size();

        if (size >= 2)
            mPathStack.pop();

        else if(size == 0)
            mPathStack.push("/");

        return populate_list();
    }

    /**
     *
     * @param path
     * @param isFullPath
     * @return
     */
    public ArrayList<String> getNextDir(String path, boolean isFullPath) {
        int size = mPathStack.size();

        if(!path.equals(mPathStack.peek()) && !isFullPath) {
            if(size == 1)
                mPathStack.push("/" + path);
            else
                mPathStack.push(mPathStack.peek() + "/" + path);
        }

        else if(!path.equals(mPathStack.peek()) && isFullPath) {
            mPathStack.push(path);
        }

        return populate_list();
    }

    /**
     *
     * @param old        the file to be copied
     * @param newDir    the directory to move the file to
     * @return
     */
    public int copyToDirectory(String old, String newDir) {
        File old_file;
        File temp_dir;
        if (getRootMode() && RootTools.isAccessGiven()) {
            Log.d(TAG, "c2d ROOT!");
            old_file = new FileRoot(old);
            temp_dir = new FileRoot(newDir);
            if (old_file.isFile()) {
                Log.d(TAG, "c2d copying File [" + old + "]");
                if (RootTools.copyFile(old, newDir, false, true)) {
                    return 0;
                } else {
                    return -1;
                }
            } else if (old_file.isDirectory()) {
                Log.d(TAG, "c2d copying Dir [" + old + "]");
                String files[] = old_file.list();
                String dir = newDir + old.substring(old.lastIndexOf("/"), old.length());
                int len = files.length;

                if(!new FileRoot(dir).mkdir()) {
                    Log.e(TAG, "Couldn't create new dir [" + dir + "]");
                    return -1;
                }

                for(int i = 0; i < len; i++) {
                    if ((new FileRoot(old + "/" + files[i])).isDirectory()) {
                        Log.d(TAG, "c2d Found a subdir [" + old + "/" + files[i] + "]");
                        if (copyToDirectory(old + "/" + files[i], dir) < 0) {
                            return -1;
                        }
                    }
                    else {
                        Log.d(TAG, "c2d Copying dir file [" + files[i] + "]");
                        if (!RootTools.copyFile(old + "/" + files[i], dir, false, true)) {
                            return -1;
                        }
                    }
                }
                 return 0;
            }
            return -1;
        } else {
            Log.d(TAG, "c2d Normal");
            old_file = new File(old);
            temp_dir = new File(newDir);
            byte[] data = new byte[BUFFER];
            int read = 0;

            if(old_file.isFile() && temp_dir.isDirectory() && temp_dir.canWrite()){
                String file_name = old.substring(old.lastIndexOf("/"), old.length());
                File cp_file = new File(newDir + file_name);

                try {
                    BufferedOutputStream o_stream = new BufferedOutputStream(
                                                    new FileOutputStream(cp_file));
                    BufferedInputStream i_stream = new BufferedInputStream(
                                                   new FileInputStream(old_file));

                    while((read = i_stream.read(data, 0, BUFFER)) != -1)
                        o_stream.write(data, 0, read);

                    o_stream.flush();
                    i_stream.close();
                    o_stream.close();

                } catch (FileNotFoundException e) {
                    Log.e("FileNotFoundException", e.getMessage());
                    return -1;

                } catch (IOException e) {
                    Log.e("IOException", e.getMessage());
                    return -1;
                }

            }else if(old_file.isDirectory() && temp_dir.isDirectory() && temp_dir.canWrite()) {
                String files[] = old_file.list();
                String dir = newDir + old.substring(old.lastIndexOf("/"), old.length());
                int len = files.length;

                if(!new File(dir).mkdir())
                    return -1;

                for(int i = 0; i < len; i++)
                    copyToDirectory(old + "/" + files[i], dir);

            } else if(!temp_dir.canWrite())
                return -1;

            return 0;
        }
    }

    /**
     *
     * @param zipName
     * @param toDir
     * @param fromDir
     */
    public void extractZipFilesFromDir(String zipName, String toDir, String fromDir) {
        if(!(toDir.charAt(toDir.length() - 1) == '/'))
            toDir += "/";
        if(!(fromDir.charAt(fromDir.length() - 1) == '/'))
            fromDir += "/";

        String org_path = fromDir + zipName;

        extractZipFiles(org_path, toDir);
    }

    /**
     *
     * @param zip_file
     * @param directory
     */
    public void extractZipFiles(String zip_file, String directory) {
        byte[] data = new byte[BUFFER];
        String name, path, zipDir;
        ZipEntry entry;
        ZipInputStream zipstream;

        if(!(directory.charAt(directory.length() - 1) == '/'))
            directory += "/";

        if(zip_file.contains("/")) {
            path = zip_file;
            name = path.substring(path.lastIndexOf("/") + 1,
                                  path.length() - 4);
            zipDir = directory + name + "/";

        } else {
            path = directory + zip_file;
            name = path.substring(path.lastIndexOf("/") + 1,
                                     path.length() - 4);
            zipDir = directory + name + "/";
        }

        new File(zipDir).mkdir();

        try {
            zipstream = new ZipInputStream(new FileInputStream(path));

            while((entry = zipstream.getNextEntry()) != null) {
                String buildDir = zipDir;
                String[] dirs = entry.getName().split("/");

                if(dirs != null && dirs.length > 0) {
                    for(int i = 0; i < dirs.length - 1; i++) {
                        buildDir += dirs[i] + "/";
                        new File(buildDir).mkdir();
                    }
                }

                int read = 0;
                FileOutputStream out = new FileOutputStream(
                                        zipDir + entry.getName());
                while((read = zipstream.read(data, 0, BUFFER)) != -1)
                    out.write(data, 0, read);

                zipstream.closeEntry();
                out.close();
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param path
     */
    public void createZipFile(String path) {
        File dir = new File(path);
        String[] list = dir.list();
        String name = path.substring(path.lastIndexOf("/"), path.length());
        String _path;

        if(!dir.canRead() || !dir.canWrite())
            return;

        int len = list.length;

                if(path.charAt(path.length() -1) != '/')
            _path = path + "/";
        else
            _path = path;

        try {
            ZipOutputStream zip_out = new ZipOutputStream(
                                      new BufferedOutputStream(
                                      new FileOutputStream(_path + name + ".zip"), BUFFER));

            for (int i = 0; i < len; i++)
                zip_folder(new File(_path + list[i]), zip_out);

            zip_out.close();

        } catch (FileNotFoundException e) {
            Log.e("File not found", e.getMessage());

        } catch (IOException e) {
            Log.e("IOException", e.getMessage());
        }
    }

    /**
     *
     * @param filePath
     * @param newName
     * @return
     */
    public int renameTarget(String filePath, String newName) {
        if(newName.length() < 1) {
            Log.d(TAG, "renameTarget Cannot Rename: No name found!");
            return -1;
        }

        boolean useRoot = false;
        File src = new File(filePath);
        if (!src.exists()) {
            Log.d(TAG, "renameTarget cannot find src, trying FileRoot");
            src = new FileRoot(filePath);
            if (!src.exists()) {
                Log.d(TAG, "renameTarget cannot find FileRoot src");
                return -1;
            } else {
                useRoot = true;
            }
        }
        File dest;

        String temp = filePath.substring(0, filePath.lastIndexOf("/"));

        if (!useRoot) {
            dest = new File(temp + "/" + newName);
        } else {
            dest = new FileRoot(temp + "/" + newName);
        }
        if (dest.exists()) {
            Log.d(TAG, "renameTarget Cannot Rename: Destination already exists!");
            return -1;
        }
        if(src.renameTo(dest)) {
            return 0;
        } else {
            return -1;
        }
    }

    /**
     *
     * @param path
     * @param name
     * @return
     */
    public int createDir(String path, String name) {
        int len = path.length();

        if(len < 1 || len < 1)
            return -1;

        if(path.charAt(len - 1) != '/')
            path += "/";

        if (new File(path+name).mkdir()) {
            return 0;
        } else if (new FileRoot(path+name).mkdir()) {
            return 0;
        }

        return -1;
    }

    /**
     * The full path name of the file to delete.
     *
     * @param path name
     * @return
     */
    public int deleteTarget(String path) {
        File target;

        if (this.getRootMode() && RootTools.isAccessGiven()) {
            Log.d(TAG, "deleteTarget - FileRoot");
            target = new FileRoot(path);
        } else {
            Log.d(TAG, "deleteTarget - File");
            target = new File(path);
        }

        if(target.exists() && target.isFile() && target.canWrite()) {
            Log.d(TAG, "deleteTarget - it is a File");
            if (target.delete()) {
                return 0;
            } else {
                return -1;
            }
        }

        else if(target.exists() && target.isDirectory() && target.canRead()) {
            Log.d(TAG, "deleteTarget - it is a Directory");
            String[] file_list = target.list();

            if(file_list != null && file_list.length == 0) {
                if (target.delete()) {
                    return 0;
                } else {
                    return -1;
                }
            } else if(file_list != null && file_list.length > 0) {

                for(int i = 0; i < file_list.length; i++) {
                    File temp_f = new File(target.getAbsolutePath() + "/" + file_list[i]);

                    if(temp_f.isDirectory())
                        deleteTarget(temp_f.getAbsolutePath());
                    else if(temp_f.isFile())
                        temp_f.delete();
                }
            }
            if(target.exists())
                if (target.delete()) {
                    return 0;
                } else {
                    return -1;
                }
        }
        Log.d(TAG, "deleteTarget - Couldn't find and/or no Read/Write on Target");
        return -1;
    }

    /**
     *
     * @param name
     * @return
     */
    public boolean isDirectory(String name) {
        if (new File(mPathStack.peek() + "/" + name).isDirectory()) {
            return true;
        } else if (new FileRoot(mPathStack.peek() + "/" + name).isDirectory()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * converts integer from wifi manager to an IP address.
     *
     * @param des
     * @return
     */
    public static String integerToIPAddress(int ip) {
        String ascii_address = "";
        int[] num = new int[4];

        num[0] = (ip & 0xff000000) >> 24;
        num[1] = (ip & 0x00ff0000) >> 16;
        num[2] = (ip & 0x0000ff00) >> 8;
        num[3] = ip & 0x000000ff;

        ascii_address = num[0] + "." + num[1] + "." + num[2] + "." + num[3];

        return ascii_address;
     }

    /**
     *
     * @param dir
     * @param pathName
     * @return
     */
    public ArrayList<String> searchInDirectory(String dir, String pathName) {
        ArrayList<String> names = new ArrayList<String>();
        search_file(dir, pathName, names);

        return names;
    }

    /**
     *
     * @param path
     * @return
     */
    public long getDirSize(String path) {
        get_dir_size(new File(path));

        return mDirSize;
    }

    private static final Comparator alph = new Comparator<String>() {
        @Override
        public int compare(String arg0, String arg1) {
            return arg0.toLowerCase().compareTo(arg1.toLowerCase());
        }
    };

    private final Comparator size = new Comparator<String>() {
        @Override
        public int compare(String arg0, String arg1) {
            String dir = mPathStack.peek();
            Long first = new File(dir + "/" + arg0).length();
            Long second = new File(dir + "/" + arg1).length();

            return first.compareTo(second);
        }
    };

    private final Comparator type = new Comparator<String>() {
        @Override
        public int compare(String arg0, String arg1) {
            String ext = null;
            String ext2 = null;
            int ret;

            try {
                ext = arg0.substring(arg0.lastIndexOf(".") + 1, arg0.length()).toLowerCase();
                ext2 = arg1.substring(arg1.lastIndexOf(".") + 1, arg1.length()).toLowerCase();

            } catch (IndexOutOfBoundsException e) {
                return 0;
            }
            ret = ext.compareTo(ext2);

            if (ret == 0)
                    return arg0.toLowerCase().compareTo(arg1.toLowerCase());

            return ret;
        }
    };

    /* (non-Javadoc)
     * this function will take the string from the top of the directory stack
     * and list all files/folders that are in it and return that list so
     * it can be displayed. Since this function is called every time we need
     * to update the the list of files to be shown to the user, this is where
     * we do our sorting (by type, alphabetical, etc).
     *
     * @return
     */
    private ArrayList<String> populate_list() {

        if(!mDirContent.isEmpty())
            mDirContent.clear();

        File file = (mRootMode && RootTools.isAccessGiven()) ? new FileRoot(mPathStack.peek()) : new File(mPathStack.peek());

        Log.d(TAG,"PATH: [" + file.getPath() + "]");

        if(file.exists() && file.canRead()) {
            String[] list = file.list();
            int len = list.length;

            /* add files/folder to arraylist depending on hidden status */
            for (int i = 0; i < len; i++) {
                if(!mShowHiddenFiles) {
                    if(list[i].toString().charAt(0) != '.')
                        mDirContent.add(list[i]);

                } else {
                    mDirContent.add(list[i]);
                }
            }

            /* sort the arraylist that was made from above for loop */
            switch(mSortType) {
                case SORT_NONE:
                    //no sorting needed
                    break;

                case SORT_ALPHA:
                    Object[] tt = mDirContent.toArray();
                    mDirContent.clear();

                    Arrays.sort(tt, alph);

                    for (Object a : tt){
                        mDirContent.add((String)a);
                    }
                    break;

                case SORT_SIZE:
                    int index = 0;
                    Object[] size_ar = mDirContent.toArray();
                    String dir = mPathStack.peek();

                    Arrays.sort(size_ar, size);

                    mDirContent.clear();
                    for (Object a : size_ar) {
                        if(new File(dir + "/" + (String)a).isDirectory())
                            mDirContent.add(index++, (String)a);
                        else
                            mDirContent.add((String)a);
                    }
                    break;

                case SORT_TYPE:
                    int dirindex = 0;
                    Object[] type_ar = mDirContent.toArray();
                    String current = mPathStack.peek();

                    Arrays.sort(type_ar, type);
                    mDirContent.clear();

                    for (Object a : type_ar) {
                        if(new File(current + "/" + (String)a).isDirectory())
                            mDirContent.add(dirindex++, (String)a);
                        else
                            mDirContent.add((String)a);
                    }
                    break;

                case SORT_FF:
                    int ffindex = 0;
                    Object[] ff = mDirContent.toArray();
                    String curr = mPathStack.peek();

                    Arrays.sort(ff, alph);
                    mDirContent.clear();

                    for (Object a : ff) {
                        if(new File(curr + "/" + (String)a).isDirectory())
                            mDirContent.add(ffindex++, (String)a);
                        else
                            mDirContent.add((String)a);
                    }
                    break;
            }

        } else {
            mDirContent.add("Empty");
        }
        return mDirContent;
    }

    private String getItemName(String line) {
        java.text.DateFormat DateFormatInstance = new SimpleDateFormat("MMM dd yyyy HH:mm:ss", Locale.ENGLISH);
        String mPath;
        String mName = line;  //FIXME
        String mPerms;
        String mSym;
        Long mDate = null;
        Long mSize = null;

        PatternMatcher pmLong = new PatternMatcher(" [1-2][0-9][0-9][0-9]\\-[0-9]+\\-[0-9]+ ",
                PatternMatcher.PATTERN_SIMPLE_GLOB);
        boolean bLong = pmLong.match(line);
        Pattern p = Pattern.compile("[0-9][0-9]\\:[0-9][0-9] "
                + (bLong ? "[1-2][0-9][0-9][0-9] " : ""));
        Matcher m = p.matcher(line);
        boolean success = false;
        if (m.matches()) {
            mName = line.substring(m.end());
            try {
                String sDate = line.substring(m.start(), m.end() - 1).trim();
                mDate = Date.parse(sDate);
                mSize = Long.parseLong(line.substring(line.lastIndexOf(" ", m.start()),
                        m.start() - 1).trim());
                success = true;
            } catch (Exception e) {
                Log.e("Couldn't parse date.", e.getMessage());
            }
            mPerms = line.split(" ")[0];
        }
        if (!success) {
            String[] parts = line.split(" +");
            if (parts.length > 5) {
                mPerms = parts[0];
                int i = 4;
                try {
                    if (parts.length >= 7)
                        mSize = Long.parseLong(parts[i++]);
                } catch (NumberFormatException e) {
                }
                try {
                    if (parts[i + 1].matches("(Sun|Mon|Tue|Wed|Thu|Fri|Sat)"))
                        i++;
                    String sDate = parts[i + 1] + " " + parts[i + 2];
                    if (parts.length > i + 3 && parts[i + 4].length() <= 4)
                        sDate += " " + parts[i + 4];
                    else {
                        sDate += " " + (Calendar.getInstance().get(Calendar.YEAR) + 1900);
                        i--;
                    }
                    if (parts.length > i + 2 && parts[i + 3].indexOf(":") > -1)
                        sDate += " " + parts[i + 3]; // Add Time
                    mDate = DateFormatInstance.parse(sDate).getTime();
                    success = true;
                } catch (Exception e) {
                }
            }
            mName = parts[parts.length - 1];
        }
        if (mName.indexOf(" -> ") > -1) {
            mSym = mName.substring(mName.indexOf(" -> ") + 4);
            mName = mName.substring(0, mName.indexOf(" -> ") - 1).trim();
        }

        return mName;
    }

    private String getItemPerms(String line) {
        java.text.DateFormat DateFormatInstance = new SimpleDateFormat("MMM dd yyyy HH:mm:ss", Locale.ENGLISH);
        String mPath;
        String mName = line;  //FIXME
        String mPerms ="";
        String mSym;
        Long mDate = null;
        Long mSize = null;

        PatternMatcher pmLong = new PatternMatcher(" [1-2][0-9][0-9][0-9]\\-[0-9]+\\-[0-9]+ ",
                PatternMatcher.PATTERN_SIMPLE_GLOB);
        boolean bLong = pmLong.match(line);
        Pattern p = Pattern.compile("[0-9][0-9]\\:[0-9][0-9] "
                + (bLong ? "[1-2][0-9][0-9][0-9] " : ""));
        Matcher m = p.matcher(line);
        boolean success = false;
        if (m.matches()) {
            mName = line.substring(m.end());
            try {
                String sDate = line.substring(m.start(), m.end() - 1).trim();
                mDate = Date.parse(sDate);
                mSize = Long.parseLong(line.substring(line.lastIndexOf(" ", m.start()),
                        m.start() - 1).trim());
                success = true;
            } catch (Exception e) {
                Log.e("Couldn't parse date.", e.getMessage());
            }
            mPerms = line.split(" ")[0];
        }
        if (!success) {
            String[] parts = line.split(" +");
            if (parts.length > 5) {
                mPerms = parts[0];
                int i = 4;
                try {
                    if (parts.length >= 7)
                        mSize = Long.parseLong(parts[i++]);
                } catch (NumberFormatException e) {
                }
                try {
                    if (parts[i + 1].matches("(Sun|Mon|Tue|Wed|Thu|Fri|Sat)"))
                        i++;
                    String sDate = parts[i + 1] + " " + parts[i + 2];
                    if (parts.length > i + 3 && parts[i + 4].length() <= 4)
                        sDate += " " + parts[i + 4];
                    else {
                        sDate += " " + (Calendar.getInstance().get(Calendar.YEAR) + 1900);
                        i--;
                    }
                    if (parts.length > i + 2 && parts[i + 3].indexOf(":") > -1)
                        sDate += " " + parts[i + 3]; // Add Time
                    mDate = DateFormatInstance.parse(sDate).getTime();
                    success = true;
                } catch (Exception e) {
                }
            }
            mName = parts[parts.length - 1];
        }
        if (mName.indexOf(" -> ") > -1) {
            mSym = mName.substring(mName.indexOf(" -> ") + 4);
            mName = mName.substring(0, mName.indexOf(" -> ") - 1).trim();
        }

        return mPerms;
    }

    /*
     *
     * @param file
     * @param zout
     * @throws IOException
     */
    private void zip_folder(File file, ZipOutputStream zout) throws IOException {
        byte[] data = new byte[BUFFER];
        int read;

        if(file.isFile()){
            ZipEntry entry = new ZipEntry(file.getName());
            zout.putNextEntry(entry);
            BufferedInputStream instream = new BufferedInputStream(
                                           new FileInputStream(file));

            while((read = instream.read(data, 0, BUFFER)) != -1)
                zout.write(data, 0, read);

            zout.closeEntry();
            instream.close();

        } else if (file.isDirectory()) {
            String[] list = file.list();
            int len = list.length;

            for(int i = 0; i < len; i++)
                zip_folder(new File(file.getPath() +"/"+ list[i]), zout);
        }
    }

    /*
     *
     * @param path
     */
    private void get_dir_size(File path) {
        File[] list = path.listFiles();
        int len;

        if(list != null) {
            len = list.length;

            for (int i = 0; i < len; i++) {
                try {
                    if(list[i].isFile() && list[i].canRead()) {
                        mDirSize += list[i].length();

                    } else if(list[i].isDirectory() && list[i].canRead() && !isSymlink(list[i])) {
                        get_dir_size(list[i]);
                    }
                } catch(IOException e) {
                    Log.e("IOException", e.getMessage());
                }
            }
        }
    }

    // Inspired by org.apache.commons.io.FileUtils.isSymlink()
    private static boolean isSymlink(File file) throws IOException {
        File fileInCanonicalDir = null;
        if (file.getParent() == null) {
            fileInCanonicalDir = file;
        } else {
            File canonicalDir = file.getParentFile().getCanonicalFile();
            fileInCanonicalDir = new File(canonicalDir, file.getName());
        }
        return !fileInCanonicalDir.getCanonicalFile().equals(fileInCanonicalDir.getAbsoluteFile());
    }

    /*
     * (non-JavaDoc)
     * I dont like this method, it needs to be rewritten. Its hacky in that
     * if you are searching in the root dir (/) then it is not going to be treated
     * as a recursive method so the user dosen't have to sit forever and wait.
     *
     * I will rewrite this ugly method.
     *
     * @param dir        directory to search in
     * @param fileName    filename that is being searched for
     * @param n            ArrayList to populate results
     */
    private void search_file(String dir, String fileName, ArrayList<String> n) {
        File root_dir = new File(dir);
        String[] list = root_dir.list();

        if(list != null && root_dir.canRead()) {
            int len = list.length;

            for (int i = 0; i < len; i++) {
                File check = new File(dir + "/" + list[i]);
                String name = check.getName();

                if(check.isFile() && name.toLowerCase().
                                        contains(fileName.toLowerCase())) {
                    n.add(check.getPath());
                }
                else if(check.isDirectory()) {
                    if(name.toLowerCase().contains(fileName.toLowerCase()))
                        n.add(check.getPath());

                    else if(check.canRead() && !dir.equals("/"))
                        search_file(check.getAbsolutePath(), fileName, n);
                }
            }
        }
    }
}
