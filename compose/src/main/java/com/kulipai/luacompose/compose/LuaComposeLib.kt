package com.kulipai.luacompose.compose


import androidx.compose.animation.VectorConverter
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
                return NIL
            }
        })

        composeTable.set("state", object : OneArgFunction() {
            override fun call(initialValue: LuaValue): LuaValue {
                val scope = LuaBridge.getActiveScope()
                    ?: throw RuntimeException("compose.state() 必须在 Compose 上下文中调用")
                return scope.get("state").call(scope, initialValue)
            }
        })

        composeTable.set("remember", object : OneArgFunction() {
            override fun call(initFunc: LuaValue): LuaValue {
                val scope = LuaBridge.getActiveScope()
                    ?: throw RuntimeException("compose.remember() 必须在 Compose 上下文中调用")
                return scope.get("remember").call(scope, initFunc)
            }
        })

        composeTable.set("derivedStateOf", object : OneArgFunction() {
            override fun call(computeFunc: LuaValue): LuaValue {
                val scope = LuaBridge.getActiveScope()
                    ?: throw RuntimeException("compose.derivedStateOf() 必须在 Compose 上下文中调用")
                return scope.get("derivedStateOf").call(scope, computeFunc)
            }
        })




        composeTable.set("Path", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                return LuaPath()
            }
        })


        val animationTable = LuaTable()

        animationTable.set("tween", object : org.luaj.lib.VarArgFunction() {
            override fun invoke(args: org.luaj.Varargs): org.luaj.Varargs {
                var duration = 300
                var delay = 0
                var easingStr = "FastOutSlowIn"

                val arg1 = args.arg1()
                if (arg1.istable()) {
                    val t = arg1.checktable()
                    duration = t.get("duration").optint(t.get("durationMillis").optint(300))
                    delay = t.get("delay").optint(t.get("delayMillis").optint(0))
                    easingStr = t.get("easing").optjstring("FastOutSlowIn")
                } else {
                    duration = args.optint(1, 300)
                    delay = args.optint(2, 0)
                    easingStr = args.optjstring(3, "FastOutSlowIn")
                }

                val table = LuaTable()
                table.set("type", "tween")
                table.set("duration", duration)
                table.set("delay", delay)
                table.set("easing", easingStr)
                return table
            }
        })

        animationTable.set("spring", object : org.luaj.lib.VarArgFunction() {
            override fun invoke(args: org.luaj.Varargs): org.luaj.Varargs {
                var damping = androidx.compose.animation.core.Spring.DampingRatioNoBouncy.toFloat()
                var stiffness = androidx.compose.animation.core.Spring.StiffnessMedium.toFloat()

                val arg1 = args.arg1()
                if (arg1.istable()) {
                    val t = arg1.checktable()
                    damping = t.get("dampingRatio").optdouble(damping.toDouble()).toFloat()
                    stiffness = t.get("stiffness").optdouble(stiffness.toDouble()).toFloat()
                } else {
                    damping = args.optdouble(1, damping.toDouble()).toFloat()
                    stiffness = args.optdouble(2, stiffness.toDouble()).toFloat()
                }

                val table = LuaTable()
                table.set("type", "spring")
                table.set("dampingRatio", damping.toDouble())
                table.set("stiffness", stiffness.toDouble())
                return table
            }
        })

        fun resolveAnimationArgs(args: org.luaj.Varargs): Pair<LuaValue, LuaValue> {
            val arg1 = args.arg1()
            if (arg1.istable() && !arg1.get("targetValue").isnil()) {
                val t = arg1.checktable()
                return Pair(t.get("targetValue"), t.get("animationSpec"))
            }
            return Pair(arg1, args.arg(2))
        }

        animationTable.set("animateFloatAsState", object : org.luaj.lib.VarArgFunction() {
            override fun invoke(args: org.luaj.Varargs): org.luaj.Varargs {
                val (targetValue, spec) = resolveAnimationArgs(args)
                val target = targetValue.tofloat()
                val activeScope = LuaBridge.getActiveScope()
                if (activeScope != null) {
                    val initFunc = object : org.luaj.lib.ZeroArgFunction() {
                        override fun call(): org.luaj.LuaValue {
                            val state = LuaAnimatableState(
                                target,
                                kotlin.Float.VectorConverter,
                                activeScope
                            )
                            return LuaStateTable(state)
                        }
                    }
                    val animState = activeScope.get("remember").call(activeScope, initFunc)
                    val javaState =
                        (animState as LuaStateTable).javaState as LuaAnimatableState<Float, androidx.compose.animation.core.AnimationVector1D>
                    if (spec.istable()) {
                        javaState.currentSpec =
                            parseAnimationSpec<Float>(spec.checktable()) as androidx.compose.animation.core.AnimationSpec<Float>
                    }
                    javaState.animateTo(target)
                    return animState
                }
                return org.luaj.LuaValue.NIL
            }
        })

        animationTable.set("animateIntAsState", object : org.luaj.lib.VarArgFunction() {
            override fun invoke(args: org.luaj.Varargs): org.luaj.Varargs {
                val (targetValue, spec) = resolveAnimationArgs(args)
                val target = targetValue.toint()
                val activeScope = LuaBridge.getActiveScope()
                if (activeScope != null) {
                    val initFunc = object : org.luaj.lib.ZeroArgFunction() {
                        override fun call(): org.luaj.LuaValue {
                            val state =
                                LuaAnimatableState(target, kotlin.Int.VectorConverter, activeScope)
                            return LuaStateTable(state)
                        }
                    }
                    val animState = activeScope.get("remember").call(activeScope, initFunc)
                    val javaState =
                        (animState as LuaStateTable).javaState as LuaAnimatableState<Int, androidx.compose.animation.core.AnimationVector1D>
                    if (spec.istable()) {
                        javaState.currentSpec =
                            parseAnimationSpec<Int>(spec.checktable()) as androidx.compose.animation.core.AnimationSpec<Int>
                    }
                    javaState.animateTo(target)
                    return animState
                }
                return org.luaj.LuaValue.NIL
            }
        })

        animationTable.set("animateDpAsState", object : org.luaj.lib.VarArgFunction() {
            override fun invoke(args: org.luaj.Varargs): org.luaj.Varargs {
                val (targetValue, spec) = resolveAnimationArgs(args)
                val target = resolveDp(LuaBridge.luaValueToJava(targetValue))
                val activeScope = LuaBridge.getActiveScope()
                if (activeScope != null) {
                    val initFunc = object : org.luaj.lib.ZeroArgFunction() {
                        override fun call(): org.luaj.LuaValue {
                            val state = LuaAnimatableState(
                                target,
                                androidx.compose.ui.unit.Dp.VectorConverter,
                                activeScope
                            )
                            return LuaStateTable(state)
                        }
                    }
                    val animState = activeScope.get("remember").call(activeScope, initFunc)
                    val javaState =
                        (animState as LuaStateTable).javaState as LuaAnimatableState<androidx.compose.ui.unit.Dp, androidx.compose.animation.core.AnimationVector1D>
                    if (spec.istable()) {
                        javaState.currentSpec =
                            parseAnimationSpec<androidx.compose.ui.unit.Dp>(spec.checktable()) as androidx.compose.animation.core.AnimationSpec<androidx.compose.ui.unit.Dp>
                    }
                    javaState.animateTo(target)
                    return animState
                }
                return org.luaj.LuaValue.NIL
            }
        })

        animationTable.set("animateColorAsState", object : org.luaj.lib.VarArgFunction() {
            override fun invoke(args: org.luaj.Varargs): org.luaj.Varargs {
                val (targetValue, spec) = resolveAnimationArgs(args)
                val target = resolveColor(LuaBridge.luaValueToJava(targetValue))
                val activeScope = LuaBridge.getActiveScope()
                if (activeScope != null) {
                    val initFunc = object : org.luaj.lib.ZeroArgFunction() {
                        override fun call(): org.luaj.LuaValue {
                            val state = LuaAnimatableState(
                                target,
                                androidx.compose.ui.graphics.Color.VectorConverter(target.colorSpace),
                                activeScope
                            )
                            return LuaStateTable(state)
                        }
                    }
                    val animState = activeScope.get("remember").call(activeScope, initFunc)
                    val javaState =
                        (animState as LuaStateTable).javaState as LuaAnimatableState<androidx.compose.ui.graphics.Color, androidx.compose.animation.core.AnimationVector4D>
                    if (spec.istable()) {
                        javaState.currentSpec =
                            parseAnimationSpec<androidx.compose.ui.graphics.Color>(spec.checktable()) as androidx.compose.animation.core.AnimationSpec<androidx.compose.ui.graphics.Color>
                    }
                    javaState.animateTo(target)
                    return animState
                }
                return org.luaj.LuaValue.NIL
            }
        })

        composeTable.set("animation", animationTable)

        // Also register old paths temporarily for backwards compatibility? No, user explicitly wants it isolated.


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
                return NIL
            }
        })

        composeTable.set("DisposableEffect", object : OneArgFunction() {
            override fun call(effectFunc: LuaValue): LuaValue {
                val activeScope = LuaBridge.getActiveScope()
                if (activeScope != null) {
                    val key = "effect_\${effectFunc.hashCode()}"
                    if (activeScope.get(key).isnil()) {
                        activeScope.set(key, valueOf(true))
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
                val fullTypeName =
                    if (plugin.namespace != null) "${plugin.namespace}.$componentName" else componentName
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
                val dampingRatio =
                    table.get("dampingRatio").optdouble(Spring.DampingRatioNoBouncy.toDouble())
                        .toFloat()
                val stiffness =
                    table.get("stiffness").optdouble(Spring.StiffnessMedium.toDouble()).toFloat()
                spring(dampingRatio = dampingRatio, stiffness = stiffness)
            }

            else -> spring()
        }
    }
}
