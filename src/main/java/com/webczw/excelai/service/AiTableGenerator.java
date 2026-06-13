package com.webczw.excelai.service;


import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import java.util.*;

public class AiTableGenerator {
    private static final String API_KEY = System.getProperty("appKey");
    private static final String URL = "https://api.deepseek.com/chat/completions";

    /**
     * AI 自动分析字段，生成专业建表语句
     * @param tableName 表名
     * @param headers 表头
     * @param sampleRows 前10行示例数据
     * @return 可直接执行的 CREATE TABLE SQL
     */
    public String generateCreateTableSql(String tableName, List<String> headers, List<Map<String, Object>> sampleRows) {
        String prompt = String.format("""
            请根据Excel表头和示例数据，生成MySQL建表语句，要求：
            1. 表名：%s
            2. 必须包含主键 id INT PRIMARY KEY AUTO_INCREMENT
            3. 自动判断字段类型：VARCHAR长度、INT、DECIMAL、DATE、DATETIME
            4. 字段名直接使用中文，不要加引号
            5. 只返回SQL语句，不要任何解释
            6. 字符集 utf8mb4
            表头：%s
            示例数据（前10行）：%s
            """,
                tableName,
                String.join(",", headers),
                JSONUtil.toJsonStr(sampleRows)
        );

        var body = new HashMap<String, Object>();
        body.put("model", "deepseek-chat");
        body.put("messages", Collections.singletonList(
                Map.of("role", "user", "content", prompt)
        ));

        try {
            String resp = HttpUtil.createPost(URL)
                    .header("Authorization", "Bearer " + API_KEY)
                    .body(JSONUtil.toJsonStr(body))
                    .execute()
                    .body();
            JSONObject jsonObject = JSONUtil.parseObj(resp);
            return jsonObject
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getStr("content")
                    .replace("```sql", "")
                    .replace("```", "")
                    .trim();
        } catch (Exception e) {
            e.printStackTrace();
            return getDefaultTableSql(tableName, headers);
        }
    }

    /**
     * 降级默认表结构（AI异常时使用）
     */
    private String getDefaultTableSql(String tableName, List<String> headers) {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE ").append(tableName).append(" (\n");
        sb.append("id INT PRIMARY KEY AUTO_INCREMENT,\n");
        for (String h : headers) {
            sb.append("`").append(h).append("` VARCHAR(255),\n");
        }
        sb.setLength(sb.length() - 2);
        sb.append("\n) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");
        return sb.toString();
    }
}