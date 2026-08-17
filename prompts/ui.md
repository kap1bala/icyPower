---
name: Ant
colors:
  primary: "#1677ff"       # blue-6, 品牌主色（Ant Design 拂晓蓝）
  secondary: "#722ED1"     # purple-6
  success: "#52C41A"       # green-6（健康 / 已处理）
  warning: "#FAAD14"       # gold-6（即将到期 / 提醒）
  danger: "#FF4D4F"        # red-6（严重超期 / 失败）
  surface: "#FFFFFF"       # 容器 / 卡片底色
  text: "rgba(0,0,0,0.88)" # 主要文字
colorsDark:
  # 表面 5 层结构 —— 容器 / 卡片 / 浮层 / 弹窗 / 模态
  surface-base: "#000000"                 # Layer 1：页面背景（Page bg）
  surface-container: "#141414"            # Layer 2：卡片背景
  surface-elevated: "#1F1F1F"             # Layer 3：浮层 / Dropdown
  surface-overlay: "#303030"              # Layer 4：下拉菜单 / Modal 背景层
  surface-modal: "#1F1F1F"                # Modal 实际
  text: "rgba(255,255,255,0.88)"          # 主要文字（暗色下用半透明白）
  # 中性色 13 阶（border / divider / bg / text 共用一套透明度阶梯）
  border: "rgba(255,255,255,0.12)"
  border-strong: "rgba(255,255,255,0.20)"
  divider: "rgba(255,255,255,0.12)"
  text-secondary: "rgba(255,255,255,0.65)"
  text-disabled: "rgba(255,255,255,0.30)"
  text-placeholder: "rgba(255,255,255,0.25)"
  # 语义色在暗色下的提亮
  primary: "#1677FF"        # 暗色下主色不变（已经在 #000 底上够亮）
  secondary: "#A77BFF"      # purple-7（暗色提亮）
  success: "#49AA19"        # green-6 暗色保持
  warning: "#FFC53D"        # gold-7（暗色提亮）
  danger: "#E5484D"         # red-7（暗色提亮）
  # 弱化容器背景（徽标 / 提示底色）
  primary-soft: "#003A8C"
  secondary-soft: "#2C1A4D"
  success-soft: "#092B00"
  warning-soft: "#3E2C00"
  danger-soft: "#3D0F0F"
  # 图表系列
  chart-series-1: "#1677FF"
  chart-series-2: "#722ED1"
  chart-series-3: "#13C2C2"
  chart-series-4: "#52C41A"
  chart-series-5: "#FAAD14"
  chart-series-6: "#FF4D4F"
typography:
  fontFamilyBase:
    - "-apple-system"
    - "BlinkMacSystemFont"
    - "'Segoe UI'"
    - "Roboto"
    - "'Helvetica Neue'"
    - "Arial"
    - "'Noto Sans'"
    - "sans-serif"
    - "'Apple Color Emoji'"
    - "'Segoe UI Emoji'"
    - "'Segoe UI Symbol'"
    - "'Noto Color Emoji'"
  fontFamilyCode:
    - "ui-monospace"
    - "SFMono-Regular"
    - "Menlo"
    - "Monaco"
    - "Consolas"
    - "'Liberation Mono'"
    - "'Courier New'"
    - "monospace"
  # 字号阶梯（FontSize / LineHeight）
  fontSize: 14
  fontSizeSM: 12
  fontSizeLG: 16
  fontSizeXL: 20
  fontSizeHeading2: 24
  fontSizeHeading1: 30
  lineHeight: 1.5714285714285714 # 22 / 14
  lineHeightSM: 1.6666666666666667 # 20 / 12
  lineHeightLG: 1.5 # 24 / 16
  lineHeightHeading: 1.2666666666666666 # 38 / 30
  fontWeightStrong: 600
  fontWeight: 400
  fontWeightMedium: 500
spacing:
  # 8 倍数 + 半步（4）—— Ant Design 标准 margin / padding token
  xxs: 4
  xs: 8
  sm: 12
  md: 16
  lg: 24
  xl: 32
  xxl: 48
rounded:
  xs: 4   # Tag / 小型徽标
  sm: 6   # Button 默认
  md: 8   # Card / Modal 默认
  lg: 16  # 主面板（Tab 切换 / 抽屉）
shadow:
  # 三档阴影 + 一档无阴影
  none: "none"
  layer-1: "0 1px 2px 0 rgba(0,0,0,0.03), 0 1px 6px -1px rgba(0,0,0,0.02), 0 2px 4px 0 rgba(0,0,0,0.02)"
  layer-2: "0 6px 16px 0 rgba(0,0,0,0.08), 0 3px 6px -4px rgba(0,0,0,0.12), 0 9px 28px 8px rgba(0,0,0,0.05)"
  layer-3: "0 6px 16px 0 rgba(0,0,0,0.08), 0 9px 28px 0 rgba(0,0,0,0.05), 0 12px 48px 16px rgba(0,0,0,0.03)"
