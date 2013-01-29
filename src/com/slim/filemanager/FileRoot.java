package com.slim.filemanager;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.os.PatternMatcher;
import android.util.Log;

import com.stericson.RootTools.Command;
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.Shell;

public class FileRoot extends File {

    private boolean isDir;
    private ArrayList<String> outLines;
    private String mPath;
    private String mName;
    private String mPerms;
    private String mSym;
    private Long mDate = null;
    private Long mSize = null;

    private static String TAG = "SFM-FileRoot";

    public FileRoot(String path) {
        super(path);
        mPath = path.substring(0, path.lastIndexOf("/"));
        if (!mPath.endsWith("/"))
            mPath += "/";
        mName = path.substring(path.lastIndexOf("/")+1);
        String w = "busybox ls -la '" + mPath + mName + "'";

        Log.d(TAG,"CONS Attempting to execute: [" + w + "]");
        outLines = new ArrayList<String>();
        try {
            Command cmd = new Command(0, w) {
                @Override
                public void output(int id, String line) {
                    if (line.indexOf("\n") > -1) {
                        for (String s : line.split("\n"))
                            output(id, s);
                    } else {
                        outLines.add(line);
                    }
                }
            };
            RootTools.getShell(true).add(cmd).waitForFinish();
            for (String line : outLines) {
                Log.d(TAG,"CONS Result: ["+line+"]");
                String parsedName = getItemName(line);
                Log.d(TAG,"CONS ParsedName: ["+parsedName+"]");
                Log.d(TAG,"CONS mName: ["+mName+"]");
                Log.d(TAG,"CONS getPath: ["+this.getPath()+"]");
                if (this.getPath().equals(parsedName) || parsedName.equals(".")) {
                    setDetails(line);
                    Log.d(TAG,"CONS mPath [" +mPath+"]");
                    Log.d(TAG,"CONS mName [" +mName+"]");
                    Log.d(TAG,"CONS mPerms [" +mPerms+"]");
                    Log.d(TAG,"CONS mSym [" +mSym+"]");
                    Log.d(TAG,"CONS mDate [" +mDate+"]");
                    Log.d(TAG,"CONS mSize [" +mSize+"]");
                    break;
                }
            }
        } catch(Exception e) {
            Log.e("Exception", e.getMessage());
        }

    }

    @Override
    public boolean canRead() {
        //always true... we're root baby!
        return true;
    }

    @Override
    public boolean canWrite() {
        //always true... we're root baby!
        return true;
    }

    @Override
    public boolean delete() {
        boolean result = true;
        String w = "busybox rm -r '" + this.getPath() + "'";
        Log.d(TAG,"delete Attempting to execute: [" + w + "]");
        outLines = new ArrayList<String>();
        try {
            Command cmd = new Command(0, w) {
                @Override
                public void output(int id, String line) {
                    if (line.indexOf("\n") > -1) {
                        for (String s : line.split("\n"))
                            output(id, s);
                    } else {
                        outLines.add(line);
                    }
                }
            };
            RootTools.getShell(true).add(cmd).waitForFinish();
            for (String line : outLines) {
                Log.d(TAG,"delete ["+line+"]");
                if (line.contains("failed") || line.contains("can't remove")) {
                    Log.d(TAG,"Delete failed [" + line +"]");
                    result = false;
                }
            }
        } catch(Exception e) {
            Log.e("Exception", e.getMessage());
        }
        return result;
    }

    @Override
    public boolean exists() {
        //if we got here then yes, we do
        //return true;
        return RootTools.exists(this.getPath());
    }

    @Override
    public boolean isDirectory() {
        if (mPerms != null) {
            if (mPerms.startsWith("d")) {
                return true;
            }
            if (mPerms.startsWith("l")) {
                return (new FileRoot(mSym)).isDirectory();
            }
        }
        return super.isDirectory();
    }

    @Override
    public boolean isFile() {
        if (mPerms != null) {
            if (mPerms.startsWith("-")) {
                return true;
            }
            if (mPerms.startsWith("l")) {
                return (new FileRoot(mSym)).isFile();
            }
        }
        return super.isFile();
    }

    @Override
    public long length() {
        if (mSize != null) {
            return mSize;
        } else {
            return 0;
        }
    }

    @Override
    public String[] list() {

        final ArrayList<String> dirContents = new ArrayList<String>();

        if (!this.isDirectory()) {
            return null;
        }
        String actualPath = this.getPath().endsWith("/") ? this.getPath() : this.getPath() + "/";
        final String w = "busybox ls -la '" + actualPath + "'";

        Log.d(TAG,"list Attempting to execute: [" + w + "]");
        outLines = new ArrayList<String>();
        try {
            Command cmd = new Command(0, w) {
                @Override
                public void output(int id, String line) {
                    if (line.indexOf("\n") > -1) {
                        for (String s : line.split("\n"))
                            output(id, s);
                    } else {
                        outLines.add(line);
                    }
                }
            };
            RootTools.getShell(true).add(cmd).waitForFinish();
            for (String line : outLines) {
                line = getItemName(line);
                if (!line.equals(".") && !line.equals(".."))
                    dirContents.add(line);
            }
        } catch(Exception e) {
            Log.e("Exception", e.getMessage());
        }
        return dirContents.toArray(new String[0]);
    }

