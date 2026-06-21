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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.*;
import java.util.List;

public class MainFrame extends JFrame {
    private final ExcelService excelService = new ExcelService();
    private final AiService aiService = new AiService();
    private final DataService dataService = new DataService();
    private final DefaultTableModel tableModel = new DefaultTableModel();
    private final JTextArea questionInput = new JTextArea(3, 20);
    private String currentTableName;

    // 分页变量（AI查询也共用）
    private int currentPage = 1;
    private int pageSize = 50;
    private long totalCount = 0;

    // 标记是否是AI查询结果
    private boolean isAiQueryResult = false;
    private List<Map<String, Object>> aiFullResult = new ArrayList<>();

    private final JList<String> tableList = new JList<>();
    private DefaultListModel<String> listModel;
    private final JTable dataTable;

    // 分页组件
    private JLabel pageTipLabel;
    private JComboBox<Integer> pageSizeBox;
    private JTextField pageJumpField;

    public MainFrame() {
        setTitle("Excel AI 数据助手（完整版·全功能分页）");
        setSize(1300, 800);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // 表格只读
        dataTable = new JTable(tableModel) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        dataTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        // 顶部按钮
        JPanel topPanel = new JPanel();
        JButton importBtn = new JButton("导入Excel");
        JButton addBtn = new JButton("新增数据");
        JButton batchDeleteBtn = new JButton("批量删除选中行");
        JButton exportBtn = new JButton("导出Excel");
        topPanel.add(importBtn);
        topPanel.add(addBtn);
        topPanel.add(batchDeleteBtn);
        topPanel.add(exportBtn);
        add(topPanel, BorderLayout.NORTH);

        // 左侧表列表
        initTableList();
        initRightClickMenu();
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(tableList), new JScrollPane(dataTable));
        splitPane.setDividerLocation(300);
        add(splitPane, BorderLayout.CENTER);

        // 底部：AI查询 + 分页
        // JPanel bottomAllPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        JPanel bottomAllPanel = new JPanel();
        bottomAllPanel.setLayout(new BoxLayout(bottomAllPanel, BoxLayout.Y_AXIS));
// 两行之间极小间距
        bottomAllPanel.add(Box.createVerticalStrut(1));

        // AI查询
        JPanel aiPanel = new JPanel(new BorderLayout(5, 5));
        questionInput.setBorder(BorderFactory.createTitledBorder("AI查询描述"));
        aiPanel.add(questionInput, BorderLayout.CENTER);
        JButton queryBtn = new JButton("执行AI查询");
        aiPanel.add(queryBtn, BorderLayout.EAST);
        bottomAllPanel.add(aiPanel);

        // 分页栏
        JPanel pagePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        pageTipLabel = new JLabel("暂无数据");
        pagePanel.add(pageTipLabel);

        pagePanel.add(new JLabel("每页条数："));
        pageSizeBox = new JComboBox<>(new Integer[]{10, 20, 50, 100, 200});
        pageSizeBox.setSelectedItem(pageSize);
        pagePanel.add(pageSizeBox);

        JButton prevBtn = new JButton("上一页");
        JButton nextBtn = new JButton("下一页");
        pagePanel.add(prevBtn);
        pagePanel.add(nextBtn);

        pagePanel.add(new JLabel("跳至："));
        pageJumpField = new JTextField(3);
        pagePanel.add(pageJumpField);
        JButton jumpBtn = new JButton("跳转");
        pagePanel.add(jumpBtn);

        bottomAllPanel.add(pagePanel);
        add(bottomAllPanel, BorderLayout.SOUTH);

        // 事件绑定
        importBtn.addActionListener(e -> importExcel());
        addBtn.addActionListener(e -> addData());
        batchDeleteBtn.addActionListener(e -> batchDeleteData());
        exportBtn.addActionListener(e -> exportData());
        queryBtn.addActionListener(e -> aiQuery());