motion:
  fast: "0.1s"
  base: "0.2s"
  slow: "0.3s"
  easing-standard: "cubic-bezier(0.4, 0, 0.2, 1)"
  easing-decelerate: "cubic-bezier(0, 0, 0.2, 1)"
  easing-accelerate: "cubic-bezier(0.4, 0, 1, 1)"
---

# Ant 设计体系 · icyPower 视觉规范

> 本文档是 icyPower 所有 UI / 主题代码的 **单一来源（Single Source of Truth）**。
> `Color.kt` / `Type.kt` / `Theme.kt` 中所有 hex、字号、间距、阴影、动效都必须从本文件派生。
> 任何组件代码 **不得** 硬编码颜色或魔法数字 —— 一律引用 `LocalXxx.current` / `MaterialTheme.colorScheme.xxx` / `MaterialTheme.typography.xxx` / `MaterialTheme.spacing.xxx`。

---

## 0. 规范如何被消费

| 资产 | 来源 | 落地文件 |
| --- | --- | --- |
| 色彩 / 字体 / 间距 / 阴影 / 动效 token | 本文件 frontmatter | `ui/theme/Color.kt` |
| 主题系统（light / dark / system） | §4 | `ui/theme/Theme.kt` |
| Material3 ColorScheme 派生 | §4 + §6 | `ui/theme/Theme.kt` |
| 字号 / 行高 / 字重 | §5 | `ui/theme/Type.kt` |
| 语义色 CompositionLocal（warning / danger / success） | §4.3 | `ui/theme/Theme.kt` |
| 组件级 spacing / sizing token | §7 | 各 Composable 内 `MaterialTheme.spacing.*` |
| 文案/i18n | `feat.md` §5 | `res/values/strings.xml` + `res/values-en/strings.xml`（后续） |

违反本规范的颜色 / 数字在 PR review 中视为 **必须修改**。

---

## 1. 设计价值观（四象限）

Antd 的四个设计价值观是**所有决策的依据**。当我们面临"这个交互应该多突兀 / 这个色彩应该多显眼"等选择时，回到这四条原则：

### 1.1 自然（Natural）
"设计应贴合用户心智与现实规律。"

- 物理隐喻优先：按钮按下时"沉入水面"，松开后"弹起带涟漪"——给用户的反馈贴合日常经验。
- 节奏贴近自然律动：动效曲线用 `cubic-bezier(0.4, 0, 0.2, 1)`（"standard" 缓动）模拟匀加速到匀减速，而不是匀速。
- 字号阶梯参考"动态秩序"——`12 / 14 / 16 / 20 / 24 / 30` 的对数间距自然落到视觉舒适区。

### 1.2 确定性（Certainty / Performant）
"用户对设计行为有预期，结果可掌控。"

- 反馈必须即时（< 100ms）：点按钮立即看到按下态，加载超过 300ms 必须出现 skeleton / spinner。
- 状态可逆：危险操作（删除）必走 `Popconfirm` 二次确认；可关闭通知必可重新查看（`Notification` 的 `duration: 0`）。
- 列表项整体行为一致：消失动效全部"同时退出"而非 stagger，避免出现一半有 / 一半无的尴尬中间态。

### 1.3 意义感（Meaningful / Concise）
"每个像素都承载业务价值，不做无意义装饰。"

- **颜色仅用于传递语义**：`primary` 意为主操作；`warning / danger` 意为告警。不为"美观"引入额外色相。
- **动效仅用于引导注意**：菜单展开时箭头方向变化"够用即可"，不做花哨翻转。
- **避免装饰性 emoji / 渐变 / 大色块**：在数据密集页面里，这些只会抢占用户的注意力焦点。

### 1.4 生长性（Growing）
"具备可持续演化能力。"

- Token 化优先：颜色 / 字号 / 间距 / 阴影都是 token，未来加新组件 / 新主题（fe.md §5 高对比度模式）无需改动现有代码。
- 新模式引入必须同步迁移指南：例如未来增加 `compact / comfortable` 两种密度，新增 token 不破坏旧组件。
- 配置驱动：周期设备清单 / HA 监控设备 / 提醒阈值等全部走 DataStore 声明式配置，不硬编码到 UI。

---

## 2. 设计基础原则（10 条）

antd 提炼的 10 条"基础视觉法则"，按重要度排序，我们逐条应用到本项目：

