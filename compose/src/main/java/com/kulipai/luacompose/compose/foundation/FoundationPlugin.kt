package com.kulipai.luacompose.compose.foundation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kulipai.luacompose.compose.runtime.ComposeBridge
import com.kulipai.luacompose.compose.LuaComposeRegistry
import com.kulipai.luacompose.compose.createComposeDrawScope
import com.kulipai.luacompose.compose.runtime.ComposeScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.detectDragGestures
import com.kulipai.luacompose.compose.ui.graphics.ComposeScopeComponent
import com.kulipai.luacompose.compose.runtime.ComposeScriptPlugin
import com.kulipai.luacompose.compose.ui.resolveDp
import com.kulipai.luacompose.compose.ui.resolveModifier
import com.kulipai.luacompose.compose.script.ScriptFunction
import com.kulipai.luacompose.compose.script.ScriptTable
import com.kulipai.luacompose.compose.script.ScriptValue

class FoundationPlugin : ComposeScriptPlugin {
    override val namespace: String? = null

    override fun getComponents(): Map<String, @Composable (props: Map<String, Any?>, childScope: ComposeScope?) -> Unit> {
        val map =
            mutableMapOf<String, @Composable (props: Map<String, Any?>, childScope: ComposeScope?) -> Unit>()
        com.kulipai.luacompose.compose.foundation.layout.registerLayoutComponents(map)
        com.kulipai.luacompose.compose.foundation.lazy.registerLazyComponents(map)

        map["Canvas"] = { props, _ ->
            val modifier = resolveModifier(props["modifier"])
            val onDraw = props["onDraw"] as? ScriptFunction

            androidx.compose.foundation.Canvas(modifier = modifier) {
                if (onDraw != null) {
                    val luaDrawScope = createComposeDrawScope(this)
                    ComposeBridge.pushActiveDrawScope(this)
                    try {
                        onDraw.call(luaDrawScope)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        ComposeBridge.popActiveDrawScope()
                    }
                }
            }
        }

        

        

        

        

        

        

        

        return map
    }

