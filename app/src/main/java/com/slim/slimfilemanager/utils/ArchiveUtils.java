package com.slim.slimfilemanager.utils;

import android.content.Context;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;
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

        location += FileUtil.removeExtension(new File(zipFile).getName()) + File.separator;

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

    public static String unTar(Context context, String input, String output) {
        File inputFile = new File(input);

        boolean deleteAfter = false;
        if (FileUtil.getExtension(inputFile).equals("gz")) {
            inputFile = new File(unGzip(input, BackgroundUtils.EXTRACTED_LOCATION));
            deleteAfter = true;
        }

        if (!output.endsWith(File.separator)) {
            output += File.separator;
        }

        output += FileUtil.removeExtension(inputFile.getName());

        File outputDir = new File(output);

        if (outputDir.exists()) {
            for (int i = 1; i < Integer.MAX_VALUE; i++) {
                File test = new File(outputDir + "-" + i);
                if (test.exists()) continue;
                outputDir = test;
                break;
            }
        }

        if (!outputDir.mkdirs()) return null;

        try {
            final InputStream is = new FileInputStream(inputFile);
            final TarArchiveInputStream tis = (TarArchiveInputStream)
                    new ArchiveStreamFactory().createArchiveInputStream("tar", is);

            TarArchiveEntry entry;
            while ((entry = (TarArchiveEntry) tis.getNextEntry()) != null) {
                File outputFile = new File(outputDir, entry.getName());
                if (entry.isDirectory()) {
                    if (!outputFile.exists()) {
                        if (!outputFile.mkdirs()) {
                            throw new IllegalStateException(
                                    String.format("Couldn't create directory %s.",
                                            outputFile.getAbsolutePath()));
                        }
                    }
                } else {
                    OutputStream out = new FileOutputStream(outputFile);
                    IOUtils.copy(tis, out);
                    out.close();
                }
            }
            tis.close();
            is.close();
        } catch (IOException|ArchiveException e) {
            e.printStackTrace();
        }

        if (deleteAfter) FileUtil.deleteFile(context, input);

        return outputDir.getAbsolutePath();
    }

    public static String unGzip(String input, String output) {
        File inputFile = new File(input);
        File outputFile = new File(output);

        outputFile = new File(outputFile, FileUtil.removeExtension(input));

        if (outputFile.exists()) {
            String ext = FileUtil.getExtension(outputFile);
            String file = FileUtil.removeExtension(outputFile.getAbsolutePath());
            for (int i = 1; i < Integer.MAX_VALUE; i++) {
                File test = new File(file + "-" + i + "." + ext);
                if (test.exists()) continue;
                outputFile = test;
                break;
            }
        }

        try {
            GZIPInputStream in = new GZIPInputStream(new FileInputStream(inputFile));
            FileOutputStream out = new FileOutputStream(outputFile);
            IOUtils.copy(in, out);
            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outputFile.getAbsolutePath();
    }
}