| # | 原则 | 在 icyPower 的落地 |
| --- | --- | --- |
| 1 | **亲密性（Proximity）** | 卡片内字段用 `spacing.xs` (8dp) 关联；卡片与卡片之间用 `spacing.md` (16dp) 区分。 |
| 2 | **对齐（Alignment）** | 所有列表项左对齐到 16dp 边距；按钮组右对齐；icon + text 沿 icon 中心垂直对齐。 |
| 3 | **对比（Contrast）** | 标题与正文至少 2:1 字号差；状态色与正文 WCAG 4.5:1 对比度。 |
| 4 | **重复（Repetition）** | 所有卡片圆角统一 `md` (8dp)；所有按钮圆角统一 `sm` (6dp)；字号阶梯全项目只 5 档。 |
| 5 | **直截了当（Direct）** | 主操作一次点击完成：主页"已充电"按钮直接 reset；进入新页面时 modal 而不是下钻子页。 |
| 6 | **足不出户（Stay in place）** | 详情用 `Drawer` 而非新页；删除确认用 `Popconfirm` 而非新页。 |
| 7 | **简化交互（Simplicity）** | 一屏一主任务；超过 3 个次级操作时收纳到 `Dropdown`。 |
| 8 | **提供邀请（Invitation）** | 主操作按钮填充 + 主色；次操作按钮描边或文字按钮；空态用 `Empty` 配引导按钮。 |
| 9 | **巧用过渡（Transition）** | 路由切换 `motion.base` 200ms；列表增删用 `motion.fast` 100ms；危险状态用 `motion.slow` 300ms 强调。 |
| 10 | **即时反应（Instant Response）** | 所有可点击元素必须 `:hover / :active / :focus-visible` 三态可见；触屏需 ripple。 |

---

## 3. 颜色 Token

### 3.1 12 主色板（Ant Design 色板）

antd 提供了 **12 个色系 × 10 阶 = 120 色**的完整色板。我们从中选用：

| 色系 | 选用 | 用途 |
| --- | --- | --- |
| **blue** `blue-1..10` | ✅ | 主色（brand） |
| purple | ✅ | `secondary` |
| cyan | （保留，图表用） | `chart-series-3` |
| green | ✅ | `success` |
| gold | ✅ | `warning`（注意：gold 而非 orange——避免和 red 混淆） |
| red | ✅ | `danger` |
| orange / volcano / yellow / lime / magenta / geekblue | （保留，图表 / 标签可选用） | 由 antd 12 阶定义中预留 |

完整 blue 色阶（单一来源在 antd 文档 `@ant-design/colors`，本规范复用，不复制）：

```
blue-1  #E6F4FF     blue-6  #1677FF ← 主色
blue-2  #BAE0FF     blue-7  #0958D9
blue-3  #91CAFF     blue-8  #003EB3
blue-4  #69B1FF     blue-9  #002C8C
blue-5  #4096FF     blue-10 #001D66
```

### 3.2 语义色（Semantic Colors）

`success / warning / danger / primary` 四色跨主题保持语义稳定，**含义不变** ——"低电量就是低电量"，但在不同背景下取合适的"提亮度"，确保 WCAG 4.5:1 对比度。

| Token | Light | Dark | 何时使用 |
| --- | --- | --- | --- |
| `primary` | `#1677FF` (blue-6) | `#1677FF` | 主操作按钮、Tab 选中态、链接 |
| `secondary` | `#722ED1` (purple-6) | `#A77BFF` (purple-7) | 次主操作、辅助强调 |
| `success` | `#52C41A` (green-6) | `#49AA19` (green-6 暗色) | "已充电"成功态、已确认处理 |
| `warning` | `#FAAD14` (gold-6) | `#FFC53D` (gold-7 提亮) | 即将超期（`OverdueSeverity.Warning`） |
| `danger` | `#FF4D4F` (red-6) | `#E5484D` (red-7 提亮) | 严重超期、删除失败、连接失败 |

**为什么不直接用 `tertiary` 当 warning**：
Material3 ColorScheme 只有 `primary / secondary / tertiary / error` 4 个语义 slot，没有 `warning`。我们**不能用 `tertiary`（紫色 secondary）冒充 warning** —— 这是颜色滥用，会破坏"颜色仅传递语义"的原则。**正确做法**：通过 `LocalWarning.current` 等 CompositionLocal 提供（详见 §4.3 / §6.2）。

### 3.3 中性色（Neutral 13 阶）

antd 的中性色用 **半透明 black/white** 实现，确保在任意背景上对比度合规。这是为什么"中性色不要写死为某个灰阶"。

