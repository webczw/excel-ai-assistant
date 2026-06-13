package com.webczw.excelai.service;


import com.alibaba.excel.EasyExcel;
import javax.swing.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class ExportService {
    public static void exportToExcel(List<Map<String, Object>> data) {
        try {
            String desktop = System.getProperty("user.home") + "/Desktop/";
            String fileName = "查询结果_" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".xlsx";
            String path = desktop + fileName;

            // 自动导出
            EasyExcel.write(path)
                    .head(generateHead(data))
                    .sheet("查询结果")
                    .doWrite(generateDataList(data));

            JOptionPane.showMessageDialog(null, "导出成功！\n已保存到桌面：" + fileName);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "导出失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    // 构建表头
    private static List<List<String>> generateHead(List<Map<String, Object>> data) {
        if (data.isEmpty()) return List.of();
        return data.get(0).keySet().stream()
                .map(List::of)
                .toList();
    }

    // 构建数据
    private static List<List<Object>> generateDataList(List<Map<String, Object>> data) {
        List<List<Object>> allList = new ArrayList<>();
        for (Map<String, Object> map : data) {
            List<Object> dataList = new ArrayList<>(map.values());
            allList.add(dataList);
        }
        return allList;
    }
}
