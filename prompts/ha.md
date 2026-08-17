# HA 接入规范

## 1. 范围与目标

icyPower 通过 HA 拉取两类数据：

- **电池电量**（核心）：用于驱动"HA 设备"Tab 的卡片显示、低电量提醒、历史趋势。
- **设备元数据**（次要）：实体名、所在区域（Area），用于分组、筛选、卡片展示。

本项目 **不** 调用 HA 的写接口，不做任何控制操作（不下发 Service）。

---

## 2. 认证方式选择

HA 提供多种认证方式，本项目选用 **Long-Lived Access Token（LLAT）**：

| 方式                              | 选择 | 理由                                                       |
| --------------------------------- | ---- | ---------------------------------------------------------- |
| Long-Lived Access Token           | ✅   | 一次申请长期有效；无需 OAuth2 跳转；适合桌面/本地应用        |
| OAuth2 Authorization Code Flow   | ❌   | 需起本地回调服务，对一次性配置成本过高                      |
| API Password                      | ❌   | 已在新版本 HA 中弃用，不应再使用                            |

### 2.1 申请流程

1. 用户登录 HA Web UI。
2. 进入个人资料页：`http://<ha-host>:8123/profile`。
3. 底部"Long-Lived Access Tokens" → Create Token。
4. 命名（建议：`icyPower`）→ 复制 **仅显示一次** 的 Token，粘贴至 icyPower 设置页。

### 2.2 安全约束

- Token **必须** 本地加密存储（参见 `feat.md` §3 数据安全与隐私）。
- 不向任何远端上传；不写入日志；不出现在崩溃报告中。
- 设置页提供"撤销并重连"操作：清除本地 Token 并标记需要重新输入。

---

## 3. 基础约定

| 项                | 值                                                          |
| ----------------- | ----------------------------------------------------------- |
| Base URL          | `http://<host>:8123`（HTTPS 可选，取决于用户 HA 配置）       |
| WebSocket URL     | `ws://<host>:8123/api/websocket`（HTTPS 时为 `wss://`）      |
| 默认端口          | `8123`                                                      |
| 认证 Header       | `Authorization: Bearer <LLAT>`                              |
| Content-Type      | `application/json`                                          |
| 时区              | HA 实体时间戳为 ISO 8601 带时区（如 `2024-05-30T21:43:29+00:00`），前端按本机时区显示 |
| 字符编码          | UTF-8                                                       |

---

## 4. REST API（用于初始化与按需拉取）

### 4.1 用到的端点

| 方法  | 路径                            | 用途                                       |
| ----- | ------------------------------- | ------------------------------------------ |
| GET   | `/api/`                         | 探活：返回 `{"message": "API running."}` 即视为连通 |
| GET   | `/api/states`                   | 一次性拉取所有实体状态，用于首屏初始化     |
| GET   | `/api/states/<entity_id>`       | 读取单个实体的最新状态                     |
| GET   | `/api/history/period/<timestamp>?filter_entity_id=<entity_id>&minimal_response` | 拉取实体历史电量，用于趋势图 |

> `/api/` 路径结尾的 `/` **不可省略**，否则返回重定向或 404。

### 4.2 单实体响应（节选）

```json
{
  "entity_id": "sensor.kitchen_battery",
  "state": "85",
  "last_changed": "2024-05-30T21:43:29.204838+00:00",
  "last_updated": "2024-05-30T21:50:30.529465+00:00",
  "attributes": {
    "friendly_name": "Kitchen Sensor",
    "battery": 85,
    "battery_level": 85,
    "unit_of_measurement": "%",
    "device_class": "battery"
  }
}
```

### 4.3 错误码

| 状态码 | 含义                 | 处理                                  |
| ------ | -------------------- | ------------------------------------- |
| 200    | 成功                 | 正常处理                              |
| 400    | 请求格式错           | 一般为实现 bug，记录日志但不弹窗      |
| 401    | Token 无效/过期       | 触发"重新认证"流程（同 `auth_invalid`）|
| 404    | 实体不存在           | 该实体被用户从 HA 删除；标记为 `unavailable` |
| 405    | 方法不允许           | 实现 bug                              |

---

## 5. WebSocket API（用于实时增量）

REST 用于初始化与历史拉取；进入主页后切换到 WebSocket 以接收电量增量。

### 5.1 握手流程

```
Client                                  Server
  |  ---- WebSocket Upgrade ----------->   |
  |                                       |
  |  <----- { type: "auth_required" } ---  |
  |                                         |
  |  ----- { type: "auth",                --|
  |         access_token: "<LLAT>" }       |
  |                                         |
  |  <----- { type: "auth_ok" } ---------  |   ← 成功
  |  <----- { type: "auth_invalid" } ---- |   ← 失败，连接关闭
```

