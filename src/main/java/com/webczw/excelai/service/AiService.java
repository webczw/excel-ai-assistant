package com.webczw.excelai.service;

import cn.hutool.http.HttpUtil;
import java.util.*;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

public class AiService {
    private static final String API_KEY = System.getProperty("appKey");
    private static final String URL = "https://api.deepseek.com/chat/completions";

    public String generateSQL(String tableName, List<String> fields, String question) {
        String prompt = String.format("""
            数据库表名：%s
            字段：%s
            用户问题：%s
            只返回可直接运行的MySQL语句，不要任何解释文字。
            """, tableName, String.join(",", fields), question);

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
                    .replace("```", "");
        } catch (Exception e) {
            return "SELECT * FROM " + tableName + " LIMIT 20";
        }
    }
}