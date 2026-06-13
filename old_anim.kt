package com.kulipai.luacompose.compose.plugins

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.Color
import com.kulipai.luacompose.compose.LuaAnimatableState
import com.kulipai.luacompose.compose.LuaBridge
import com.kulipai.luacompose.compose.LuaScope
import com.kulipai.luacompose.compose.LuaScopeComponent
import com.kulipai.luacompose.compose.LuaStateTable
import com.kulipai.luacompose.compose.resolveColor
import com.kulipai.luacompose.compose.resolveDp
import com.kulipai.luacompose.compose.resolveModifier
import org.luaj.LuaFunction
import org.luaj.LuaTable
import org.luaj.LuaValue


class LuaContentTransform(val transform: androidx.compose.animation.ContentTransform) : org.luaj.LuaUserdata(transform)

class LuaEnterTransition(val transition: EnterTransition) : org.luaj.LuaUserdata(transition) {
    init {
        val meta = LuaTable()
        meta.set(LuaValue.ADD, object : org.luaj.lib.TwoArgFunction() {
            override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
                if (arg1 is LuaEnterTransition && arg2 is LuaEnterTransition) {
                    return LuaEnterTransition(arg1.transition + arg2.transition)
                }
                return arg1
            }
        })
        meta.set("togetherWith", object : org.luaj.lib.TwoArgFunction() {
            override fun call(self: LuaValue, exitVal: LuaValue): LuaValue {
                val enter = (self as LuaEnterTransition).transition
                var exit: androidx.compose.animation.ExitTransition = androidx.compose.animation.fadeOut()
                if (exitVal is LuaExitTransition) exit = exitVal.transition
                else if (exitVal.isuserdata() && exitVal.checkuserdata() is androidx.compose.animation.ExitTransition) exit = exitVal.checkuserdata() as androidx.compose.animation.ExitTransition
                
                return LuaContentTransform(enter.togetherWith(exit))
            }
        })
        meta.set("__index", meta)
        setmetatable(meta)
    }
}

class LuaExitTransition(val transition: ExitTransition) : org.luaj.LuaUserdata(transition) {
    init {
        val meta = LuaTable()
        meta.set(LuaValue.ADD, object : org.luaj.lib.TwoArgFunction() {
            override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
                if (arg1 is LuaExitTransition && arg2 is LuaExitTransition) {
                    return LuaExitTransition(arg1.transition + arg2.transition)
                }
                return arg1
            }
        })
        setmetatable(meta)
    }
}

class AnimationPlugin : LuaComposePlugin {
    override val namespace: String = "animation"

