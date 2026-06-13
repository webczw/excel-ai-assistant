package com.webczw.excelai.util;

import java.io.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ResourceExtractor {

    /**
     * 从 JAR 包中解压指定目录到目标位置
     * @param resourceDir JAR 内的资源目录路径（如 "mysql"）
     * @param targetDir 目标目录
     * @return 是否成功解压
     */
    public static boolean extractResourceDir(String resourceDir, File targetDir) {
        try {
            // 如果目标目录已存在且有内容，跳过解压
            if (targetDir.exists() && targetDir.listFiles() != null && targetDir.listFiles().length > 0) {
                System.out.println("资源目录已存在，跳过解压: " + targetDir.getAbsolutePath());
                return true;
            }

            System.out.println("正在解压资源目录: " + resourceDir + " -> " + targetDir.getAbsolutePath());

            // 获取 JAR 文件路径
            String jarPath = ResourceExtractor.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            File jarFile = new File(jarPath);

            if (!jarFile.exists() || !jarPath.endsWith(".jar")) {
                System.out.println("非 JAR 运行环境，跳过解压");
                return false;
            }

            // 打开 JAR 文件
            try (JarFile jar = new JarFile(jarFile)) {
                Enumeration<JarEntry> entries = jar.entries();
                List<JarEntry> mysqlEntries = new ArrayList<>();

                // 收集所有 mysql 目录下的文件
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (entry.getName().startsWith(resourceDir + "/")) {
                        mysqlEntries.add(entry);
                    }
                }

                if (mysqlEntries.isEmpty()) {
                    System.out.println("未找到资源目录: " + resourceDir);
                    return false;
                }

                // 创建目标目录
                targetDir.mkdirs();

                // 解压每个文件
                for (JarEntry entry : mysqlEntries) {
                    File outputFile = new File(targetDir, entry.getName().substring(resourceDir.length() + 1));

                    if (entry.isDirectory()) {
                        outputFile.mkdirs();
                    } else {
                        // 确保父目录存在
                        outputFile.getParentFile().mkdirs();

                        // 解压文件
                        try (InputStream is = jar.getInputStream(entry);
                             FileOutputStream fos = new FileOutputStream(outputFile)) {
                            byte[] buffer = new byte[8192];
                            int bytesRead;
                            while ((bytesRead = is.read(buffer)) != -1) {
                                fos.write(buffer, 0, bytesRead);
                            }
                        }

                        // 保留文件属性（可选）
                        System.out.println("  解压: " + entry.getName());
                    }
                }

                System.out.println("资源目录解压完成: " + targetDir.getAbsolutePath());
                return true;
            }

        } catch (Exception e) {
            System.err.println("解压资源失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 从 JAR 包中解压单个文件到目标位置
     * @param resourceFile JAR 内的资源文件路径（如 "my.ini"）
     * @param targetFile 目标文件
     * @return 是否成功解压
     */
    public static boolean extractResourceFile(String resourceFile, File targetFile) {
        try {
            // 如果目标文件已存在，跳过解压
            if (targetFile.exists()) {
                System.out.println("资源文件已存在，跳过解压: " + targetFile.getAbsolutePath());
                return true;
            }

            System.out.println("正在解压资源文件: " + resourceFile + " -> " + targetFile.getAbsolutePath());

            String jarPath = ResourceExtractor.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            File jarFile = new File(jarPath);

            if (!jarFile.exists() || !jarPath.endsWith(".jar")) {
                System.out.println("非 JAR 运行环境，跳过解压");
                return false;
            }

            try (JarFile jar = new JarFile(jarFile)) {
                JarEntry entry = jar.getJarEntry(resourceFile);
                if (entry == null) {
                    System.out.println("未找到资源文件: " + resourceFile);
                    return false;
                }

                try (InputStream is = jar.getInputStream(entry);
                     FileOutputStream fos = new FileOutputStream(targetFile)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                }

                System.out.println("资源文件解压完成: " + targetFile.getAbsolutePath());
                return true;
            }

        } catch (Exception e) {
            System.err.println("解压资源失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
