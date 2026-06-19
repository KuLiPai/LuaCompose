package com.kulipai.luacompose

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.kulipai.luacompose.TestPointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kulipai.luacompose.compose.ui.LuaModifier
import com.kulipai.luacompose.compose.runtime.ComposeScope
import com.kulipai.luacompose.compose.ui.graphics.ComposeScopeComponent
import com.kulipai.luacompose.ui.theme.LuaComposeTheme
import com.kulipai.luacompose.adapter.LuajEngine
import com.kulipai.luacompose.compose.LuaComposeLib
import com.kulipai.luacompose.compose.runtime.ComposeBridge
import org.luaj.Globals
import org.luaj.LuaFunction
import org.luaj.LuaValue
import org.luaj.lib.ZeroArgFunction
import org.luaj.lib.jse.CoerceJavaToLua
import org.luaj.lib.jse.JsePlatform
import java.io.File
import android.util.Log

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        setContent {
            LuaComposeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        LuaAppRunner(this@MainActivity)
                    }
                }
            }
        }
    }
}

@Composable
fun LuaAppRunner(context: Context) {
    var reloadTrigger by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var rootScope by remember { mutableStateOf<ComposeScope?>(null) }

    // 当 reloadTrigger 变化时，重新从 SD 卡加载 Lua 代码
    LaunchedEffect(reloadTrigger) {
        try {
    
            errorMessage = null
            rootScope = loadLuaScope(context)
        } catch (e: Exception) {
            e.printStackTrace()
            errorMessage = e.message ?: e.toString()
            rootScope = null
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 自定义顶部状态栏（无需使用实验性 TopAppBar 接口）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Lua Compose Live",
                style = MaterialTheme.typography.titleLarge
            )
            Button(
                onClick = { reloadTrigger++ }
            ) {
                Text("Reload")
            }
        }

        HorizontalDivider()

        // 渲染主布局
        Box(modifier = Modifier.fillMaxSize()) {
            if (errorMessage != null) {
                Text(
                    text = "加载错误:\n$errorMessage",
                    color = Color.Red,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                rootScope?.let {
                    key(rootScope) {
                        ComposeScopeComponent(it)
                    }
                } ?: Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

// 动态读取并加载 Lua 执行环境
fun loadLuaScope(context: Context): ComposeScope {
    try {
        // 1. 初始化 luaj++ 虚拟机环境
        val globals: Globals = JsePlatform.standardGlobals()

        // 2. 初始化 Kotlin 侧实现的 Compose DSL 库
        ComposeBridge.engine = LuajEngine
        ComposeBridge.luaValueUnwrapper = { value ->
            if (value is org.luaj.LuaValue) {
                ComposeBridge.scriptToJava(LuajEngine.wrap(value))
            } else value
        }
        val env = LuajEngine.wrap(globals).asTable()
        LuaComposeLib.inject(env)

        runCatching {
            val preflight = globals.load(
                """
                local compose = compose
                local foundation = compose.foundation
                local layout = foundation and foundation.layout
                local material3 = compose.material3
                return
                    layout ~= nil and layout.Column ~= nil,
                    foundation ~= nil and foundation.Canvas ~= nil,
                    material3 ~= nil and material3.Text ~= nil
                """.trimIndent(),
                "preflight.lua"
            ).invoke()
            Log.d(
                "LUA_PREFLIGHT",
                "Column=${preflight.arg1().toboolean()} Canvas=${preflight.arg(2).toboolean()} Text=${preflight.arg(3).toboolean()}"
            )
        }.onFailure {
            Log.e("LUA_PREFLIGHT", "preflight failed", it)
        }

        // 3. 注册 Modifier 全局对象
        globals.set("Modifier", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                return CoerceJavaToLua.coerce(LuaModifier())
            }
        })

        // 4. 定位外置存储路径 /sdcard/Android/data/<packagename>/files/
        val externalDir = context.getExternalFilesDir(null)
            ?: throw RuntimeException("外置存储不可用")
        if (!externalDir.exists()) {
            externalDir.mkdirs()
        }
        val mainLuaFile = File(externalDir, "main.lua")

        // 如果主测试文件不存在，自动写入一个功能完整的示例
        if (!mainLuaFile.exists()) {
            val sampleCode = """
            -- main.lua
            local compose = compose
            local Column = compose.foundation.layout.Column
            local Row = compose.foundation.layout.Row
            local Text = compose.material3.Text
            local Button = compose.material3.Button
            local Spacer = compose.foundation.layout.Spacer
            local TextField = compose.material3.TextField
            
            -- 直接使用 setContent 加载布局，无需在末尾使用 return
            compose.setContent(function()
              -- 创建响应式状态
              local count = compose.state(0)
              local textInput = compose.state("Hello AndroLua")
            
              Column {
                modifier = Modifier().fillMaxSize().padding(16),
                content = function()
                  
                  Text {
                    text = "欢迎使用 AndroLua Compose！",
                    color = "#6200EE",
                    modifier = Modifier().padding(8)
                  }
            
                  Spacer { modifier = Modifier().height(16) }
            
                  -- 文本框双向绑定示例
                  TextField {
                    value = textInput,
                    onValueChange = function(newVal)
                      textInput.value = newVal
                    end,
                    modifier = Modifier().fillMaxWidth()
                  }
            
                  Spacer { modifier = Modifier().height(8) }
            
                  Text {
                    text = "你输入的文本是: " .. tostring(textInput)
                  }
            
                  Spacer { modifier = Modifier().height(24) }
            
                  -- 计数器示例
                  Row {
                    modifier = Modifier().fillMaxWidth(),
                    content = function()
                      Text {
                        text = "计数器当前值: " .. tostring(count),
                        modifier = Modifier().padding(8)
                      }
            
                      Button {
                        onClick = function()
                          count.value = count.value + 1
                        end,
                        content = function()
                          Text { text = "加 1" }
                        end
                      }
                    end
                  }
                  
                end
              }
            end)
            """.trimIndent()
            mainLuaFile.writeText(sampleCode)
        }

        // 5. 加载并运行主 Lua 脚本
        val scriptContent = mainLuaFile.readText()
        Log.d(
            "LUA_SCRIPT",
            "Loading ${mainLuaFile.absolutePath} bytes=${scriptContent.length} firstLine=${scriptContent.lineSequence().firstOrNull()}"
        )
        val userScriptResult = globals.load(scriptContent, "main.lua").call()

        // 6. 优先从 compose.rootContentFunc 读取布局函数，其次从脚本返回值中读取
        val rootLuaFunction = LuaComposeLib.rootContentFunc
            ?: (LuajEngine.wrap(userScriptResult).takeIf { it.isFunction() }?.asFunction())
            ?: throw RuntimeException("请使用 compose.setContent(function) 设置布局，或者在 main.lua 结尾返回布局函数")

        LuaComposeLib.clearRuntimeState()

        // 7. 创建 Kotlin 侧的 ComposeScope
        return ComposeScope(rootLuaFunction)
    } catch (e: Exception) {
        LuaComposeLib.clearRuntimeState()
        throw e
    }
}