| Token | Light (rgba) | Dark (rgba) | 用途 |
| --- | --- | --- | --- |
| `text` | `rgba(0, 0, 0, 0.88)` | `rgba(255, 255, 255, 0.88)` | 主要文字 |
| `text-secondary` | `rgba(0, 0, 0, 0.65)` | `rgba(255, 255, 255, 0.65)` | 次要文字（卡片 meta 等） |
| `text-disabled` | `rgba(0, 0, 0, 0.45)` | `rgba(255, 255, 255, 0.30)` | 禁用 / 占位 |
| `text-placeholder` | `rgba(0, 0, 0, 0.25)` | `rgba(255, 255, 255, 0.25)` | 输入框 placeholder |
| `border` | `#D9D9D9` | `rgba(255, 255, 255, 0.12)` | 控件边框 |
| `border-strong` | `#BFBFBF` | `rgba(255, 255, 255, 0.20)` | 选中态边框 |
| `divider` | `rgba(5, 5, 5, 0.06)` | `rgba(255, 255, 255, 0.12)` | 列表分隔线 |
| `bg-layout` | `#F5F5F5` | `#000000` | 页面大背景（区别于容器的 surface） |
| `bg-container` | `#FFFFFF` | `#141414` | 卡片 / 容器背景 |
| `bg-elevated` | `#FFFFFF` | `#1F1F1F` | Dropdown / Popover 弹出层 |
| `bg-overlay` | `rgba(0, 0, 0, 0.45)` | `rgba(0, 0, 0, 0.65)` | Modal 蒙层 |
| `fill-tertiary` | `rgba(0, 0, 0, 0.04)` | `rgba(255, 255, 255, 0.08)` | 控件 disabled 背景 / 极弱背景 |
| `fill-quaternary` | `rgba(0, 0, 0, 0.02)` | `rgba(255, 255, 255, 0.04)` | hover 弱背景 |

### 3.4 弱化容器背景（*-soft 系列）

每个语义色配套一个 **soft 版本**，用作徽标底色 / Tint 区域，**不抢主色焦点**。

| Token | Light | Dark |
| --- | --- | --- |
| `primary-soft` | `rgba(22, 119, 255, 0.10)` | `#003A8C` |
| `secondary-soft` | `rgba(114, 46, 209, 0.10)` | `#2C1A4D` |
| `success-soft` | `rgba(82, 196, 26, 0.10)` | `#092B00` |
| `warning-soft` | `rgba(250, 173, 20, 0.10)` | `#3E2C00` |
| `danger-soft` | `rgba(255, 77, 79, 0.10)` | `#3D0F0F` |

### 3.5 图表系列（Chart Series）

历史趋势 / 对比图必须从 token 取色（feat.md §5.5 强制）：

| Token | Light = Dark | 说明 |
| --- | --- | --- |
| `chart-series-1` | `#1677FF` | 主对比 |
| `chart-series-2` | `#722ED1` | |
| `chart-series-3` | `#13C2C2` | |
| `chart-series-4` | `#52C41A` | |
| `chart-series-5` | `#FAAD14` | |
| `chart-series-6` | `#FF4D4F` | |

跨主题保持一致：图表代表的是数据本身而非 UI 状态，颜色应稳定。

---

## 4. 暗色模式

### 4.1 设计目标

> antd 暗色模式的文档原话：**"避免使用对比很强的色彩或内容，减少长时间使用的疲劳感" + "信息一致性：暗黑模式下信息内容需与浅色模式保持一致性，不打破原有层级关系"。**

暗色 **不是反色**：亮色到暗色不是逐像素 `invert()`，而是 **降低对比度** + **翻转层级方向**——surface 从"白上叠灰卡"变为"黑下叠亮卡"。

### 4.2 表面 5 层结构

| 层 | Light | Dark | 用途 |
| --- | --- | --- | --- |
| Layer 1 | `#F5F5F5` | `#000000` | 页面大背景 |
| Layer 2 | `#FFFFFF` | `#141414` | 卡片 / 容器 |
| Layer 3 | `#FFFFFF` | `#1F1F1F` | Dropdown / Tooltip / Popover |
| Layer 4 | `#FFFFFF` | `#303030` | Modal 蒙层后景 |
| Layer 5 | Modal | `#1F1F1F` Modal | 模态对话框本身 |

> 5 层只用于"层级叙事"，**不必每个组件都明确区分**；最常见用到的就是 Layer 1（页面）vs Layer 2（卡片）。

### 4.3 暗色阴影降级

深色背景上"投影"几乎不可见，反而显脏。

- **暗色下，阴影权重降低**（不是完全消失，是降一档）。
- **改用边框 + 微亮表面** 表达层次：`surface-elevated` 比 `surface` 略亮；`border` 用于分隔。
- 三档阴影映射：
  - Layer 0 → `none`
  - Layer 1（Card hover）→ `0 0 0 1px rgba(255,255,255,0.06)`（描边代替）
  - Layer 2（Dropdown）→ `0 0 0 1px rgba(255,255,255,0.08), 0 6px 16px rgba(0,0,0,0.48)`
  - Layer 3（Modal）→ `0 0 0 1px rgba(255,255,255,0.10), 0 9px 28px rgba(0,0,0,0.64)`

### 4.4 焦点环

- 亮色：`outline: 2px solid #4096FF; outline-offset: 2px;`
- 暗色：`outline: 2px solid #69B1FF; outline-offset: 2px;`
- **禁止纯黑 / 纯白焦点环**（feat.md §5.6）。

