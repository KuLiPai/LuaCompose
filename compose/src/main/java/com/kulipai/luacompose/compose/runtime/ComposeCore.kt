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
                if (isState.isBoolean() && isState.toBoolean()) {
                    table.get("javaState").asUserdata()
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
            else -> engine.coerceJavaToScript(value)
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

    private val states = mutableMapOf<Any, ScriptTable>()
    private var statesCount = 0

    private val remembers = mutableMapOf<Any, Any?>()
    private var remembersCount = 0

    private val childScopes = mutableMapOf<Any, ComposeScope>()
    private var childScopesCount = 0

    private val accessedStates = mutableSetOf<Any>()
    private val accessedRemembers = mutableSetOf<Any>()
    private val accessedChildScopes = mutableSetOf<Any>()

    fun getOrCreateState(initialValue: ScriptValue): ScriptTable {
        val actualKey = statesCount++
        accessedStates.add(actualKey)
        if (states[actualKey] == null) {
            val javaState = ComposeState(ComposeBridge.scriptToJava(initialValue), this)
            states[actualKey] = createComposeStateTable(javaState)
        }
        return states[actualKey]!!
    }

    fun getOrCreateRemember(initFunc: ScriptFunction): ScriptValue {
        val actualKey = remembersCount++
        accessedRemembers.add(actualKey)
        if (!remembers.containsKey(actualKey)) {
            val initialValue = initFunc.call()
            remembers[actualKey] = initialValue
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