    @Override
    public boolean mkdir() {
        boolean result = true;
        String w = "busybox mkdir '" + this.getPath() + "'";
        Log.d(TAG,"mkdir Attempting to execute: [" + w + "]");
        outLines = new ArrayList<String>();
        try {
            Command cmd = new Command(0, w) {
                @Override
                public void output(int id, String line) {
                    if (line.indexOf("\n") > -1) {
                        for (String s : line.split("\n"))
                            output(id, s);
                    } else {
                        outLines.add(line);
                    }
                }
            };
            RootTools.getShell(true).add(cmd).waitForFinish();
            for (String line : outLines) {
                Log.d(TAG,"mkdir ["+line+"]");
                if (line.contains("failed") || line.contains("can't create")) {
                    Log.d(TAG,"mkdir failed [" + line +"]");
                    result = false;
                }
            }
        } catch(Exception e) {
            Log.e("Exception", e.getMessage());
        }
        return result;
    }

    @Override
    public boolean renameTo(File dest) {
        boolean result = true;
        String w = "busybox mv '" + this.getPath() + "' '" + dest.getPath() + "'";
        Log.d(TAG,"renameTo Attempting to execute: [" + w + "]");
        outLines = new ArrayList<String>();
        try {
            Command cmd = new Command(0, w) {
                @Override
                public void output(int id, String line) {
                    if (line.indexOf("\n") > -1) {
                        for (String s : line.split("\n"))
                            output(id, s);
                    } else {
                        outLines.add(line);
                    }
                }
            };
            RootTools.getShell(true).add(cmd).waitForFinish();
            for (String line : outLines) {
                if (line.contains("failed") || line.contains("can't rename")) {
                    Log.d(TAG,"renameTo failed [" + line +"]");
                    result = false;
                }
            }
        } catch(Exception e) {
            Log.e("Exception", e.getMessage());
        }
        return result;
    }

    private String getItemName(String line) {

        String theName = "";

        PatternMatcher pmLong = new PatternMatcher(" [1-2][0-9][0-9][0-9]\\-[0-9]+\\-[0-9]+ ",
                PatternMatcher.PATTERN_SIMPLE_GLOB);
        boolean bLong = pmLong.match(line);
        Pattern p = Pattern.compile("[0-9][0-9]\\:[0-9][0-9] "
                + (bLong ? "[1-2][0-9][0-9][0-9] " : ""));
        Matcher m = p.matcher(line);
        boolean success = false;
        if (m.matches()) {
            theName = line.substring(m.end());
        }
        if (!success) {
            String[] parts = line.split(" +");
            if (parts.length >= 9)  {
                for (int i=8; i < parts.length; i++) {
                    theName += parts[i] + " ";
                }
                theName = theName.trim();
            } else {
                theName = parts[parts.length-1];
            }

        }
        if (theName.indexOf(" -> ") > -1) {
            mSym = theName.substring(theName.indexOf(" -> ") + 4);
            String wtfhaxbbq = theName.substring(0, theName.indexOf(" -> ")).trim();
            if (wtfhaxbbq.indexOf("/") > -1) {
                theName = wtfhaxbbq.substring(wtfhaxbbq.lastIndexOf("/") +1);
            } else {
                theName = wtfhaxbbq;
            }
        }
        return theName;
    }

    private void setDetails(String line) {
        java.text.DateFormat DateFormatInstance = new SimpleDateFormat("MMM dd yyyy HH:mm:ss", Locale.ENGLISH);

        String theName = "";

        PatternMatcher pmLong = new PatternMatcher(" [1-2][0-9][0-9][0-9]\\-[0-9]+\\-[0-9]+ ",
                PatternMatcher.PATTERN_SIMPLE_GLOB);
        boolean bLong = pmLong.match(line);
        Pattern p = Pattern.compile("[0-9][0-9]\\:[0-9][0-9] "
                + (bLong ? "[1-2][0-9][0-9][0-9] " : ""));
        Matcher m = p.matcher(line);
        boolean success = false;
        if (m.matches()) {
            theName = line.substring(m.end());
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
            theName = parts[parts.length - 1];
        }
        if (theName.indexOf(" -> ") > -1) {
            mSym = theName.substring(theName.indexOf(" -> ") + 4);
            theName = theName.substring(0, theName.indexOf(" -> ") - 1).trim();
        }
    }

}