package com.kulipai.luacompose.compose.runtime

import android.content.Context
import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.Density
import com.kulipai.luacompose.compose.script.ScriptEngine
import com.kulipai.luacompose.compose.script.ScriptFunction
import com.kulipai.luacompose.compose.script.ScriptTable
import com.kulipai.luacompose.compose.script.ScriptValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Stack

data class ComposeNode(
    val type: String,
    val props: Map<String, Any?>,
    val childScope: ComposeScope? = null
)

object ComposeBridge {
    lateinit var engine: ScriptEngine

    private val activeScopes = ThreadLocal.withInitial { Stack<ComposeScope>() }
    private val activeNodeLists = ThreadLocal.withInitial { Stack<MutableList<ComposeNode>>() }
    private val activeSharedTransitionScopes = ThreadLocal.withInitial { Stack<androidx.compose.animation.SharedTransitionScope>() }
    private val activeAnimatedVisibilityScopes = ThreadLocal.withInitial { Stack<androidx.compose.animation.AnimatedVisibilityScope>() }
    
    val converters = mutableMapOf<Class<*>, (Any) -> ScriptValue>()

    fun getActiveScope(): ComposeScope? {
        val stack = activeScopes.get()!!
        return if (stack.isNotEmpty()) stack.peek() else null
    }

    fun pushActiveScope(scope: ComposeScope) {
        activeScopes.get()!!.push(scope)
    }

    fun popActiveScope() {
        val stack = activeScopes.get()!!
        if (stack.isNotEmpty()) stack.pop()
    }

    fun getActiveNodeList(): MutableList<ComposeNode>? {
        val stack = activeNodeLists.get()!!
        return if (stack.isNotEmpty()) stack.peek() else null
    }

    fun pushActiveNodeList(list: MutableList<ComposeNode>) {
        activeNodeLists.get()!!.push(list)
    }

    fun popActiveNodeList() {
        val stack = activeNodeLists.get()!!
        if (stack.isNotEmpty()) stack.pop()
    }

    fun getActiveSharedTransitionScope(): androidx.compose.animation.SharedTransitionScope? {
        val stack = activeSharedTransitionScopes.get()!!
        return if (stack.isNotEmpty()) stack.peek() else null
    }

    fun pushActiveSharedTransitionScope(scope: androidx.compose.animation.SharedTransitionScope) {
        activeSharedTransitionScopes.get()!!.push(scope)
    }

    fun popActiveSharedTransitionScope() {
        val stack = activeSharedTransitionScopes.get()!!
        if (stack.isNotEmpty()) stack.pop()
    }

    fun getActiveAnimatedVisibilityScope(): androidx.compose.animation.AnimatedVisibilityScope? {
        val stack = activeAnimatedVisibilityScopes.get()!!
        return if (stack.isNotEmpty()) stack.peek() else null
    }

    fun pushActiveAnimatedVisibilityScope(scope: androidx.compose.animation.AnimatedVisibilityScope) {
        activeAnimatedVisibilityScopes.get()!!.push(scope)
    }

    fun popActiveAnimatedVisibilityScope() {
        val stack = activeAnimatedVisibilityScopes.get()!!
        if (stack.isNotEmpty()) stack.pop()
    }

    private val activeDrawScopes = ThreadLocal.withInitial { java.util.Stack<androidx.compose.ui.graphics.drawscope.DrawScope>() }
    fun getActiveDrawScope(): androidx.compose.ui.graphics.drawscope.DrawScope? = if (activeDrawScopes.get()!!.isNotEmpty()) activeDrawScopes.get()!!.peek() else null
    fun pushActiveDrawScope(scope: androidx.compose.ui.graphics.drawscope.DrawScope) { activeDrawScopes.get()!!.push(scope) }
    fun popActiveDrawScope() {
        val stack = activeDrawScopes.get()!!
        if (stack.isNotEmpty()) stack.pop()
    }

    private val activePointerInputScopeActions = ThreadLocal.withInitial { java.util.Stack<MutableList<suspend androidx.compose.ui.input.pointer.PointerInputScope.() -> Unit>>() }
    fun getActivePointerInputScopeActions(): MutableList<suspend androidx.compose.ui.input.pointer.PointerInputScope.() -> Unit>? = if (activePointerInputScopeActions.get()!!.isNotEmpty()) activePointerInputScopeActions.get()!!.peek() else null
    fun pushActivePointerInputScopeActions(actions: MutableList<suspend androidx.compose.ui.input.pointer.PointerInputScope.() -> Unit>) { activePointerInputScopeActions.get()!!.push(actions) }
    fun popActivePointerInputScopeActions() {
        val stack = activePointerInputScopeActions.get()!!
        if (stack.isNotEmpty()) stack.pop()
    }

