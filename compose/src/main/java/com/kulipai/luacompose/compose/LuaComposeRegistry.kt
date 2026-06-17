package com.kulipai.luacompose.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import com.kulipai.luacompose.compose.animation.AnimationPlugin
import com.kulipai.luacompose.compose.foundation.FoundationPlugin
import com.kulipai.luacompose.compose.foundation.layout.FoundationLayoutPlugin
import com.kulipai.luacompose.compose.runtime.ComposeScriptPlugin
import com.kulipai.luacompose.compose.material3.Material3Plugin
import com.kulipai.luacompose.compose.runtime.ComposeScope
import com.kulipai.luacompose.compose.ui.resolveColor
import com.kulipai.luacompose.compose.script.ScriptTable
import com.kulipai.luacompose.compose.script.ScriptValue
import com.kulipai.luacompose.compose.runtime.ComposeBridge

fun createScriptPath(path: Path = Path()): ScriptTable {
    val table = ComposeBridge.engine.createTable()
    table.set("path", ComposeBridge.engine.createUserdata(path))
    
    table.set("moveTo", ComposeBridge.engine.createFunction { args ->
        path.moveTo(args[0].toFloat(), args[1].toFloat())
        table
    })
    table.set("lineTo", ComposeBridge.engine.createFunction { args ->
        path.lineTo(args[0].toFloat(), args[1].toFloat())
        table
    })
    table.set("quadraticBezierTo", ComposeBridge.engine.createFunction { args ->
        path.quadraticBezierTo(
            args[0].toFloat(), args[1].toFloat(),
            args[2].toFloat(), args[3].toFloat()
        )
        table
    })
    table.set("cubicTo", ComposeBridge.engine.createFunction { args ->
        path.cubicTo(
            args[0].toFloat(), args[1].toFloat(),
            args[2].toFloat(), args[3].toFloat(),
            args[4].toFloat(), args[5].toFloat()
        )
        table
    })
    table.set("close", ComposeBridge.engine.createFunction {
        path.close()
        table
    })
    
    return table
}

