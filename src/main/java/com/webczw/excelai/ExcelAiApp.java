package com.webczw.excelai;

import com.webczw.excelai.ui.MainFrame;
import com.webczw.excelai.util.MysqlStarter;
import com.webczw.excelai.util.TableMappingUtil;

import javax.swing.*;

public class ExcelAiApp {
    public static void main(String[] args) {
        // 自动启动内置MySQL
        MysqlStarter.startMysql();
        // 初始化表映射（关键）
        TableMappingUtil.initMappingTable();
        // Swing 必须用这种方式启动
        SwingUtilities.invokeLater(() -> {
            new MainFrame().setVisible(true);
        });
    }
}
