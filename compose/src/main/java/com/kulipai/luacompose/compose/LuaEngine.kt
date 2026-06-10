package com.kulipai.luacompose.compose

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import org.luaj.LuaFunction
import org.luaj.LuaValue
import java.util.Stack

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.TwoWayConverter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.rememberCoroutineScope

// --- 1. 递归虚拟节点定义 ---
data class LuaNode(
    val type: String,
    val props: Map<String, Any?>,
    val childScope: LuaScope? = null
)

// --- 2. 桥接与作用域管理 ---
object LuaBridge {
    private val activeScopes = ThreadLocal.withInitial { Stack<LuaScope>() }
    private val activeNodeLists = ThreadLocal.withInitial { Stack<MutableList<LuaNode>>() }

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

    fun getActiveNodeList(): MutableList<LuaNode>? {
        val stack = activeNodeLists.get()!!
        return if (stack.isNotEmpty()) stack.peek() else null
    }

    fun pushActiveNodeList(list: MutableList<LuaNode>) {
        activeNodeLists.get()!!.push(list)
    }

    fun popActiveNodeList() {
        val stack = activeNodeLists.get()!!
        if (stack.isNotEmpty()) stack.pop()
    }

    fun luaValueToJava(value: LuaValue): Any? {
        return when {
            value.isnil() -> null
            value.isboolean() -> value.toboolean()
            value.islong() -> {
                val l = value.tolong()
                if (l.toInt().toLong() == l) l.toInt() else l
            }
            value.isint() -> value.toint()
            value.isnumber() -> value.todouble()
            value.isstring() -> value.tojstring()
            value.isuserdata() -> value.touserdata()
            value.isfunction() -> value
            value.istable() -> {
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

    fun javaToLuaValue(value: Any?): LuaValue {
        return when (value) {
            null -> LuaValue.NIL
            is Boolean -> LuaValue.valueOf(value)
            is Int -> LuaValue.valueOf(value)
            is Long -> LuaValue.valueOf(value.toDouble())
            is Double -> LuaValue.valueOf(value)
            is Float -> LuaValue.valueOf(value.toDouble())
            is String -> LuaValue.valueOf(value)
            is LuaValue -> value
            else -> org.luaj.lib.jse.CoerceJavaToLua.coerce(value)
        }
    }
}

class LuaStateTable(val javaState: LuaState) : org.luaj.LuaTable() {
    init {
        set("_isState", LuaValue.valueOf(true))
        set("javaState", org.luaj.lib.jse.CoerceJavaToLua.coerce(javaState))
        val meta = org.luaj.LuaTable()
        meta.set(LuaValue.TOSTRING, object : org.luaj.lib.OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                return LuaValue.valueOf(javaState.get().toString())
            }
        })
        setmetatable(meta)
    }

    override fun get(key: LuaValue): LuaValue {
        if (key.tojstring() == "value") {
            return LuaBridge.javaToLuaValue(javaState.get())
        }
        return super.get(key)
    }

    override fun set(key: LuaValue, value: LuaValue) {
        if (key.tojstring() == "value") {
            javaState.set(LuaBridge.luaValueToJava(value))
        } else {
            super.set(key, value)
        }
    }
}

class LuaScope(var contentFunc: LuaFunction) : org.luaj.LuaTable() {
    private val _recomposeVersion = mutableStateOf(0)
    val recomposeVersion: State<Int> = _recomposeVersion

    var coroutineScope: CoroutineScope? = null
    var context: Context? = null
    var density: Density? = null
    var configuration: Configuration? = null

    private val states = mutableMapOf<Any, LuaStateTable>()
    private var statesCount = 0

    private val remembers = mutableMapOf<Any, Any?>()
    private var remembersCount = 0

    private val childScopes = mutableMapOf<Any, LuaScope>()
    private var childScopesCount = 0

    private val accessedStates = mutableSetOf<Any>()
    private val accessedRemembers = mutableSetOf<Any>()
    private val accessedChildScopes = mutableSetOf<Any>()

