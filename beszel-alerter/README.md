# Beszel Alerter

独立 Spring Boot 工程：按 JSON 配置轮询多个 Beszel Hub，对 CPU / 内存 / 磁盘做 `gt` / `lt` 阈值判断，命中后通过企业微信 Webhook 推送告警。

## 技术栈

- Java 17
- Spring Boot 3.3
- Maven
- `RestClient`（Beszel PocketBase API + 企业微信）

## 快速开始

### 1. 准备配置

```bash
cp config/monitor-config.example.json config/monitor-config.json
```

编辑 `config/monitor-config.json`：填入各项目的 Beszel `baseUrl` / 账号密码，以及企业微信 Webhook `key`。**不要把真实密钥提交到 Git。**

### 2. 构建

```bash
mvn -q package
```

### 3. 运行

```bash
java -jar target/beszel-alerter-1.0.0-SNAPSHOT.jar --monitor.config=./config/monitor-config.json
```

或开发模式：

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--monitor.config=./config/monitor-config.json
```

### 4. 测试

```bash
mvn -q test
```

## 配置说明

| 层级 | 含义 |
|------|------|
| `channels` | 全局可复用推送渠道（当前实现 `wecom_webhook`） |
| `defaults` | 默认轮询间隔、渠道、冷却秒数、阈值 |
| `projects[]` | 一个业务平台 + 一个 Beszel Hub |
| `projects[].hosts[]` | 该 Hub 下要盯的机器（IP + 可选显示名 + 阈值） |

### 规则

- **新增项目**：在 `projects` 再加一项，填 `name` + `beszel.baseUrl` 等即可
- `hosts[].thresholds` 可省略，继承项目级 `thresholds`，再继承 `defaults.thresholds`
- `gt` / `lt`：大于 / 小于触发；`null` 表示该方向关闭
- `channel` 指向全局 `channels`；不同项目可共用或分渠道
- `cooldownSeconds`：同一 **项目名 + IP + 指标 + 方向** 冷却期内不重复推
- 主机 `status != up`：推「离线」告警（同样冷却）
- **`hosts` 为空或未配置**：监控该 Hub 下全部 systems，套用项目/默认阈值；配置了 `hosts` 则只盯名单内机器

### 主机匹配

配置的 `ip` 优先对 Beszel 系统的 `host` 字段，其次对 `name`。

### 指标来源

Beszel `systems` 记录的 `info.cpu` / `info.mp` / `info.dp`（百分比）。

## 告警文案示例

```
【Beszel告警】科研管理平台
主机: 应用节点1 (10.0.0.1)
指标: CPU 91.2% > 80
Hub: https://xm.gdstc.gd.gov.cn/beszel
状态: up
时间: 2026-11-05 15:40:00
```

## 工程结构

```
beszel-alerter/
  pom.xml
  README.md
  config/monitor-config.example.json
  src/main/java/com/gdstc/beszel/
    BeszelAlerterApplication.java
    config/…          # JSON 模型与加载
    beszel/…          # Hub 客户端与指标
    alert/…           # 阈值、冷却、事件
    notify/…          # 企业微信 Webhook
    schedule/…        # 多项目调度
```

## 安全

- 仓库只提供 `monitor-config.example.json`（占位符，无真实密钥）
- 本地复制为 `monitor-config.json` 再填真实值
- 建议将真实配置放在部署机外置路径，通过 `--monitor.config=` 传入
