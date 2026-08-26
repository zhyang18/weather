# 实时天气 (Weather App)

一款基于 **Kotlin + Jetpack Compose (Material 3)** 打造的现代、轻量、沉浸式 Android 天气实况与预报应用。对接中国气象局国家气象中心（CMA/中央气象台）官方权威气象数据源，原生支持多数据源扩展架构、多城市手势滑屏、动态拟真天空渲染与精细化气象指标分析。

---

## 🌟 核心特性

### 1. 沉浸式动态天气天空
- **拟真天空渲染**：根据当前天气现象（晴、多云、阴、雨、雪、雾、霾等）与昼夜时段自动计算天色渐变、太阳高光、云层飘动与星月夜空。
- **真实雨雪粒子系统**：基于 Canvas 极速绘制拟真雨滴连线、雪花飘扬与雷暴闪电光效。
- **景深镜头动效**：滑动切城或数据刷新时提供平滑的景深镜头推远与渐变过渡，视觉流畅自然。

### 2. 权威多源气象数据架构
- **中央气象台 (CMA)**：直连官方权威国家级气象观测网，提供高准确度气象实况、逐小时预报与 7 天中期天气预报。
- **可扩展多源调度**：内置天气数据源统一抽象层，支持按需热插拔切换气象服务提供商。
- **全真实数据保障**：严格过滤无效占位符（如 9999 / "-" / "无"），杜绝虚假数据，仅渲染真实存在的有效气象指标。

### 3. 多城市手势滑屏与便捷管理
- **灵敏滑屏切城**：居中城市名展示与下方紧凑排列圆点指示器，低阈值平滑手势切城。
- **全屏沉浸式城市管理**：支持向左滑动露出浅珊瑚粉色方块、点击切换为“✓”二次确认删除，并支持底部毛玻璃 Snackbar 一键“撤销恢复”。
- **全国 34 省市分级下钻与搜索**：支持拼音与中文关键字实时检索，以及全国省份、直辖市、下辖区县大视野分级选择。

### 4. 智能定位与颗粒度展示切换
- **双定位体系**：设备原生 GPS/基站网络定位 + 数据源 IP 兜底定位，保障定位高成功率。
- **定位颗粒度切换**：
  - **地标/乡镇/街道模式**（微观）：智能精简提取最后一级核心地标、实体园区、街道或乡镇名称（如“软件谷”、“紫峰大厦”）。
  - **附近区县模式**（宏观）：展示归属行政区县（如“雨花台区”、“雁塔区”）。

### 5. 深度气象指标与灾害预警
- **气象灾害预警**：有官方预警时自动高亮展示预警级别（蓝/黄/橙/红）与详细防范指引，无预警时不占位。
- **2小时分钟级降水走势**：实时降雨或即将降水时智能呈现分钟级降水量走势曲线。
- **24小时逐时预报与 7 天趋势图**：支持折线走势图模式与每日温差列表模式自由切换并持久化保存。
- **拟物化六联指标卡片**：
  - **空气质量 (AQI)**：动态彩虹谱条与健康出行建议；
  - **体感温度**：受湿度与风速影响的实际温差分析；
  - **风向风速罗盘**：360° 贯穿式指针、实心白球箭尾、导向箭头与中心风级表盘；
  - **相对湿度**：舒适度与补水提示；
  - **大气压强**：270° 发光弧带刻度环与居中大字百帕表盘；
  - **实时降水**：降水级别与出行防雨建议。

### 6. 前后台协同自动刷新机制
- **前台轻量轮询**：根据用户设置（无、30分钟、1/2/6/12/24小时）在超时后自动静默更新数据，不阻塞界面操作。
- **后台省电调度**：基于 Android WorkManager 周期性调度，满足网络条件时在后台静默同步离线天气快照。
- **即时失效检查**：切换更新间隔或应用切回前台时，自动检查数据新鲜度并按需刷新。

### 7. 95% 深灰蓝磨砂视觉系统
- 全局弹出面板（城市选择、更新间隔、天气数据源、定位设置、右上角菜单）统一采用 **95% 不透明度磨砂深灰蓝底色**（`Color(0xF2182230)`），搭配半透明拖动手柄与高对比度文字，具备精致的高级视觉质感。