    init {
        set("state", object : org.luaj.lib.TwoArgFunction() {
            override fun call(scopeObj: LuaValue, initialValue: LuaValue): LuaValue {
                val actualKey = statesCount++
                accessedStates.add(actualKey)
                if (states[actualKey] == null) {
                    val javaState = LuaState(LuaBridge.luaValueToJava(initialValue), this@LuaScope)
                    states[actualKey] = LuaStateTable(javaState)
                }
                return states[actualKey]!!
            }
        })

        set("remember", object : org.luaj.lib.TwoArgFunction() {
            override fun call(scopeObj: LuaValue, initFunc: LuaValue): LuaValue {
                val actualKey = remembersCount++
                accessedRemembers.add(actualKey)
                if (!remembers.containsKey(actualKey)) {
                    val initialValue = initFunc.checkfunction().call()
                    remembers[actualKey] = initialValue
                }
                return remembers[actualKey] as LuaValue
            }
        })

        set("derivedStateOf", object : org.luaj.lib.TwoArgFunction() {
            override fun call(scopeObj: LuaValue, computeFunc: LuaValue): LuaValue {
                val actualKey = statesCount++ 
                accessedStates.add(actualKey)
                if (states[actualKey] == null) {
                    val javaState = LuaDerivedState(computeFunc.checkfunction(), this@LuaScope)
                    states[actualKey] = LuaStateTable(javaState)
                }
                return states[actualKey]!!
            }
        })
    }

    fun getOrCreateChildScope(func: LuaFunction, key: Any? = null): LuaScope {
        val actualKey = key ?: childScopesCount++
        accessedChildScopes.add(actualKey)
        val scope = childScopes.getOrPut(actualKey) { LuaScope(func) }
        scope.contentFunc = func // 保持闭包最新
        return scope
    }

    fun invalidate() {
        _recomposeVersion.value++
    }

    fun execute(vararg args: LuaValue): List<LuaNode> {
        LuaBridge.pushActiveScope(this)
        val rootNodes = mutableListOf<LuaNode>()
        LuaBridge.pushActiveNodeList(rootNodes)
        
        statesCount = 0
        remembersCount = 0
        childScopesCount = 0
        accessedStates.clear()
        accessedRemembers.clear()
        accessedChildScopes.clear()
        
        try {
            contentFunc.invoke(LuaValue.varargsOf(args))
        } catch (e: Exception) {
            e.printStackTrace()
            rootNodes.add(
                LuaNode(
                    "Text",
                    mapOf("text" to "Lua Error: ${e.message}", "color" to "#ff0000"),
                    null
                )
            )
        } finally {
            LuaBridge.popActiveNodeList()
            LuaBridge.popActiveScope()
        }
        
        // 自动清理不再被访问的状态、记忆值和子作用域 (模拟 Compose 组内存管理)
        states.keys.retainAll(accessedStates)
        remembers.keys.retainAll(accessedRemembers)
        childScopes.keys.retainAll(accessedChildScopes)

        return rootNodes
    }
}

open class LuaState(initialValue: Any?, val scope: LuaScope) {
    open val composeState: State<Any?> = mutableStateOf(initialValue)
    protected val dependentScopes = mutableSetOf<LuaScope>()

    fun registerDependency(scope: LuaScope) {
        dependentScopes.add(scope)
    }

    open fun get(): Any? {
        val active = LuaBridge.getActiveScope()
        if (active != null) {
            dependentScopes.add(active)
        }
        return composeState.value
    }

    open fun set(newValue: Any?) {
        if (composeState is androidx.compose.runtime.MutableState<Any?>) {
            val ms = composeState as androidx.compose.runtime.MutableState<Any?>
            if (ms.value != newValue) {
                ms.value = newValue
                for (scope in dependentScopes) {
                    scope.invalidate()
                }
            }
        }
    }
}

class LuaDerivedState(val computeFunc: LuaFunction, scope: LuaScope) : LuaState(null, scope) {
    override val composeState = androidx.compose.runtime.derivedStateOf {
        LuaBridge.luaValueToJava(computeFunc.call())
    }

    override fun set(newValue: Any?) {
        // Read-only
    }
}

class LuaAnimatableState<T, V : AnimationVector>(
    initialValue: T,
    val typeConverter: TwoWayConverter<T, V>,
    scope: LuaScope
) : LuaState(initialValue, scope) {
    
    var currentSpec: androidx.compose.animation.core.AnimationSpec<T>? = null
    val animatable = Animatable(initialValue, typeConverter)
    private var job: Job? = null

    fun animateTo(target: T) {
        if (animatable.targetValue == target) return
        job?.cancel()
        job = scope.coroutineScope?.launch {
            val block: Animatable<T, V>.() -> Unit = {
                if (composeState is androidx.compose.runtime.MutableState<Any?>) {
                    val ms = composeState as androidx.compose.runtime.MutableState<Any?>
                    ms.value = this.value
                }
                for (dep in dependentScopes) {
                    dep.invalidate()
                }
            }
            if (currentSpec != null) {
                animatable.animateTo(target, animationSpec = currentSpec!!, block = block)
            } else {
                animatable.animateTo(target, block = block)
            }
        }
    }
    
    override fun set(newValue: Any?) {
        // Discard if wrong type, but here we expect correct type mapped from Lua
        @Suppress("UNCHECKED_CAST")
        try {
            animateTo(newValue as T)
        } catch (e: Exception) { e.printStackTrace() }
    }
}

// --- 3. 极其优雅的链式 Modifier 封装 ---
class LuaModifier(var modifier: Modifier = Modifier) {
    var alignmentStr: String? = null
    var weightVal: Float? = null

