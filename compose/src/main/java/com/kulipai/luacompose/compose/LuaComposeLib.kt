package com.kulipai.luacompose.compose


import androidx.compose.animation.VectorConverter
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.luaj.LuaFunction
import org.luaj.LuaTable
import org.luaj.LuaValue
import org.luaj.lib.OneArgFunction
import org.luaj.lib.TwoArgFunction
import org.luaj.lib.ZeroArgFunction

class LuaComposeLib : TwoArgFunction() {
    var rootContentFunc: LuaFunction? = null

    override fun call(modname: LuaValue, env: LuaValue): LuaValue {
        val composeTable = LuaTable()
        env.set("compose", composeTable)

        composeTable.set("dp", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                return LuaBridge.javaToLuaValue(resolveDp(LuaBridge.luaValueToJava(arg)))
            }
        })
        composeTable.set("sp", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                return LuaBridge.javaToLuaValue(resolveSp(LuaBridge.luaValueToJava(arg)))
            }
        })
        
        composeTable.set("setContent", object : OneArgFunction() {
            override fun call(func: LuaValue): LuaValue {
                rootContentFunc = func.checkfunction()
                return LuaValue.NIL
            }
        })

        composeTable.set("Color", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                return LuaBridge.javaToLuaValue(resolveColor(LuaBridge.luaValueToJava(arg)))
            }
        })

        composeTable.set("Path", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                return LuaPath()
            }
        })

        composeTable.set("animateFloatAsState", object : TwoArgFunction() {
            override fun call(targetValue: LuaValue, spec: LuaValue): LuaValue {
                val target = targetValue.tofloat()
                val activeScope = LuaBridge.getActiveScope()
                if (activeScope != null) {
                    val key = "anim_float_\${targetValue.hashCode()}"
                    var animState = activeScope.get(key)
                    if (animState.isnil()) {
                        val state = LuaAnimatableState(target, Float.VectorConverter, activeScope)
                        animState = LuaStateTable(state)
                        activeScope.set(key, animState)
                    }
                    val javaState = (animState as LuaStateTable).javaState as LuaAnimatableState<Float, AnimationVector1D>
                    if (spec.istable()) {
                        javaState.currentSpec = parseAnimationSpec<Float>(spec.checktable()) as AnimationSpec<Float>
                    }
                    javaState.animateTo(target)
                    return animState
                }
                return LuaValue.NIL
            }
        })

        composeTable.set("animateIntAsState", object : TwoArgFunction() {
            override fun call(targetValue: LuaValue, spec: LuaValue): LuaValue {
                val target = targetValue.toint()
                val activeScope = LuaBridge.getActiveScope()
                if (activeScope != null) {
                    val key = "anim_int_\${targetValue.hashCode()}"
                    var animState = activeScope.get(key)
                    if (animState.isnil()) {
                        val state = LuaAnimatableState(target, Int.VectorConverter, activeScope)
                        animState = LuaStateTable(state)
                        activeScope.set(key, animState)
                    }
                    val javaState = (animState as LuaStateTable).javaState as LuaAnimatableState<Int, AnimationVector1D>
                    if (spec.istable()) {
                        javaState.currentSpec = parseAnimationSpec<Int>(spec.checktable()) as AnimationSpec<Int>
                    }
                    javaState.animateTo(target)
                    return animState
                }
                return LuaValue.NIL
            }
        })

        composeTable.set("animateDpAsState", object : TwoArgFunction() {
            override fun call(targetValue: LuaValue, spec: LuaValue): LuaValue {
                val target = resolveDp(LuaBridge.luaValueToJava(targetValue))
                val activeScope = LuaBridge.getActiveScope()
                if (activeScope != null) {
                    val key = "anim_dp_\${targetValue.hashCode()}"
                    var animState = activeScope.get(key)
                    if (animState.isnil()) {
                        val state = LuaAnimatableState(target, Dp.VectorConverter, activeScope)
                        animState = LuaStateTable(state)
                        activeScope.set(key, animState)
                    }
                    val javaState = (animState as LuaStateTable).javaState as LuaAnimatableState<Dp, AnimationVector1D>
                    if (spec.istable()) {
                        javaState.currentSpec = parseAnimationSpec<Dp>(spec.checktable()) as AnimationSpec<Dp>
                    }
                    javaState.animateTo(target)
                    return animState
                }
                return LuaValue.NIL
            }
        })

        composeTable.set("animateColorAsState", object : TwoArgFunction() {
            override fun call(targetValue: LuaValue, spec: LuaValue): LuaValue {
                val target = resolveColor(LuaBridge.luaValueToJava(targetValue))
                val activeScope = LuaBridge.getActiveScope()
                if (activeScope != null) {
                    val key = "anim_color_\${targetValue.hashCode()}"
                    var animState = activeScope.get(key)
                    if (animState.isnil()) {
                        val state = LuaAnimatableState(target, Color.VectorConverter(target.colorSpace), activeScope)
                        animState = LuaStateTable(state)
                        activeScope.set(key, animState)
                    }
                    val javaState = (animState as LuaStateTable).javaState as LuaAnimatableState<Color, AnimationVector4D>
                    if (spec.istable()) {
                        javaState.currentSpec = parseAnimationSpec<Color>(spec.checktable()) as AnimationSpec<Color>
                    }
                    javaState.animateTo(target)
                    return animState
                }
                return LuaValue.NIL
            }
        })

        composeTable.set("LaunchedEffect", object : OneArgFunction() {
            override fun call(effectFunc: LuaValue): LuaValue {
                val activeScope = LuaBridge.getActiveScope()
                if (activeScope != null) {
                    val key = "effect_\${effectFunc.hashCode()}"
                    if (activeScope.get(key).isnil()) {
                        activeScope.set(key, LuaValue.valueOf(true))
                        activeScope.coroutineScope?.launch(Dispatchers.Default) {
                            try {
                                effectFunc.checkfunction().call()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
                return LuaValue.NIL
            }
        })

        composeTable.set("DisposableEffect", object : OneArgFunction() {
            override fun call(effectFunc: LuaValue): LuaValue {
                val activeScope = LuaBridge.getActiveScope()
                if (activeScope != null) {
                    val key = "effect_\${effectFunc.hashCode()}"
                    if (activeScope.get(key).isnil()) {
                        activeScope.set(key, LuaValue.valueOf(true))
                        effectFunc.checkfunction().call()
                    }
                }
                return LuaValue.NIL
            }
        })

        LuaComposeRegistry.plugins.forEach { plugin ->
            val targetTable = if (plugin.namespace != null) {
                var nsTable = composeTable.get(plugin.namespace)
                if (nsTable.isnil()) {
                    nsTable = LuaTable()
                    composeTable.set(plugin.namespace, nsTable)
                }
                nsTable as LuaTable
            } else {
                composeTable
            }

            plugin.injectGlobals(targetTable)

            plugin.getComponents().forEach { (componentName, _) ->
                val fullTypeName = if (plugin.namespace != null) "${plugin.namespace}.$componentName" else componentName
                val func = object : OneArgFunction() {
                    override fun call(arg: LuaValue): LuaValue {
                        val props = mutableMapOf<String, Any?>()
                        var contentFunc: org.luaj.LuaFunction? = null
                        if (arg.isfunction()) {
                            contentFunc = arg.checkfunction()
                        } else if (arg.istable()) {
                            val luaMap = arg.checktable()
                            props.putAll(LuaBridge.luaTableToMap(luaMap))
                            val content = luaMap.get("content")
                            if (content.isfunction()) {
                                contentFunc = content.checkfunction()
                                props.remove("content")
                            }
                        }

                        val activeScope = LuaBridge.getActiveScope()
                        val childScope = if (contentFunc != null && activeScope != null) {
                            activeScope.getOrCreateChildScope(contentFunc)
                        } else null

                        val node = LuaNode(fullTypeName, props, childScope)
                        LuaBridge.getActiveNodeList()?.add(node)
                        return LuaValue.NIL
                    }
                }
                targetTable.set(componentName, func)
            }
        }

        return composeTable
    }

    private fun <T> parseAnimationSpec(table: LuaTable): AnimationSpec<T> {
        val type = table.get("type").optjstring("spring")
        return when (type) {
            "tween" -> {
                val duration = table.get("duration").optint(300)
                val delay = table.get("delay").optint(0)
                tween(durationMillis = duration, delayMillis = delay)
            }
            "spring" -> {
                val dampingRatio = table.get("dampingRatio").optdouble(Spring.DampingRatioNoBouncy.toDouble()).toFloat()
                val stiffness = table.get("stiffness").optdouble(Spring.StiffnessMedium.toDouble()).toFloat()
                spring(dampingRatio = dampingRatio, stiffness = stiffness)
            }
            else -> spring()
        }
    }
}