### 4.5 主题切换与持久化

- 用户选择持久化到 DataStore（key `theme.mode`），存的是字面 `"system" / "light" / "dark"`。
- 系统主题变化：通过 `Configuration` 监听 `uiMode` 变化，触发 recompose；仅当用户选择 `system` 时响应（feat.md §5.8）。
- 首屏闪烁：Android `Theme.Material3.DayNight.NoActionBar` 已在 `themes.xml` 配置，承接 OS 颜色作为 windowBackground，避免白闪。

---

## 5. 字体（Typography）

### 5.1 字体家族

```css
font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto,
  'Helvetica Neue', Arial, 'Noto Sans', sans-serif,
  'Apple Color Emoji', 'Segoe UI Emoji', 'Segoe UI Symbol',
  'Noto Color Emoji';
```

自动适配 macOS / Windows / Android / iOS / Linux 的系统默认字体。**不打包自定义字体文件**（v1 维持系统字体；后续若要做 Plus Jakarta Sans 走字体子集化）。

`code` / `mono` 走另一套（SFMono / Menlo / Consolas 优先）。

### 5.2 字号阶梯（仅 5 档）

| Token | FontSize (sp) | LineHeight | 用途 |
| --- | --- | --- | --- |
| `fontSizeSM` | 12 | 20 | 辅助 / 徽标 / caption |
| `fontSize` | 14 | 22 | 正文 / Button 文字 |
| `fontSizeLG` | 16 | 24 | 卡片标题 / ListItem headline |
| `fontSizeXL` | 20 | 28 | Tab 标题 / Section title |
| `fontSizeHeading2` | 24 | 32 | 页面 H2 |
| `fontSizeHeading1` | 30 | 38 | 页面 H1（暂未使用，预留） |

> **约束**：本项目只允许这 5 档字号。新页面如果觉得正文 14 太小、想用 15，**请用 14 + 字重 + 颜色对比，而不是加 1px 字号**。这是 antd 的"动态秩序"原则。

### 5.3 字重

| Token | 值 | 用途 |
| --- | --- | --- |
| `fontWeight` | 400 | 正文 |
| `fontWeightMedium` | 500 | Card 标题、Tab 选中态 |
| `fontWeightStrong` | 600 | 数字强调（H1 / Statistic value） |

### 5.4 数字与对齐

- 数字 / 表格里的电量百分比，使用 `font-variant-numeric: tabular-nums`，让数字等宽对齐。这对"周期设备清单 7 30 90 天"这种数字对比类表格尤其重要。
- Material3 Typeface 在 `Type.kt` 的 `bodyMedium` 中开启 `fontFeatureSettings = "tnum"`。

---

## 6. 间距 / 栅格 / 圆角 / 阴影

### 6.1 间距（Spacing Scale）

| Token | px | 典型用途 |
| --- | --- | --- |
| `spacing.xxs` | 4 | 文字垂直间距、Icon 与文字间隙 |
| `spacing.xs` | 8 | 卡片内部字段间距、按钮内 padding y |
| `spacing.sm` | 12 | 列表项内 padding y、Button sm size padding |
| `spacing.md` | 16 | 卡片外边距、页面内容 padding x、卡片间 gap |
| `spacing.lg` | 24 | 区域间距、Modal 内 padding |
| `spacing.xl` | 32 | 大区域分隔、Card headerHeight（medium） |
| `spacing.xxl` | 48 | 页头大间距 |

> **不要** 用 1/2/3 这种非 8 倍数。如果"看起来 8dp 太大"，用 12dp；如果"看起来太大"，用 24dp。**永远不要 6dp / 10dp / 14dp / 18dp 这样的中间值**。

### 6.2 栅格 / 断点（响应式，fe.md §4）

| Token | px | 用途 |
| --- | --- | --- |
| `xs` | 360 | 窄屏（最低支持） |
| `sm` | 576 | 横屏 7 寸 |
| `md` | 768 | 平板竖屏 |
| `lg` | 992 | 平板横屏 |
| `xl` | 1200 | 桌面 |
| `xxl` | 1600 | 大屏 |

容器最大宽度：`xxl` 时 1200px；不强制居中，靠 Left / Center 对齐。

### 6.3 圆角

| Token | px | 典型组件 |
| --- | --- | --- |
| `rounded.xs` | 4 | Tag / 极小徽标 |
| `rounded.sm` | 6 | Button（默认）/ Input |
| `rounded.md` | 8 | Card / Modal / Drawer |
| `rounded.lg` | 16 | 主面板圆角（首次启动骨架） |

> **不要**在同一个项目里同时用 `4 / 8 / 12 / 16` 圆角阶 —— 选 2~3 个标志性档位即可。本项目保留 4 / 6 / 8 / 16 共 4 档，分别对应"微型 / 控件 / 容器 / 大面板"四个粒度。