        // 分页事件
        pageSizeBox.addActionListener(e -> {
            pageSize = (Integer) pageSizeBox.getSelectedItem();
            currentPage = 1;
            refreshCurrentPageData();
        });
        prevBtn.addActionListener(e -> {
            if (currentPage > 1) {
                currentPage--;
                refreshCurrentPageData();
            }
        });
        nextBtn.addActionListener(e -> {
            long maxPage = getMaxPage();
            if (currentPage < maxPage) {
                currentPage++;
                refreshCurrentPageData();
            }
        });
        jumpBtn.addActionListener(e -> {
            try {
                int target = Integer.parseInt(pageJumpField.getText().trim());
                long maxPage = getMaxPage();
                if (target >= 1 && target <= maxPage) {
                    currentPage = target;
                    refreshCurrentPageData();
                } else {
                    JOptionPane.showMessageDialog(this, "页码超出范围");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "请输入合法数字");
            }
        });

        // 切换表
        tableList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String t = tableList.getSelectedValue();
                if (t != null) {
                    isAiQueryResult = false;
                    currentTableName = t;
                    currentPage = 1;
                    loadTableData(t);
                }
            }
        });

        // 双击修改
        dataTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    updateData();
                }
            }
        });

        refreshTableList();
    }

    private long getMaxPage() {
        if (totalCount == 0) return 1;
        return totalCount % pageSize == 0 ? totalCount / pageSize : totalCount / pageSize + 1;
    }

    private void refreshCurrentPageData() {
        if (isAiQueryResult) {
            showAiPageData();
        } else {
            loadTableData(currentTableName);
        }
    }

    private void initTableList() {
        listModel = new DefaultListModel<>();
        tableList.setModel(listModel);
        tableList.setBorder(BorderFactory.createTitledBorder("数据表列表（右键删除）"));
    }

    private void initRightClickMenu() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem deleteItem = new JMenuItem("删除当前表");
        menu.add(deleteItem);
        deleteItem.addActionListener(e -> {
            String table = tableList.getSelectedValue();
            if (table == null) return;
            int confirm = JOptionPane.showConfirmDialog(this,
                    "确定删除表：" + table + "？数据将永久丢失！",
                    "删除确认", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                new Thread(() -> {
                    TableMappingUtil.dropTable(table);
                    SwingUtilities.invokeLater(() -> {
                        refreshTableList();
                        tableModel.setRowCount(0);
                        tableModel.setColumnCount(0);
                        currentTableName = null;
                        pageTipLabel.setText("暂无数据");
                    });
                }).start();
            }
        });
        tableList.setComponentPopupMenu(menu);
    }

    private void refreshTableList() {
        listModel.clear();
        TableMappingUtil.getAllTables().forEach(m -> listModel.addElement(m.get("table_name").toString()));
    }

    // 加载真实表分页数据
    private void loadTableData(String table) {
        if (table == null) return;
        new Thread(() -> {
            try {
                List<Map<String, Object>> countRes = dataService.query("SELECT COUNT(*) cnt FROM " + table);
                totalCount = Long.parseLong(countRes.get(0).get("cnt").toString());

                long offset = (currentPage - 1L) * pageSize;
                String pageSql = "SELECT * FROM " + table + " LIMIT " + offset + "," + pageSize;
                List<Map<String, Object>> list = dataService.query(pageSql);

                SwingUtilities.invokeLater(() -> {
                    renderTable(list);
                    long maxPage = getMaxPage();
                    pageTipLabel.setText(String.format("总条数：%d  总页数：%d  当前第%d页", totalCount, maxPage, currentPage));
                    pageJumpField.setText(String.valueOf(currentPage));
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // AI查询（支持分页）
    private void aiQuery() {
        if (currentTableName == null) {
            JOptionPane.showMessageDialog(this, "请先选择表");
            return;
        }
        String q = questionInput.getText().trim();
        if (q.isBlank()) return;

        new Thread(() -> {
            try {
                List<String> fields = dataService.query("SELECT * FROM " + currentTableName + " LIMIT 1")
                        .stream().findFirst().map(Map::keySet).orElse(Set.of()).stream().toList();
                String sql = aiService.generateSQL(currentTableName, fields, q);
                List<Map<String, Object>> fullResult = dataService.query(sql);

                isAiQueryResult = true;
                aiFullResult = fullResult;
                totalCount = fullResult.size();
                currentPage = 1;

                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "生成SQL：\n" + sql);
                    showAiPageData();
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // 显示AI分页数据
    private void showAiPageData() {
        int fromIndex = (currentPage - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, aiFullResult.size());
        List<Map<String, Object>> pageData = aiFullResult.subList(fromIndex, toIndex);

        renderTable(pageData);
        long maxPage = getMaxPage();
        pageTipLabel.setText(String.format("AI结果：%d条  总页数：%d  当前第%d页", totalCount, maxPage, currentPage));
        pageJumpField.setText(String.valueOf(currentPage));
    }

    // 统一渲染表格
    private void renderTable(List<Map<String, Object>> list) {
        tableModel.setRowCount(0);
        tableModel.setColumnCount(0);
        if (list.isEmpty()) return;

        Map<String, Object> first = list.get(0);
        first.keySet().forEach(tableModel::addColumn);
        list.forEach(row -> tableModel.addRow(row.values().toArray()));
    }

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

    private void addData() {
        if (currentTableName == null || isAiQueryResult) {
            JOptionPane.showMessageDialog(this, "请选择真实数据表才能新增");
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
            fieldMap.put(col, new JTextField());
            panel.add(fieldMap.get(col));
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
                    currentPage = 1;
                    loadTableData(currentTableName);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, "新增失败");
                }
            }).start();
        }
    }

    private void updateData() {
        if (isAiQueryResult) {
            JOptionPane.showMessageDialog(this, "AI查询结果无法直接修改，请切换原表进行修改");
            return;
        }
        if (currentTableName == null) {
            JOptionPane.showMessageDialog(this, "请先选择表");
            return;
        }
        int row = dataTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请选择一行数据");
            return;
        }

        int idIndex = -1;
        String idName = null;
        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            if (tableModel.getColumnName(i).equalsIgnoreCase("id")) {
                idIndex = i;
                idName = tableModel.getColumnName(i);
                break;
            }
        }
        if (idIndex == -1) {
            JOptionPane.showMessageDialog(this, "未找到主键ID");
            return;
        }
        Object idVal = tableModel.getValueAt(row, idIndex);

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
            String finalIdName = idName;
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
                    if (!set.isEmpty()) set.setLength(set.length() - 1);
                    String sql = "UPDATE " + currentTableName + " SET " + set + " WHERE `" + finalIdName + "`='" + idVal + "'";
                    dataService.update(sql);
                    refreshCurrentPageData();
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, "修改失败");
                }
            }).start();
        }
    }

    private void batchDeleteData() {
        if (isAiQueryResult) {
            JOptionPane.showMessageDialog(this, "AI结果无法删除，请切换原表");
            return;
        }
        if (currentTableName == null) {
            JOptionPane.showMessageDialog(this, "请选表");
            return;
        }
        int[] rows = dataTable.getSelectedRows();
        if (rows == null || rows.length == 0) {
            JOptionPane.showMessageDialog(this, "请选择行");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "确定删除选中 " + rows.length + " 行？");
        if (confirm != JOptionPane.YES_OPTION) return;

        new Thread(() -> {
            try {
                int idIndex = -1;
                for (int i = 0; i < tableModel.getColumnCount(); i++) {
                    if (tableModel.getColumnName(i).equalsIgnoreCase("id")) {
                        idIndex = i;
                        break;
                    }
                }
                if (idIndex == -1) {
                    JOptionPane.showMessageDialog(this, "未找到主键");
                    return;
                }
                for (int r : rows) {
                    Object val = tableModel.getValueAt(r, idIndex);
                    dataService.update("DELETE FROM " + currentTableName + " WHERE `id`='" + val + "'");
                }
                currentPage = 1;
                refreshCurrentPageData();
                JOptionPane.showMessageDialog(this, "删除成功");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "删除失败");
            }
        }).start();
    }

    private void exportData() {
        if (isAiQueryResult) {
            if (!aiFullResult.isEmpty()) {
                ExportService.exportToExcel(aiFullResult);
            } else {
                JOptionPane.showMessageDialog(this, "无数据");
            }
        } else {
            if (currentTableName != null) {
                new Thread(() -> {
                    List<Map<String, Object>> all = dataService.query("SELECT * FROM " + currentTableName);
                    SwingUtilities.invokeLater(() -> {
                        if (!all.isEmpty()) {
                            ExportService.exportToExcel(all);
                        } else {
                            JOptionPane.showMessageDialog(this, "无数据");
                        }
                    });
                }).start();
            }
        }
    }
}