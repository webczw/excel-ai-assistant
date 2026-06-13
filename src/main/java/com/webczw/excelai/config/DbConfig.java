package com.webczw.excelai.config;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import com.zaxxer.hikari.HikariDataSource;

public class DbConfig {
    private static JdbcTemplate jdbcTemplate;

    static {
        try {
            // 1. 先无库连接，创建数据库（自动创建！）
            DriverManagerDataSource tmpDs = new DriverManagerDataSource();
            tmpDs.setUrl("jdbc:mysql://127.0.0.1:3306/?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true");
            tmpDs.setUsername("root");
            tmpDs.setPassword("");
            new JdbcTemplate(tmpDs).execute("CREATE DATABASE IF NOT EXISTS excel_ai");
            System.out.println("✅ 数据库检查/创建完成：excel_ai");

            // 2. 正常连接池
            HikariDataSource ds = new HikariDataSource();
            ds.setJdbcUrl("jdbc:mysql://127.0.0.1:3306/excel_ai?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true");
            ds.setUsername("root");
            ds.setPassword("");
            ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
            jdbcTemplate = new JdbcTemplate(ds);

        } catch (Exception e) {
            System.err.println("数据库初始化失败：" + e.getMessage());
        }
    }

    public static JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }
}