### 5.2 消息规范

- 认证成功后的每条消息 **必须** 携带整数 `id`。
- 命令执行完毕，服务器返回同 `id` 的 `{ type: "result", success: true/false, result?: ..., error?: { code, message } }`。
- 订阅类命令在执行成功后还会持续收到 `{ type: "event", id, event: {...} }` 推送。

### 5.3 本项目用到的消息

**初始化（连接建立后立即发）：**

```json
{ "id": 1, "type": "get_states" }
```

→ 返回全量实体数组，用于建立内存基线（避免 WS 推送只含增量）。

**订阅电量变化（推荐窄订阅，按需订阅受监控实体）：**

> HA 原生 `subscribe_events` 是按 `event_type` 订阅，无法按 entity 过滤；
> 因此本项目采用 **`subscribe_message`** 模式，订阅 `state_changed` 但在客户端
> 过滤目标 entity（见官方文档 `subscribe_message`）。
>
> 备选：先 `subscribe_events { event_type: "state_changed" }`，再在客户端
> 对 `event.data.entity_id` 做白名单匹配；本项目优先用前者以减少消息量。

```json
{
  "id": 2,
  "type": "subscribe_message",
  "event_type": "state_changed",
  "data": { "entity_id": ["sensor.kitchen_battery", "sensor.door_battery"] }
}
```

**保活：** 每 30 秒发送一次 `ping`；收到 `pong` 即视为连接健康；连续 2 次未收到则重连。

```json
{ "id": 99, "type": "ping" }
```

**取消订阅（退出/重置时）：**

```json
{ "id": 3, "type": "unsubscribe_message", "subscription": 2 }
```

### 5.4 事件载荷（节选）

```json
{
  "id": 2,
  "type": "event",
  "event": {
    "event_type": "state_changed",
    "time_fired": "2024-05-30T21:43:29.265429+00:00",
    "origin": "LOCAL",
    "data": {
      "entity_id": "sensor.kitchen_battery",
      "new_state": {
        "entity_id": "sensor.kitchen_battery",
        "state": "82",
        "attributes": { "battery": 82, "battery_level": 82, "unit_of_measurement": "%" },
        "last_updated": "2024-05-30T21:43:29.204838+00:00"
      },
      "old_state": { "...": "略" }
    }
  }
}
```

### 5.5 重连策略

- 任何非主动断开（`auth_invalid`、网络错误、连续 ping 失败）→ 等待 `2^n × 1s`（n 重试次数）后重连，封顶 60s。
- 重连成功后立即重发 `get_states` + `subscribe_message`，恢复基线。
- 401 / `auth_invalid` → **不** 自动重连，标记 Token 失效，进入设置页引导重新认证。

---

## 6. 电池电量数据契约

不同集成暴露电量字段的方式不统一，读取顺序如下（命中即返回）：

1. `attributes.battery_level`（数值，0–100）
2. `attributes.battery`（数值，0–100）
3. `state`（字符串，先尝试 `parseFloat`，失败则视为无效）

无效处理：

- 上述三处都拿不到数值 → 该实体视为 **无电量属性**，在设置页的"HA 设备"列表中标灰，并附说明"该实体不暴露电池字段"。
- `state` 为 `unknown` / `unavailable` / `none` → 标记为 `unavailable`，不参与阈值判断。

---

## 7. 本项目接入架构（高层）

```
┌─────────────┐  get_states      ┌──────────────┐
│ icyPower UI │ <─────────────── │   HA Core    │
│             │  state_changed   │              │
│  内存基线   │ <─────────────── │  WS /api/ws  │
│  + 本地缓存 │                  └──────────────┘
└─────────────┘
        │
        ▼
   REST /api/history/period
   （仅在设备详情页请求历史趋势时按需拉取）
```

- **冷启动**：进入主页 → REST `GET /api/` 探活 → REST `GET /api/states` 建基线 → WS 升级。
- **运行期**：WS 推 `state_changed` → 客户端按白名单过滤 → 更新内存 → 触发卡片重渲染。
- **降级**：WS 不可用时自动 fallback 到 REST 轮询（建议 60s 间隔，仅轮询白名单内实体）。

---

## 8. 相关资源

- 认证总览：<https://developers.home-assistant.io/docs/auth_index>
- REST API：<https://developers.home-assistant.io/docs/api/rest>
- WebSocket API：<https://developers.home-assistant.io/docs/api/websocket>
- 权限策略：<https://developers.home-assistant.io/docs/auth_permissions>
- HA 用户文档：<https://www.home-assistant.io/docs/>
