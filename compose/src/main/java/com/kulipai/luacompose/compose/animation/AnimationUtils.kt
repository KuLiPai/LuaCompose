package com.kulipai.luacompose.compose.animation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.Color
import com.kulipai.luacompose.compose.runtime.ComposeAnimatableState
import com.kulipai.luacompose.compose.runtime.ComposeBridge
import com.kulipai.luacompose.compose.runtime.ComposeScope
import com.kulipai.luacompose.compose.ui.graphics.ComposeScopeComponent
import com.kulipai.luacompose.compose.runtime.ComposeScriptPlugin
import com.kulipai.luacompose.compose.ui.resolveColor
import com.kulipai.luacompose.compose.ui.resolveDp
import com.kulipai.luacompose.compose.ui.resolveModifier
import com.kulipai.luacompose.compose.script.ScriptFunction
import com.kulipai.luacompose.compose.script.ScriptTable
import com.kulipai.luacompose.compose.script.ScriptValue

fun createEnterTransition(transition: EnterTransition): ScriptTable {
    val meta = ComposeBridge.engine.createTable()
    meta.set("togetherWith", ComposeBridge.engine.createFunction { args -> 
        val self = args[0]
        val exitVal = args[1]
        val enter = self.asTable().get("_transition").asUserdata() as EnterTransition
        var exit: ExitTransition = fadeOut()
        if (exitVal.isTable() && exitVal.asTable().get("_transition").isUserdata()) {
            exit = exitVal.asTable().get("_transition").asUserdata() as ExitTransition
        } else if (exitVal.isUserdata() && exitVal.asUserdata() is ExitTransition) {
            exit = exitVal.asUserdata() as ExitTransition
        }
        createContentTransform(enter.togetherWith(exit))
    })
    meta.set("__index", meta)
    
    val table = ComposeBridge.engine.createTableWithAdd { args ->
        val arg1 = args[0]
        val arg2 = args[1]
        var t1: EnterTransition? = null
        var t2: EnterTransition? = null
        if (arg1.isTable() && arg1.asTable().get("_transition").isUserdata()) {
            t1 = arg1.asTable().get("_transition").asUserdata() as EnterTransition
        }
        if (arg2.isTable() && arg2.asTable().get("_transition").isUserdata()) {
            t2 = arg2.asTable().get("_transition").asUserdata() as EnterTransition
        }
        if (t1 != null && t2 != null) {
            createEnterTransition(t1 + t2)
        } else {
            arg1
        }
    }
    table.set("_transition", ComposeBridge.engine.createUserdata(transition))
    table.setMetatable(meta)
    return table
}

fun createExitTransition(transition: ExitTransition): ScriptTable {
    val meta = ComposeBridge.engine.createTable()
    meta.set("__index", meta)
    
    val table = ComposeBridge.engine.createTableWithAdd { args ->
        val arg1 = args[0]
        val arg2 = args[1]
        var t1: ExitTransition? = null
        var t2: ExitTransition? = null
        if (arg1.isTable() && arg1.asTable().get("_transition").isUserdata()) {
            t1 = arg1.asTable().get("_transition").asUserdata() as ExitTransition
        }
        if (arg2.isTable() && arg2.asTable().get("_transition").isUserdata()) {
            t2 = arg2.asTable().get("_transition").asUserdata() as ExitTransition
        }
        if (t1 != null && t2 != null) {
            createExitTransition(t1 + t2)
        } else {
            arg1
        }
    }
    table.set("_transition", ComposeBridge.engine.createUserdata(transition))
    table.setMetatable(meta)
    return table
}

fun createContentTransform(transform: ContentTransform): ScriptTable {
    val table = ComposeBridge.engine.createTable()
    table.set("_transform", ComposeBridge.engine.createUserdata(transform))
    return table
}

fun <T> parseAnimationSpec(table: ScriptTable): AnimationSpec<T> {
    val typeVal = table.get("type")
    val type = if (!typeVal.isNil()) typeVal.toStringValue() else "spring"
    return when (type) {
        "tween" -> {
            val dur = table.get("durationMillis")
            val dur2 = table.get("duration")
            val duration = if (!dur.isNil()) dur.toInt() else if (!dur2.isNil()) dur2.toInt() else 300
            val del = table.get("delayMillis")
            val del2 = table.get("delay")
            val delay = if (!del.isNil()) del.toInt() else if (!del2.isNil()) del2.toInt() else 0
            val eas = table.get("easing")
            val easingStr = if (!eas.isNil()) eas.toStringValue() else "FastOutSlowIn"
            val easing = when (easingStr) {
                "Linear" -> LinearEasing
                "FastOutLinearIn" -> FastOutLinearInEasing
                "LinearOutSlowIn" -> LinearOutSlowInEasing
                else -> FastOutSlowInEasing
            }
            tween(durationMillis = duration, delayMillis = delay, easing = easing)
        }
        "spring" -> {
            val damp = table.get("dampingRatio")
            val dampingRatio = if (!damp.isNil()) damp.toFloat() else Spring.DampingRatioNoBouncy.toFloat()
            val stiff = table.get("stiffness")
            val stiffness = if (!stiff.isNil()) stiff.toFloat() else Spring.StiffnessMedium.toFloat()
            spring(dampingRatio = dampingRatio, stiffness = stiffness)
        }
        else -> spring()
    }
}