    fun scriptToJava(value: ScriptValue?): Any? {
        if (value == null || value.isNil()) return null
        return when {
            value.isBoolean() -> value.toBoolean()
            value.isNumber() -> {
                val d = value.toDouble()
                val l = d.toLong()
                if (d == l.toDouble()) {
                    if (l in Int.MIN_VALUE..Int.MAX_VALUE) l.toInt() else l
                } else {
                    d
                }
            }
            value.isString() -> value.toStringValue()
            value.isFunction() -> value
            value.isUserdata() -> value.asUserdata()
            value.isTable() -> {
                val table = value.asTable()
                val isState = table.get("_isState")
                val isColor = table.get("_javaColor")
                val isDp = table.get("_javaDp")
                val isSize = table.get("_javaSize")
                val isOffset = table.get("_javaOffset")
                val isIntOffset = table.get("_javaIntOffset")
                val isStroke = table.get("_javaStroke")

                if (isState.isBoolean() && isState.toBoolean()) {
                    table.get("javaState").asUserdata()
                } else if (isColor.isUserdata()) {
                    isColor.asUserdata()
                } else if (isDp.isUserdata()) {
                    isDp.asUserdata()
                } else if (isSize.isUserdata()) {
                    isSize.asUserdata()
                } else if (isOffset.isUserdata()) {
                    isOffset.asUserdata()
                } else if (isIntOffset.isUserdata()) {
                    isIntOffset.asUserdata()
                } else if (isStroke.isUserdata()) {
                    isStroke.asUserdata()
                } else {
                    val len = table.length()
                    if (len > 0) {
                        val list = mutableListOf<Any?>()
                        for (i in 1..len) {
                            list.add(scriptToJava(table.get(i)))
                        }
                        list
                    } else {
                        scriptTableToMap(table)
                    }
                }
            }
            else -> value
        }
    }

    fun scriptTableToMap(table: ScriptTable): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        val keys = table.keys()
        for (key in keys) {
            val kStr = key.toStringValue()
            map[kStr] = scriptToJava(table.get(key))
        }
        return map
    }

    
    var luaValueUnwrapper: ((Any?) -> Any?)? = null

    fun unwrapAny(value: Any?): Any? {
        if (value is ScriptValue) {
            val unwrapped = scriptToJava(value)
            if (unwrapped != null && unwrapped !is ScriptValue) return unwrapped
        }
        return luaValueUnwrapper?.invoke(value) ?: value
    }

    fun javaToScript(value: Any?): ScriptValue {
        if (value == null) return engine.createNil()
        return when (value) {
            is Boolean -> engine.createValue(value)
            is Int -> engine.createValue(value)
            is Long -> engine.createValue(value.toDouble())
            is Double -> engine.createValue(value)
            is Float -> engine.createValue(value.toDouble())
            is String -> engine.createValue(value)
            is ScriptValue -> value
            else -> {
                val clazz = value::class.java
                android.util.Log.d("LUA_ANIM", "javaToScript for clazz: ${clazz.name}")
                for ((cls, converter) in converters) {
                    if (cls.isAssignableFrom(clazz)) {
                        android.util.Log.d("LUA_ANIM", "javaToScript MATCHED CONVERTER for clazz: ${clazz.name}")
                        return converter(value)
                    }
                }
                engine.coerceJavaToScript(value)
            }
        }
    }
}

fun createComposeStateTable(javaState: ComposeState): ScriptTable {
    val table = ComposeBridge.engine.createTable()
    table.set("_isState", ComposeBridge.engine.createValue(true))
    table.set("javaState", ComposeBridge.engine.createUserdata(javaState))
    
    val meta = ComposeBridge.engine.createTable()
    meta.set("__tostring", ComposeBridge.engine.createFunction { 
        ComposeBridge.engine.createValue(javaState.get().toString()) 
    })
    meta.set("__index", ComposeBridge.engine.createFunction { args -> 
        val key = args[1].toStringValue()
        if (key == "value") {
            ComposeBridge.javaToScript(javaState.get())
        } else {
            table.get(args[1]) // fallback to table's raw get? Or just nil.
            // Wait, __index is called when the key is not in the table. So nil.
            ComposeBridge.engine.createNil()
        }
    })
    meta.set("__newindex", ComposeBridge.engine.createFunction { args -> 
        val key = args[1].toStringValue()
        if (key == "value") {
            javaState.set(ComposeBridge.scriptToJava(args[2]))
        } else {
            table.set(args[1], args[2])
        }
        ComposeBridge.engine.createNil()
    })
    table.setMetatable(meta)
    return table
}

class ComposeScope(var contentFunc: ScriptFunction) {
    private val _recomposeVersion = mutableStateOf(0)
    val recomposeVersion: State<Int> = _recomposeVersion

    var coroutineScope: CoroutineScope? = null
    var context: Context? = null
    var density: Density? = null
    var configuration: Configuration? = null
    
    var colorScheme: ColorScheme? = null
    var typography: Typography? = null
    var shapes: Shapes? = null

    internal val states = mutableMapOf<Any, ScriptTable>()
    internal var statesCount = 0

    internal val remembers = mutableMapOf<Any, Any?>()
    internal var remembersCount = 0

    internal val childScopes = mutableMapOf<Any, ComposeScope>()
    internal var childScopesCount = 0

