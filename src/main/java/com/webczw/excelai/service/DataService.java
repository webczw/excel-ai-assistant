package com.webczw.excelai.service;


import com.webczw.excelai.config.DbConfig;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;
import java.util.Map;

public class DataService {
    private static final JdbcTemplate jdbc = DbConfig.getJdbcTemplate();

    public List<Map<String, Object>> query(String sql) {
        try {
            return jdbc.queryForList(sql);
        } catch (Exception e) {
            return List.of(Map.of("错误", e.getMessage()));
        }
    }
}
