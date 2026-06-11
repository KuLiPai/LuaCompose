package com.kulipai.luacompose.compose

import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import org.luaj.LuaValue
import org.luaj.LuaTable
import androidx.compose.ui.graphics.Path
import com.kulipai.luacompose.compose.plugins.FoundationPlugin
import com.kulipai.luacompose.compose.plugins.LuaComposePlugin
import com.kulipai.luacompose.compose.plugins.Material3Plugin

class LuaPath(val path: Path = Path()) : LuaTable() {
    init {
        set("moveTo", object : org.luaj.lib.TwoArgFunction() {
            override fun call(x: LuaValue, y: LuaValue): LuaValue {
                path.moveTo(x.tofloat(), y.tofloat())
                return this@LuaPath
            }
        })
        set("lineTo", object : org.luaj.lib.TwoArgFunction() {
            override fun call(x: LuaValue, y: LuaValue): LuaValue {
                path.lineTo(x.tofloat(), y.tofloat())
                return this@LuaPath
            }
        })
        set("quadraticBezierTo", object : org.luaj.lib.VarArgFunction() {
            override fun invoke(args: org.luaj.Varargs): org.luaj.Varargs {
                path.quadraticBezierTo(
                    args.checkdouble(1).toFloat(), args.checkdouble(2).toFloat(),
                    args.checkdouble(3).toFloat(), args.checkdouble(4).toFloat()
                )
                return this@LuaPath
            }
        })
        set("cubicTo", object : org.luaj.lib.VarArgFunction() {
            override fun invoke(args: org.luaj.Varargs): org.luaj.Varargs {
                path.cubicTo(
                    args.checkdouble(1).toFloat(), args.checkdouble(2).toFloat(),
                    args.checkdouble(3).toFloat(), args.checkdouble(4).toFloat(),
                    args.checkdouble(5).toFloat(), args.checkdouble(6).toFloat()
                )
                return this@LuaPath
            }
        })
        set("close", object : org.luaj.lib.ZeroArgFunction() {
            override fun call(): LuaValue {
                path.close()
                return this@LuaPath
            }
        })
    }
}

class LuaDrawScope(val drawScope: DrawScope) : LuaTable() {
    init {
        set("drawRect", object : org.luaj.lib.OneArgFunction() {
            override fun call(args: LuaValue): LuaValue {
                val color = resolveColor(args.get("color"))
                val x = args.get("x").optdouble(0.0).toFloat()
                val y = args.get("y").optdouble(0.0).toFloat()
                val width = args.get("width").optdouble(100.0).toFloat()
                val height = args.get("height").optdouble(100.0).toFloat()
                drawScope.drawRect(
                    color = color,
                    topLeft = Offset(x, y),
                    size = Size(width, height)
                )
                return NIL
            }
        })
        set("drawRoundRect", object : org.luaj.lib.OneArgFunction() {
            override fun call(args: LuaValue): LuaValue {
                val color = resolveColor(args.get("color"))
                val x = args.get("x").optdouble(0.0).toFloat()
                val y = args.get("y").optdouble(0.0).toFloat()
                val width = args.get("width").optdouble(100.0).toFloat()
                val height = args.get("height").optdouble(100.0).toFloat()
                val cornerX = args.get("cornerRadiusX").optdouble(0.0).toFloat()
                val cornerY = args.get("cornerRadiusY").optdouble(cornerX.toDouble()).toFloat()
                drawScope.drawRoundRect(
                    color = color,
                    topLeft = Offset(x, y),
                    size = Size(width, height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerX, cornerY)
                )
                return NIL
            }
        })
        set("drawCircle", object : org.luaj.lib.OneArgFunction() {
            override fun call(args: LuaValue): LuaValue {
                val color = resolveColor(args.get("color"))
                val radius = args.get("radius").optdouble(50.0).toFloat()
                val centerX = args.get("centerX").let { if (it.isnil()) drawScope.center.x else it.tofloat() }
                val centerY = args.get("centerY").let { if (it.isnil()) drawScope.center.y else it.tofloat() }
                drawScope.drawCircle(
                    color = color,
                    radius = radius,
                    center = Offset(centerX, centerY)
                )
                return NIL
            }
        })
        set("drawPath", object : org.luaj.lib.OneArgFunction() {
            override fun call(args: LuaValue): LuaValue {
                val color = resolveColor(args.get("color"))
                val luaPath = args.get("path")
                if (luaPath is LuaPath) {
                    drawScope.drawPath(
                        path = luaPath.path,
                        color = color
                    )
                }
                return NIL
            }
        })
    }
}

object LuaComposeRegistry {
    val components = mutableMapOf<String, @Composable (props: Map<String, Any?>, childScope: LuaScope?) -> Unit>()
    val plugins = mutableListOf<LuaComposePlugin>()

    init {
        registerPlugin(FoundationPlugin())
        registerPlugin(Material3Plugin())
    }

    fun registerPlugin(plugin: LuaComposePlugin) {
        plugins.add(plugin)
        plugin.getComponents().forEach { (name, composable) ->
            val fullName = if (plugin.namespace != null) "${plugin.namespace}.$name" else name
            components[fullName] = composable
        }
    }

    fun resolveVerticalArrangement(prop: Any?): Arrangement.Vertical {
        return when (prop?.toString()?.lowercase()) {
            "top" -> Arrangement.Top
            "bottom" -> Arrangement.Bottom
            "center" -> Arrangement.Center
            "spacebetween" -> Arrangement.SpaceBetween
            "spacearound" -> Arrangement.SpaceAround
            "spaceevenly" -> Arrangement.SpaceEvenly
            else -> Arrangement.Top
        }
    }

    fun resolveHorizontalArrangement(prop: Any?): Arrangement.Horizontal {
        return when (prop?.toString()?.lowercase()) {
            "start" -> Arrangement.Start
            "end" -> Arrangement.End
            "center" -> Arrangement.Center
            "spacebetween" -> Arrangement.SpaceBetween
            "spacearound" -> Arrangement.SpaceAround
            "spaceevenly" -> Arrangement.SpaceEvenly
            else -> Arrangement.Start
        }
    }

    fun resolveHorizontalAlignment(prop: Any?): Alignment.Horizontal {
        return when (prop?.toString()?.lowercase()) {
            "start" -> Alignment.Start
            "end" -> Alignment.End
            "center", "centerhorizontally" -> Alignment.CenterHorizontally
            else -> Alignment.Start
        }
    }

    fun resolveVerticalAlignment(prop: Any?): Alignment.Vertical {
        return when (prop?.toString()?.lowercase()) {
            "top" -> Alignment.Top
            "bottom" -> Alignment.Bottom
            "center", "centervertically" -> Alignment.CenterVertically
            else -> Alignment.Top
        }
    }

    fun resolveAlignment(prop: Any?): Alignment {
        return when (prop?.toString()?.lowercase()) {
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
