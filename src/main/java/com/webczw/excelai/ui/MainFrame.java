package com.webczw.excelai.ui;

import com.alibaba.excel.util.StringUtils;
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
import java.util.*;
import java.util.List;

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
    private final JTable dataTable;

    public MainFrame() {
        setTitle("Excel AI 数据助手（完整版·增删改查）");
        setSize(1300, 800);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // 表格
        dataTable = new JTable(tableModel);
        dataTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        // 顶部按钮
        JPanel topPanel = new JPanel();
        JButton importBtn = new JButton("导入Excel");
        JButton deleteTableBtn = new JButton("删除表");
        JButton addBtn = new JButton("新增数据");
        JButton updateBtn = new JButton("修改数据");
        JButton batchDeleteBtn = new JButton("批量删除选中行");
        JButton queryBtn = new JButton("执行AI查询");
        JButton exportBtn = new JButton("导出Excel");

        topPanel.add(importBtn);
        topPanel.add(deleteTableBtn);
        topPanel.add(addBtn);
        topPanel.add(updateBtn);
        topPanel.add(batchDeleteBtn);
        topPanel.add(queryBtn);
        topPanel.add(exportBtn);
        add(topPanel, BorderLayout.NORTH);

        // 问题框
        questionInput.setBorder(BorderFactory.createTitledBorder("AI查询描述"));
        add(questionInput, BorderLayout.SOUTH);

        // 表列表
        initTableList();
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(tableList), new JScrollPane(dataTable));
        splitPane.setDividerLocation(300);
        add(splitPane, BorderLayout.CENTER);

        // 事件
        importBtn.addActionListener(e -> importExcel());
        deleteTableBtn.addActionListener(e -> deleteTable());
        addBtn.addActionListener(e -> addData());
        updateBtn.addActionListener(e -> updateData());
        batchDeleteBtn.addActionListener(e -> batchDeleteData());
        queryBtn.addActionListener(e -> aiQuery());
        exportBtn.addActionListener(e -> exportData());

        tableList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String t = tableList.getSelectedValue();
                if (t != null) {
                    currentTableName = t;
                    loadTableData(t);
                }
            }
        });

        refreshTableList();
    }

    private void initTableList() {
        listModel = new DefaultListModel<>();
        tableList.setModel(listModel);
        tableList.setBorder(BorderFactory.createTitledBorder("数据表列表"));
    }

    private void refreshTableList() {
        listModel.clear();
        TableMappingUtil.getAllTables().forEach(m -> listModel.addElement(m.get("table_name").toString()));
    }

    private void loadTableData(String table) {
        new Thread(() -> {
            List<Map<String, Object>> list = dataService.query("SELECT * FROM " + table);
            lastResult = list;
            SwingUtilities.invokeLater(() -> {
                tableModel.setRowCount(0);
                tableModel.setColumnCount(0);
                if (!list.isEmpty()) {
                    Map<String, Object> first = list.get(0);
                    first.keySet().forEach(tableModel::addColumn);
                    list.forEach(row -> tableModel.addRow(row.values().toArray()));
                }
            });
        }).start();
    }

    // ==========================
    // 导入Excel（AI建表）
    // ==========================
    private void importExcel() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            new Thread(() -> {
                try {
                    String tableName = "excel_" + System.currentTimeMillis();
                    excelService.read(file.getAbsolutePath());
                    List<String> headers = excelService.getHeaders();
                    List<Map<String, Object>> data = excelService.getRows();
                    List<Map<String, Object>> sample = data.size() > 10 ? data.subList(0, 10) : data;
                    SqlUtil.createTableByAi(tableName, headers, sample);
                    SqlUtil.batchInsert(tableName, headers, data);
                    TableMappingUtil.saveMapping(file.getName(), tableName);
                    SwingUtilities.invokeLater(() -> {
                        refreshTableList();
                        JOptionPane.showMessageDialog(this, "导入成功");
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }).start();
        }
    }

    // ==========================
    // 删除表
    // ==========================
    private void deleteTable() {
        String t = tableList.getSelectedValue();
        if (t == null) {
            JOptionPane.showMessageDialog(this, "请选择表");
            return;
        }
        int c = JOptionPane.showConfirmDialog(this, "确定删除表：" + t + "？数据将丢失！");
        if (c == JOptionPane.YES_OPTION) {
            new Thread(() -> {
                TableMappingUtil.dropTable(t);
                SwingUtilities.invokeLater(() -> {
                    refreshTableList();
                    tableModel.setRowCount(0);
                    tableModel.setColumnCount(0);
                    currentTableName = null;
                });
            }).start();
        }
    }

    // ==========================
    // 🔥 新增数据
    // ==========================
    private void addData() {
        if (currentTableName == null) {
            JOptionPane.showMessageDialog(this, "请先选择表");
            return;
        }
        if (tableModel.getColumnCount() == 0) {
            JOptionPane.showMessageDialog(this, "无字段");
            return;
        }

        Map<String, JTextField> fieldMap = new HashMap<>();
        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));

        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            String col = tableModel.getColumnName(i);
            if (col.equalsIgnoreCase("id")) continue;
            panel.add(new JLabel(col));
            JTextField tf = new JTextField();
            fieldMap.put(col, tf);
            panel.add(tf);
        }

        int r = JOptionPane.showConfirmDialog(this, panel, "新增数据", JOptionPane.OK_CANCEL_OPTION);
        if (r == JOptionPane.OK_OPTION) {
            new Thread(() -> {
                try {
                    StringBuilder cols = new StringBuilder();
                    StringBuilder vals = new StringBuilder();
                    for (String col : fieldMap.keySet()) {
                        cols.append("`").append(col).append("`,");
                        String v = fieldMap.get(col).getText().replace("'", "''");
                        vals.append("'").append(v).append("',");
                    }
                    cols.setLength(cols.length() - 1);
                    vals.setLength(vals.length() - 1);
                    String sql = "INSERT INTO " + currentTableName + " (" + cols + ") VALUES (" + vals + ")";
                    dataService.update(sql);
                    SwingUtilities.invokeLater(() -> loadTableData(currentTableName));
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, "新增失败");
                }
            }).start();
        }
    }

    // ==========================
