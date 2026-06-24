# WeChat Channel Java SDK

> 基于 WeChat ilink 协议的 Java 库。一行 `onMessage` 回调 = 接入任意 AI。

**状态**: 开发中 (v0.1.0-SNAPSHOT)

---

## 功能概览

本项目是 [`@esyion/wechat-channel`](https://github.com/esyion/wechat-channel) 的 Java 版本，实现以下功能：

- ✅ 扫码登录 (QR Code Login)
- ✅ 长轮询消息接收 (Long Poll)
- ✅ 文本消息发送
- ✅ 媒体文件收发 (图片/视频/文件/语音)
- ✅ 媒体加解密 (AES-128-ECB)
- ✅ CDN 上传/下载
- ✅ "正在输入"状态提醒
- ✅ 会话持久化 (FileStore / MemoryStore)
- ✅ 优雅退出

---

## 项目结构

```
src/main/java/io/github/esyion/wechat/
├── Main.java                    # 示例入口
├── WechatChannelProvider.java    # 顶层API入口
├── config/
│   └── Config.java              # 配置类
├── error/
│   ├── ChannelError.java        # 通道错误
│   ├── WechatApiError.java      # API错误
│   └── MediaError.java          # 媒体错误
├── store/
│   ├── Store.java               # Store接口
│   ├── FileStore.java           # 文件持久化
│   └── MemoryStore.java         # 内存存储
├── wechat/
│   ├── WechatApiClient.java     # API客户端 (11个端点)
│   ├── WechatTypes.java         # 协议类型定义
│   ├── CryptoUtil.java          # AES-128-ECB加解密
│   ├── MediaUtil.java           # CDN媒体处理
│   ├── QRLogin.java             # 扫码登录
│   ├── LoginResult.java         # 登录结果
│   └── LoginHandle.java         # 登录句柄
└── channel/
    ├── ChannelHandle.java       # ChannelHandle接口
    ├── ChannelMsg.java          # 消息类型
    ├── CreateChannelOpts.java   # 创建选项
    ├── MediaRef.java           # 媒体引用
    ├── Reply.java              # Reply接口
    ├── Inbound.java             # 接收消息处理
    ├── Outbound.java            # 发送消息处理
    ├── ReplyImpl.java           # Reply实现
    ├── LongPoll.java            # 长轮询循环
    └── TypingKeepalive.java     # 输入状态心跳
```

---

## 快速开始

### 1. 安装依赖

需要 Java 17+。项目使用 Maven 管理依赖：

```bash
# 安装 Maven: https://maven.apache.org/install.html
mvn install
```

### 2. 日志配置

SDK 使用 **SLF4J** 作为日志接口。需要用户在应用中配置具体的日志实现（如 Logback）。

**pom.xml 添加依赖**：

```xml
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-api</artifactId>
    <version>2.0.13</version>
</dependency>
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>1.5.6</version>
    <scope>runtime</scope>
</dependency>
```

**src/main/resources/logback.xml**：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>[%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="STDOUT"/>
    </root>

    <!-- SDK日志级别 -->
    <logger name="io.github.esyion.wechat" level="DEBUG"/>
</configuration>
```

### 3. 扫码登录获取凭证

```java
import io.github.esyion.wechat.*;
import io.github.esyion.wechat.wechat.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// 注意：示例中使用 System.out 仅用于演示
// 实际应用应使用 Logger
private static final Logger log = LoggerFactory.getLogger(MyBot.class);

// 1. 请求二维码（无需任何凭证）
WechatApiClient api = new WechatApiClient(
    "https://ilinkai.weixin.qq.com",
    "https://novac2c.cdn.weixin.qq.com/c2c",
    null,  // 登录阶段不需要 token
    "wechat-channel/0.1.0",
    "wechat-channel/0.1.0",
    15000, 35000
);

QRLogin login = new QRLogin(api, "3");
QRLogin.QrCodeResult qr = login.requestQrCode();

// 2. 获取二维码内容，用于渲染
// qr.qrcode - 二维码原始内容
// qr.qrcodeImgContent - 可能是 data:image/png;base64,... 或 URL
log.info("请扫描二维码: {}", qr.qrcode);

// 3. 等待用户扫码确认登录
LoginResult result = login.pollQrLogin(qr.qrcode, 120000, null, null, null);

if (!result.connected) {
    log.error("登录失败: {}", result.message);
    return;
}

log.info("登录成功! botToken={}, accountId={}", result.botToken, result.accountId);

// 4. 保存这三个值：result.botToken, result.accountId, result.baseUrl
```

### 4. 使用凭证创建Channel

```java
import io.github.esyion.wechat.*;
import io.github.esyion.wechat.channel.*;
import java.util.concurrent.CompletableFuture;

private static final Logger log = LoggerFactory.getLogger(MyBot.class);

// 创建Channel（使用登录获取的凭证）
ChannelHandle handle = WechatChannelProvider.createChannel(
    new CreateChannelOpts()
        .botToken(result.botToken)
        .accountId(result.accountId)
        .baseUrl(result.baseUrl)  // 可选
        .onMessage((msg, reply) -> {
            log.info("收到消息 from {}: {}", msg.fromUserId, msg.text);

            // 回复文本
            reply.text("你说了: " + msg.text);

            // 或回复媒体
            // reply.media("/path/to/image.png");
        })
        .onError((err, ctx) -> {
            log.error("错误 (phase={}): {}", ctx.get("phase"), err.getMessage());
        })
);

// 5. 启动
handle.start();
log.info("Channel 已启动，按 Ctrl+C 停止");

// 6. 优雅退出
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    log.info("正在停止 Channel...");
    handle.stop();
}));
```

---

## API 参考

### CreateChannelOpts

| 参数 | 类型 | 说明 |
|------|------|------|
| `botToken` | String | 机器人令牌 (必需) |
| `accountId` | String | 账号ID (必需) |
| `baseUrl` | String | ilink网关地址 (默认: https://ilinkai.weixin.qq.com) |
| `cdnBaseUrl` | String | CDN地址 (默认: https://novac2c.cdn.weixin.qq.com/c2c) |
| `channelVersion` | String | 协议版本 (默认: wechat-channel/0.1.0) |
| `botAgent` | String | Bot代理标识 |
| `botType` | String | Bot类型 (默认: 3) |
| `stateDir` | String | 状态文件目录 (默认: ~/.wechat-channel) |
| `store` | Store | 持久化存储 (默认: FileStore) |
| `mediaTmpDir` | String | 媒体临时目录 |
| `onMessage` | BiFunction<ChannelMsg, Reply, CompletableFuture<Void>> | 消息回调 |
| `onError` | BiConsumer<Throwable, Map<String, Object>> | 错误回调 |
| `longPollTimeoutMs` | int | 长轮询超时 (默认: 35000) |
| `blockedUsers` | Set<String> | 屏蔽的用户ID |

### ChannelMsg

| 字段 | 类型 | 说明 |
|------|------|------|
| `fromUserId` | String | 发送者微信ID |
| `contextToken` | String | 会话token |
| `text` | String | 文本内容 |
| `media` | List<MediaRef> | 已解密媒体文件 |
| `raw` | WeixinMessage | 原始协议消息 |

### MediaRef

| 字段 | 类型 | 说明 |
|------|------|------|
| `path` | Path | 本地文件路径 |
| `mime` | String | MIME类型 |

### Reply

| 方法 | 说明 |
|------|------|
| `text(content)` | 发送文本 |
| `text(content, maxChars)` | 分块发送文本 |
| `media(filePath)` | 发送媒体 |
| `media(filePath, caption)` | 发送媒体+文字说明 |
| `typing(on)` | 开启/关闭"正在输入" |

### ChannelHandle

| 方法 | 说明 |
|------|------|
| `start()` | 启动Channel，开始长轮询 |
| `stop()` | 停止Channel |

### LoginResult

| 字段 | 类型 | 说明 |
|------|------|------|
| `connected` | boolean | 是否登录成功 |
| `botToken` | String | 机器人令牌 |
| `accountId` | String | 账号ID |
| `baseUrl` | String | ilink网关地址 |
| `userId` | String | 用户ID |
| `alreadyConnected` | Boolean | 是否已连接 |
| `message` | String | 结果消息 |

### 错误处理

SDK定义了三类错误：

| 错误类 | 说明 | 典型场景 |
|--------|------|----------|
| `ChannelError` | 通道错误 | 缺少botToken、accountId无效 |
| `WechatApiError` | API错误 | 服务器返回错误码 |
| `MediaError` | 媒体错误 | 下载/解密/上传失败 |

错误回调中的 phase 值：

| phase | 说明 |
|-------|------|
| `notifyStart` | 启动通知失败 |
| `getUpdates` | 长轮询失败 |
| `sessionExpired` | Session过期（自动暂停1小时） |
| `inbound` | 接收消息处理失败 |
| `handler` | 业务回调异常 |
| `notifyStop` | 停止通知失败 |

---

## 与 TypeScript 版本对比

| TypeScript | Java |
|------------|------|
| `loginQR()` | `QRLogin.requestQrCode()` + `pollQrLogin()` |
| `createChannel()` | `WechatChannelProvider.createChannel()` |
| `ChannelMsg` | `ChannelMsg` |
| `Reply` | `Reply` (interface) |
| `QRLoginHandle` | `QRLogin.QrCodeResult` |
| `LoginResult` | `LoginResult` |
| `Store` | `Store` (interface) |
| `JsonFileStore` | `FileStore` |
| `MemoryStore` | `MemoryStore` |
| `WechatApiError` | `WechatApiError` |
| `MediaError` | `MediaError` |
| `ChannelError` | `ChannelError` |
| SLF4J (pino) | SLF4J (Logback) |

---

## 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `WECHAT_CHANNEL_BASE_URL` | ilink网关地址 | https://ilinkai.weixin.qq.com |
| `WECHAT_CHANNEL_CDN_BASE_URL` | CDN地址 | https://novac2c.cdn.weixin.qq.com/c2c |
| `WECHAT_CHANNEL_STATE_DIR` | 状态目录 | ~/.wechat-channel |
| `WECHAT_CHANNEL_LONG_POLL_TIMEOUT_MS` | 长轮询超时 | 35000 |

---

## 已知问题

- [ ] 需要 Maven 或 Gradle 来编译项目
- [ ] 二维码渲染功能 (toPng/toSvg) 需要额外库支持
- [ ] 尚未进行实际环境测试

---

## License

MIT