fun createComposeDrawScope(drawScope: DrawScope): ScriptTable {
    val table = ComposeBridge.engine.createTable()
    
    table.set("drawRect", ComposeBridge.engine.createFunction { args ->
        val mapArgs = args[0].asTable()
        val color = resolveColor(ComposeBridge.scriptToJava(mapArgs.get("color")))
        val xVal = mapArgs.get("x")
        val yVal = mapArgs.get("y")
        val widthVal = mapArgs.get("width")
        val heightVal = mapArgs.get("height")
        val x = if (!xVal.isNil()) xVal.toFloat() else 0f
        val y = if (!yVal.isNil()) yVal.toFloat() else 0f
        val width = if (!widthVal.isNil()) widthVal.toFloat() else 100f
        val height = if (!heightVal.isNil()) heightVal.toFloat() else 100f
        
        drawScope.drawRect(
            color = color,
            topLeft = Offset(x, y),
            size = Size(width, height)
        )
        ComposeBridge.engine.createNil()
    })
    
    table.set("drawRoundRect", ComposeBridge.engine.createFunction { args ->
        val mapArgs = args[0].asTable()
        val color = resolveColor(ComposeBridge.scriptToJava(mapArgs.get("color")))
        val xVal = mapArgs.get("x")
        val yVal = mapArgs.get("y")
        val widthVal = mapArgs.get("width")
        val heightVal = mapArgs.get("height")
        val cornerXVal = mapArgs.get("cornerRadiusX")
        val cornerYVal = mapArgs.get("cornerRadiusY")
        
        val x = if (!xVal.isNil()) xVal.toFloat() else 0f
        val y = if (!yVal.isNil()) yVal.toFloat() else 0f
        val width = if (!widthVal.isNil()) widthVal.toFloat() else 100f
        val height = if (!heightVal.isNil()) heightVal.toFloat() else 100f
        val cornerX = if (!cornerXVal.isNil()) cornerXVal.toFloat() else 0f
        val cornerY = if (!cornerYVal.isNil()) cornerYVal.toFloat() else cornerX
        
        drawScope.drawRoundRect(
            color = color,
            topLeft = Offset(x, y),
            size = Size(width, height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerX, cornerY)
        )
        ComposeBridge.engine.createNil()
    })
    
    table.set("drawCircle", ComposeBridge.engine.createFunction { args ->
        val mapArgs = args[0].asTable()
        val color = resolveColor(ComposeBridge.scriptToJava(mapArgs.get("color")))
        val radiusVal = mapArgs.get("radius")
        val centerVal = mapArgs.get("center")
        val cxVal = mapArgs.get("centerX")
        val cyVal = mapArgs.get("centerY")
        
        val radius = if (!radiusVal.isNil()) radiusVal.toFloat() else 50f
        
        var centerX = if (!cxVal.isNil()) cxVal.toFloat() else drawScope.center.x
        var centerY = if (!cyVal.isNil()) cyVal.toFloat() else drawScope.center.y
        
        if (centerVal != null && !centerVal.isNil()) {
            val offset = centerVal.asTable().get("_javaOffset").asUserdata() as Offset
            centerX = offset.x
            centerY = offset.y
        }
        
        drawScope.drawCircle(
            color = color,
            radius = radius,
            center = Offset(centerX, centerY)
        )
        ComposeBridge.engine.createNil()
    })
    
    table.set("drawPath", ComposeBridge.engine.createFunction { args ->
        val mapArgs = args[0].asTable()
        val color = resolveColor(ComposeBridge.scriptToJava(mapArgs.get("color")))
        val scriptPath = mapArgs.get("path")
        
        if (scriptPath.isTable()) {
            val pathData = scriptPath.asTable().get("_javaPath")
            if (!pathData.isNil() && pathData.isUserdata()) {
                val path = pathData.asUserdata() as androidx.compose.ui.graphics.Path
                drawScope.drawPath(
                    path = path,
                    color = color
                )
            }
        }
        ComposeBridge.engine.createNil()
    })
    
    table.set("rotate", ComposeBridge.engine.createFunction { args ->
        val map = args[0].asTable()
        val degreesVal = map.get("degrees")
        val degrees = if (!degreesVal.isNil()) degreesVal.toFloat() else 0f
        val pivotXVal = map.get("pivotX")
        val pivotYVal = map.get("pivotY")
        val pivotX = if (pivotXVal != null && !pivotXVal.isNil()) pivotXVal.toFloat() else drawScope.center.x
        val pivotY = if (pivotYVal != null && !pivotYVal.isNil()) pivotYVal.toFloat() else drawScope.center.y
        val block = map.get("block") as? com.kulipai.luacompose.compose.script.ScriptFunction
        if (block != null) {
            drawScope.withTransform({
                rotate(degrees, Offset(pivotX, pivotY))
            }) {
                block.call()
            }
        }
        ComposeBridge.engine.createNil()
    })

    table.set("drawArc", ComposeBridge.engine.createFunction { args ->
        val map = args[0].asTable()
        val colorVal = map.get("color")
        val color = com.kulipai.luacompose.compose.ui.resolveColor(ComposeBridge.scriptToJava(colorVal), androidx.compose.ui.graphics.Color.Black)
        
        val startAngle = map.get("startAngle")?.let { if (!it.isNil()) it.toFloat() else 0f } ?: 0f
        val sweepAngle = map.get("sweepAngle")?.let { if (!it.isNil()) it.toFloat() else 0f } ?: 0f
        val useCenter = map.get("useCenter")?.let { if (!it.isNil()) it.toBoolean() else false } ?: false
        
        val topLeftVal = map.get("topLeft")
        val topLeft = if (topLeftVal != null && !topLeftVal.isNil()) {
            topLeftVal.asTable().get("_javaOffset").asUserdata() as Offset
        } else Offset.Zero
        
        val sizeVal = map.get("size")
        val size = if (sizeVal != null && !sizeVal.isNil()) {
            sizeVal.asTable().get("_javaSize").asUserdata() as Size
        } else Size(drawScope.size.width - topLeft.x, drawScope.size.height - topLeft.y)
        
        val styleVal = map.get("style")
        val style = if (styleVal != null && !styleVal.isNil()) {
            styleVal.asTable().get("_javaStroke").asUserdata() as androidx.compose.ui.graphics.drawscope.DrawStyle
        } else androidx.compose.ui.graphics.drawscope.Fill
        
        drawScope.drawArc(
            color = color,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = useCenter,
            topLeft = topLeft,
            size = size,
            style = style
        )
        ComposeBridge.engine.createNil()
    })

    val sizeMeta = ComposeBridge.engine.createTable()
    sizeMeta.set("__index", ComposeBridge.engine.createFunction { args ->
        val key = args[1].toStringValue()
        if (key == "width") return@createFunction ComposeBridge.engine.createValue(drawScope.size.width.toDouble())
        if (key == "height") return@createFunction ComposeBridge.engine.createValue(drawScope.size.height.toDouble())
        ComposeBridge.engine.createNil()
    })
    val sizeTable = ComposeBridge.engine.createTable()
    sizeTable.setMetatable(sizeMeta)
    table.set("size", sizeTable)

    return table
}

object LuaComposeRegistry {
    val components =
        mutableMapOf<String, @Composable (props: Map<String, Any?>, childScope: ComposeScope?) -> Unit>()
    val plugins = mutableListOf<ComposeScriptPlugin>()

