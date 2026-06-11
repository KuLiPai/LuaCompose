package com.kulipai.luacompose.compose

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
import org.luaj.LuaFunction
import org.luaj.LuaValue
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput

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
    fun background(colorProp: Any, shape: Shape): LuaModifier {
        try { modifier = modifier.background(resolveColor(colorProp), shape) } catch (e: Exception) { e.printStackTrace() }
        return this
    }
    fun alpha(alpha: Float): LuaModifier { modifier = modifier.alpha(alpha); return this }
    fun aspectRatio(ratio: Float): LuaModifier { modifier = modifier.aspectRatio(ratio); return this }
    fun offset(x: Any, y: Any): LuaModifier { modifier = modifier.offset(resolveDp(x), resolveDp(y)); return this }
    
    fun clickable(onClick: LuaFunction): LuaModifier {
        modifier = modifier.clickable {
            try { onClick.call() } catch (e: Exception) { e.printStackTrace() }
        }
        return this
    }
    
    fun animateContentSize(): LuaModifier {
        modifier = modifier.animateContentSize()
        return this
    }

    fun pointerInput(gestures: org.luaj.LuaTable): LuaModifier {
        val onTap = gestures.get("onTap").takeIf { it.isfunction() }?.checkfunction()
        val onDoubleTap = gestures.get("onDoubleTap").takeIf { it.isfunction() }?.checkfunction()
        val onLongPress = gestures.get("onLongPress").takeIf { it.isfunction() }?.checkfunction()
        val onDrag = gestures.get("onDrag").takeIf { it.isfunction() }?.checkfunction()

        if (onTap != null || onDoubleTap != null || onLongPress != null) {
            modifier = modifier.pointerInput("tapGestures") {
                detectTapGestures(
                    onTap = onTap?.let { fn -> { offset -> fn.call(LuaValue.valueOf(offset.x.toDouble()), LuaValue.valueOf(offset.y.toDouble())) } },
                    onDoubleTap = onDoubleTap?.let { fn -> { offset -> fn.call(LuaValue.valueOf(offset.x.toDouble()), LuaValue.valueOf(offset.y.toDouble())) } },
                    onLongPress = onLongPress?.let { fn -> { offset -> fn.call(LuaValue.valueOf(offset.x.toDouble()), LuaValue.valueOf(offset.y.toDouble())) } }
                )
            }
        }
        
        if (onDrag != null) {
            modifier = modifier.pointerInput("dragGestures") {
                detectDragGestures { change, dragAmount -> 
                    change.consume()
                    onDrag.call(LuaValue.valueOf(dragAmount.x.toDouble()), LuaValue.valueOf(dragAmount.y.toDouble()))
                }
            }
        }
        
        return this
    }
    
    fun clip(shape: String, radius: Int): LuaModifier {
        val clipShape = when (shape.lowercase()) {
            "circle" -> CircleShape
            "rounded" -> RoundedCornerShape(radius.dp)
            else -> null
        }
        if (clipShape != null) { modifier = modifier.clip(clipShape) }
        return this
    }
    fun clip(shape: String): LuaModifier = clip(shape, 0)
    
    fun border(width: Int, color: Any): LuaModifier {
        try { modifier = modifier.border(width.dp, resolveColor(color)) } catch (e: Exception) { e.printStackTrace() }
        return this
    }
    
    fun align(alignStr: String): LuaModifier { this.alignmentStr = alignStr; return this }
    fun weight(weight: Float): LuaModifier { this.weightVal = weight; return this }
}
