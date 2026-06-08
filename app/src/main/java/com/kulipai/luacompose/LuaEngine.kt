package com.kulipai.luacompose

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.luaj.Globals
import org.luaj.LuaFunction
import org.luaj.LuaValue
import org.luaj.lib.ZeroArgFunction
import org.luaj.lib.jse.CoerceJavaToLua
import org.luaj.lib.jse.JsePlatform
import java.io.File
import java.util.Stack

// --- 1. 递归虚拟节点定义 ---
data class LuaNode(
    val type: String,
    val props: Map<String, Any?>,
    val children: List<LuaNode>
)

// --- 2. 桥接与作用域管理 ---
object LuaBridge {
    private val activeScopes = ThreadLocal.withInitial { Stack<LuaScope>() }

    fun getActiveScope(): LuaScope? {
        val stack = activeScopes.get()!!
        return if (stack.isNotEmpty()) stack.peek() else null
    }

    fun pushActiveScope(scope: LuaScope) {
        activeScopes.get()!!.push(scope)
    }

    fun popActiveScope() {
        val stack = activeScopes.get()!!
        if (stack.isNotEmpty()) stack.pop()
    }

    // 将 Lua Value 转换成 Java/Kotlin 类型
    fun luaValueToJava(value: LuaValue): Any? {
        return when {
            value.isnil() -> null
            value.isboolean() -> value.toboolean()
            value.isint() -> value.toint()
            value.islong() -> value.tolong()
            value.isnumber() -> value.todouble()
            value.isstring() -> value.tojstring()
            value.isuserdata() -> value.touserdata()
            value.isfunction() -> value // 保留函数对象，在 onClick 等事件中回调
            value.istable() -> {
                // 如果是 State 对象
                if (value.get("_isState").toboolean()) {
                    value.get("javaState").touserdata()
                } else {
                    val len = value.len().toint()
                    if (len > 0) {
                        val list = mutableListOf<Any?>()
                        for (i in 1..len) {
                            list.add(luaValueToJava(value.get(i)))
                        }
                        list
                    } else {
                        luaTableToMap(value)
                    }
                }
            }
            else -> value
        }
    }

    fun luaTableToMap(table: LuaValue): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        if (!table.istable()) return map
        val luaTable = table.checktable()
        var key = LuaValue.NIL
        while (true) {
            val varargs = luaTable.next(key)
            key = varargs.arg1()
            if (key.isnil()) break
            val value = varargs.arg(2)
            map[key.tojstring()] = luaValueToJava(value)
        }
        return map
    }

    // 递归解析子节点列表
    fun parseLuaNodes(resultTable: LuaValue): List<LuaNode> {
        val list = mutableListOf<LuaNode>()
        if (!resultTable.istable()) return list
        val len = resultTable.len().toint()
        for (i in 1..len) {
            val nodeVal = resultTable.get(i)
            if (nodeVal.istable()) {
                val type = nodeVal.get("type").tojstring()
                val propsVal = nodeVal.get("props")
                val props = luaTableToMap(propsVal)
                val childrenVal = nodeVal.get("children")
                val children = parseLuaNodes(childrenVal)
                list.add(LuaNode(type, props, children))
            }
        }
        return list
    }
}

class LuaScope(val contentFunc: LuaFunction) {
    private val _recomposeVersion = mutableStateOf(0)
    val recomposeVersion: State<Int> = _recomposeVersion

    private var luaScopeObj: LuaValue? = null

    fun setLuaScopeObj(obj: LuaValue) {
        this.luaScopeObj = obj
    }

    fun invalidate() {
        _recomposeVersion.value++
    }

    fun execute(): List<LuaNode> {
        val luaScope = luaScopeObj ?: return emptyList()
        LuaBridge.pushActiveScope(this)
        return try {
            val resultTable = luaScope.get("run").call(luaScope)
            LuaBridge.parseLuaNodes(resultTable)
        } catch (e: Exception) {
            e.printStackTrace()
            // 如果执行出错，生成一个错误信息的 Node 树
            listOf(LuaNode("Text", mapOf("text" to "Lua Error: ${e.message}", "color" to "#ff0000"), emptyList()))
        } finally {
            LuaBridge.popActiveScope()
        }
    }
}

class LuaState(initialValue: Any?, val scope: LuaScope) {
    val composeState = mutableStateOf(initialValue)
    private val dependentScopes = mutableSetOf<LuaScope>()

    fun registerDependency(scope: LuaScope) {
        dependentScopes.add(scope)
    }

    fun get(): Any? {
        // 在 Compose 重新渲染获取属性值时，自动收集依赖
        val active = LuaBridge.getActiveScope()
        if (active != null) {
            dependentScopes.add(active)
        }
        return composeState.value
    }

    fun set(newValue: Any?) {
        if (composeState.value != newValue) {
            composeState.value = newValue
            // 局部刷新：仅触发依赖此状态的作用域重新执行 Lua
            for (scope in dependentScopes) {
                scope.invalidate()
            }
        }
    }
}

