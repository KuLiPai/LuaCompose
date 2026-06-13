package com.kulipai.luacompose.compose.ui.graphics

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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
        
        map["Canvas"] = { props, _ ->
                    val modifier = resolveModifier(props["modifier"])
                    val onDraw = props["onDraw"] as? ScriptFunction
        
                    Canvas(modifier = modifier) {
                        if (onDraw != null) {
                            val luaDrawScope = createComposeDrawScope(this)
                            try {
                                onDraw.call(luaDrawScope)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
        
        return map
    }

    override fun injectGlobals(scriptTable: ScriptTable) {
        // -------------- Color ------------------
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
        
                val colorMethods = ComposeBridge.engine.createTable()
                colorMethods.set("luminance", ComposeBridge.engine.createFunction { args ->
                    val self = args[0]
                    val colorJava = self.asUserdata() as Color
                    ComposeBridge.javaToScript(colorJava.luminance())
                })
        
                val colorTableMeta = ComposeBridge.engine.createTable()
                colorTableMeta.set("__index", ComposeBridge.engine.createFunction { args ->
                    val key = args[1]
                    colorCompanionTable.get(key.toStringValue())
                })
                colorTableMeta.set("__call", ComposeBridge.engine.createFunction { args ->
                    val params = args[1]
                    val instance = ComposeBridge.engine.createTable()
                    instance.set("_javaColor", ComposeBridge.engine.createUserdata(Color(params.toInt())))
        
                    val newMeta = ComposeBridge.engine.createTable()
                    newMeta.set("__index", ComposeBridge.engine.createFunction { innerArgs ->
                        val key = innerArgs[1]
                        val custom = colorMethods.get(key)
                        if (!custom.isNil()) return@createFunction custom
                        ComposeBridge.engine.createNil()
                    })
        
                    instance.setMetatable(newMeta)
                    instance
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
            
    }
}
