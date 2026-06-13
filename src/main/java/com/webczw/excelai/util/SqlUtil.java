package com.webczw.excelai.util;

import com.webczw.excelai.config.DbConfig;
import com.webczw.excelai.service.AiTableGenerator;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;
import java.util.Map;

public class SqlUtil {
    private static final JdbcTemplate jdbc = DbConfig.getJdbcTemplate();
    private static final AiTableGenerator aiTableGenerator = new AiTableGenerator();
    /**
     * AI 智能建表（核心）
     */
    public static void createTableByAi(String tableName, List<String> headers, List<Map<String, Object>> sampleRows) {
        try {
            jdbc.execute("DROP TABLE IF EXISTS " + tableName);
            String sql = aiTableGenerator.generateCreateTableSql(tableName, headers, sampleRows);
            jdbc.execute(sql);
            System.out.println("✅ AI 建表完成：\n" + sql);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 高性能批量插入（10万行 < 1秒）
    public static void batchInsert(String tableName, List<String> headers, List<Map<String, Object>> rows) {
        if (rows.isEmpty()) return;

        StringBuilder colSb = new StringBuilder();
        headers.forEach(h -> colSb.append(clean(h)).append(","));
        colSb.setLength(colSb.length() - 1);

        StringBuilder valSb = new StringBuilder();
        for (Map<String, Object> row : rows) {
            valSb.append("(");
            for (String h : headers) {
                String v = row.getOrDefault(h, "").toString();
                if("NULL".equals(v)){
                    valSb.append(v).append(",");
                }else {
                    valSb.append("'").append(escape(v)).append("',");
                }
            }
            valSb.setLength(valSb.length() - 1);
            valSb.append("),");
        }

        String sql = "INSERT INTO " + tableName + " (" + colSb + ") VALUES " + valSb.substring(0, valSb.length() - 1);
        jdbc.execute(sql);
    }

    private static String clean(String s) {
        // 保留中文、字母、数字和下划线，其他字符替换为下划线
        if (s == null) {
            return "field_" + System.nanoTime();
        }
        return s.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9_]", "_");
    }

    private static String escape(String s) {
        return s.replace("'", "''");
    }
}