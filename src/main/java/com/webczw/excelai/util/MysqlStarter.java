package com.webczw.excelai.util;

import java.io.File;

public class MysqlStarter {
    private static Process mysqlProcess;

    public static void startMysql() {
        try {
            // 0. 从 JAR 包中解压 MySQL 资源
            extractMysqlResources();

            // 1. 查找 MySQL 目录
            File mysqlHome = findMysqlHome();

            if (mysqlHome == null || !mysqlHome.exists()) {
                System.err.println("ERROR: mysql 目录不存在！");
                return;
            }

            File mysqld = new File(mysqlHome, "bin/mysqld.exe");
            if (!mysqld.exists()) {
                System.err.println("ERROR: mysqld.exe 不存在");
                return;
            }

            // 2. 查找 my.ini 配置文件
            File configFile = new File(mysqlHome.getParentFile(), "mysql/my.ini");
            if (!configFile.exists()) {
                System.err.println("ERROR: my.ini 配置文件不存在");
                return;
            }

            // 3. 启动 MySQL
            ProcessBuilder pb = new ProcessBuilder(
                    mysqld.getAbsolutePath(),
                    "--defaults-file=" + configFile.getAbsolutePath(),
                    "--datadir=" + new File(mysqlHome, "data").getAbsolutePath(),
                    "--port=3306",
                    "--console"
            );

            pb.directory(mysqlHome);
            pb.redirectErrorStream(true);
            mysqlProcess = pb.start();

            System.out.println("MySQL 正在启动...等待5秒确保完全启动");

            Thread.sleep(5000);

            // 关闭时自动停止
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (mysqlProcess != null && mysqlProcess.isAlive()) {
                    mysqlProcess.destroy();
                    System.out.println("MySQL 已停止");
                }
            }));

        } catch (Exception e) {
            System.err.println("MySQL 启动可能已运行：" + e.getMessage());
        }
    }

    /**
     * 从 JAR 包中解压 MySQL 资源到本地
     */
    private static void extractMysqlResources() {
        // 获取 JAR 包所在目录
        String jarPath = MysqlStarter.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        File jarFile = new File(jarPath);
        File jarParent = jarFile.getParentFile();

        // 解压 mysql 目录
        File mysqlTargetDir = new File(jarParent, "mysql");
        ResourceExtractor.extractResourceDir("mysql", mysqlTargetDir);
    }

    /**
     * 查找 MySQL 安装目录
     */
    private static File findMysqlHome() {
        // 尝试从 JAR 包所在目录查找
        String jarPath = MysqlStarter.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        try {
            File jarFile = new File(jarPath);
            File jarParent = jarFile.getParentFile();
            File mysqlInJarDir = new File(jarParent, "mysql");
            if (mysqlInJarDir.exists()) {
                System.out.println("找到 MySQL 目录: " + mysqlInJarDir.getAbsolutePath());
                return mysqlInJarDir;
            }
        } catch (Exception e) {
            // 忽略异常
        }

        // 尝试从当前工作目录查找
        File mysqlInWorkDir = new File("mysql");
        if (mysqlInWorkDir.exists()) {
            System.out.println("找到 MySQL 目录: " + mysqlInWorkDir.getAbsolutePath());
            return mysqlInWorkDir;
        }

        return null;
    }
}
