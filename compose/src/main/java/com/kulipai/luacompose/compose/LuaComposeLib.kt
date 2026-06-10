package com.kulipai.luacompose.compose

import org.luaj.LuaFunction
import org.luaj.LuaTable
import org.luaj.LuaValue
import org.luaj.lib.OneArgFunction
import org.luaj.lib.TwoArgFunction
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.Color

class LuaComposeLib : LuaTable() {
    var rootContentFunc: LuaFunction? = null

    init {
        // Register setContent
        set("setContent", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                rootContentFunc = arg.checkfunction()
                return NIL
            }
        })

        // Register LaunchedEffect
        set("LaunchedEffect", object : TwoArgFunction() {
            override fun call(key: LuaValue, contentFunc: LuaValue): LuaValue {
                val node = LuaNode("LaunchedEffect", mapOf("key" to key, "content" to contentFunc))
                LuaBridge.getActiveNodeList()?.add(node)
                return NIL
            }
        })

        // Register DisposableEffect
        set("DisposableEffect", object : TwoArgFunction() {
            override fun call(key: LuaValue, contentFunc: LuaValue): LuaValue {
                val node = LuaNode("DisposableEffect", mapOf("key" to key, "content" to contentFunc))
                LuaBridge.getActiveNodeList()?.add(node)
                return NIL
            }
        })

        // Register global state
        set("state", object : OneArgFunction() {
            override fun call(initialValue: LuaValue): LuaValue {
                val scope = LuaBridge.getActiveScope() ?: throw RuntimeException("compose.state() 必须在 Compose 上下文中调用")
                return scope.get("state").call(scope, initialValue)
            }
        })

        // Register global remember
        set("remember", object : OneArgFunction() {
            override fun call(initFunc: LuaValue): LuaValue {
                val scope = LuaBridge.getActiveScope() ?: throw RuntimeException("compose.remember() 必须在 Compose 上下文中调用")
                return scope.get("remember").call(scope, initFunc)
            }
        })

        // Register global derivedStateOf
        set("derivedStateOf", object : OneArgFunction() {
            override fun call(computeFunc: LuaValue): LuaValue {
                val scope = LuaBridge.getActiveScope() ?: throw RuntimeException("compose.derivedStateOf() 必须在 Compose 上下文中调用")
                return scope.get("derivedStateOf").call(scope, computeFunc)
            }
        })

        // --- Animation Specs ---
        set("tween", object : org.luaj.lib.VarArgFunction() {
            override fun invoke(args: org.luaj.Varargs): org.luaj.Varargs {
                val duration = args.optint(1, 300)
                val delay = args.optint(2, 0)
                val easingStr = args.optjstring(3, "FastOutSlowIn")
                val easing = when (easingStr) {
                    "Linear" -> androidx.compose.animation.core.LinearEasing
                    "FastOutLinearIn" -> androidx.compose.animation.core.FastOutLinearInEasing
                    "LinearOutSlowIn" -> androidx.compose.animation.core.LinearOutSlowInEasing
                    else -> androidx.compose.animation.core.FastOutSlowInEasing
                }
                val spec = androidx.compose.animation.core.tween<Any>(duration, delay, easing)
                return LuaBridge.javaToLuaValue(spec)
            }
        })

        set("spring", object : org.luaj.lib.VarArgFunction() {
            override fun invoke(args: org.luaj.Varargs): org.luaj.Varargs {
                val damping = args.optdouble(1, androidx.compose.animation.core.Spring.DampingRatioNoBouncy.toDouble()).toFloat()
                val stiffness = args.optdouble(2, androidx.compose.animation.core.Spring.StiffnessMedium.toDouble()).toFloat()
                val spec = androidx.compose.animation.core.spring<Any>(dampingRatio = damping, stiffness = stiffness)
                return LuaBridge.javaToLuaValue(spec)
            }
        })

        // --- Animations ---
        set("animateFloatAsState", object : TwoArgFunction() {
            override fun call(targetValue: LuaValue, specValue: LuaValue): LuaValue {
                val scope = LuaBridge.getActiveScope() ?: throw RuntimeException("必须在 Compose 上下文中调用")
                val stateObj = scope.get("remember").call(scope, object : org.luaj.lib.ZeroArgFunction() {
                    override fun call(): LuaValue {
                        val state = LuaAnimatableState(targetValue.tofloat(), kotlin.Float.VectorConverter, scope)
                        return LuaStateTable(state)
                    }
                })
                val state = (stateObj.checktable() as LuaStateTable).javaState as LuaAnimatableState<Float, *>
                state.currentSpec = LuaBridge.luaValueToJava(specValue) as? androidx.compose.animation.core.AnimationSpec<Float>
                state.set(targetValue.tofloat())
                return stateObj
            }
        })

        set("animateIntAsState", object : TwoArgFunction() {
            override fun call(targetValue: LuaValue, specValue: LuaValue): LuaValue {
                val scope = LuaBridge.getActiveScope() ?: throw RuntimeException("必须在 Compose 上下文中调用")
                val stateObj = scope.get("remember").call(scope, object : org.luaj.lib.ZeroArgFunction() {
                    override fun call(): LuaValue {
                        val state = LuaAnimatableState(targetValue.toint(), kotlin.Int.VectorConverter, scope)
                        return LuaStateTable(state)
                    }
                })
                val state = (stateObj.checktable() as LuaStateTable).javaState as LuaAnimatableState<Int, *>
                state.currentSpec = LuaBridge.luaValueToJava(specValue) as? androidx.compose.animation.core.AnimationSpec<Int>
                state.set(targetValue.toint())
                return stateObj
            }
        })

        set("animateDpAsState", object : TwoArgFunction() {
            override fun call(targetValue: LuaValue, specValue: LuaValue): LuaValue {
                val scope = LuaBridge.getActiveScope() ?: throw RuntimeException("必须在 Compose 上下文中调用")
                val targetDp = resolveDp(LuaBridge.luaValueToJava(targetValue))
                val stateObj = scope.get("remember").call(scope, object : org.luaj.lib.ZeroArgFunction() {
                    override fun call(): LuaValue {
                        val state = LuaAnimatableState(targetDp, androidx.compose.ui.unit.Dp.VectorConverter, scope)
                        return LuaStateTable(state)
                    }
                })
                val state = (stateObj.checktable() as LuaStateTable).javaState as LuaAnimatableState<androidx.compose.ui.unit.Dp, *>
                state.currentSpec = LuaBridge.luaValueToJava(specValue) as? androidx.compose.animation.core.AnimationSpec<androidx.compose.ui.unit.Dp>
                state.set(targetDp)
                return stateObj
            }
        })

        set("animateColorAsState", object : TwoArgFunction() {
            override fun call(targetValue: LuaValue, specValue: LuaValue): LuaValue {
                val scope = LuaBridge.getActiveScope() ?: throw RuntimeException("必须在 Compose 上下文中调用")
                val targetColor = resolveColor(LuaBridge.luaValueToJava(targetValue))
                val stateObj = scope.get("remember").call(scope, object : org.luaj.lib.ZeroArgFunction() {
                    override fun call(): LuaValue {
                        val converter = androidx.compose.animation.core.TwoWayConverter<androidx.compose.ui.graphics.Color, androidx.compose.animation.core.AnimationVector4D>(
                            convertToVector = { androidx.compose.animation.core.AnimationVector4D(it.red, it.green, it.blue, it.alpha) },
                            convertFromVector = { androidx.compose.ui.graphics.Color(it.v1, it.v2, it.v3, it.v4) }
                        )
                        val state = LuaAnimatableState(targetColor, converter, scope)
                        return LuaStateTable(state)
                    }
                })
                val state = (stateObj.checktable() as LuaStateTable).javaState as LuaAnimatableState<androidx.compose.ui.graphics.Color, *>
                state.currentSpec = LuaBridge.luaValueToJava(specValue) as? androidx.compose.animation.core.AnimationSpec<androidx.compose.ui.graphics.Color>
                state.set(targetColor)
                return stateObj
            }
        })

        // --- CompositionLocal equivalents ---
        set("getScreenWidth", object : org.luaj.lib.ZeroArgFunction() {
            override fun call(): LuaValue {
                val scope = LuaBridge.getActiveScope()
                val config = scope?.configuration
                val widthDp = config?.screenWidthDp?.toDouble() ?: 0.0
                return LuaValue.valueOf(widthDp)
            }
        })

        set("getScreenHeight", object : org.luaj.lib.ZeroArgFunction() {
            override fun call(): LuaValue {
                val scope = LuaBridge.getActiveScope()
                val config = scope?.configuration
                val heightDp = config?.screenHeightDp?.toDouble() ?: 0.0
                return LuaValue.valueOf(heightDp)
            }
        })

        set("getDensity", object : org.luaj.lib.ZeroArgFunction() {
            override fun call(): LuaValue {
                val scope = LuaBridge.getActiveScope()
                val density = scope?.density?.density?.toDouble() ?: 1.0
                return LuaValue.valueOf(density)
            }
        })

        // --- Path API ---
        set("Path", object : org.luaj.lib.ZeroArgFunction() {
            override fun call(): LuaValue {
                return LuaPath()
            }
        })

        // --- Units API ---
        set("dp", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                return LuaBridge.javaToLuaValue(resolveDp(LuaBridge.luaValueToJava(arg)))
            }
        })

        set("sp", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                return LuaBridge.javaToLuaValue(resolveSp(LuaBridge.luaValueToJava(arg)))
            }
        })

        // Register components from Registry
        LuaComposeRegistry.components.keys.forEach { componentName ->
            set(componentName, object : OneArgFunction() {
                override fun call(propsVal: LuaValue): LuaValue {
                    var props = mutableMapOf<String, Any?>()
                    var contentFunc: LuaFunction? = null

                    if (propsVal.isstring()) {
                        props["text"] = propsVal.tojstring()
                    } else if (propsVal.isfunction()) {
                        contentFunc = propsVal.checkfunction()
                    } else if (propsVal.istable()) {
                        props = LuaBridge.luaTableToMap(propsVal).toMutableMap()
                        val contentVal = propsVal.get("content")
                        if (contentVal.isfunction()) {
                            contentFunc = contentVal.checkfunction()
                        }
                        props.remove("content")
                    }

                    val key = props["key"]
                    props.remove("key")

                    val parentScope = LuaBridge.getActiveScope()
                    val childScope = if (contentFunc != null && parentScope != null) {
                        parentScope.getOrCreateChildScope(contentFunc, key)
                    } else {
                        null
                    }

                    val node = LuaNode(componentName, props, childScope)
                    LuaBridge.getActiveNodeList()?.add(node)
                    
                    return NIL
                }
            })
        }
    }
}