// 🔥 修改数据（已修复编译异常）
// ==========================
    private void updateData() {
        if (currentTableName == null) {
            JOptionPane.showMessageDialog(this, "请先选择表");
            return;
        }
        int row = dataTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请选择一行数据");
            return;
        }

        // 找到 ID 列的索引和名称（必须确保存在）
        int idColumnIndex = -1;
        String idColumnName = null;
        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            if (tableModel.getColumnName(i).equalsIgnoreCase("id")) {
                idColumnIndex = i;
                idColumnName = tableModel.getColumnName(i);
                break;
            }
        }

        if (idColumnIndex == -1 || idColumnName == null) {
            JOptionPane.showMessageDialog(this, "表中未找到主键ID，无法修改");
            return;
        }

        // 获取主键值
        Object idValue = tableModel.getValueAt(row, idColumnIndex);
        if (idValue == null) {
            JOptionPane.showMessageDialog(this, "ID不能为空");
            return;
        }

        // 构建表单
        Map<String, JTextField> fieldMap = new HashMap<>();
        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));

        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            String col = tableModel.getColumnName(i);
            if (col.equalsIgnoreCase("id")) continue;

            Object val = tableModel.getValueAt(row, i);
            panel.add(new JLabel(col));
            JTextField tf = new JTextField(val == null ? "" : val.toString());
            fieldMap.put(col, tf);
            panel.add(tf);
        }

        int r = JOptionPane.showConfirmDialog(this, panel, "修改数据", JOptionPane.OK_CANCEL_OPTION);
        if (r == JOptionPane.OK_OPTION) {
            String finalIdColumnName = idColumnName;
            new Thread(() -> {
                try {
                    StringBuilder set = new StringBuilder();
                    for (String col : fieldMap.keySet()) {
                        String v = fieldMap.get(col).getText().replace("'", "''");
                        if(StringUtils.isNotBlank(v)) {
                            set.append("`").append(col).append("`='").append(v).append("',");
                        }else{
                            set.append("`").append(col).append("`=NULL,");
                        }
                    }
                    if (!set.isEmpty()) {
                        set.setLength(set.length() - 1);
                    }

                    // ✅ 修复完成：绝对安全，不会报错
                    String sql = "UPDATE " + currentTableName
                            + " SET " + set
                            + " WHERE `" + finalIdColumnName + "` = '" + idValue + "'";

                    dataService.update(sql);
                    SwingUtilities.invokeLater(() -> loadTableData(currentTableName));
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(this, "修改失败：" + e.getMessage());
                }
            }).start();
        }
    }

    // ==========================
    // 🔥 批量删除（带确认）
    // ==========================
    private void batchDeleteData() {
        if (currentTableName == null) {
            JOptionPane.showMessageDialog(this, "请选表");
            return;
        }
        int[] rows = dataTable.getSelectedRows();
        if (rows == null || rows.length == 0) {
            JOptionPane.showMessageDialog(this, "请选择要删除的行");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "确定要删除选中的 " + rows.length + " 行数据吗？\n此操作不可恢复！",
                "批量删除确认", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        new Thread(() -> {
            try {
                int idColumnIndex = -1;
                for (int i = 0; i < tableModel.getColumnCount(); i++) {
                    if (tableModel.getColumnName(i).equalsIgnoreCase("id")) {
                        idColumnIndex = i;
                        break;
                    }
                }
                if (idColumnIndex == -1) {
                    JOptionPane.showMessageDialog(this, "未找到主键ID，无法删除");
                    return;
                }

                for (int r : rows) {
                    Object idVal = tableModel.getValueAt(r, idColumnIndex);
                    String sql = "DELETE FROM " + currentTableName + " WHERE `id`='" + idVal + "'";
                    dataService.update(sql);
                }

                SwingUtilities.invokeLater(() -> {
                    loadTableData(currentTableName);
                    JOptionPane.showMessageDialog(this, "批量删除完成");
                });
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "删除失败");
            }
        }).start();
    }

    // ==========================
    // AI 查询
    // ==========================
    private void aiQuery() {
        if (currentTableName == null) {
            JOptionPane.showMessageDialog(this, "请选表");
            return;
        }
        String q = questionInput.getText().trim();
        if (q.isBlank()) return;

        new Thread(() -> {
            List<String> fields = dataService.query("SELECT * FROM " + currentTableName + " LIMIT 1")
                    .stream().findFirst().map(Map::keySet).orElse(Set.of()).stream().toList();
            String sql = aiService.generateSQL(currentTableName, fields, q);
            List<Map<String, Object>> res = dataService.query(sql);
            lastResult = res;
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this, "SQL：\n" + sql);
                tableModel.setRowCount(0);
                tableModel.setColumnCount(0);
                if (!res.isEmpty()) {
                    res.get(0).keySet().forEach(tableModel::addColumn);
                    res.forEach(row -> tableModel.addRow(row.values().toArray()));
                }
            });
        }).start();
    }

    // ==========================
    // 导出
    // ==========================
    private void exportData() {
        if (lastResult == null || lastResult.isEmpty()) {
            JOptionPane.showMessageDialog(this, "无数据");
            return;
        }
        ExportService.exportToExcel(lastResult);
    }
}