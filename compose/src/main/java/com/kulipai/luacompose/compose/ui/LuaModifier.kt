package com.kulipai.luacompose.compose.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.rotate
import com.kulipai.luacompose.compose.script.ScriptFunction
import com.kulipai.luacompose.compose.script.ScriptTable
import com.kulipai.luacompose.compose.script.ScriptValue
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import com.kulipai.luacompose.compose.runtime.ComposeBridge

// --- 3. 极其优雅的链式 Modifier 封装 ---
class LuaModifier(var modifier: Modifier = Modifier) {
    var alignmentStr: String? = null
    var weightVal: Float? = null

    fun padding(dp: Any): LuaModifier { modifier = modifier.padding(resolveDp(dp)); return this }
    fun padding(horizontal: Any, vertical: Any): LuaModifier { modifier = modifier.padding(resolveDp(horizontal), resolveDp(vertical)); return this }
    fun padding(start: Any, top: Any, end: Any, bottom: Any): LuaModifier { modifier = modifier.padding(resolveDp(start), resolveDp(top), resolveDp(end), resolveDp(bottom)); return this }
    fun fillMaxSize(): LuaModifier { modifier = modifier.fillMaxSize(); return this }
    fun fillMaxWidth(): LuaModifier { modifier = modifier.fillMaxWidth(); return this }
    fun fillMaxHeight(): LuaModifier { modifier = modifier.fillMaxHeight(); return this }
    fun size(size: Any): LuaModifier { modifier = modifier.size(resolveDp(size)); return this }
    fun size(width: Any, height: Any): LuaModifier { modifier = modifier.size(resolveDp(width), resolveDp(height)); return this }
    fun width(width: Any): LuaModifier { modifier = modifier.width(resolveDp(width)); return this }
    fun height(height: Any): LuaModifier { modifier = modifier.height(resolveDp(height)); return this }
    fun wrapContentSize(): LuaModifier { modifier = modifier.wrapContentSize(); return this }
    
    fun background(colorProp: Any): LuaModifier {
        try { modifier = modifier.background(resolveColor(colorProp)) } catch (e: Exception) { e.printStackTrace() }
        return this
    }
    fun background(colorProp: Any, shapeProp: Any): LuaModifier {
        val resolvedShape = resolveShape(shapeProp)
        try { 
            if (resolvedShape != null) {
                modifier = modifier.background(resolveColor(colorProp), resolvedShape) 
            } else {
                modifier = modifier.background(resolveColor(colorProp))
            }
        } catch (e: Exception) { e.printStackTrace() }
        return this
    }
    fun alpha(alpha: Float): LuaModifier { modifier = modifier.alpha(alpha); return this }
    fun aspectRatio(ratio: Float): LuaModifier { modifier = modifier.aspectRatio(ratio); return this }
    fun rotate(degrees: Float): LuaModifier { modifier = modifier.rotate(degrees); return this }
    fun offset(x: Any, y: Any): LuaModifier { modifier = modifier.offset(resolveDp(x), resolveDp(y)); return this }
    
    fun clickable(onClick: ScriptFunction): LuaModifier {
        modifier = modifier.clickable {
            try { onClick.call() } catch (e: Exception) { e.printStackTrace() }
        }
        return this
    }
    
    fun animateContentSize(): LuaModifier {
        modifier = modifier.animateContentSize()
        return this
    }

    fun pointerInput(gestures: ScriptTable): LuaModifier {
        val onTap = gestures.get("onTap").takeIf { it.isFunction() }?.asFunction()
        val onDoubleTap = gestures.get("onDoubleTap").takeIf { it.isFunction() }?.asFunction()
        val onLongPress = gestures.get("onLongPress").takeIf { it.isFunction() }?.asFunction()
        val onDrag = gestures.get("onDrag").takeIf { it.isFunction() }?.asFunction()

        if (onTap != null || onDoubleTap != null || onLongPress != null) {
            modifier = modifier.pointerInput("tapGestures") {
                detectTapGestures(
                    onTap = onTap?.let { fn -> { offset -> fn.call(ComposeBridge.engine.createValue(offset.x.toDouble()), ComposeBridge.engine.createValue(offset.y.toDouble())) } },
                    onDoubleTap = onDoubleTap?.let { fn -> { offset -> fn.call(ComposeBridge.engine.createValue(offset.x.toDouble()), ComposeBridge.engine.createValue(offset.y.toDouble())) } },
                    onLongPress = onLongPress?.let { fn -> { offset -> fn.call(ComposeBridge.engine.createValue(offset.x.toDouble()), ComposeBridge.engine.createValue(offset.y.toDouble())) } }
                )
            }
        }
        
        if (onDrag != null) {
            modifier = modifier.pointerInput("dragGestures") {
                detectDragGestures { change, dragAmount -> 
                    change.consume()
                    onDrag.call(ComposeBridge.engine.createValue(dragAmount.x.toDouble()), ComposeBridge.engine.createValue(dragAmount.y.toDouble()))
                }
            }
        }
        
        return this
    }
    