    internal val accessedStates = mutableSetOf<Any>()
    internal val accessedRemembers = mutableSetOf<Any>()
    internal val accessedChildScopes = mutableSetOf<Any>()

    fun getOrCreateState(initialValue: ScriptValue): ScriptTable {
        val actualKey = statesCount++
        accessedStates.add(actualKey)
        if (states[actualKey] == null) {
            val javaState = ComposeState(ComposeBridge.scriptToJava(initialValue), this)
            states[actualKey] = createComposeStateTable(javaState)
        }
        return states[actualKey]!!
    }

    internal val rememberKeys = mutableMapOf<Any, List<Any?>>()

    fun getOrCreateRemember(initFunc: ScriptFunction, keys: List<Any?> = emptyList()): ScriptValue {
        val actualKey = remembersCount++
        accessedRemembers.add(actualKey)
        val oldKeys = rememberKeys[actualKey]
        if (!remembers.containsKey(actualKey) || oldKeys != keys) {
            val initialValue = initFunc.call()
            remembers[actualKey] = initialValue
            rememberKeys[actualKey] = keys
        }
        return remembers[actualKey] as ScriptValue
    }

    fun getOrCreateDerivedState(computeFunc: ScriptFunction): ScriptTable {
        val actualKey = statesCount++ 
        accessedStates.add(actualKey)
        if (states[actualKey] == null) {
            val javaState = ComposeDerivedState(computeFunc, this)
            states[actualKey] = createComposeStateTable(javaState)
        }
        return states[actualKey]!!
    }

    // Storage for effect keys
    val effectStates = mutableMapOf<String, Boolean>()

    fun getOrCreateChildScope(func: ScriptFunction, key: Any? = null): ComposeScope {
        val actualKey = key ?: childScopesCount++
        accessedChildScopes.add(actualKey)
        val scope = childScopes.getOrPut(actualKey) { ComposeScope(func) }
        scope.contentFunc = func
        scope.coroutineScope = this.coroutineScope
        scope.context = this.context
        scope.density = this.density
        scope.configuration = this.configuration
        scope.colorScheme = this.colorScheme
        scope.typography = this.typography
        scope.shapes = this.shapes
        return scope
    }

    fun invalidate() {
        _recomposeVersion.value++
    }

    fun execute(vararg args: ScriptValue): List<ComposeNode> {
        ComposeBridge.pushActiveScope(this)
        val rootNodes = mutableListOf<ComposeNode>()
        ComposeBridge.pushActiveNodeList(rootNodes)
        
        statesCount = 0
        remembersCount = 0
        childScopesCount = 0
        accessedStates.clear()
        accessedRemembers.clear()
        accessedChildScopes.clear()
        
        try {
            contentFunc.call(*args)
        } catch (e: Exception) {
            e.printStackTrace()
            val errorMsg = "Script Error: ${e.message}\n\n${android.util.Log.getStackTraceString(e)}"
            rootNodes.add(
                ComposeNode(
                    "LuaError", // Keeping name for compatibility with renderer
                    mapOf("text" to errorMsg, "color" to "#ff0000"),
                    null
                )
            )
        } finally {
            ComposeBridge.popActiveNodeList()
            ComposeBridge.popActiveScope()
        }
        
        states.keys.retainAll(accessedStates)
        remembers.keys.retainAll(accessedRemembers)
        rememberKeys.keys.retainAll(accessedRemembers)
        childScopes.keys.retainAll(accessedChildScopes)

        return rootNodes
    }
}

open class ComposeState(initialValue: Any?, val scope: ComposeScope) {
    open val composeState: State<Any?> = mutableStateOf(initialValue)
    protected val dependentScopes = mutableSetOf<ComposeScope>()

    fun registerDependency(scope: ComposeScope) {
        dependentScopes.add(scope)
    }

    fun invalidateDependents() {
        for (scope in dependentScopes) {
            scope.invalidate()
        }
    }

    open fun get(): Any? {
        val active = ComposeBridge.getActiveScope()
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

class ComposeDerivedState(val computeFunc: ScriptFunction, scope: ComposeScope) : ComposeState(null, scope) {
    override val composeState = androidx.compose.runtime.derivedStateOf {
        ComposeBridge.scriptToJava(computeFunc.call())
    }

    override fun set(newValue: Any?) {
        // Read-only
    }
}

class ComposeAnimatableState<T, V : AnimationVector>(
    initialValue: T,
    val typeConverter: TwoWayConverter<T, V>,
    scope: ComposeScope
) : ComposeState(initialValue, scope) {
    
    var currentSpec: androidx.compose.animation.core.AnimationSpec<T>? = null
    val animatable = Animatable(initialValue, typeConverter)
    fun animateTo(target: T) {
        if (animatable.targetValue == target) return
        scope.coroutineScope?.launch {
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
        @Suppress("UNCHECKED_CAST")
        try {
            animateTo(newValue as T)
        } catch (e: Exception) { e.printStackTrace() }
    }
}