### 6.4 阴影

详见 §3.5 三档定义。落地：

| Token | 适用场景 |
| --- | --- |
| `shadow.none` | Layer 0：Input、表格行 |
| `shadow.layer-1` | Card 默认 / 按钮按下 |
| `shadow.layer-2` | Dropdown / Popover / Tooltip |
| `shadow.layer-3` | Modal / Drawer / Notification |

**不要在组件内手算 `elevation * dp` 转 `shadow` 参数** —— 直接 `Modifier.shadow(LocalShadow.current.layer1)` 或包在 `Card(elevation = CardDefaults.cardElevation(defaultElevation = ...))` 里。

---

## 7. 动效（Motion）

antd 的动效原则是 **Natural / Performant / Concise**，落地到代码：

### 7.1 时长（Duration）

| Token | s | 用途 |
| --- | --- | --- |
| `motion.fast` | 0.1 | 颜色 / 透明度切换、按钮按下反馈 |
| `motion.base` | 0.2 | 列表展开收起、Modal 出现 |
| `motion.slow` | 0.3 | 大区域过渡、Drawer 滑动 |

### 7.2 缓动（Easing）

| Token | cubic-bezier | 用途 |
| --- | --- | --- |
| `motion.easing-standard` | `(0.4, 0, 0.2, 1)` | 默认进入离开 |
| `motion.easing-decelerate` | `(0, 0, 0.2, 1)` | 进入（淡入） |
| `motion.easing-accelerate` | `(0.4, 0, 1, 1)` | 离开（淡出） |

> 列表删除用 `accelerate`（快），新增用 `decelerate`（慢），符合"消失比出现更快、整体协调"原则。

### 7.3 设计意图

| 原则 | 落地 |
| --- | --- |
| **Natural** | 物理隐喻：按钮按下"沉"、松开"弹"、列表项删除"飘出" |
| **Performant** | transition 最小化；无 stagger；所有项同步消失 |
| **Concise** | 不做戏剧动画；箭头方向变化"够用即可" |

### 7.4 减少动效

尊重用户 `prefers-reduced-motion`：开启时所有时长缩短到 0，并关闭非必要 transform。

---

## 8. 组件规范

> 以下规范不是"列出 antd 组件的属性表"，而是 **本项目用到 antd 哪些组件 / 怎么落地**。

### 8.1 Button

```kotlin
Button(
    onClick = ...,
    modifier = Modifier.fillMaxWidth(),
    colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
    ),
) { Text(stringResource(R.string.action_save)) }
```

**本项目的按钮语义**：

| variant | 用途 | 落地 |
| --- | --- | --- |
| `primary` | 主操作（保存 / 新增 / 提交） | `Button` 填充主色 |
| `default` | 次操作（取消 / 返回） | `OutlinedButton` 描边 |
| `danger` | 危险操作（删除 / 清空） | `Button(colors = ... containerColor = LocalDanger.current)` |
| `text` | 文字按钮（链接类） | `TextButton` |
| `tonal` | 中等强调 | `FilledTonalButton`（容器背景 `secondaryContainer`） |

**尺寸**：

- 大（`large`）：40dp 高，水平 padding 15dp，字号 16 — 仅用于首屏骨架、CTA 大按钮
- 中（默认）：32dp 高，水平 padding 15dp，字号 14 — **多数场景**
- 小：`small`：24dp 高，水平 padding 7dp，字号 14 — 卡片内紧凑按钮

**圆角**：6dp（`rounded.sm`）。

**禁止**：
- ❌ 在卡片内部塞 3 个以上的按钮（超过 3 个收纳到 Dropdown）
- ❌ 自定义 hex 容器色
- ❌ 永远不要把 `disabled` 态用灰色按钮 + 白文字（对比度不够）；改用 `fill-tertiary` 背景 + 45% alpha 文字

### 8.2 Card

```kotlin
Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(8.dp),                          // rounded.md
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface,    // 卡片底色
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
) {
    Column(modifier = Modifier.padding(16.dp), ...)            // spacing.md
}
```

**两种风格**：

| 用途 | 容器 | elevation | border |
| --- | --- | --- | --- |
| 在 Layer 1（bg-layout）页面上的卡片 | `surface` 容器 | 0（依靠 border） | `border` token |
| 在 Layer 1 页面上的"凸起卡片" | `surface` | `layer-1` shadow | 无 border |
| 在 Layer 1 页面上的"悬停浮起卡片" | `surface` | hover → `layer-1` | 默认无 |

**内容 layout**：

- 内边距 16dp（`spacing.md`）；卡片间距 16dp（同）
- 标题字号 `fontSizeLG` 16 / Medium 500；描述用 `fontSize` 14 / text-secondary
- 操作区（`actions`）：底部横排 + `actionsLiMargin: 12px 0`，每项纵向间距 12dp