    init {
        registerPlugin(FoundationLayoutPlugin())
        registerGeneratedPlugin("com.kulipai.luacompose.generated.FoundationGeneratedPlugin")
        registerGeneratedPlugin("com.kulipai.luacompose.generated.FoundationLayoutGeneratedPlugin")
        registerPlugin(com.kulipai.luacompose.compose.foundation.FoundationPlugin())
        registerPlugin(com.kulipai.luacompose.compose.ui.graphics.UiGraphicsPlugin())
        registerPlugin(com.kulipai.luacompose.compose.material3.Material3Plugin())
        registerGeneratedPlugin("com.kulipai.luacompose.generated.Material3GeneratedPlugin")
        registerPlugin(AnimationPlugin())
        registerPlugin(com.kulipai.luacompose.compose.animation.core.AnimationCorePlugin())
        registerPlugin(com.kulipai.luacompose.compose.ui.geometry.UiGeometryPlugin())
        registerPlugin(com.kulipai.luacompose.compose.ui.unit.UiUnitPlugin())

        com.kulipai.luacompose.compose.runtime.ComposeBridge.converters[androidx.compose.ui.graphics.drawscope.DrawScope::class.java] = { scope ->
            createComposeDrawScope(scope as androidx.compose.ui.graphics.drawscope.DrawScope) 
        }
    }

    private fun registerGeneratedPlugin(className: String) {
        val pluginClass = runCatching { Class.forName(className) }.getOrNull() ?: return
        val plugin = runCatching {
            pluginClass.getDeclaredConstructor().newInstance() as ComposeScriptPlugin
        }.getOrNull() ?: return
        registerPlugin(plugin)
    }

    fun registerPlugin(plugin: ComposeScriptPlugin) {
        plugins.add(plugin)
        plugin.getComponents().forEach { (name, composable) ->
            val fullName = if (plugin.namespace != null) "${plugin.namespace}.$name" else name
            components[fullName] = composable
        }
    }

    fun resolveVerticalArrangement(prop: Any?): Arrangement.Vertical {

        return when (prop) {
            is Arrangement.Vertical -> {
                prop
            }

            is String -> {
                when (prop.lowercase()) {
                    "top" -> Arrangement.Top
                    "bottom" -> Arrangement.Bottom
                    "center" -> Arrangement.Center
                    "spacebetween" -> Arrangement.SpaceBetween
                    "spacearound" -> Arrangement.SpaceAround
                    "spaceevenly" -> Arrangement.SpaceEvenly
                    else -> Arrangement.Top
                }
            }


            else -> {
                Arrangement.Top
            }
        }

    }

    fun resolveHorizontalArrangement(prop: Any?): Arrangement.Horizontal {

        return when (prop) {
            is Arrangement.Horizontal -> {
                prop
            }

            is String -> {
                when (prop.lowercase()) {
                    "start" -> Arrangement.Start
                    "end" -> Arrangement.End
                    "center" -> Arrangement.Center
                    "spacebetween" -> Arrangement.SpaceBetween
                    "spacearound" -> Arrangement.SpaceAround
                    "spaceevenly" -> Arrangement.SpaceEvenly
                    else -> Arrangement.Start
                }
            }


            else -> {
                Arrangement.Start
            }
        }


    }

    fun resolveHorizontalAlignment(prop: Any?): Alignment.Horizontal {

        return when (prop) {
            is Alignment.Horizontal -> {
                prop
            }
            is String -> {
                when (prop.lowercase()) {
                    "start" -> Alignment.Start
                    "end" -> Alignment.End
                    "center", "centerhorizontally" -> Alignment.CenterHorizontally
                    else -> Alignment.Start
                }}


            else -> {
                Alignment.Start
            }
        }

    }

    fun resolveVerticalAlignment(prop: Any?): Alignment.Vertical {
        return when (prop) {
            is Alignment.Vertical -> {
                prop
            }
            is String -> {
                when (prop.lowercase()) {
                    "top" -> Alignment.Top
                    "bottom" -> Alignment.Bottom
                    "center", "centervertically" -> Alignment.CenterVertically
                    else -> Alignment.Top
                }}


            else -> {
                Alignment.Top
            }
        }
    }

    fun resolveAlignment(prop: Any?): Alignment {

        return when (prop) {
            is Alignment -> {
                prop
            }
            is String -> {
                when (prop.lowercase()) {
                    "topstart" -> Alignment.TopStart
                    "topcenter" -> Alignment.TopCenter
                    "topend" -> Alignment.TopEnd
                    "centerstart" -> Alignment.CenterStart
                    "center" -> Alignment.Center
                    "centerend" -> Alignment.CenterEnd
                    "bottomstart" -> Alignment.BottomStart
                    "bottomcenter" -> Alignment.BottomCenter
                    "bottomend" -> Alignment.BottomEnd
                    else -> Alignment.TopStart
                }}


            else -> {
                Alignment.TopStart
            }
        }
    }

    fun resolveBoxAlignment(alignStr: String): Alignment {
        return resolveAlignment(alignStr)
    }

    fun resolveColumnAlignment(alignStr: String): Alignment.Horizontal {
        return resolveHorizontalAlignment(alignStr)
    }

    fun resolveRowAlignment(alignStr: String): Alignment.Vertical {
        return resolveVerticalAlignment(alignStr)
    }
}
