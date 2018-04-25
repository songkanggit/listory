package com.listory.songkang.utils;

import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by songkang on 2017/6/25.
 */

public class FileUtil {
    private static final boolean DEFAULT_IS_APPEND = true; //
    private static final int DEFAULT_CHAR_SIZE = 1024;

    public static String getFileSimpleName(String path) {
        String fileName = path.trim();
        return fileName.substring(fileName.lastIndexOf("/") + 1);
    }

    public static boolean writeStringToFile(File file, String content) {
        return writeStringToFile(file, content, DEFAULT_IS_APPEND);
    }

    private static boolean writeStringToFile(File file, String content,
                                             boolean isAppend) {
        boolean isOk = false;
        char[] buffer = null;
        int count = 0;
        BufferedReader br = null;
        BufferedWriter bw = null;
        try {
            if (!file.exists()) {
                createNewFileAndParentDir(file);
            }
            if (file.exists()) {
                br = new BufferedReader(new StringReader(content));
                bw = new BufferedWriter(new FileWriter(file, isAppend));
                buffer = new char[DEFAULT_CHAR_SIZE];
                int len = 0;
                while ((len = br.read(buffer, 0, DEFAULT_CHAR_SIZE)) != -1) {
                    bw.write(buffer, 0, len);
                    count += len;
                }
                bw.flush();
            }
            isOk = content.length() == count;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bw != null) {
                    bw.close();
                }
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return isOk;
    }
    public static boolean createNewFileAndParentDir(File file) {
        boolean isCreateNewFileOk = true;
        isCreateNewFileOk = createParentDir(file);
        // 创建父目录失败，直接返回false，不再创建子文件
        if (isCreateNewFileOk) {
            if (!file.exists()) {
                try {
                    isCreateNewFileOk = file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                    isCreateNewFileOk = false;
                }
            }
        }
        return isCreateNewFileOk;
    }

    public static boolean createParentDir(File file) {
        boolean isMkdirs = true;
        if (!file.exists()) {
            File dir = file.getParentFile();
            if (!dir.exists()) {
                isMkdirs = dir.mkdirs();
            }
        }
        return isMkdirs;
    }

    public static void replaceCertainLineInFileLine(File file, String containsString, String newLine) throws IOException {
        List<String> lines = new ArrayList<>();
        String line;
        FileReader fr = new FileReader(file);
        BufferedReader br = new BufferedReader(fr);
        while ((line = br.readLine()) != null) {
            if (line.contains(containsString))
                line = newLine;
            line += "\n";
            lines.add(line);
        }
        fr.close();
        br.close();

        FileWriter fw = new FileWriter(file);
        BufferedWriter out = new BufferedWriter(fw);
        for (String s : lines)
            out.write(s);
        out.flush();
        out.close();
    }

    public static String renameFileWithApacheApi(String oldName, String newName) {
        try {
            FileUtils.moveFile(new File(oldName), new File(newName));
            return newName;
        } catch (IOException e) {
            e.printStackTrace();
            return oldName;
        }
    }

    public static void copyFileUsingApacheApi(File source, File dest) {
        try {
            FileUtils.copyFile(source, dest);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void copyDirectoryUsingApacheApi(File source, File dest) {
        try {
            FileUtils.copyDirectory(source, dest, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getFileNameFromAbsolutePath(String path) {
        int index = path.lastIndexOf(File.separator);
        return path.substring(index + 1);
    }


    public static void appendContentToFile(String fileName, String data) {
        appendContentToFile(fileName, data, true);
    }

    public static void appendContentToFile(String fileName, String data, boolean force) {
        BufferedWriter bw = null;
        FileWriter fw = null;
        try {
            File file = new File(fileName);
            boolean exist = file.exists();
            if (!exist) {
                file.createNewFile();
            }
            boolean ignoreAppendContent = exist && !force;
            if (ignoreAppendContent) return;
            fw = new FileWriter(file.getAbsoluteFile(), true);
            bw = new BufferedWriter(fw);
            bw.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeSilently(bw);
            closeSilently(fw);
        }
    }

    private static void closeSilently(Closeable c) {
        if (c == null) return;
        try {
            c.close();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
