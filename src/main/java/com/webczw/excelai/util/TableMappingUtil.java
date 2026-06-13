package com.webczw.excelai.util;


import com.webczw.excelai.config.DbConfig;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;
import java.util.Map;

public class TableMappingUtil {
    private static final JdbcTemplate jdbc = DbConfig.getJdbcTemplate();

    // 初始化映射表（启动自动创建）
    public static void initMappingTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS excel_table_mapping (
                id INT PRIMARY KEY AUTO_INCREMENT,
                excel_name VARCHAR(500) NOT NULL,
                table_name VARCHAR(255) NOT NULL,
                create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        """;
        jdbc.execute(sql);
    }

    // 保存映射关系
    public static void saveMapping(String excelName, String tableName) {
        String sql = "INSERT INTO excel_table_mapping (excel_name, table_name) VALUES (?, ?)";
        jdbc.update(sql, excelName, tableName);
    }

    // 获取所有导入过的表
    public static List<Map<String, Object>> getAllTables() {
        return jdbc.queryForList("SELECT * FROM excel_table_mapping ORDER BY id DESC");
    }

    // 删除表 + 删除映射关系
    public static void dropTable(String tableName) {
        try {
            jdbc.execute("DROP TABLE IF EXISTS " + tableName);
            jdbc.update("DELETE FROM excel_table_mapping WHERE table_name = ?", tableName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}