---

## 🛠️ 技术栈与架构设计

| 层次 / 领域 | 选型与技术说明 |
| :--- | :--- |
| **编程语言** | Kotlin 1.9+（协程 Coroutines、StateFlow、Flow） |
| **UI 框架** | Jetpack Compose (Material 3), Navigation, Foundation Pager |
| **架构模式** | MVVM (Model-View-ViewModel) + 单向数据流 (UDF) + Repository 模式 |
| **网络通信** | Retrofit 2 + OkHttp 3 + Gson 自定义宽容类型适配器 |
| **后台任务** | Android Jetpack WorkManager (CoroutineWorker 定时任务) |
| **定位与逆地理** | Android LocationManager, GPS/Network Provider, Android Geocoder |
| **持久化存储** | SharedPreferences 离线天气快照与用户偏好持久化 |
| **构建工具** | Gradle (Kotlin DSL), Android Gradle Plugin 8.2+ |

---

## 📁 目录结构

```text
weather/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/weather/app/
│   │   │   │   ├── datasource/          # 天气数据源统一接口与各提供商实现 (CMA 等)
│   │   │   │   ├── location/            # 原生 GPS/网络定位与逆地理编码解析
│   │   │   │   ├── model/               # 气象数据、城市实体与定位模式数据模型
│   │   │   │   ├── repository/          # 业务数据仓库 (缓存管理、多源调度、城市持久化)
│   │   │   │   ├── ui/                  # 界面层
│   │   │   │   │   ├── components/      # 天空背景、Hero天气、折线图、逐时预报、指标卡片等
│   │   │   │   │   ├── dialogs/         # 城市选择、更新间隔、数据源切换、设置菜单等弹窗
│   │   │   │   │   ├── theme/           # Compose 主题、配色与样式
│   │   │   │   │   ├── CityManagementScreen.kt   # 全屏城市管理界面
│   │   │   │   │   ├── LocationSettingsScreen.kt # 定位设置底部面板
│   │   │   │   │   └── WeatherScreen.kt          # 天气主界面 (Pager 容器与联动逻辑)
│   │   │   │   ├── viewmodel/           # WeatherViewModel 业务逻辑与 UI 状态驱动
│   │   │   │   ├── worker/              # WeatherAutoUpdateWorker 后台定时刷新调度
│   │   │   │   └── MainActivity.kt      # 应用入口 Activity
│   │   │   ├── res/                     # 图标、字串与矢量资源
│   │   │   └── AndroidManifest.xml      # 权限与应用配置
│   │   └── test/                        # 单元测试与数据源验证用例
│   ├── build.gradle.kts                 # 模块构建与依赖配置
│   └── proguard-rules.pro               # 混淆与反射保护规则
├── gradle/                              # Gradle Wrapper 配置
├── build.gradle.kts                     # 项目根构建脚本
├── settings.gradle.kts                  # 模块设置
└── README.md                            # 项目说明文档
```

---

## 🚀 快速开始与构建

### 1. 环境要求
- **Android Studio**：Hedgehog (2023.1.1) 或更高版本
- **JDK**：Java 17
- **Android SDK**：Min SDK 26 (Android 8.0)，Target SDK 34 (Android 14)

### 2. 编译与运行 Debug 版本
```bash
# 克隆项目仓库
git clone https://github.com/zhyang18/weather.git

# 编译 Debug APK
./gradlew assembleDebug

# 运行单元测试
./gradlew testDebugUnitTest
```

### 3. 签名与 Release 版本配置
项目已配置从根目录 `local.properties` 动态读取签名密钥（也可通过环境变量配置）：
```properties
# 在 local.properties 中配置（可选）
RELEASE_KEY_ALIAS=your_key_alias
RELEASE_KEY_PASSWORD=your_key_password
RELEASE_STORE_FILE=your_keystore_path.jks
RELEASE_STORE_PASSWORD=your_keystore_password
```
执行编译 Release APK：
```bash
./gradlew assembleRelease
```

---

## 📄 开源许可证
本项目遵循 [MIT License](LICENSE) 开源许可协议。