### 8.3 List / 列表项

**周期设备列表 / 设置项 / HA 设备列表** 用 List 风格，统一 padding `12px 0`（`spacing.sm`）。

ListItem 内部结构：

```
[Avatar/Icon 32dp]  [Title   fontSizeLG]
                    [Caption fontSize / text-secondary]
```

- Avatar / Icon：固定 32dp 容器
- Meta 之间：margin-right 16dp（`spacing.md`）
- Title 与 caption：margin-bottom 12dp（`spacing.sm`）
- split：默认 `1px rgba(5,5,5,0.06)` 分隔线

### 8.4 Tag / 状态徽标

本项目里，徽标不是装饰，是 **状态信号**（feat.md §5.9："颜色 + 图标 + 文字三重冗余"）。

```kotlin
// 已超期（Warning）
Surface(
    shape = RoundedCornerShape(4.dp),                                // rounded.xs
    color = LocalWarning.current.copy(alpha = 0.10f),                // warning-soft
) {
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)) {
        Icon(Icons.Outlined.WarningAmber, contentDescription = null,
             modifier = Modifier.size(12.dp), tint = LocalWarning.current)
        Spacer(Modifier.width(4.dp))
        Text(stringResource(R.string.cycle_badge_warning),
             style = MaterialTheme.typography.labelSmall,
             color = LocalWarning.current)
    }
}
```

**5 种内置 status**（必须只用这 5 种，多了会乱）：

| status | 颜色 token | 弱化背景 | 图标 |
| --- | --- | --- | --- |
| `success` | `LocalSuccess` | `success-soft` | `CheckCircle` |
| `processing` | `LocalPrimary` | `primary-soft` | `Sync` |
| `error` | `LocalDanger` | `danger-soft` | `Cancel` |
| `warning` | `LocalWarning` | `warning-soft` | `WarningAmber` |
| `default` | `text-secondary` | `fill-tertiary` | `FiberManualRecord` |

### 8.5 Empty / 空态

**永远不要让用户看到"什么都没有"的死页面**。空态必须：

1. **乐观引导**：文案说"准备好后就开始"，而不是"暂无数据"。
2. **明确操作**：必有一个主操作按钮（`Button type="primary"`）直达添加路径。
3. **视觉留白**：垂直 padding 至少 48dp（`spacing.xxl`），让人呼吸。

```kotlin
Empty(
    image = EmptyDefaults.IMAGE_MODERN,
    description = stringResource(R.string.home_empty_cycle),
    modifier = Modifier.padding(48.dp),
) {
    FilledTonalButton(onClick = onAdd) {
        Text(stringResource(R.string.settings_cycle_devices))
    }
}
```

### 8.6 Tabs

| Token | 值 | 用途 |
| --- | --- | --- |
| 高度 | 46dp | 默认 |
| indicator | 2dp 高，圆角 `2px 2px 0 0` | 选中态下划线（Material3 `PrimaryTabRow`） |
| 内边距 | `12px 16px` | Tab 文字外 padding |

Tab 数量 **不超过 5 个**；超过时改用 Segmented 或新页面。

### 8.7 Form Fields / 输入

| 控件 | 高度 | 圆角 | 内边距 |
| --- | --- | --- | --- |
| `OutlinedTextField` 默认 | 32dp | 6dp | `4 11 4 11` |
| 大尺寸 | 40dp | 6dp | — |
| 小尺寸 | 24dp | 6dp | — |

错误态：边框变 `error`/`danger` 色（不光是红色，**配合图标 + supportingText**）。

### 8.8 Radio / 单选

主题选择（`AppearanceScreen`）就是典型的"3 选 1"场景。用 `RadioGroup` 而不是 `Tab` / `Segmented`：

- `Segmented`：控件形态紧凑、强调切换感（适用于"日 / 周 / 月"视图切换）
- `Radio`：每个选项都需要文字说明（适用于主题模式这种带说明的选项）
- **本项目主题选择用 Radio**，且每个 Radio 下方加 12dp 行高的小字说明当前模式的效果。

### 8.9 Divider

```kotlin
HorizontalDivider(
    thickness = 1.dp,
    color = MaterialTheme.colorScheme.outlineVariant,  // divider token
)
```

不要用 `Surface` 自己堆"假装分隔线"，直接用 Divider。

### 8.10 Skeleton / Loading

骨架屏用 `placeholder` 闪烁动画，避免布局抖动：

```kotlin
val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
val alpha by infiniteTransition.animateFloat(
    initialValue = 0.6f, targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
        animation = tween(1000, easing = LinearEasing),
        repeatMode = RepeatMode.Reverse,
    ),
    label = "alpha",
)
```

`reducedMotion` 时降级为恒定 alpha 0.85。

### 8.11 Drawer / Modal / Dialog

