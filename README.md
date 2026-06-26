# luaCompose 📱

> **🚨 注意：本项目目前处于 `1.0.0-experimental` 阶段，属于不稳定的实验性、开发中版本。API 接口与功能实现可能会有较大变动。**

`luaCompose` 是一个将 **Lua 脚本** 与 **Android Jetpack Compose** 结合的动态化 UI 框架。它允许开发者使用 Lua 脚本动态定义、渲染和更新原生的 Compose 组件，旨在探索 Android 端基于 Lua 语言的动态化 UI 布局与热重载。

---

## ✨ 核心特性

- 🔗 **Compose 与 Lua 绑定**：支持 `Text`, `Column`, `Row`, `Box`, `Button` 等核心 Jetpack Compose 组件的动态渲染与控制。
- 🔄 **热重载支持**：可在运行时重新读取并执行外部 Lua 脚本，实现无缝的 UI 界面热更新。
- 📦 **Kotlin 友好**：为 Lua 核心对象（如 `LuaValue`）提供 Kotlin 拓展函数，简化了数据类型转换与异步调用。

---

## 🛠️ 项目结构

- **`annotations/`**：存放专门用于 KSP 扫描的自定义注解（例如 `@LuaBridgePackage`、`@LuaBridgeLocals`）。
- **`compiler/`**：基于 KSP 构建的符号处理器，负责在编译期自动扫描分析 Compose 源码并生成 Lua 到 Compose 的静态桥接插件代码，替代运行时反射提升性能。
- **`compose/`**：Lua 桥接核心层模块，包含 `ComposeBridge`、`LuaComposeRegistry` 以及底层的 Java-Lua 对象包裹器（Wrapper）与全局工具函数（例如 `dump`）。
- **`app/`**：宿主应用层，包含启动和执行 Lua 脚本的入口（`MainActivity.kt`）。
