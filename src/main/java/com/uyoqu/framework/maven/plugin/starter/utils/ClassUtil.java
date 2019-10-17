package com.uyoqu.framework.maven.plugin.starter.utils;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.asm.ClassReader;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;

public class ClassUtil {

    private static final Logger logger = LoggerFactory.getLogger(ClassUtil.class);

    private static final FileFilter CLASS_FILE_FILTER = ClassUtil::isClassFile;

    private static final FileFilter PACKAGE_FOLDER_FILTER = ClassUtil::isPackageFolder;

    private static final String DOT_CLASS = ".class";

    private static String matchClass;

    public static String findSingleMainClass(File rootFolder, String matchClass) throws IOException {
        if (StringUtils.isBlank(matchClass) || rootFolder == null){ return ""; }
        ClassUtil.matchClass = matchClass;
        List<String> callback = new ArrayList<>();
        findMainClasses(rootFolder, callback);
        return callbackCheck(callback);
    }

    private static String callbackCheck(List<String> callback) {
        if (callback.size() >= 2){
            logger.error("符合匹配规则的类不止一个");
            logger.error("检测到的多个启动类为：{}", callback);
            return "";
        } else if (callback.size() == 1){
            return callback.get(0);
        } else {
            return "";
        }
    }

    static void findMainClasses(File rootFolder, List<String> callback)
            throws IOException {
        if (!rootFolder.exists()) {
            return; // nothing to do
        }
        if (!rootFolder.isDirectory()) {
            throw new IllegalArgumentException(
                    "根目录必须为正常的目录");
        }
        String prefix = rootFolder.getAbsolutePath() + "/";
        Deque<File> stack = new ArrayDeque<>();
        stack.push(rootFolder);
        while (!stack.isEmpty()) {
            File file = stack.pop();
            if (file.isFile()) {
                try (InputStream inputStream = new FileInputStream(file)) {
                    if (isMatchMainClass(inputStream)) {
                        String className = convertToClassName(file.getAbsolutePath(), prefix);
                        callback.add(className);
                    }
                }
            } else if (file.isDirectory()) {
                pushAllSorted(stack, file.listFiles(PACKAGE_FOLDER_FILTER));
                pushAllSorted(stack, file.listFiles(CLASS_FILE_FILTER));
            }
        }
    }

    private static void pushAllSorted(Deque<File> stack, File[] files) {
        Arrays.sort(files, Comparator.comparing(File::getName));
        for (File file : files) {
            stack.push(file);
        }
    }

    private static boolean isMatchMainClass(InputStream inputStream) {
        try {
            ClassReader classReader = new ClassReader(inputStream);
            String className = ClassUtil.matchClass;
            if (classReader.getSuperName().contains(className)
                    || classReader.getClassName().contains(className)){
                return true;
            } else {
                return  false;
            }
        }
        catch (IOException ex) {
            logger.error("主方法匹配出错",ex);
        }
        return false;
    }

    private static String convertToClassName(String name, String prefix) {
        name = name.replace('/', '.');
        name = name.replace('\\', '.');
        name = name.substring(0, name.length() - DOT_CLASS.length());
        if (prefix != null) {
            name = name.substring(prefix.length());
        }
        return name;
    }

    private static boolean isPackageFolder(File file) {
        return file.isDirectory() && !file.getName().startsWith(".");
    }

    private static boolean isClassFile(File file) {
        return file.isFile() && file.getName().endsWith(DOT_CLASS);
    }
}