// --- 3. 极其优雅的链式 Modifier 封装 (覆盖几乎所有核心 Modifier 方法) ---
class LuaModifier(var modifier: Modifier = Modifier) {
    var alignmentStr: String? = null
    var weightVal: Float? = null

    // 基础内边距
    fun padding(dp: Int): LuaModifier {
        modifier = modifier.padding(dp.dp)
        return this
    }

    fun padding(horizontal: Int, vertical: Int): LuaModifier {
        modifier = modifier.padding(horizontal.dp, vertical.dp)
        return this
    }

    fun padding(start: Int, top: Int, end: Int, bottom: Int): LuaModifier {
        modifier = modifier.padding(start.dp, top.dp, end.dp, bottom.dp)
        return this
    }

    // 填充与尺寸
    fun fillMaxSize(): LuaModifier {
        modifier = modifier.fillMaxSize()
        return this
    }

    fun fillMaxWidth(): LuaModifier {
        modifier = modifier.fillMaxWidth()
        return this
    }

    fun fillMaxHeight(): LuaModifier {
        modifier = modifier.fillMaxHeight()
        return this
    }

    fun size(size: Int): LuaModifier {
        modifier = modifier.size(size.dp)
        return this
    }

    fun size(width: Int, height: Int): LuaModifier {
        modifier = modifier.size(width.dp, height.dp)
        return this
    }

    fun width(width: Int): LuaModifier {
        modifier = modifier.width(width.dp)
        return this
    }

    fun height(height: Int): LuaModifier {
        modifier = modifier.height(height.dp)
        return this
    }

    fun wrapContentSize(): LuaModifier {
        modifier = modifier.wrapContentSize()
        return this
    }

    // 背景与外观颜色
    fun background(colorHex: String): LuaModifier {
        try {
            val color = Color(android.graphics.Color.parseColor(colorHex))
            modifier = modifier.background(color)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return this
    }

    fun alpha(alpha: Float): LuaModifier {
        modifier = modifier.alpha(alpha)
        return this
    }

    // 宽高比与偏移量
    fun aspectRatio(ratio: Float): LuaModifier {
        modifier = modifier.aspectRatio(ratio)
        return this
    }

    fun offset(x: Int, y: Int): LuaModifier {
        modifier = modifier.offset(x.dp, y.dp)
        return this
    }

    // 点击事件修饰符
    fun clickable(onClick: LuaFunction): LuaModifier {
        modifier = modifier.clickable {
            try {
                onClick.call()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return this
    }

    // 裁剪形状 (支持圆角和圆形)
    fun clip(shape: String, radius: Int): LuaModifier {
        val clipShape = when (shape.lowercase()) {
            "circle" -> CircleShape
            "rounded" -> RoundedCornerShape(radius.dp)
            else -> null
        }
        if (clipShape != null) {
            modifier = modifier.clip(clipShape)
        }
        return this
    }

    fun clip(shape: String): LuaModifier = clip(shape, 0)

    // 边框修饰符
    fun border(width: Int, colorHex: String): LuaModifier {
        try {
            val color = Color(android.graphics.Color.parseColor(colorHex))
            modifier = modifier.border(width.dp, color)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return this
    }

    // 容器专属：子项对齐与权重 (延迟解析应用)
    fun align(alignStr: String): LuaModifier {
        this.alignmentStr = alignStr
        return this
    }

    fun weight(weight: Float): LuaModifier {
        this.weightVal = weight
        return this
    }
}

// 解析 Modifier 属性
fun resolveModifier(prop: Any?): Modifier {
    return when (prop) {
        is LuaModifier -> prop.modifier
        is Modifier -> prop
        else -> Modifier
    }
}

// 解析颜色属性
fun resolveColor(colorProp: Any?, defaultColor: Color = Color.Unspecified): Color {
    if (colorProp == null) return defaultColor
    val colorStr = colorProp.toString()
    return try {
        Color(android.graphics.Color.parseColor(colorStr))
    } catch (e: Exception) {
        defaultColor
    }
}

// --- 4. Compose 核心入口渲染器 ---
@Composable
fun LuaScopeComponent(scope: LuaScope) {
    val version by scope.recomposeVersion

    val nodes = remember(version) {
        scope.execute()
    }

    for (node in nodes) {
        LuaNodeRenderer(node)
    }
}

@Composable
fun LuaNodeRenderer(node: LuaNode) {
    // 统一通过全局注册表进行多态分发
    val renderer = LuaComposeRegistry.components[node.type]
    if (renderer != null) {
        renderer(node.props, node.children)
    } else {
        // 如果未注册，显示明显的错误提示
        Text("组件 [${node.type}] 未在注册表注册", color = Color.Red, modifier = Modifier.padding(8.dp))
    }
}