- **Modal**：宽 ≤ 520dp；标题 + 内容 + footer；footer 默认 `[取消] [确认]`，确认按钮按语义选 `primary` / `danger`
- **Drawer**：右侧滑出，宽 378dp（移动端友好）
- **Popconfirm**：轻量确认，浮在按钮旁边

### 8.12 Status 颜色（含 Loading / Error / Empty / Unauthorized）

四种基础状态（feat.md §3）：

| 状态 | 视觉元素 | 含义 |
| --- | --- | --- |
| Empty | `Empty` + 主操作按钮 | "尚未配置"引导添加 |
| Loading | `Skeleton` 1s 闪动 | 数据获取中 |
| Error | 红 Icon + 错误文字 + 重试按钮 | 数据获取失败 |
| Unauthorized | 灰 Icon + 跳设置按钮 | Token 失效 |

---

## 9. 可访问性

### 9.1 对比度

- **正文**：`text` on `surface` / `bg-container` ≥ **4.5:1**（WCAG 2.2 AA）
- **大字**（≥ 18px 或 14px bold）：≥ **3:1**
- **图标**（仅作辅助信息载体）：≥ **3:1**
- **状态色**（warning / danger / success）背景上的文字：≥ **4.5:1**。这意味着亮色下 `danger #FF4D4F` 上不能放白色文字 —— 因为 #FF4D4F 不够饱和，红+白的对比仅 ~3.5:1。应改用 `danger` token + 文字加粗到 600，或背景用 `danger-soft`（10% alpha）+ 主色字。

### 9.2 焦点可见

- 所有交互元素必须 `:focus-visible` 时显示焦点环
- 焦点环色见 §4.4
- **禁止** 用 `outline: none` 一笔抹掉

### 9.3 状态指示的多重冗余

仅靠颜色区分状态（如"电池低"红 vs "电池正常"绿）对色盲用户不友好。**必须** 颜色 + 图标 + 文字三重：

- ✅ "已超期"（红色徽标 + WarningAmber 图标 + 文字"已超期"）
- ✅ "严重超期"（深红徽标 + WarningAmber 图标 + 文字"严重超期"）

### 9.4 触控目标

- 最小可点击区 **48 × 48 dp**（Android Material 规范）
- 视觉按钮可以更小（视觉 32dp），但 hit area 通过 `Modifier.minimumInteractiveComponentSize()` 撑到 48dp

### 9.5 键盘可达

- Tab 顺序遵循阅读顺序
- 所有按钮支持 Space / Enter 触发
- 表单字段在按下 Enter 时提交

### 9.6 系统通知

- 系统通知颜色由 OS 决定，应用层不强制覆盖
- 但通知必须包含 **app name + icon + 文字**，不能只靠颜色

---

## 10. 实施约束（不可协商）

### 10.1 单一来源

- **所有** hex 值定义在 `ui/theme/Color.kt` 一个文件，前端是 `prompts/ui.md` 这个文档
- 任何 `Color(0xFF...)` 出现在 Composable 文件都是违规
- 例外：`Color.kt` 内部、preview / mock 文件

### 10.2 组件不接触 token 名字以外的资源

- ❌ `Color(0xFF1677FF)`
- ✅ `LocalPrimary.current` 或 `MaterialTheme.colorScheme.primary`
- ✅ 通过 `LocalDanger.current` / `LocalWarning.current` / `LocalSuccess.current`（CompositionLocal 暴露 §3.2 语义色，因 Material3 ColorScheme 没有 warning / success slot）

### 10.3 字号阶

- 一屏最多 5 档字号。请审稿时人工核查。

### 10.4 主题切换响应

- `themeMode == system` 时响应 `Configuration.uiMode` 变化
- `themeMode ∈ {light, dark}` 时 **不** 响应 OS 主题变化
- DataStore 持久化存的是字面 `"system"`，不是解析后的 `"dark"`

### 10.5 i18n

- 所有面向用户的文案通过 `R.string.*` 引用，禁止 Composable 内写死中文
- v1 默认 `values/strings.xml`（中文），后续 `values-en/strings.xml`（英文）

### 10.6 PR Review Checklist

每条 PR 在改动 UI 时应当能回答这些问题：

- [ ] 我引用的所有颜色 / 字号 / 间距是否都是 token？
- [ ] 我的字号阶没有超出 5 档？
- [ ] 我的阴影是否只用了三档 token 之一？
- [ ] 我的交互是否在亮 / 暗双主题下都满足对比度？
- [ ] 焦点环是否在两个主题下都可见？
- [ ] 状态指示是否多重冗余（颜色 + 图标 + 文字）？
- [ ] 最小触控目标是否 ≥ 48dp？
- [ ] 空态 / 加载 / 错误 三态是否都覆盖？
- [ ] 是否有任何"装饰性"颜色或动效？这些装饰是否携带了语义？如果没有，请删除。

违反任意一条 = 必需修改才能合并。
