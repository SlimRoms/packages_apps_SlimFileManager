package com.slim.slimfilemanager.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ArchiveUtils {

    private static final int BUFFER = 4096;

    public static String extractZipFiles(String zipFile, String location) {
        byte[] data = new byte[BUFFER];
        ZipEntry entry;
        ZipInputStream zipstream;

        if (!location.endsWith(File.separator)) {
            location += File.separator;
        }

        location += FileUtils.removeExtension(new File(zipFile).getName()) + File.separator;

        if (!new File(location).mkdirs()) return null;

        try {
            zipstream = new ZipInputStream(new FileInputStream(zipFile));

            while((entry = zipstream.getNextEntry()) != null) {
                String buildDir = location;
                String[] dirs = entry.getName().split("/");

                if(dirs.length > 0) {
                    for(int i = 0; i < dirs.length - 1; i++) {
                        buildDir += dirs[i] + "/";
                        if (!new File(buildDir).mkdirs()) return null;
                    }
                }

                int read;
                FileOutputStream out = new FileOutputStream(
                        location + entry.getName());
                while((read = zipstream.read(data, 0, BUFFER)) != -1)
                    out.write(data, 0, read);

                zipstream.closeEntry();
                out.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return location;
    }

    public static String createZipFile(String location, ArrayList<String> files) {
        try {
            if (!new File(location).exists())
                if (!new File(location).mkdirs()) return null;
            FileOutputStream dest = new FileOutputStream(location + "/test.zip");
            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(
                    dest));

            for (String s : files) {
                File file = new File(s);

                if (file.isDirectory()) {
                    zipFolder(out, file, file.getParent().length());
                } else {
                    zipFile(out, file);
                }
            }

            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return location;
    }

    private static void zipFolder(ZipOutputStream out, File folder,
                                     int basePathLength) throws IOException {
        File[] fileList = folder.listFiles();

        for (File file : fileList) {
            if (file.isDirectory()) {
                zipFolder(out, file, basePathLength);
            } else {
                BufferedInputStream origin;
                byte data[] = new byte[BUFFER];
                String unmodifiedFilePath = file.getPath();
                String relativePath = unmodifiedFilePath
                        .substring(basePathLength);

                FileInputStream fi = new FileInputStream(unmodifiedFilePath);
                origin = new BufferedInputStream(fi, BUFFER);
                ZipEntry entry = new ZipEntry(relativePath);
                out.putNextEntry(entry);
                int count;
                while ((count = origin.read(data, 0, BUFFER)) != -1) {
                    out.write(data, 0, count);

                }
                origin.close();
            }
        }
    }

    private static void zipFile(ZipOutputStream out, File file)
            throws IOException {
        BufferedInputStream origin;
        byte data[] = new byte[BUFFER];
        String str = file.getPath();

        FileInputStream fi = new FileInputStream(str);
        origin = new BufferedInputStream(fi, BUFFER);
        ZipEntry entry = new ZipEntry(str.substring(str.lastIndexOf("/") + 1));
        out.putNextEntry(entry);
        int count;
        while ((count = origin.read(data, 0, BUFFER)) != -1) {
            out.write(data, 0, count);

        }
        origin.close();
    }
}
