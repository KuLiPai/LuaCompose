package com.kulipai.luacompose.compose.ui.graphics

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.rotate
import com.kulipai.luacompose.compose.runtime.ComposeBridge
import com.kulipai.luacompose.compose.createComposeDrawScope
import com.kulipai.luacompose.compose.runtime.ComposeScope
import com.kulipai.luacompose.compose.runtime.ComposeScriptPlugin
import com.kulipai.luacompose.compose.ui.resolveModifier
import com.kulipai.luacompose.compose.script.ScriptFunction
import com.kulipai.luacompose.compose.script.ScriptTable
import com.kulipai.luacompose.compose.script.ScriptValue

class UiGraphicsPlugin : ComposeScriptPlugin {
    override val namespace: String = "ui.graphics"

    override fun getComponents(): Map<String, @Composable (props: Map<String, Any?>, childScope: ComposeScope?) -> Unit> {
        val map = mutableMapOf<String, @Composable (props: Map<String, Any?>, childScope: ComposeScope?) -> Unit>()
        

        
        return map
    }

    override fun injectGlobals(scriptTable: ScriptTable) {
        // -------------- Path ------------------
        ComposeBridge.converters[androidx.compose.ui.graphics.Path::class.java] = { obj ->
            val path = obj as androidx.compose.ui.graphics.Path
            val instance = ComposeBridge.engine.createTable()
            instance.set("_javaPath", ComposeBridge.engine.createUserdata(path))
            
            instance.set("moveTo", ComposeBridge.engine.createFunction { innerArgs ->
                val startIdx = if (innerArgs.size > 0 && innerArgs[0].isTable() && !innerArgs[0].asTable().get("_javaPath").isNil()) 1 else 0
                path.moveTo(innerArgs[startIdx].toFloat(), innerArgs[startIdx + 1].toFloat())
                instance
            })
            
            instance.set("lineTo", ComposeBridge.engine.createFunction { innerArgs ->
                val startIdx = if (innerArgs.size > 0 && innerArgs[0].isTable() && !innerArgs[0].asTable().get("_javaPath").isNil()) 1 else 0
                path.lineTo(innerArgs[startIdx].toFloat(), innerArgs[startIdx + 1].toFloat())
                instance
            })
            
            instance.set("close", ComposeBridge.engine.createFunction { innerArgs ->
                path.close()
                instance
            })
            instance
        }

        val pathCompanionTable = ComposeBridge.engine.createTable()
        val pathTableMeta = ComposeBridge.engine.createTable()
        pathTableMeta.set("__call", ComposeBridge.engine.createFunction { args ->
            val path = androidx.compose.ui.graphics.Path()
            ComposeBridge.javaToScript(path)
        })
        pathCompanionTable.setMetatable(pathTableMeta)
        scriptTable.set("Path", pathCompanionTable)
        // -------------- Color ------------------
        ComposeBridge.converters[Color::class.java] = { obj ->
            val color = obj as Color
            val instance = ComposeBridge.engine.createTable()
            instance.set("_javaColor", ComposeBridge.engine.createUserdata(color))
            
            instance.set("luminance", ComposeBridge.engine.createFunction { innerArgs ->
                ComposeBridge.javaToScript(color.luminance())
            })
            instance.set("copy", ComposeBridge.engine.createFunction { innerArgs ->
                val startIdx = if (innerArgs.isNotEmpty() && innerArgs[0].isTable() && !innerArgs[0].asTable().get("_javaColor").isNil()) 1 else 0
                val params = innerArgs.getOrNull(startIdx)
                val alpha = if (params != null && params.isTable()) {
                    val alphaVal = params.asTable().get("alpha")
                    if (!alphaVal.isNil()) alphaVal.toFloat() else color.alpha
                } else if (params != null && !params.isNil()) {
                    params.toFloat()
                } else {
                    color.alpha
                }
                ComposeBridge.javaToScript(color.copy(alpha = alpha))
            })
            instance
        }

                val colorCompanionTable = ComposeBridge.engine.createTable()
                colorCompanionTable.set("Black", ComposeBridge.javaToScript(Color.Black))
                colorCompanionTable.set("DarkGray", ComposeBridge.javaToScript(Color.DarkGray))
                colorCompanionTable.set("Gray", ComposeBridge.javaToScript(Color.Gray))
                colorCompanionTable.set("LightGray", ComposeBridge.javaToScript(Color.LightGray))
                colorCompanionTable.set("White", ComposeBridge.javaToScript(Color.White))
                colorCompanionTable.set("Red", ComposeBridge.javaToScript(Color.Red))
                colorCompanionTable.set("Green", ComposeBridge.javaToScript(Color.Green))
                colorCompanionTable.set("Blue", ComposeBridge.javaToScript(Color.Blue))
                colorCompanionTable.set("Yellow", ComposeBridge.javaToScript(Color.Yellow))
                colorCompanionTable.set("Cyan", ComposeBridge.javaToScript(Color.Cyan))
                colorCompanionTable.set("Magenta", ComposeBridge.javaToScript(Color.Magenta))
                colorCompanionTable.set("Transparent", ComposeBridge.javaToScript(Color.Transparent))
                colorCompanionTable.set("Unspecified", ComposeBridge.javaToScript(Color.Unspecified))
        
                val colorTableMeta = ComposeBridge.engine.createTable()
                colorTableMeta.set("__index", ComposeBridge.engine.createFunction { args ->
                    val key = args[1]
                    colorCompanionTable.get(key.toStringValue())
                })
                colorTableMeta.set("__call", ComposeBridge.engine.createFunction { args ->
                    val params = args[1]
                    val colorInt = params.toDouble().toLong().toInt()
                    ComposeBridge.javaToScript(Color(colorInt))
                })
                val colorTable = ComposeBridge.engine.createTable()
                colorTable.setMetatable(colorTableMeta)
                scriptTable.set("Color", colorTable)
        
                
        // ------------------- TransformOrigin ---------------
                val transformOriginMethods = ComposeBridge.engine.createTable()
                transformOriginMethods.set("copy", ComposeBridge.engine.createFunction { args ->
                    val self = args[0].asUserdata() as androidx.compose.ui.graphics.TransformOrigin
                    val pX = args.getOrNull(1)
                    val pY = args.getOrNull(2)
                    val pivotFractionX = if (pX != null && !pX.isNil()) pX.toFloat() else self.pivotFractionX
                    val pivotFractionY = if (pY != null && !pY.isNil()) pY.toFloat() else self.pivotFractionY
                    
                    val newInstance = ComposeBridge.engine.createTable()
                    newInstance.set("_javaTransform", ComposeBridge.engine.createUserdata(androidx.compose.ui.graphics.TransformOrigin(pivotFractionX, pivotFractionY)))
                    newInstance.setMetatable(args[0].asTable().getMetatable()!!)
                    newInstance
                })
                
                fun wrapTransformOrigin(to: androidx.compose.ui.graphics.TransformOrigin): ScriptValue {
                    val instance = ComposeBridge.engine.createTable()
                    instance.set("_javaTransform", ComposeBridge.engine.createUserdata(to))
                    val newMeta = ComposeBridge.engine.createTable()
                    newMeta.set("__index", ComposeBridge.engine.createFunction { args ->
                        val obj = args[0]
                        val key = args[1]
                        val realObj = obj.asTable().get("_javaTransform").asUserdata() as androidx.compose.ui.graphics.TransformOrigin
                        val k = key.toStringValue()
                        if (k == "pivotFractionX") return@createFunction ComposeBridge.engine.createValue(realObj.pivotFractionX.toDouble())
                        if (k == "pivotFractionY") return@createFunction ComposeBridge.engine.createValue(realObj.pivotFractionY.toDouble())
                        
                        val custom = transformOriginMethods.get(key)
                        if (!custom.isNil()) return@createFunction custom
                        ComposeBridge.engine.createNil()
                    })
                    instance.setMetatable(newMeta)
                    return instance
                }
                
                val transformOriginMeta = ComposeBridge.engine.createTable()
                transformOriginMeta.set("__index", ComposeBridge.engine.createFunction { args ->
                    val key = args[1]
                    if (key.toStringValue() == "Center") {
                        wrapTransformOrigin(androidx.compose.ui.graphics.TransformOrigin.Center)
                    } else {
                        ComposeBridge.engine.createNil()
                    }
                })
                transformOriginMeta.set("__call", ComposeBridge.engine.createFunction { args ->
                    val pX = args.getOrNull(1)
                    val pY = args.getOrNull(2)
                    val pivotFractionX = if (pX != null && !pX.isNil()) pX.toFloat() else 0.5f
                    val pivotFractionY = if (pY != null && !pY.isNil()) pY.toFloat() else 0.5f
                    wrapTransformOrigin(androidx.compose.ui.graphics.TransformOrigin(pivotFractionX, pivotFractionY))
                })
                
                val transformOriginTable = ComposeBridge.engine.createTable()
                transformOriginTable.setMetatable(transformOriginMeta)
                scriptTable.set("TransformOrigin", transformOriginTable)
            
                val strokeCapTable = ComposeBridge.engine.createTable()
                strokeCapTable.set("Butt", ComposeBridge.engine.createUserdata(StrokeCap.Butt))
                strokeCapTable.set("Round", ComposeBridge.engine.createUserdata(StrokeCap.Round))
                strokeCapTable.set("Square", ComposeBridge.engine.createUserdata(StrokeCap.Square))
                scriptTable.set("StrokeCap", strokeCapTable)

                val strokeMeta = ComposeBridge.engine.createTable()
                strokeMeta.set("__call", ComposeBridge.engine.createFunction { args ->
                    val params = args.getOrNull(1)
                    var width = 0.0f
                    var cap = StrokeCap.Butt
                    if (params != null && params.isTable()) {
                        val pTable = params.asTable()
                        val w = pTable.get("width")
                        if (!w.isNil()) width = w.toFloat()
                        val c = pTable.get("cap")
                        if (!c.isNil()) cap = c.asUserdata() as StrokeCap
                    }
                    val table = ComposeBridge.engine.createTable()
                    table.set("_javaStroke", ComposeBridge.engine.createUserdata(Stroke(width = width, cap = cap)))
                    table
                })
                val strokeObj = ComposeBridge.engine.createTable()
                strokeObj.setMetatable(strokeMeta)
                scriptTable.set("Stroke", strokeObj)
                scriptTable.set("StrokeCap", strokeCapTable)
    }
}
