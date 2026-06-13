# Excel AI Assistant

基于 Java Swing + AI 的智能 Excel 数据分析助手,支持通过自然语言查询 Excel 数据并导出结果。

## 一、功能特性

- **Excel 导入**: 一键导入 Excel 文件,自动识别表头和数据
- **AI 智能建表**: 利用 DeepSeek AI 自动分析字段类型并创建 MySQL 表结构
- **自然语言查询**: 使用大白话描述查询需求,AI 自动生成 SQL 执行
- **实时数据展示**: 表格形式展示查询结果,支持动态刷新
- **结果导出**: 将查询结果导出为 Excel 文件
- **内置 MySQL**: 自带便携式 MySQL 数据库,无需额外安装
- **多表管理**: 支持导入多个 Excel 表,自由切换查看

## 二、技术栈

- **开发语言**: Java 17
- **构建工具**: Maven
- **UI 框架**: Java Swing
- **Excel 处理**: Alibaba EasyExcel 3.3.2
- **数据库**: MySQL 8.0.33 (内置便携版)
- **连接池**: HikariCP 4.0.3
- **HTTP 客户端**: Hutool HTTP 5.8.25
- **JSON 处理**: Hutool JSON 5.8.25
- **日志框架**: SLF4J 1.7.36
- **AI 服务**: DeepSeek API

## 三、前置要求

- JDK 17 或更高版本
- Maven 3.6+
- Windows 操作系统 (内置 MySQL 为 Windows 版本)

## 四、快速开始

### 1. 克隆项目

git clone <repository-url>
cd excel-ai-assistant
### 2. 配置 API Key

在运行前需要配置 DeepSeek API Key,有两种方式:

**方式一: VM Options (推荐)**

在 IDEA 的 Run/Debug Configurations → VM options 中添加:
-DappKey=你的DeepSeek_API_Key

**方式二: 命令行运行**
java -DappKey=你的DeepSeek_API_Key -jar target/excel-ai-assistant-1.0-SNAPSHOT-jar-with-dependencies.jar

> **安全提示**: 请勿将 API Key 硬编码在代码中,生产环境建议使用环境变量或配置文件管理密钥。

### 3. 编译打包
mvn clean package

### 4. 运行应用

**IDEA 中运行:**
- 配置好 VM options 后直接运行 `ExcelAiApp` 主类

**JAR 包运行:**

java -DappKey=你的DeepSeek_API_Key -jar target/excel-ai-assistant-1.0-SNAPSHOT-jar-with-dependencies.jar
> **重要**: 必须在项目根目录下执行 java -jar 命令,确保 mysql 目录和 my.ini 配置文件存在。

## 五、使用说明

### 1.导入 Excel

1. 点击 **"导入Excel"** 按钮
2. 选择要导入的 `.xlsx` 或 `.xls` 文件
3. 系统会自动:
   - 读取 Excel 数据和表头
   - 调用 AI 分析字段类型
   - 创建对应的 MySQL 表
   - 批量插入数据
4. 导入成功后左侧列表会显示新表

### 2.AI 查询

1. 在左侧列表中选择要查询的表
2. 在底部输入框用自然语言描述查询需求,例如:
   - "找出销售额大于1000的记录"
   - "按部门统计平均工资"
   - "查询最近一个月的订单"
3. 点击 **"执行AI查询"** 按钮
4. 系统会显示生成的 SQL 语句和执行结果

### 3.导出数据

1. 执行查询后,如果有结果数据
2. 点击 **"导出Excel"** 按钮
3. 选择保存位置即可导出

### 4.删除表

1. 在左侧列表中选择要删除的表
2. 点击 **"删除选中表"** 按钮
3. 确认删除操作

## 六、项目结构
excel-ai-assistant/
├── mysql/                      # 内置 MySQL 数据库(运行时自动解压)
├── src/main/java/com/webczw/excelai/
│   ├── ExcelAiApp.java        # 应用入口
│   ├── config/                # 配置类
│   ├── service/               # 业务服务层
│   │   ├── AiService.java    # AI 对话服务(生成 SQL)
│   │   ├── AiTableGenerator.java  # AI 建表服务
│   │   ├── DataService.java  # 数据查询服务
│   │   ├── ExcelService.java # Excel 读写服务
│   │   └── ExportService.java # 结果导出服务
│   ├── ui/                    # UI 界面层
│   │   └── MainFrame.java    # 主窗口
│   └── util/                  # 工具类
│       ├── MysqlStarter.java  # MySQL 启动器
│       ├── TableMappingUtil.java  # 表映射管理
│       └── SqlUtil.java      # SQL 工具类
├── pom.xml                    # Maven 配置
└── README.md                  # 项目文档

## 七、配置说明

### Maven 打包配置

项目使用 `maven-assembly-plugin` 打包成 Fat JAR,包含所有依赖:
<mainClass>com.webczw.excelai.ExcelAiApp</mainClass>

打包命令:
mvn clean package


生成的 JAR 包位于: `target/excel-ai-assistant-1.0-SNAPSHOT-jar-with-dependencies.jar`

### MySQL 资源配置

`mysql/` 目录会被打包进 JAR,运行时自动解压到 JAR 包同级目录,实现便携部署。

### 运行目录要求

**必须在项目根目录下运行**,因为:
- 项目需要访问 `mysql/` 目录
- 需要读取 `my.ini` 配置文件
- 从其他目录(如 target)运行时无法正确定位资源

## 八、常见问题

### 1. 运行时报错 "没有主清单属性"

**原因**: JAR 包未配置主类

**解决**: 确保使用 `mvn clean package` 重新打包,生成的 `-jar-with-dependencies.jar` 已配置主类。

### 2. 找不到 API Key

**原因**: 未配置 VM options

**解决**: 添加 `-DappKey=你的密钥` 到 JVM 启动参数。

### 3. MySQL 启动失败

**原因**: 
- 工作目录不正确
- MySQL 端口被占用

**解决**: 
- 确保从项目根目录运行
- 检查 3306 端口是否被占用

### 4. NoClassDefFoundError

**原因**: 依赖未正确打包

**解决**: 执行 `mvn clean install -U` 刷新依赖后重新打包。

### 5. 找不到 mysql 目录或 my.ini

**原因**: 从错误的目录运行

**解决**: 必须切换到项目根目录后再执行 java -jar 命令。

## 九、注意事项

1. **API Key 安全**: 不要将 API Key 提交到版本控制系统
2. **网络要求**: 需要访问 DeepSeek API,请确保网络连接正常
3. **MySQL 端口**: 内置 MySQL 默认使用 3306 端口,如有冲突需修改配置
4. **文件格式**: 目前支持 `.xlsx` 和 `.xls` 格式的 Excel 文件
5. **数据量限制**: 建议单次导入数据不超过 10 万行,以保证性能
6. **运行目录**: 必须在项目根目录下运行,确保能访问 mysql 目录和配置文件







