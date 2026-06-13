package com.kulipai.luacompose.compose.animation.core

import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import com.kulipai.luacompose.compose.animation.parseAnimationSpec
import com.kulipai.luacompose.compose.runtime.ComposeAnimatableState
import com.kulipai.luacompose.compose.runtime.ComposeBridge
import com.kulipai.luacompose.compose.runtime.ComposeScope
import com.kulipai.luacompose.compose.runtime.ComposeScriptPlugin
import com.kulipai.luacompose.compose.script.ScriptFunction
import com.kulipai.luacompose.compose.script.ScriptTable
import com.kulipai.luacompose.compose.script.ScriptValue

class AnimationCorePlugin : ComposeScriptPlugin {
    override val namespace: String = "animation.core"

    override fun getComponents(): Map<String, @Composable (props: Map<String, Any?>, childScope: ComposeScope?) -> Unit> {
        return emptyMap()
    }

    override fun injectGlobals(scriptTable: ScriptTable) {
        val engine = ComposeBridge.engine

        scriptTable.set("tween", engine.createFunction { args ->
            var duration = 300
            var delay = 0
            var easingStr = "FastOutSlowIn"

            val arg1 = args.getOrNull(0) ?: engine.createNil()
            if (arg1.isTable()) {
                val t = arg1.asTable()
                val dur = t.get("durationMillis")
                val dur2 = t.get("duration")
                duration = if (!dur.isNil()) dur.toInt() else if (!dur2.isNil()) dur2.toInt() else 300
                val del = t.get("delayMillis")
                val del2 = t.get("delay")
                delay = if (!del.isNil()) del.toInt() else if (!del2.isNil()) del2.toInt() else 0
                val eas = t.get("easing")
                easingStr = if (!eas.isNil()) eas.toStringValue() else "FastOutSlowIn"
            } else {
                val dur = args.getOrNull(0)
                val del = args.getOrNull(1)
                val eas = args.getOrNull(2)
                duration = if (dur != null && !dur.isNil()) dur.toInt() else 300
                delay = if (del != null && !del.isNil()) del.toInt() else 0
                easingStr = if (eas != null && !eas.isNil()) eas.toStringValue() else "FastOutSlowIn"
            }

            val table = engine.createTable()
            table.set("type", engine.createValue("tween"))
            table.set("duration", engine.createValue(duration))
            table.set("delay", engine.createValue(delay))
            table.set("easing", engine.createValue(easingStr))
            table
        })

        scriptTable.set("spring", engine.createFunction { args ->
            var damping = Spring.DampingRatioNoBouncy
            var stiffness = Spring.StiffnessMedium

            val arg1 = args.getOrNull(0) ?: engine.createNil()
            if (arg1.isTable()) {
                val t = arg1.asTable()
                val damp = t.get("dampingRatio")
                val stiff = t.get("stiffness")
                if (!damp.isNil()) damping = damp.toFloat()
                if (!stiff.isNil()) stiffness = stiff.toFloat()
            } else {
                val damp = args.getOrNull(0)
                val stiff = args.getOrNull(1)
                if (damp != null && !damp.isNil()) damping = damp.toFloat()
                if (stiff != null && !stiff.isNil()) stiffness = stiff.toFloat()
            }

            val table = engine.createTable()
            table.set("type", engine.createValue("spring"))
            table.set("dampingRatio", engine.createValue(damping.toDouble()))
            table.set("stiffness", engine.createValue(stiffness.toDouble()))
            table
        })

        scriptTable.set("infiniteRepeatable", engine.createFunction { args ->
            val arg1 = args.getOrNull(0) ?: engine.createNil()
            var animSpec: ScriptValue = engine.createNil()
            var repeatModeStr = "Restart"
            if (arg1.isTable()) {
                val t = arg1.asTable()
                val spec = t.get("animation")
                if (!spec.isNil()) animSpec = spec
                val rm = t.get("repeatMode")
                if (!rm.isNil()) {
                    if (rm.isString()) repeatModeStr = rm.toStringValue()
                    else if (rm.isUserdata()) {
                        val ud = rm.asUserdata()
                        if (ud is RepeatMode) repeatModeStr = ud.name
                    }
                }
            } else {
                val spec = args.getOrNull(0)
                if (spec != null && !spec.isNil()) animSpec = spec
                val rm = args.getOrNull(1)
                if (rm != null && !rm.isNil()) {
                    if (rm.isString()) repeatModeStr = rm.toStringValue()
                    else if (rm.isUserdata()) {
                        val ud = rm.asUserdata()
                        if (ud is RepeatMode) repeatModeStr = ud.name
                    }
                }
            }

            val table = engine.createTable()
            table.set("type", engine.createValue("infiniteRepeatable"))
            table.set("animation", animSpec)
            table.set("repeatMode", ComposeBridge.javaToScript(if (repeatModeStr == "Reverse") RepeatMode.Reverse else RepeatMode.Restart))
            table
        })

        scriptTable.set("rememberInfiniteTransition", engine.createFunction {
            val transitionTable = engine.createTable()
            transitionTable.set("animateFloat", engine.createFunction { args ->
                val arg1 = args.getOrNull(0)
                var initialValue = 0f
                var targetValue = 0f
                var specValue: ScriptValue? = null

                if (arg1 != null && arg1.isTable()) {
                    val t = arg1.asTable()
                    val init = t.get("initialValue")
                    if (!init.isNil()) initialValue = init.toFloat()
                    val target = t.get("targetValue")
                    if (!target.isNil()) targetValue = target.toFloat()
                    val spec = t.get("animationSpec")
                    if (!spec.isNil()) specValue = spec
                } else {
                    val init = args.getOrNull(0)
                    if (init != null && !init.isNil()) initialValue = init.toFloat()
                    val target = args.getOrNull(1)
                    if (target != null && !target.isNil()) targetValue = target.toFloat()
                    val spec = args.getOrNull(2)
                    if (spec != null && !spec.isNil()) specValue = spec
                }

                val activeScope = ComposeBridge.getActiveScope()
                if (activeScope != null) {
                    val initFunc = engine.createFunction {
                        val state = ComposeAnimatableState(initialValue, kotlin.Float.VectorConverter, activeScope)
                        com.kulipai.luacompose.compose.runtime.createComposeStateTable(state)
                    }
                    val animState = activeScope.getOrCreateRemember(initFunc)
                    val javaState = (animState.asTable().get("javaState").asUserdata() as ComposeAnimatableState<Float, AnimationVector1D>)
                    if (specValue != null && specValue.isTable()) {
                        javaState.currentSpec = parseAnimationSpec<Float>(specValue.asTable()) as AnimationSpec<Float>
                    }
                    if (!javaState.animatable.isRunning) {
                        javaState.animateTo(targetValue)
                    }
                    return@createFunction animState
                }
                engine.createNil()
            })
            transitionTable
        })

        scriptTable.set("animateFloat", scriptTable.get("rememberInfiniteTransition").asFunction().call().asTable().get("animateFloat"))

        scriptTable.set("FastOutSlowInEasing", engine.createValue("FastOutSlowIn"))
        scriptTable.set("LinearEasing", engine.createValue("Linear"))
        scriptTable.set("FastOutLinearInEasing", engine.createValue("FastOutLinearIn"))
        scriptTable.set("LinearOutSlowInEasing", engine.createValue("LinearOutSlowIn"))

        val repeatModeTable = engine.createTable()
        repeatModeTable.set("Restart", ComposeBridge.javaToScript(RepeatMode.Restart))
        repeatModeTable.set("Reverse", ComposeBridge.javaToScript(RepeatMode.Reverse))
        scriptTable.set("RepeatMode", repeatModeTable)


        // ------------------ Spring ------------------
        val springTable = ComposeBridge.engine.createTable()
        springTable.set("StiffnessHigh", ComposeBridge.javaToScript(Spring.StiffnessHigh))
        springTable.set("StiffnessMedium", ComposeBridge.javaToScript(Spring.StiffnessMedium))
        springTable.set("StiffnessMediumLow", ComposeBridge.javaToScript(Spring.StiffnessMediumLow))
        springTable.set("StiffnessLow", ComposeBridge.javaToScript(Spring.StiffnessLow))
        springTable.set("StiffnessVeryLow", ComposeBridge.javaToScript(Spring.StiffnessVeryLow))
        springTable.set("DampingRatioHighBouncy", ComposeBridge.javaToScript(Spring.DampingRatioHighBouncy))
        springTable.set("DampingRatioMediumBouncy", ComposeBridge.javaToScript(Spring.DampingRatioMediumBouncy))
        springTable.set("DampingRatioLowBouncy", ComposeBridge.javaToScript(Spring.DampingRatioLowBouncy))
        springTable.set("DampingRatioNoBouncy", ComposeBridge.javaToScript(Spring.DampingRatioNoBouncy))
        springTable.set("DefaultDisplacementThreshold", ComposeBridge.javaToScript(Spring.DefaultDisplacementThreshold))


        scriptTable.set("Spring",springTable)


    }
}
