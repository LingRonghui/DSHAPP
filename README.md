# DSHAPP - DeepSeek Harness Android App

高度原生的安卓客户端，基于 Kotlin + Jetpack Compose + Material Design 3。

## 功能特性

- 完全原生开发，无任何 WebView / 浏览器嵌入
- MVVM 架构：Hilt DI + Retrofit + Room + DataStore
- 会话管理（工作区、会话树、新建/搜索）
- 消息流（用户消息、助手回复、工具调用卡片、富文本Markdown）
- 9 Tab 会话面板（对话/轨迹/记忆/技能/待办/记忆同步/画板/模型设置/Memory Evolve）
- 插件市场（推荐/搜索/整合包/收藏/已装、新手/个性化模式）
- 设置面板（通用/模型/插件/Agent预设/侧边卡片 5个分类）
- 前台服务：支持 Agent 后台任务持续运行
- Material 3 主题：浅色/深色/跟随系统

## 在线构建

推送即触发 GitHub Actions 自动构建：
- Debug APK（always）
- Release APK（配置 keystore Secrets 后启用）

构建页面：https://github.com/LingRonghui/DSHAPP/actions