    override fun injectGlobals(scriptTable: ScriptTable) {
        scriptTable.set("RoundedCornerShape", ComposeBridge.engine.createFunction { args ->
            val arg = args[0]
            val radius = resolveDp(ComposeBridge.scriptToJava(arg))
            ComposeBridge.javaToScript(RoundedCornerShape(radius))
        })
        scriptTable.set("CircleShape", ComposeBridge.javaToScript(CircleShape))

        scriptTable.set("Modifier", ComposeBridge.engine.createFunction { _ ->
            ComposeBridge.javaToScript(com.kulipai.luacompose.compose.ui.LuaModifier())
        })

        // -------------- Arrangement ------------------
        val arrangementTable = ComposeBridge.engine.createTable()
        arrangementTable.set("Top", ComposeBridge.javaToScript(Arrangement.Top))
        arrangementTable.set("Center", ComposeBridge.javaToScript(Arrangement.Center))
        arrangementTable.set("Start", ComposeBridge.javaToScript(Arrangement.Start))
        arrangementTable.set("End", ComposeBridge.javaToScript(Arrangement.End))
        arrangementTable.set("Bottom", ComposeBridge.javaToScript(Arrangement.Bottom))
        arrangementTable.set("SpaceAround", ComposeBridge.javaToScript(Arrangement.SpaceAround))
        arrangementTable.set("SpaceBetween", ComposeBridge.javaToScript(Arrangement.SpaceBetween))
        arrangementTable.set("SpaceEvenly", ComposeBridge.javaToScript(Arrangement.SpaceEvenly))
        arrangementTable.set("spacedBy", ComposeBridge.engine.createFunction { args ->
            val arg1 = args[0]
            var spaceObj: ScriptValue = ComposeBridge.engine.createNil()
            var alignmentObj: ScriptValue = ComposeBridge.engine.createNil()

            if (arg1.isTable()) {
                val tableArg = arg1.asTable()
                val sp = tableArg.get("space")
                spaceObj = if (!sp.isNil()) sp else tableArg.get(1)
                val al = tableArg.get("alignment")
                alignmentObj = if (!al.isNil()) al else tableArg.get(2)
            } else {
                spaceObj = arg1
                alignmentObj = args.getOrNull(1) ?: ComposeBridge.engine.createNil()
            }

            val space = resolveDp(ComposeBridge.scriptToJava(spaceObj))
            val javaAlign = ComposeBridge.scriptToJava(alignmentObj)

            if (javaAlign is androidx.compose.ui.Alignment.Horizontal) {
                ComposeBridge.javaToScript(androidx.compose.foundation.layout.Arrangement.spacedBy(space, javaAlign))
            } else if (javaAlign is androidx.compose.ui.Alignment.Vertical) {
                ComposeBridge.javaToScript(androidx.compose.foundation.layout.Arrangement.spacedBy(space, javaAlign))
            } else {
                ComposeBridge.javaToScript(androidx.compose.foundation.layout.Arrangement.spacedBy(space))
            }
        })

        arrangementTable.set("aligned", ComposeBridge.engine.createFunction { args ->
            val arg = args[0]
            val space = ComposeBridge.scriptToJava(arg)
            if (space is Alignment.Horizontal) {
                ComposeBridge.javaToScript(Arrangement.aligned(space))
            } else if (space is Alignment.Vertical) {
                ComposeBridge.javaToScript(Arrangement.aligned(space))
            } else {
                ComposeBridge.engine.createNil()
            }
        })

        scriptTable.set("Arrangement", arrangementTable)

        // -------------- Alignment ------------------
        val alignmentTable = ComposeBridge.engine.createTable()
        alignmentTable.set("TopStart", ComposeBridge.javaToScript(Alignment.TopStart))
        alignmentTable.set("TopCenter", ComposeBridge.javaToScript(Alignment.TopCenter))
        alignmentTable.set("TopEnd", ComposeBridge.javaToScript(Alignment.TopEnd))
        alignmentTable.set("CenterStart", ComposeBridge.javaToScript(Alignment.CenterStart))
        alignmentTable.set("Center", ComposeBridge.javaToScript(Alignment.Center))
        alignmentTable.set("CenterEnd", ComposeBridge.javaToScript(Alignment.CenterEnd))
        alignmentTable.set("BottomStart", ComposeBridge.javaToScript(Alignment.BottomStart))
        alignmentTable.set("BottomCenter", ComposeBridge.javaToScript(Alignment.BottomCenter))
        alignmentTable.set("BottomEnd", ComposeBridge.javaToScript(Alignment.BottomEnd))

        // 1D Alignment.Verticals.
        alignmentTable.set("Top", ComposeBridge.javaToScript(Alignment.Top))
        alignmentTable.set("CenterVertically", ComposeBridge.javaToScript(Alignment.CenterVertically))
        alignmentTable.set("Bottom", ComposeBridge.javaToScript(Alignment.Bottom))

        // 1D Alignment.Horizontals.
        alignmentTable.set("Start", ComposeBridge.javaToScript(Alignment.Start))
        alignmentTable.set("CenterHorizontally", ComposeBridge.javaToScript(Alignment.CenterHorizontally))
        alignmentTable.set("End", ComposeBridge.javaToScript(Alignment.End))

        scriptTable.set("Alignment", alignmentTable)

        // -------------- FontWeight ------------------
        val fontWeightCompanionTable = ComposeBridge.engine.createTable()
        fontWeightCompanionTable.set("W100", ComposeBridge.javaToScript(FontWeight.W100))
        fontWeightCompanionTable.set("W200", ComposeBridge.javaToScript(FontWeight.W200))
        fontWeightCompanionTable.set("W300", ComposeBridge.javaToScript(FontWeight.W300))
        fontWeightCompanionTable.set("W400", ComposeBridge.javaToScript(FontWeight.W400))
        fontWeightCompanionTable.set("W500", ComposeBridge.javaToScript(FontWeight.W500))
        fontWeightCompanionTable.set("W600", ComposeBridge.javaToScript(FontWeight.W600))
        fontWeightCompanionTable.set("W700", ComposeBridge.javaToScript(FontWeight.W700))
        fontWeightCompanionTable.set("W800", ComposeBridge.javaToScript(FontWeight.W800))
        fontWeightCompanionTable.set("W900", ComposeBridge.javaToScript(FontWeight.W900))
        fontWeightCompanionTable.set("Thin", ComposeBridge.javaToScript(FontWeight.Thin))
        fontWeightCompanionTable.set("ExtraLight", ComposeBridge.javaToScript(FontWeight.ExtraLight))
        fontWeightCompanionTable.set("Light", ComposeBridge.javaToScript(FontWeight.Light))
        fontWeightCompanionTable.set("Normal", ComposeBridge.javaToScript(FontWeight.Normal))
        fontWeightCompanionTable.set("Medium", ComposeBridge.javaToScript(FontWeight.Medium))
        fontWeightCompanionTable.set("SemiBold", ComposeBridge.javaToScript(FontWeight.SemiBold))
        fontWeightCompanionTable.set("Bold", ComposeBridge.javaToScript(FontWeight.Bold))
        fontWeightCompanionTable.set("ExtraBold", ComposeBridge.javaToScript(FontWeight.ExtraBold))
        fontWeightCompanionTable.set("Black", ComposeBridge.javaToScript(FontWeight.Black))

        val fontWeightTableMeta = ComposeBridge.engine.createTable()
        fontWeightTableMeta.set("__index", ComposeBridge.engine.createFunction { args ->
            val key = args[1]
            fontWeightCompanionTable.get(key.toStringValue())
        })
        fontWeightTableMeta.set("__call", ComposeBridge.engine.createFunction { args ->
            val params = args[1]
            ComposeBridge.javaToScript(FontWeight(params.toInt()))
        })
        val fontWeightTable = ComposeBridge.engine.createTable()
        fontWeightTable.setMetatable(fontWeightTableMeta)

        scriptTable.set("FontWeight", fontWeightTable)

        // -------------- Gestures ------------------

        val gesturesTable = ComposeBridge.engine.createTable()
        gesturesTable.set("detectDragGestures", ComposeBridge.engine.createFunction { args ->
            val actions = ComposeBridge.getActivePointerInputScopeActions() ?: return@createFunction ComposeBridge.engine.createNil()
            
            var onDrag: com.kulipai.luacompose.compose.script.ScriptFunction? = null
            var onDragEnd: com.kulipai.luacompose.compose.script.ScriptFunction? = null
            
            val arg1 = args.getOrNull(0)
            if (arg1 != null && arg1.isTable()) {
                val t = arg1.asTable()
                val od = t.get("onDrag")
                if (od.isFunction()) onDrag = od.asFunction()
                val ode = t.get("onDragEnd")
                if (ode.isFunction()) onDragEnd = ode.asFunction()
            }
            
            actions.add {
                detectDragGestures(
                    onDragEnd = {
                        if (onDragEnd != null) {
                            try {
                                onDragEnd!!.call()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    },
                    onDrag = { change: androidx.compose.ui.input.pointer.PointerInputChange, dragAmount: androidx.compose.ui.geometry.Offset ->
                        if (onDrag != null) {
                            try {
                                val changeTable = ComposeBridge.engine.createTable()
                                changeTable.set("consume", ComposeBridge.engine.createFunction { 
                                    change.consume()
                                    ComposeBridge.engine.createNil()
                                })
                                val dragAmountTable = ComposeBridge.engine.createTable()
                                dragAmountTable.set("x", ComposeBridge.engine.createValue(dragAmount.x.toDouble()))
                                dragAmountTable.set("y", ComposeBridge.engine.createValue(dragAmount.y.toDouble()))
                                onDrag!!.call(changeTable, dragAmountTable)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                )
            }
            ComposeBridge.engine.createNil()
        })
        scriptTable.set("gestures", gesturesTable)
    }
}
