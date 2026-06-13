package com.webczw.excelai.ui;

import com.webczw.excelai.service.AiService;
import com.webczw.excelai.service.DataService;
import com.webczw.excelai.service.ExcelService;
import com.webczw.excelai.service.ExportService;
import com.webczw.excelai.util.SqlUtil;
import com.webczw.excelai.util.TableMappingUtil;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainFrame extends JFrame {
    private final ExcelService excelService = new ExcelService();
    private final AiService aiService = new AiService();
    private final DataService dataService = new DataService();
    private final DefaultTableModel tableModel = new DefaultTableModel();
    private final JTextArea questionInput = new JTextArea(3, 20);
    private List<Map<String, Object>> lastResult;
    private String currentTableName;

    private final JList<String> tableList = new JList<>();
    private DefaultListModel<String> listModel;

    public MainFrame() {
        setTitle("Excel AI 数据助手（完整版）");
        setSize(1200, 750);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // 顶部按钮
        JPanel topPanel = new JPanel();
        JButton importBtn = new JButton("导入Excel");
        JButton deleteBtn = new JButton("删除选中表");
        JButton queryBtn = new JButton("执行AI查询");
        JButton exportBtn = new JButton("导出Excel");
        topPanel.add(importBtn);
        topPanel.add(deleteBtn);
        topPanel.add(queryBtn);
        topPanel.add(exportBtn);
        add(topPanel, BorderLayout.NORTH);

        // 问题输入框
        questionInput.setBorder(BorderFactory.createTitledBorder("输入AI查询问题（大白话）"));
        add(questionInput, BorderLayout.SOUTH);

        // 表列表
        initTableList();

        // 结果表格
        JScrollPane tableScroll = new JScrollPane(new JTable(tableModel));

        // 左右布局
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(tableList), tableScroll);
        splitPane.setDividerLocation(280);
        add(splitPane, BorderLayout.CENTER);

        // 导入
        importBtn.addActionListener(e -> importExcel());

        // 删除
        deleteBtn.addActionListener(e -> deleteSelectedTable());

        // AI 查询
        queryBtn.addActionListener(e -> aiQuery());

        // 导出
        exportBtn.addActionListener(e -> {
            if (lastResult != null && !lastResult.isEmpty()) {
                ExportService.exportToExcel(lastResult);
            } else {
                JOptionPane.showMessageDialog(this, "暂无数据可导出");
            }
        });

        // 切换表
        tableList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String table = tableList.getSelectedValue();
                if (table != null) {
                    currentTableName = table;
                    loadTableData(table);
                }
            }
        });

        refreshTableList();
    }

    private void initTableList() {
        listModel = new DefaultListModel<>();
        tableList.setModel(listModel);
        tableList.setBorder(BorderFactory.createTitledBorder("已导入Excel表"));
    }

    private void refreshTableList() {
        listModel.clear();
        TableMappingUtil.getAllTables().forEach(map -> {
            listModel.addElement(map.get("table_name").toString());
        });
    }

    private void importExcel() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            new Thread(() -> {
                try {
                    String excelName = file.getName();
                    String tableName = "excel_" + System.currentTimeMillis();

                    excelService.read(file.getAbsolutePath());
                    List<String> headers = excelService.getHeaders();
                    List<Map<String, Object>> data = excelService.getRows();

                    // 取前10行样本给AI分析字段类型
                    List<Map<String, Object>> sampleData = data.size() > 10 ? data.subList(0, 10) : data;

                    // AI 智能创建表结构
                    SqlUtil.createTableByAi(tableName, headers, sampleData);
                    SqlUtil.batchInsert(tableName, headers, data);
                    TableMappingUtil.saveMapping(excelName, tableName);

                    SwingUtilities.invokeLater(() -> {
                        refreshTableList();
                        JOptionPane.showMessageDialog(this, "导入成功：" + tableName);
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private void deleteSelectedTable() {
        String table = tableList.getSelectedValue();
        if (table == null) {
            JOptionPane.showMessageDialog(this, "请先选择表");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "确定删除表：" + table + "？");
        if (confirm == JOptionPane.YES_OPTION) {
            new Thread(() -> {
                TableMappingUtil.dropTable(table);
                SwingUtilities.invokeLater(() -> {
                    refreshTableList();
                    tableModel.setRowCount(0);
                    tableModel.setColumnCount(0);
                    currentTableName = null;
                    JOptionPane.showMessageDialog(this, "删除成功");
                });
            }).start();
        }
    }

    private void loadTableData(String tableName) {
        new Thread(() -> {
            List<Map<String, Object>> data = dataService.query("SELECT * FROM " + tableName);
            lastResult = data;
            SwingUtilities.invokeLater(() -> showResult(data));
        }).start();
    }

    private void aiQuery() {
        if (currentTableName == null) {
            JOptionPane.showMessageDialog(this, "请先选择表");
            return;
        }
        String question = questionInput.getText().trim();
        if (question.isBlank()) {
            JOptionPane.showMessageDialog(this, "请输入查询问题");
            return;
        }

        new Thread(() -> {
            Set<String> fields = dataService.query("SELECT * FROM " + currentTableName + " LIMIT 1")
                    .stream().findFirst().map(Map::keySet).orElse(Set.of());

            String sql = aiService.generateSQL(currentTableName, new ArrayList<>(fields), question);
            List<Map<String, Object>> result = dataService.query(sql);
            lastResult = result;

            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this, "生成SQL：\n" + sql);
                showResult(result);
            });
        }).start();
    }

    private void showResult(List<Map<String, Object>> list) {
        tableModel.setRowCount(0);
        tableModel.setColumnCount(0);
        if (list.isEmpty()) return;

        Map<String, Object> first = list.get(0);
        first.keySet().forEach(tableModel::addColumn);
        list.forEach(row -> tableModel.addRow(row.values().toArray()));
    }
}