    fun padding(dp: Any): LuaModifier { modifier = modifier.padding(resolveDp(dp)); return this }
    fun padding(horizontal: Any, vertical: Any): LuaModifier { modifier = modifier.padding(resolveDp(horizontal), resolveDp(vertical)); return this }
    fun padding(start: Any, top: Any, end: Any, bottom: Any): LuaModifier { modifier = modifier.padding(resolveDp(start), resolveDp(top), resolveDp(end), resolveDp(bottom)); return this }
    fun fillMaxSize(): LuaModifier { modifier = modifier.fillMaxSize(); return this }
    fun fillMaxWidth(): LuaModifier { modifier = modifier.fillMaxWidth(); return this }
    fun fillMaxHeight(): LuaModifier { modifier = modifier.fillMaxHeight(); return this }
    fun size(size: Any): LuaModifier { modifier = modifier.size(resolveDp(size)); return this }
    fun size(width: Any, height: Any): LuaModifier { modifier = modifier.size(resolveDp(width), resolveDp(height)); return this }
    fun width(width: Any): LuaModifier { modifier = modifier.width(resolveDp(width)); return this }
    fun height(height: Any): LuaModifier { modifier = modifier.height(resolveDp(height)); return this }
    fun wrapContentSize(): LuaModifier { modifier = modifier.wrapContentSize(); return this }
    
    fun background(colorProp: Any): LuaModifier {
        try { modifier = modifier.background(resolveColor(colorProp)) } catch (e: Exception) { e.printStackTrace() }
        return this
    }
    fun background(colorProp: Any, shape: Shape): LuaModifier {
        try { modifier = modifier.background(resolveColor(colorProp), shape) } catch (e: Exception) { e.printStackTrace() }
        return this
    }
    fun alpha(alpha: Float): LuaModifier { modifier = modifier.alpha(alpha); return this }
    fun aspectRatio(ratio: Float): LuaModifier { modifier = modifier.aspectRatio(ratio); return this }
    fun offset(x: Any, y: Any): LuaModifier { modifier = modifier.offset(resolveDp(x), resolveDp(y)); return this }
    
    fun clickable(onClick: LuaFunction): LuaModifier {
        modifier = modifier.clickable {
            try { onClick.call() } catch (e: Exception) { e.printStackTrace() }
        }
        return this
    }
    
    fun animateContentSize(): LuaModifier {
        modifier = modifier.animateContentSize()
        return this
    }

    fun pointerInput(gestures: org.luaj.LuaTable): LuaModifier {
        val onTap = gestures.get("onTap").takeIf { it.isfunction() }?.checkfunction()
        val onDoubleTap = gestures.get("onDoubleTap").takeIf { it.isfunction() }?.checkfunction()
        val onLongPress = gestures.get("onLongPress").takeIf { it.isfunction() }?.checkfunction()
        val onDrag = gestures.get("onDrag").takeIf { it.isfunction() }?.checkfunction()

        if (onTap != null || onDoubleTap != null || onLongPress != null) {
            modifier = modifier.pointerInput("tapGestures") {
                detectTapGestures(
                    onTap = onTap?.let { fn -> { offset -> fn.call(LuaValue.valueOf(offset.x.toDouble()), LuaValue.valueOf(offset.y.toDouble())) } },
                    onDoubleTap = onDoubleTap?.let { fn -> { offset -> fn.call(LuaValue.valueOf(offset.x.toDouble()), LuaValue.valueOf(offset.y.toDouble())) } },
                    onLongPress = onLongPress?.let { fn -> { offset -> fn.call(LuaValue.valueOf(offset.x.toDouble()), LuaValue.valueOf(offset.y.toDouble())) } }
                )
            }
        }
        
        if (onDrag != null) {
            modifier = modifier.pointerInput("dragGestures") {
                detectDragGestures { change, dragAmount -> 
                    change.consume()
                    onDrag.call(LuaValue.valueOf(dragAmount.x.toDouble()), LuaValue.valueOf(dragAmount.y.toDouble()))
                }
            }
        }
        
        return this
    }
    
