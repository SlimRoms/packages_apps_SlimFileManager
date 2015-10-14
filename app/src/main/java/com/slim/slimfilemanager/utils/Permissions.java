package com.slim.slimfilemanager.utils;

import android.content.Context;

import java.io.File;

public class Permissions {

    public String owner;
    public String group;

    public boolean userRead;
    public boolean userWrite;
    public boolean userExecute;

    public boolean groupRead;
    public boolean groupWrite;
    public boolean groupExecute;

    public boolean otherRead;
    public boolean otherWrite;
    public boolean otherExecute;

    public Permissions() {
    }

    public void loadFromFile(Context context, File file) {
        String[] fileInfo = FileUtils.getFileProperties(context, file);
        String line = null;

        if (fileInfo != null) {
            owner = fileInfo[1];
            group = fileInfo[2];
            line = fileInfo[0];
        }

        if (line == null || line.length() != 10) {
            return;
        }

        userRead = line.charAt(1) == 'r';
        userWrite = line.charAt(2) == 'w';
        userExecute = line.charAt(3) == 'x';

        groupRead = line.charAt(4) == 'r';
        groupWrite = line.charAt(5) == 'w';
        groupExecute = line.charAt(6) == 'x';

        otherRead = line.charAt(7) == 'r';
        otherWrite = line.charAt(8) == 'w';
        otherExecute = line.charAt(9) == 'x';
    }

    public String getOctalPermissions() {
        byte user = 0;
        byte group = 0;
        byte other = 0;

        if (userRead) {
            user += 4;
        }
        if (userWrite) {
            user += 2;
        }
        if (userExecute) {
            user += 1;
        }

        if (groupRead) {
            group += 4;
        }
        if (groupWrite) {
            group += 2;
        }
        if (groupExecute) {
            group += 1;
        }

        if (otherRead) {
            other += 4;
        }
        if (otherWrite) {
            other += 2;
        }
        if (otherExecute) {
            other += 1;
        }

        return String.valueOf(user) + group + other;
    }

    public String getString() {
        String string = "";
        string += userRead ? 'r' : '-';
        string += userWrite ? 'w' : '-';
        string += userExecute ? 'x' : '-';
        string += groupRead ? 'r' : '-';
        string += groupWrite ? 'w' : '-';
        string += groupExecute ? 'x' : '-';
        string += otherRead ? 'r' : '-';
        string += otherWrite ? 'w' : '-';
        string += otherExecute ? 'x' : '-';
        return string;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Permissions)) {
            return false;
        }
        final Permissions p = (Permissions) o;
        return getString().equals(p.getString()) && owner.equals(p.owner) && group.equals(p.group);
    }
}
