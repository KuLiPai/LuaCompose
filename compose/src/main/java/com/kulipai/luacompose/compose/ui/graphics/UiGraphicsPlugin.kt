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
        // -------------- Color ------------------
                val colorMethods = ComposeBridge.engine.createTable()
                colorMethods.set("luminance", ComposeBridge.engine.createFunction { args ->
                    val self = args[0]
                    val colorJava = self.asTable().get("_javaColor").asUserdata() as Color
                    ComposeBridge.javaToScript(colorJava.luminance())
                })

                ComposeBridge.converters[Color::class.java] = { obj ->
                    val color = obj as Color
                    val instance = ComposeBridge.engine.createTable()
                    instance.set("_javaColor", ComposeBridge.engine.createUserdata(color))
                    
                    val newMeta = ComposeBridge.engine.createTable()
                    newMeta.set("__index", ComposeBridge.engine.createFunction { innerArgs ->
                        val key = innerArgs[1]
                        val custom = colorMethods.get(key)
                        if (!custom.isNil()) return@createFunction custom
                        ComposeBridge.engine.createNil()
                    })
                    instance.setMetatable(newMeta)
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
                val _G = com.kulipai.luacompose.compose.LuaComposeLib.globalEnv
        if (_G != null) {
            _G.set("StrokeCap", strokeCapTable)
            _G.set("Stroke", strokeObj)
        }
        scriptTable.set("StrokeCap", strokeCapTable)
        scriptTable.set("Stroke", strokeObj)

        // ------------------- Global DrawScope utilities ---------------
        val sizeGlobalMeta = ComposeBridge.engine.createTable()
        sizeGlobalMeta.set("__index", ComposeBridge.engine.createFunction { args ->
            val key = args[1].toStringValue()
            val scope = ComposeBridge.getActiveDrawScope() ?: return@createFunction ComposeBridge.engine.createNil()
            if (key == "width") return@createFunction ComposeBridge.engine.createValue(scope.size.width.toDouble())
            if (key == "height") return@createFunction ComposeBridge.engine.createValue(scope.size.height.toDouble())
            ComposeBridge.engine.createNil()
        })
        val sizeGlobal = ComposeBridge.engine.createTable()
        sizeGlobal.setMetatable(sizeGlobalMeta)
        scriptTable.set("size", sizeGlobal)

        val rotateFunc = ComposeBridge.engine.createFunction { args ->
            val scope = ComposeBridge.getActiveDrawScope() ?: return@createFunction ComposeBridge.engine.createNil()
            val map = args[0].asTable()
            val degreesVal = map.get("degrees")
            val degrees = if (!degreesVal.isNil()) degreesVal.toFloat() else 0f
            val pivotXVal = map.get("pivotX")
            val pivotYVal = map.get("pivotY")
            val pivotX = if (pivotXVal != null && !pivotXVal.isNil()) pivotXVal.toFloat() else scope.center.x
            val pivotY = if (pivotYVal != null && !pivotYVal.isNil()) pivotYVal.toFloat() else scope.center.y
            val block = map.get("block") as? ScriptFunction
            if (block != null) {
                scope.withTransform({
                    rotate(degrees, androidx.compose.ui.geometry.Offset(pivotX, pivotY))
                }) {
                    block.call()
                }
            }
            ComposeBridge.engine.createNil()
        }
        scriptTable.set("rotate", rotateFunc)

        val drawArcFunc = ComposeBridge.engine.createFunction { args ->
            val scope = ComposeBridge.getActiveDrawScope() ?: return@createFunction ComposeBridge.engine.createNil()
            val map = args[0].asTable()
            
            val colorVal = map.get("color")
            val color = if (colorVal != null && !colorVal.isNil()) {
                val javaColor = ComposeBridge.unwrapAny(colorVal) as? Color
                javaColor ?: Color.Black
            } else Color.Black
            
            val startAngle = map.get("startAngle")?.let { if (!it.isNil()) it.toFloat() else 0f } ?: 0f
            val sweepAngle = map.get("sweepAngle")?.let { if (!it.isNil()) it.toFloat() else 0f } ?: 0f
            val useCenter = map.get("useCenter")?.let { if (!it.isNil()) it.toBoolean() else false } ?: false
            
            val topLeftVal = map.get("topLeft")
            val topLeft = if (topLeftVal != null && !topLeftVal.isNil()) {
                topLeftVal.asTable().get("_javaOffset").asUserdata() as androidx.compose.ui.geometry.Offset
            } else androidx.compose.ui.geometry.Offset.Zero
            
            val sizeVal = map.get("size")
            val size = if (sizeVal != null && !sizeVal.isNil()) {
                sizeVal.asTable().get("_javaSize").asUserdata() as androidx.compose.ui.geometry.Size
            } else androidx.compose.ui.geometry.Size(scope.size.width - topLeft.x, scope.size.height - topLeft.y)
            
            val styleVal = map.get("style")
            val style = if (styleVal != null && !styleVal.isNil()) {
                styleVal.asTable().get("_javaStroke").asUserdata() as androidx.compose.ui.graphics.drawscope.DrawStyle
            } else androidx.compose.ui.graphics.drawscope.Fill
            
            scope.drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = useCenter,
                topLeft = topLeft,
                size = size,
                style = style
            )
            ComposeBridge.engine.createNil()
        }
        
        scriptTable.set("drawArc", drawArcFunc)
        if (_G != null) {
            _G.set("size", sizeGlobal)
            _G.set("rotate", rotateFunc)
            _G.set("drawArc", drawArcFunc)
        }
    }
}