    override fun getComponents(): Map<String, @Composable (props: Map<String, Any?>, childScope: LuaScope?) -> Unit> {
        return mapOf(
            
            "AnimatedContent" to { props, childScope ->
                val targetState = props["targetState"]
                val modifier = resolveModifier(props["modifier"])
                val transitionSpecObj = props["transitionSpec"]
                
                var actualChildScope = childScope
                val contentObj = props["content"]
                if (actualChildScope == null && contentObj is LuaFunction) {
                    actualChildScope = LuaScope(contentObj)
                }

                val actualTarget = targetState

                AnimatedContent(
                    targetState = actualTarget,
                    modifier = modifier,
                    transitionSpec = {
                        var transform: ContentTransform = fadeIn(animationSpec = tween(220, delayMillis = 90)) togetherWith fadeOut(animationSpec = tween(90))
                        if (transitionSpecObj is LuaFunction) {
                            try {
                                val scopeTable = LuaTable()
                                scopeTable.set("initialState", LuaBridge.javaToLuaValue(this.initialState))
                                scopeTable.set("targetState", LuaBridge.javaToLuaValue(this.targetState))
                                scopeTable.set("isTransitioningTo", object : org.luaj.lib.TwoArgFunction() {
                                    override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
                                        val isMatch = this@AnimatedContent.initialState == LuaBridge.luaValueToJava(arg1) && 
                                                      this@AnimatedContent.targetState == LuaBridge.luaValueToJava(arg2)
                                        return LuaValue.valueOf(isMatch)
                                    }
                                })
                                val res = transitionSpecObj.call(scopeTable)
                                if (res is LuaContentTransform) {
                                    transform = res.transform
                                } else if (res.isuserdata() && res.checkuserdata() is ContentTransform) {
                                    transform = res.checkuserdata() as ContentTransform
                                }
                            } catch (e: Exception) { e.printStackTrace() }
                        }
                        transform
                    },
                    label = props["label"] as? String ?: "AnimatedContent"
                ) { stateValue ->
                    if (actualChildScope != null) {
                        LuaScopeComponent(actualChildScope, this, LuaBridge.javaToLuaValue(stateValue))
                    }
                }
            },

            "AnimatedVisibility" to { props, childScope ->
                val visible = props["visible"] as? Boolean ?: true
                val modifier = resolveModifier(props["modifier"])
                val enterObj = props["enter"]
                val exitObj = props["exit"]
                
                var enter = fadeIn() + expandIn()
                if (enterObj is androidx.compose.animation.EnterTransition) {
                    enter = enterObj
                } else if (enterObj is LuaEnterTransition) {
                    enter = enterObj.transition
                }
                
                var exit = shrinkOut() + fadeOut()
                if (exitObj is androidx.compose.animation.ExitTransition) {
                    exit = exitObj
                } else if (exitObj is LuaExitTransition) {
                    exit = exitObj.transition
                }
                
                val label = props["label"] as? String ?: "AnimatedVisibility"
                
                var actualChildScope = childScope
                val contentObj = props["content"]
                if (actualChildScope == null && contentObj is LuaFunction) {
                    actualChildScope = LuaScope(contentObj)
                }

                AnimatedVisibility(
                    visible = visible,
                    modifier = modifier,
                    enter = enter,
                    exit = exit,
                    label = label
                ) {
                    if (actualChildScope != null) {
                        LuaScopeComponent(actualChildScope, this)
                    }
                }
            }
        )
    }

    private fun resolveAnimationArgs(args: org.luaj.Varargs): Pair<LuaValue, LuaValue> {
        val arg1 = args.arg1()
        if (arg1.istable() && !arg1.get("targetValue").isnil()) {
            val t = arg1.checktable()
            return Pair(t.get("targetValue"), t.get("animationSpec"))
        }
        return Pair(arg1, args.arg(2))
    }
    
    private inline fun <reified T> getFiniteSpec(specTable: LuaValue?, defaultSpec: FiniteAnimationSpec<T>): FiniteAnimationSpec<T> {
        if (specTable == null || specTable.isnil()) return defaultSpec
        if (specTable.istable()) {
            return parseAnimationSpec<T>(specTable.checktable()) as? FiniteAnimationSpec<T> ?: defaultSpec
        }
        return defaultSpec
    }

    override fun injectGlobals(luaTable: LuaTable) {
        luaTable.set("tween", object : org.luaj.lib.VarArgFunction() {
            override fun invoke(args: org.luaj.Varargs): org.luaj.Varargs {
                var duration = 300
                var delay = 0
                var easingStr = "FastOutSlowIn"

                val arg1 = args.arg1()
                if (arg1.istable()) {
                    val t = arg1.checktable()
                    duration = t.get("durationMillis").optint(t.get("duration").optint(300))
                    delay = t.get("delayMillis").optint(t.get("delay").optint(0))
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

        luaTable.set("spring", object : org.luaj.lib.VarArgFunction() {
            override fun invoke(args: org.luaj.Varargs): org.luaj.Varargs {
                var damping = Spring.DampingRatioNoBouncy.toFloat()
                var stiffness = Spring.StiffnessMedium.toFloat()

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

        // Transitions
        luaTable.set("fadeIn", object : org.luaj.lib.VarArgFunction() {
            override fun invoke(args: org.luaj.Varargs): org.luaj.Varargs {
                var specValue: LuaValue? = null
                var initialAlpha = 0f
                val arg1 = args.arg1()
                if (arg1.istable() && arg1.get("animationSpec") != LuaValue.NIL) {
                    specValue = arg1.get("animationSpec")
                    initialAlpha = arg1.get("initialAlpha").optdouble(0.0).toFloat()
                } else if (arg1.istable() && arg1.get("type") != LuaValue.NIL) {
                    specValue = arg1
                    initialAlpha = args.arg(2).optdouble(0.0).toFloat()
                } else {
                    initialAlpha = arg1.optdouble(0.0).toFloat()
                }
                val spec = getFiniteSpec(specValue, spring<Float>(stiffness = Spring.StiffnessMediumLow))
                return LuaEnterTransition(fadeIn(animationSpec = spec, initialAlpha = initialAlpha))
            }
        })
        
        luaTable.set("fadeOut", object : org.luaj.lib.VarArgFunction() {
            override fun invoke(args: org.luaj.Varargs): org.luaj.Varargs {
                var specValue: LuaValue? = null
                var targetAlpha = 0f
                val arg1 = args.arg1()
                if (arg1.istable() && arg1.get("animationSpec") != LuaValue.NIL) {
                    specValue = arg1.get("animationSpec")
                    targetAlpha = arg1.get("targetAlpha").optdouble(0.0).toFloat()
                } else if (arg1.istable() && arg1.get("type") != LuaValue.NIL) {
                    specValue = arg1
                    targetAlpha = args.arg(2).optdouble(0.0).toFloat()
                } else {
                    targetAlpha = arg1.optdouble(0.0).toFloat()
                }
                val spec = getFiniteSpec(specValue, spring<Float>(stiffness = Spring.StiffnessMediumLow))
                return LuaExitTransition(fadeOut(animationSpec = spec, targetAlpha = targetAlpha))
            }
        })
        
        luaTable.set("expandIn", object : org.luaj.lib.VarArgFunction() {
            override fun invoke(args: org.luaj.Varargs): org.luaj.Varargs {
                var specValue: LuaValue? = null
                val arg1 = args.arg1()
                if (arg1.istable() && arg1.get("animationSpec") != LuaValue.NIL) {
                    specValue = arg1.get("animationSpec")
                } else if (arg1.istable() && arg1.get("type") != LuaValue.NIL) {
                    specValue = arg1
                }
                val spec = getFiniteSpec(specValue, spring<androidx.compose.ui.unit.IntSize>(stiffness = Spring.StiffnessMediumLow))
                return LuaEnterTransition(expandIn(animationSpec = spec))
            }
        })
        
        luaTable.set("shrinkOut", object : org.luaj.lib.VarArgFunction() {
            override fun invoke(args: org.luaj.Varargs): org.luaj.Varargs {
                var specValue: LuaValue? = null
                val arg1 = args.arg1()
                if (arg1.istable() && arg1.get("animationSpec") != LuaValue.NIL) {
                    specValue = arg1.get("animationSpec")
                } else if (arg1.istable() && arg1.get("type") != LuaValue.NIL) {
                    specValue = arg1
                }
                val spec = getFiniteSpec(specValue, spring<androidx.compose.ui.unit.IntSize>(stiffness = Spring.StiffnessMediumLow))
                return LuaExitTransition(shrinkOut(animationSpec = spec))
            }
        })
        
        luaTable.set("slideInHorizontally", object : org.luaj.lib.VarArgFunction() {
            override fun invoke(args: org.luaj.Varargs): org.luaj.Varargs {
                var specValue: LuaValue? = null
                var initialOffsetX: LuaFunction? = null
                val arg1 = args.arg1()
                if (arg1.istable() && arg1.get("animationSpec") != LuaValue.NIL) {
                    specValue = arg1.get("animationSpec")
                    val fn = arg1.get("initialOffsetX")
                    if (fn.isfunction()) initialOffsetX = fn.checkfunction()
                } else if (arg1.istable() && arg1.get("type") != LuaValue.NIL) {
                    specValue = arg1
                    val fn = args.arg(2)
                    if (fn.isfunction()) initialOffsetX = fn.checkfunction()
                } else {
                    if (arg1.isfunction()) initialOffsetX = arg1.checkfunction()
                }
                val spec = getFiniteSpec(specValue, spring<IntOffset>(stiffness = Spring.StiffnessMediumLow, visibilityThreshold = IntOffset.VisibilityThreshold))
                val offsetFunc: (Int) -> Int = { fullWidth ->
                    if (initialOffsetX != null) {
                        initialOffsetX.call(LuaValue.valueOf(fullWidth)).toint()
                    } else {
                        -fullWidth / 2
                    }
                }
                return LuaEnterTransition(slideInHorizontally(animationSpec = spec, initialOffsetX = offsetFunc))
            }
        })
        
        luaTable.set("slideOutHorizontally", object : org.luaj.lib.VarArgFunction() {
            override fun invoke(args: org.luaj.Varargs): org.luaj.Varargs {
                var specValue: LuaValue? = null
                var targetOffsetX: LuaFunction? = null
                val arg1 = args.arg1()
                if (arg1.istable() && arg1.get("animationSpec") != LuaValue.NIL) {
                    specValue = arg1.get("animationSpec")
                    val fn = arg1.get("targetOffsetX")
                    if (fn.isfunction()) targetOffsetX = fn.checkfunction()
                } else if (arg1.istable() && arg1.get("type") != LuaValue.NIL) {
                    specValue = arg1
                    val fn = args.arg(2)
                    if (fn.isfunction()) targetOffsetX = fn.checkfunction()
                } else {
                    if (arg1.isfunction()) targetOffsetX = arg1.checkfunction()
                }
                val spec = getFiniteSpec(specValue, spring<IntOffset>(stiffness = Spring.StiffnessMediumLow, visibilityThreshold = IntOffset.VisibilityThreshold))
                val offsetFunc: (Int) -> Int = { fullWidth ->
                    if (targetOffsetX != null) {
                        targetOffsetX.call(LuaValue.valueOf(fullWidth)).toint()
                    } else {
                        -fullWidth / 2
                    }
                }
                return LuaExitTransition(slideOutHorizontally(animationSpec = spec, targetOffsetX = offsetFunc))
            }
        })
        
        luaTable.set("slideInVertically", object : org.luaj.lib.VarArgFunction() {
            override fun invoke(args: org.luaj.Varargs): org.luaj.Varargs {
                var specValue: LuaValue? = null
                var initialOffsetY: LuaFunction? = null
                val arg1 = args.arg1()
                if (arg1.istable() && arg1.get("animationSpec") != LuaValue.NIL) {
                    specValue = arg1.get("animationSpec")
                    val fn = arg1.get("initialOffsetY")
                    if (fn.isfunction()) initialOffsetY = fn.checkfunction()
                } else if (arg1.istable() && arg1.get("type") != LuaValue.NIL) {
                    specValue = arg1
                    val fn = args.arg(2)
                    if (fn.isfunction()) initialOffsetY = fn.checkfunction()
                } else {
                    if (arg1.isfunction()) initialOffsetY = arg1.checkfunction()
                }
                val spec = getFiniteSpec(specValue, spring<IntOffset>(stiffness = Spring.StiffnessMediumLow, visibilityThreshold = IntOffset.VisibilityThreshold))
                val offsetFunc: (Int) -> Int = { fullHeight ->
                    if (initialOffsetY != null) {
                        initialOffsetY.call(LuaValue.valueOf(fullHeight)).toint()
                    } else {
                        -fullHeight / 2
                    }
                }
                return LuaEnterTransition(slideInVertically(animationSpec = spec, initialOffsetY = offsetFunc))
            }
        })
        
        luaTable.set("slideOutVertically", object : org.luaj.lib.VarArgFunction() {
            override fun invoke(args: org.luaj.Varargs): org.luaj.Varargs {
                var specValue: LuaValue? = null
                var targetOffsetY: LuaFunction? = null
                val arg1 = args.arg1()
                if (arg1.istable() && arg1.get("animationSpec") != LuaValue.NIL) {
                    specValue = arg1.get("animationSpec")
                    val fn = arg1.get("targetOffsetY")
                    if (fn.isfunction()) targetOffsetY = fn.checkfunction()
                } else if (arg1.istable() && arg1.get("type") != LuaValue.NIL) {
                    specValue = arg1
                    val fn = args.arg(2)
                    if (fn.isfunction()) targetOffsetY = fn.checkfunction()
                } else {
                    if (arg1.isfunction()) targetOffsetY = arg1.checkfunction()
                }
                val spec = getFiniteSpec(specValue, spring<IntOffset>(stiffness = Spring.StiffnessMediumLow, visibilityThreshold = IntOffset.VisibilityThreshold))
                val offsetFunc: (Int) -> Int = { fullHeight ->
                    if (targetOffsetY != null) {
                        targetOffsetY.call(LuaValue.valueOf(fullHeight)).toint()
                    } else {
                        -fullHeight / 2
                    }
                }
                return LuaExitTransition(slideOutVertically(animationSpec = spec, targetOffsetY = offsetFunc))
            }
        })

        luaTable.set("scaleIn", object : org.luaj.lib.VarArgFunction() {
            override fun invoke(args: org.luaj.Varargs): org.luaj.Varargs {
                var specValue: LuaValue? = null
                var initialScale = 0f
                var transformOrigin = androidx.compose.ui.graphics.TransformOrigin.Center
                val arg1 = args.arg1()
                if (arg1.istable() && arg1.get("animationSpec") != LuaValue.NIL) {
                    specValue = arg1.get("animationSpec")
                    initialScale = arg1.get("initialScale").optdouble(0.0).toFloat()
                    val toVal = arg1.get("transformOrigin")
                    if (toVal.isuserdata()) {
                        transformOrigin = toVal.checkuserdata() as androidx.compose.ui.graphics.TransformOrigin
                    }
                } else if (arg1.istable() && arg1.get("type") != LuaValue.NIL) {
                    specValue = arg1
                    initialScale = args.arg(2).optdouble(0.0).toFloat()
                    val toVal = args.arg(3)
                    if (toVal.isuserdata()) {
                        transformOrigin = toVal.checkuserdata() as androidx.compose.ui.graphics.TransformOrigin
                    }
                } else {
                    initialScale = arg1.optdouble(0.0).toFloat()
                    val toVal = args.arg(2)
                    if (toVal.isuserdata()) {
                        transformOrigin = toVal.checkuserdata() as androidx.compose.ui.graphics.TransformOrigin
                    }
                }
                val spec = getFiniteSpec(specValue, spring<Float>(stiffness = Spring.StiffnessMediumLow))
                return LuaEnterTransition(scaleIn(animationSpec = spec, initialScale = initialScale, transformOrigin = transformOrigin))
            }
        })
        
        luaTable.set("scaleOut", object : org.luaj.lib.VarArgFunction() {
            override fun invoke(args: org.luaj.Varargs): org.luaj.Varargs {
                var specValue: LuaValue? = null
                var targetScale = 0f
                var transformOrigin = androidx.compose.ui.graphics.TransformOrigin.Center
                val arg1 = args.arg1()
                if (arg1.istable() && arg1.get("animationSpec") != LuaValue.NIL) {
                    specValue = arg1.get("animationSpec")
                    targetScale = arg1.get("targetScale").optdouble(0.0).toFloat()
                    val toVal = arg1.get("transformOrigin")
                    if (toVal.isuserdata()) {
                        transformOrigin = toVal.checkuserdata() as androidx.compose.ui.graphics.TransformOrigin
                    }
                } else if (arg1.istable() && arg1.get("type") != LuaValue.NIL) {
                    specValue = arg1
                    targetScale = args.arg(2).optdouble(0.0).toFloat()
                    val toVal = args.arg(3)
                    if (toVal.isuserdata()) {
                        transformOrigin = toVal.checkuserdata() as androidx.compose.ui.graphics.TransformOrigin
                    }
                } else {
                    targetScale = arg1.optdouble(0.0).toFloat()
                    val toVal = args.arg(2)
                    if (toVal.isuserdata()) {
                        transformOrigin = toVal.checkuserdata() as androidx.compose.ui.graphics.TransformOrigin
                    }
                }
                val spec = getFiniteSpec(specValue, spring<Float>(stiffness = Spring.StiffnessMediumLow))
                return LuaExitTransition(scaleOut(animationSpec = spec, targetScale = targetScale, transformOrigin = transformOrigin))
            }
        })

        // AsState animators
        luaTable.set("animateFloatAsState", object : org.luaj.lib.VarArgFunction() {
            override fun invoke(args: org.luaj.Varargs): org.luaj.Varargs {
                val (targetValue, spec) = resolveAnimationArgs(args)
                val target = targetValue.tofloat()
                val activeScope = LuaBridge.getActiveScope()
                if (activeScope != null) {
                    val initFunc = object : org.luaj.lib.ZeroArgFunction() {
                        override fun call(): org.luaj.LuaValue {
                            val state = LuaAnimatableState(target, kotlin.Float.VectorConverter, activeScope)
                            return LuaStateTable(state)
                        }
                    }
                    val animState = activeScope.get("remember").call(activeScope, initFunc)
                    val javaState = (animState as LuaStateTable).javaState as LuaAnimatableState<Float, AnimationVector1D>
                    if (spec.istable()) {
                        javaState.currentSpec = parseAnimationSpec<Float>(spec.checktable()) as AnimationSpec<Float>
                    }
                    javaState.animateTo(target)
                    return animState
                }
                return org.luaj.LuaValue.NIL
            }
        })

        luaTable.set("animateIntAsState", object : org.luaj.lib.VarArgFunction() {
            override fun invoke(args: org.luaj.Varargs): org.luaj.Varargs {
                val (targetValue, spec) = resolveAnimationArgs(args)
                val target = targetValue.toint()
                val activeScope = LuaBridge.getActiveScope()
                if (activeScope != null) {
                    val initFunc = object : org.luaj.lib.ZeroArgFunction() {
                        override fun call(): org.luaj.LuaValue {
                            val state = LuaAnimatableState(target, kotlin.Int.VectorConverter, activeScope)
                            return LuaStateTable(state)
                        }
                    }
                    val animState = activeScope.get("remember").call(activeScope, initFunc)
                    val javaState = (animState as LuaStateTable).javaState as LuaAnimatableState<Int, AnimationVector1D>
                    if (spec.istable()) {
                        javaState.currentSpec = parseAnimationSpec<Int>(spec.checktable()) as AnimationSpec<Int>
                    }
                    javaState.animateTo(target)
                    return animState
                }
                return org.luaj.LuaValue.NIL
            }
        })

        luaTable.set("animateDpAsState", object : org.luaj.lib.VarArgFunction() {
            override fun invoke(args: org.luaj.Varargs): org.luaj.Varargs {
                val (targetValue, spec) = resolveAnimationArgs(args)
                val target = resolveDp(LuaBridge.luaValueToJava(targetValue))
                val activeScope = LuaBridge.getActiveScope()
                if (activeScope != null) {
                    val initFunc = object : org.luaj.lib.ZeroArgFunction() {
                        override fun call(): org.luaj.LuaValue {
                            val state = LuaAnimatableState(target, androidx.compose.ui.unit.Dp.VectorConverter, activeScope)
                            return LuaStateTable(state)
                        }
                    }
                    val animState = activeScope.get("remember").call(activeScope, initFunc)
                    val javaState = (animState as LuaStateTable).javaState as LuaAnimatableState<androidx.compose.ui.unit.Dp, AnimationVector1D>
                    if (spec.istable()) {
                        javaState.currentSpec = parseAnimationSpec<androidx.compose.ui.unit.Dp>(spec.checktable()) as AnimationSpec<androidx.compose.ui.unit.Dp>
                    }
                    javaState.animateTo(target)
                    return animState
                }
                return org.luaj.LuaValue.NIL
            }
        })

        
        luaTable.set("togetherWith", object : org.luaj.lib.TwoArgFunction() {
            override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
                var enter: androidx.compose.animation.EnterTransition = androidx.compose.animation.fadeIn()
                var exit: androidx.compose.animation.ExitTransition = androidx.compose.animation.fadeOut()
                
                if (arg1 is LuaEnterTransition) enter = arg1.transition
                else if (arg1.isuserdata() && arg1.checkuserdata() is androidx.compose.animation.EnterTransition) enter = arg1.checkuserdata() as androidx.compose.animation.EnterTransition
                
                if (arg2 is LuaExitTransition) exit = arg2.transition
                else if (arg2.isuserdata() && arg2.checkuserdata() is androidx.compose.animation.ExitTransition) exit = arg2.checkuserdata() as androidx.compose.animation.ExitTransition
                
                return LuaContentTransform(enter.togetherWith(exit))
            }
        })

luaTable.set("animateColorAsState", object : org.luaj.lib.VarArgFunction() {
            override fun invoke(args: org.luaj.Varargs): org.luaj.Varargs {
                val (targetValue, spec) = resolveAnimationArgs(args)
                val target = resolveColor(LuaBridge.luaValueToJava(targetValue))
                val activeScope = LuaBridge.getActiveScope()
                if (activeScope != null) {
                    val initFunc = object : org.luaj.lib.ZeroArgFunction() {
                        override fun call(): org.luaj.LuaValue {
                            val state = LuaAnimatableState(target, androidx.compose.ui.graphics.Color.VectorConverter(target.colorSpace), activeScope)
                            return LuaStateTable(state)
                        }
                    }
                    val animState = activeScope.get("remember").call(activeScope, initFunc)
                    val javaState = (animState as LuaStateTable).javaState as LuaAnimatableState<androidx.compose.ui.graphics.Color, AnimationVector4D>
                    if (spec.istable()) {
                        javaState.currentSpec = parseAnimationSpec<androidx.compose.ui.graphics.Color>(spec.checktable()) as AnimationSpec<androidx.compose.ui.graphics.Color>
                    }
                    javaState.animateTo(target)
                    return animState
                }
                return org.luaj.LuaValue.NIL
            }
        })


        // ------------------ Spring ------------------
        val springTable = LuaTable()
        springTable.set("StiffnessHigh", LuaBridge.javaToLuaValue(Spring.StiffnessHigh))
        springTable.set("StiffnessMedium", LuaBridge.javaToLuaValue(Spring.StiffnessMedium))
        springTable.set("StiffnessMediumLow", LuaBridge.javaToLuaValue(Spring.StiffnessMediumLow))
        springTable.set("StiffnessLow", LuaBridge.javaToLuaValue(Spring.StiffnessLow))
        springTable.set("StiffnessVeryLow", LuaBridge.javaToLuaValue(Spring.StiffnessVeryLow))
        springTable.set("DampingRatioHighBouncy", LuaBridge.javaToLuaValue(Spring.DampingRatioHighBouncy))
        springTable.set("DampingRatioMediumBouncy", LuaBridge.javaToLuaValue(Spring.DampingRatioMediumBouncy))
        springTable.set("DampingRatioLowBouncy", LuaBridge.javaToLuaValue(Spring.DampingRatioLowBouncy))
        springTable.set("DampingRatioNoBouncy", LuaBridge.javaToLuaValue(Spring.DampingRatioNoBouncy))
        springTable.set("DefaultDisplacementThreshold", LuaBridge.javaToLuaValue(Spring.DefaultDisplacementThreshold))


        luaTable.set("Spring",springTable)




    }
}

fun <T> parseAnimationSpec(table: LuaTable): AnimationSpec<T> {
    val type = table.get("type").optjstring("spring")
    return when (type) {
        "tween" -> {
            val duration = table.get("durationMillis").optint(table.get("duration").optint(300))
            val delay = table.get("delayMillis").optint(table.get("delay").optint(0))
            val easingStr = table.get("easing").optjstring("FastOutSlowIn")
            val easing = when (easingStr) {
                "Linear" -> LinearEasing
                "FastOutLinearIn" -> FastOutLinearInEasing
                "LinearOutSlowIn" -> LinearOutSlowInEasing
                else -> FastOutSlowInEasing
            }
            tween(durationMillis = duration, delayMillis = delay, easing = easing)
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