    fun clip(shape: String, radius: Int): LuaModifier {
        val clipShape = when (shape.lowercase()) {
            "circle" -> CircleShape
            "rounded" -> RoundedCornerShape(radius.dp)
            else -> null
        }
        if (clipShape != null) { modifier = modifier.clip(clipShape) }
        return this
    }
    fun clip(shape: String): LuaModifier = clip(shape, 0)
    
    fun border(width: Int, color: Any): LuaModifier {
        try { modifier = modifier.border(width.dp, resolveColor(color)) } catch (e: Exception) { e.printStackTrace() }
        return this
    }
    
    fun align(alignStr: String): LuaModifier { this.alignmentStr = alignStr; return this }
    fun weight(weight: Float): LuaModifier { this.weightVal = weight; return this }
}

fun resolveModifier(prop: Any?): Modifier {
    return when (prop) {
        is LuaModifier -> prop.modifier
        is Modifier -> prop
        else -> Modifier
    }
}

fun resolveColor(colorProp: Any?, defaultColor: Color = Color.Unspecified): Color {
    return when (colorProp) {
        null -> defaultColor
        is Color -> colorProp
        is Long -> {
            if (colorProp in 0L..4294967295L) { Color(colorProp) } else { Color(colorProp.toULong()) }
        }
        else -> {
            val colorStr = colorProp.toString()
            try {
                val longVal = colorStr.toLongOrNull()
                if (longVal != null) { resolveColor(longVal, defaultColor) } else { Color(colorStr.toColorInt()) }
            } catch (e: Exception) { defaultColor }
        }
    }
}

fun resolveDp(value: Any?): androidx.compose.ui.unit.Dp {
    return when (value) {
        is androidx.compose.ui.unit.Dp -> value
        is Number -> value.toFloat().dp
        is String -> value.toFloatOrNull()?.dp ?: 0.dp
        else -> 0.dp
    }
}

fun resolveSp(value: Any?): androidx.compose.ui.unit.TextUnit {
    return when (value) {
        is androidx.compose.ui.unit.TextUnit -> value
        is Number -> value.toFloat().sp
        is String -> value.toFloatOrNull()?.sp ?: androidx.compose.ui.unit.TextUnit.Unspecified
        else -> androidx.compose.ui.unit.TextUnit.Unspecified
    }
}

// --- 4. Compose 核心入口渲染器 ---
@Composable
fun LuaScopeComponent(scope: LuaScope, parentComposeScope: Any? = null, vararg args: LuaValue) {
    scope.coroutineScope = rememberCoroutineScope()
    scope.context = LocalContext.current
    scope.density = LocalDensity.current
    scope.configuration = LocalConfiguration.current

    val version by scope.recomposeVersion
    val nodes = remember(version, *args) { scope.execute(*args) }

    for (node in nodes) {
        LuaNodeRenderer(node, parentComposeScope)
    }
}

@Composable
fun LuaNodeRenderer(node: LuaNode, parentComposeScope: Any? = null) {
    val childModifier = resolveModifier(node.props["modifier"])
    val alignmentStr = (node.props["modifier"] as? LuaModifier)?.alignmentStr
    val weightVal = (node.props["modifier"] as? LuaModifier)?.weightVal

    var finalModifier = if (alignmentStr != null && parentComposeScope != null) {
        when (parentComposeScope) {
            is androidx.compose.foundation.layout.BoxScope -> {
                val alignment = LuaComposeRegistry.resolveBoxAlignment(alignmentStr)
                with(parentComposeScope) { childModifier.align(alignment) }
            }
            is androidx.compose.foundation.layout.ColumnScope -> {
                val alignment = LuaComposeRegistry.resolveColumnAlignment(alignmentStr)
                with(parentComposeScope) { childModifier.align(alignment) }
            }
            is androidx.compose.foundation.layout.RowScope -> {
                val alignment = LuaComposeRegistry.resolveRowAlignment(alignmentStr)
                with(parentComposeScope) { childModifier.align(alignment) }
            }
            else -> childModifier
        }
    } else {
        childModifier
    }

    if (weightVal != null && parentComposeScope != null) {
        finalModifier = when (parentComposeScope) {
            is androidx.compose.foundation.layout.ColumnScope -> with(parentComposeScope) { finalModifier.weight(weightVal) }
            is androidx.compose.foundation.layout.RowScope -> with(parentComposeScope) { finalModifier.weight(weightVal) }
            else -> finalModifier
        }
    }

    val finalProps = node.props.toMutableMap()
    finalProps["modifier"] = finalModifier

    val renderer = LuaComposeRegistry.components[node.type]
    if (renderer != null) {
        renderer(finalProps, node.childScope)
    } else {
        Text("组件 [${node.type}] 未在注册表注册", color = Color.Red, modifier = Modifier.padding(8.dp))
    }
}