    fun clip(shape: String, radius: Int): LuaModifier {
        val clipShape = when (shape.lowercase()) {
            "circle", "circleshape" -> CircleShape
            "rounded", "roundedcornershape" -> RoundedCornerShape(radius.dp)
            "rectangle", "rectangleshape" -> androidx.compose.ui.graphics.RectangleShape
            else -> null
        }
        if (clipShape != null) { modifier = modifier.clip(clipShape) }
        return this
    }
    fun clip(shapeProp: Any): LuaModifier {
        val clipShape = resolveShape(shapeProp)
        if (clipShape != null) { modifier = modifier.clip(clipShape) }
        return this
    }
    fun clip(shape: Shape): LuaModifier {
        modifier = modifier.clip(shape)
        return this
    }
    
    fun border(width: Int, color: Any): LuaModifier {
        try { modifier = modifier.border(width.dp, resolveColor(color)) } catch (e: Exception) { e.printStackTrace() }
        return this
    }
    
    fun align(alignStr: String): LuaModifier { this.alignmentStr = alignStr; return this }
    fun weight(weight: Float): LuaModifier { this.weightVal = weight; return this }

    private fun anyFromScript(v: ScriptValue?): Any? = if (v == null || v.isNil()) null else ComposeBridge.scriptToJava(v)

    fun padding(table: ScriptTable): LuaModifier {
        val all = anyFromScript(table.get("all").takeIf { !it.isNil() } ?: table.get(1).takeIf { !it.isNil() && table.length() == 1 })
        if (all != null) return padding(all)
        
        val horizontal = anyFromScript(table.get("horizontal").takeIf { !it.isNil() } ?: table.get(1).takeIf { !it.isNil() && table.length() == 2 })
        val vertical = anyFromScript(table.get("vertical").takeIf { !it.isNil() } ?: table.get(2).takeIf { !it.isNil() && table.length() == 2 })
        if (horizontal != null || vertical != null) return padding(horizontal ?: 0, vertical ?: 0)
        
        val start = anyFromScript(table.get("start").takeIf { !it.isNil() } ?: table.get(1).takeIf { !it.isNil() && table.length() == 4 })
        val top = anyFromScript(table.get("top").takeIf { !it.isNil() } ?: table.get(2).takeIf { !it.isNil() && table.length() == 4 })
        val end = anyFromScript(table.get("end").takeIf { !it.isNil() } ?: table.get(3).takeIf { !it.isNil() && table.length() == 4 })
        val bottom = anyFromScript(table.get("bottom").takeIf { !it.isNil() } ?: table.get(4).takeIf { !it.isNil() && table.length() == 4 })
        if (start != null || top != null || end != null || bottom != null) {
            return padding(start ?: 0, top ?: 0, end ?: 0, bottom ?: 0)
        }
        return this
    }

    fun size(table: ScriptTable): LuaModifier {
        val size = anyFromScript(table.get("size").takeIf { !it.isNil() } ?: table.get(1).takeIf { !it.isNil() && table.length() == 1 })
        if (size != null) return size(size)
        
        val width = anyFromScript(table.get("width").takeIf { !it.isNil() } ?: table.get(1).takeIf { !it.isNil() && table.length() == 2 })
        val height = anyFromScript(table.get("height").takeIf { !it.isNil() } ?: table.get(2).takeIf { !it.isNil() && table.length() == 2 })
        if (width != null || height != null) return size(width ?: 0, height ?: 0)
        
        return this
    }

    fun offset(table: ScriptTable): LuaModifier {
        val x = anyFromScript(table.get("x").takeIf { !it.isNil() } ?: table.get(1).takeIf { !it.isNil() })
        val y = anyFromScript(table.get("y").takeIf { !it.isNil() } ?: table.get(2).takeIf { !it.isNil() })
        if (x != null || y != null) return offset(x ?: 0, y ?: 0)
        return this
    }

    fun background(table: ScriptTable): LuaModifier {
        val color = anyFromScript(table.get("color").takeIf { !it.isNil() } ?: table.get(1).takeIf { !it.isNil() })
        val shape = anyFromScript(table.get("shape").takeIf { !it.isNil() } ?: table.get(2).takeIf { !it.isNil() })
        if (color != null) {
            if (shape != null) return background(color, shape)
            return background(color)
        }
        return this
    }

    fun clip(table: ScriptTable): LuaModifier {
        val shape = anyFromScript(table.get("shape").takeIf { !it.isNil() } ?: table.get(1).takeIf { !it.isNil() })
        if (shape != null) return clip(shape)
        return this
    }

    fun rotate(table: ScriptTable): LuaModifier {
        val degrees = anyFromScript(table.get("degrees").takeIf { !it.isNil() } ?: table.get(1).takeIf { !it.isNil() })
        if (degrees != null && degrees is Number) return rotate(degrees.toFloat())
        return this
    }

    fun border(table: ScriptTable): LuaModifier {
        val width = anyFromScript(table.get("width").takeIf { !it.isNil() } ?: table.get(1).takeIf { !it.isNil() })
        val color = anyFromScript(table.get("color").takeIf { !it.isNil() } ?: table.get(2).takeIf { !it.isNil() })
        val shape = anyFromScript(table.get("shape").takeIf { !it.isNil() } ?: table.get(3).takeIf { !it.isNil() })
        
        if (width != null && color != null) {
            val resolvedShape = resolveShape(shape)
            val w = resolveDp(width)
            val c = resolveColor(color)
            try { 
                if (resolvedShape != null) {
                    modifier = modifier.border(w, c, resolvedShape)
                } else {
                    modifier = modifier.border(w, c)
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
        return this
    }
}
