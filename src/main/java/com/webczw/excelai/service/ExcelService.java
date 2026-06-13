package com.webczw.excelai.service;


import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import java.util.*;

public class ExcelService {
    private final List<String> headers = new ArrayList<>();
    private final List<Map<String, Object>> rows = new ArrayList<>();

    public void read(String path) {
        headers.clear();
        rows.clear();

        EasyExcel.read(path, new AnalysisEventListener<Map<Integer, String>>() {
            @Override
            public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
                headers.addAll(headMap.values());
            }

            @Override
            public void invoke(Map<Integer, String> row, AnalysisContext context) {
                Map<String, Object> line = new HashMap<>();
                for (int i = 0; i < headers.size(); i++) {
                    line.put(headers.get(i), row.get(i));
                }
                rows.add(line);
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {}
        }).sheet().doRead();
    }

    public List<String> getHeaders() { return headers; }
    public List<Map<String, Object>> getRows() { return rows; }
}