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

- **`app/src/main/java/com/kulipai/luacompose`**
  - **`LuaComposeRegistry.kt`**：Compose 组件的注册与映射表。
  - **`LuaEngine.kt`**：虚拟节点解析与渲染引擎核心。
  - **`MainActivity.kt`**：加载 Lua 脚本并承载渲染的主活动。
- **`app/src/main/java/com/nekolaska/ktx`**：对 Lua 对象的 Kotlin 语法糖与拓展方法。
- **`app/src/main/java/org/luaj/lib/jse` & `dx`**：底层的 Java-Lua 反射桥接适配和动态生成组件。
