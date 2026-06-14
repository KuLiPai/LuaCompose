package com.kulipai.luacompose.compose.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kulipai.luacompose.compose.runtime.ComposeBridge
import com.kulipai.luacompose.compose.script.ScriptFunction
import com.kulipai.luacompose.compose.script.ScriptTable
import com.kulipai.luacompose.compose.script.ScriptValue
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

// --- 3. 极其优雅的链式 Modifier 封装 ---
class LuaModifier(var modifier: Modifier = Modifier) {
    var alignmentStr: String? = null
    var alignObject: Any? = null
    var weightVal: Float? = null
    var weightFill: Boolean = true

    fun padding(dpOrTable: Any): LuaModifier {
        val unwrapped = ComposeBridge.unwrapAny(dpOrTable)
        if (unwrapped is Map<*, *>) {
            val all = unwrapped["all"] ?: unwrapped[1.0] ?: unwrapped[1]
            if (all != null) return padding(all)

            val horizontal = unwrapped["horizontal"] ?: unwrapped[1.0] ?: unwrapped[1]
            val vertical = unwrapped["vertical"] ?: unwrapped[2.0] ?: unwrapped[2]
            if (horizontal != null || vertical != null) return padding(horizontal ?: 0, vertical ?: 0)

            val start = unwrapped["start"] ?: unwrapped[1.0] ?: unwrapped[1]
            val top = unwrapped["top"] ?: unwrapped[2.0] ?: unwrapped[2]
            val end = unwrapped["end"] ?: unwrapped[3.0] ?: unwrapped[3]
            val bottom = unwrapped["bottom"] ?: unwrapped[4.0] ?: unwrapped[4]
            if (start != null || top != null || end != null || bottom != null) {
                return padding(start ?: 0, top ?: 0, end ?: 0, bottom ?: 0)
            }
            return this
        }
        modifier = modifier.padding(resolveDp(unwrapped)); return this
    }

    fun padding(horizontal: Any, vertical: Any): LuaModifier {
        modifier = modifier.padding(resolveDp(horizontal), resolveDp(vertical)); return this
    }

    fun padding(start: Any, top: Any, end: Any, bottom: Any): LuaModifier {
        modifier = modifier.padding(
            resolveDp(start),
            resolveDp(top),
            resolveDp(end),
            resolveDp(bottom)
        ); return this
    }

    fun fillMaxSize(): LuaModifier {
        modifier = modifier.fillMaxSize(); return this
    }

    fun fillMaxWidth(): LuaModifier {
        modifier = modifier.fillMaxWidth(); return this
    }

    fun fillMaxHeight(): LuaModifier {
        modifier = modifier.fillMaxHeight(); return this
    }

    fun size(size: Any): LuaModifier {
        val unwrapped = ComposeBridge.unwrapAny(size)
        if (unwrapped is Map<*, *>) {
            val width = unwrapped["width"] ?: unwrapped[1.0] ?: unwrapped[1]
            val height = unwrapped["height"] ?: unwrapped[2.0] ?: unwrapped[2]
            if (width != null || height != null) return size(width ?: 0, height ?: 0)
        }
        modifier = modifier.size(resolveDp(unwrapped))
        return this
    }

    fun size(width: Any, height: Any): LuaModifier {
        modifier = modifier.size(resolveDp(width), resolveDp(height)); return this
    }

    fun width(width: Any): LuaModifier {
        modifier = modifier.width(resolveDp(width)); return this
    }

    fun height(height: Any): LuaModifier {
        modifier = modifier.height(resolveDp(height)); return this
    }

    fun wrapContentSize(): LuaModifier {
        modifier = modifier.wrapContentSize(); return this
    }

    fun background(colorProp: Any): LuaModifier {
        val unwrapped = ComposeBridge.unwrapAny(colorProp)
        if (unwrapped is Map<*, *>) {
            val color = unwrapped["color"] ?: unwrapped[1.0] ?: unwrapped[1]
            val shape = unwrapped["shape"] ?: unwrapped[2.0] ?: unwrapped[2]
            if (color != null) {
                if (shape != null) return background(color, shape)
                return background(color)
            }
        }
        try {
            modifier = modifier.background(resolveColor(unwrapped))
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return this
    }

    fun alpha(alpha: Float): LuaModifier {
        modifier = modifier.alpha(alpha); return this
    }

    fun aspectRatio(ratio: Float): LuaModifier {
        modifier = modifier.aspectRatio(ratio); return this
    }

    fun rotate(degrees: Any): LuaModifier {
        val unwrapped = ComposeBridge.unwrapAny(degrees)
        if (unwrapped is Map<*, *>) {
            val deg = unwrapped["degrees"] ?: unwrapped[1.0] ?: unwrapped[1]
            if (deg is Number) {
                modifier = modifier.rotate(deg.toFloat())
            }
        } else if (unwrapped is Number) {
            modifier = modifier.rotate(unwrapped.toFloat())
        }
        return this
    }

    fun offset(x: Any, y: Any): LuaModifier {
        modifier = modifier.offset(resolveDp(x), resolveDp(y)); return this
    }

    fun offset(tableRaw: Any): LuaModifier {
        val tableValue = if (tableRaw is ScriptValue) tableRaw else ComposeBridge.engine.coerceJavaToScript(tableRaw)
        if (tableValue.isFunction()) {
            val table = tableValue.asFunction()
            modifier = modifier.offset {
                val res = table.call()
                val unwrapped = ComposeBridge.unwrapAny(res)
                
                if (unwrapped is androidx.compose.ui.unit.IntOffset) {
                    unwrapped
                } else {
                    androidx.compose.ui.unit.IntOffset.Zero
                }
            }
            return this
        }
        val unwrapped = ComposeBridge.unwrapAny(tableRaw)
        if (unwrapped is Map<*, *>) {
            val x = unwrapped["x"] ?: unwrapped[1.0] ?: unwrapped[1]
            val y = unwrapped["y"] ?: unwrapped[2.0] ?: unwrapped[2]
            if (x != null || y != null) return offset(x ?: 0, y ?: 0)
        }
        return offset(unwrapped ?: 0, 0)
    }

    fun clickable(onClick: ScriptFunction): LuaModifier {
        modifier = modifier.clickable {
            try {
                onClick.call()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return this
    }

    fun animateContentSize(): LuaModifier {
        modifier = modifier.animateContentSize()
        return this
    }

    fun pointerInput(gestures: Any): LuaModifier {
        val unwrapped = ComposeBridge.unwrapAny(gestures)
        if (unwrapped !is Map<*, *>) return this
        val onTap = unwrapped["onTap"] as? ScriptFunction
        val onDoubleTap = unwrapped["onDoubleTap"] as? ScriptFunction
        val onLongPress = unwrapped["onLongPress"] as? ScriptFunction
        val onDrag = unwrapped["onDrag"] as? ScriptFunction

        if (onTap != null || onDoubleTap != null || onLongPress != null) {
            modifier = modifier.pointerInput("tapGestures") {
                detectTapGestures(
                    onTap = onTap?.let { fn ->
                        { offset ->
                            fn.call(
                                ComposeBridge.engine.createValue(
                                    offset.x.toDouble()
                                ), ComposeBridge.engine.createValue(offset.y.toDouble())
                            )
                        }
                    },
                    onDoubleTap = onDoubleTap?.let { fn ->
                        { offset ->
                            fn.call(
                                ComposeBridge.engine.createValue(
                                    offset.x.toDouble()
                                ), ComposeBridge.engine.createValue(offset.y.toDouble())
                            )
                        }
                    },
                    onLongPress = onLongPress?.let { fn ->
                        { offset ->
                            fn.call(
                                ComposeBridge.engine.createValue(
                                    offset.x.toDouble()
                                ), ComposeBridge.engine.createValue(offset.y.toDouble())
                            )
                        }
                    }
                )
            }
        }

        if (onDrag != null) {
            modifier = modifier.pointerInput("dragGestures") {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag.call(
                        ComposeBridge.engine.createValue(dragAmount.x.toDouble()),
                        ComposeBridge.engine.createValue(dragAmount.y.toDouble())
                    )
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
        if (clipShape != null) {
            modifier = modifier.clip(clipShape)
        }
        return this
    }

    fun pointerInput(vararg args: Any): LuaModifier {
        if (args.isEmpty()) return this
        val keys = args.dropLast(1).map { ComposeBridge.unwrapAny(it) }.toTypedArray()
        val blockValueRaw = args.last()
        val blockValue = if (blockValueRaw is ScriptValue) blockValueRaw else ComposeBridge.engine.coerceJavaToScript(blockValueRaw)
        val block = if (blockValue.isFunction()) blockValue.asFunction() else null
        
        if (block != null) {
            val lambda: suspend androidx.compose.ui.input.pointer.PointerInputScope.() -> Unit = {
                android.util.Log.e("LUAMODIFIER_LOG", "POINTER INPUT LAMBDA EXECUTING!!")
                val actions = mutableListOf<suspend androidx.compose.ui.input.pointer.PointerInputScope.() -> Unit>()
                ComposeBridge.pushActivePointerInputScopeActions(actions)
                try {
                    block.call()
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    ComposeBridge.popActivePointerInputScopeActions()
                }
                kotlinx.coroutines.coroutineScope {
                    for (action in actions) {
                        launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) { action() }
                    }
                }
            }
            modifier = when (keys.size) {
                0 -> modifier.pointerInput(Unit, lambda)
                1 -> modifier.pointerInput(keys[0], lambda)
                2 -> modifier.pointerInput(keys[0], keys[1], lambda)
                else -> modifier.pointerInput(*keys, block = lambda)
            }
        }
        return this
    }

    fun clip(shapeProp: Any): LuaModifier {
        val unwrapped = ComposeBridge.unwrapAny(shapeProp)
        if (unwrapped is Map<*, *>) {
            val shape = unwrapped["shape"] ?: unwrapped[1.0] ?: unwrapped[1]
            if (shape != null) return clip(shape)
        }
        val clipShape = resolveShape(unwrapped)
        if (clipShape != null) {
            modifier = modifier.clip(clipShape)
        }
        return this
    }

    fun clip(shape: Shape): LuaModifier {
        modifier = modifier.clip(shape)
        return this
    }

    fun border(tableOrWidth: Any): LuaModifier {
        val unwrapped = ComposeBridge.unwrapAny(tableOrWidth)
        if (unwrapped is Map<*, *>) {
            val width = unwrapped["width"] ?: unwrapped[1.0] ?: unwrapped[1]
            val color = unwrapped["color"] ?: unwrapped[2.0] ?: unwrapped[2]
            val shape = unwrapped["shape"] ?: unwrapped[3.0] ?: unwrapped[3]
            if (width != null && color != null) {
                val resolvedShape = resolveShape(shape)
                val w = resolveDp(width)
                val c = resolveColor(color)
                if (resolvedShape != null) {
                    modifier = modifier.border(w, c, resolvedShape)
                } else {
                    modifier = modifier.border(w, c)
                }
            }
        }
        return this
    }

    fun border(width: Any, color: Any): LuaModifier {
        try {
            modifier = modifier.border(resolveDp(width), resolveColor(color))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return this
    }



    fun align(alignStrOrObj: Any): LuaModifier {
        val unwrapped = ComposeBridge.unwrapAny(alignStrOrObj)
        if (unwrapped is String) {
            this.alignmentStr = unwrapped
        } else {
            this.alignObject = unwrapped
        }
        return this
    }

    fun weight(weight: Any): LuaModifier {
        val unwrapped = ComposeBridge.unwrapAny(weight)
        if (unwrapped is Map<*, *>) {
            val w = unwrapped["weight"] ?: unwrapped[1.0] ?: unwrapped[1]
            val f = unwrapped["fill"] ?: unwrapped[2.0] ?: unwrapped[2]
            if (w != null) {
                this.weightVal = (w as? Number)?.toFloat() ?: 1f
            }
            if (f != null) {
                this.weightFill = f as? Boolean ?: true
            }
        } else if (unwrapped is Number) {
            this.weightVal = unwrapped.toFloat()
        }
        return this
    }

    fun weight(weight: Any, fill: Any): LuaModifier {
        val unwrappedWeight = ComposeBridge.unwrapAny(weight)
        if (unwrappedWeight is Number) {
            this.weightVal = unwrappedWeight.toFloat()
        }
        val unwrappedFill = ComposeBridge.unwrapAny(fill)
        if (unwrappedFill is Boolean) {
            this.weightFill = unwrappedFill
        }
        return this
    }





    fun widthIn(): LuaModifier {
        modifier = modifier.widthIn()
        return this
    }

    fun widthIn(min: Any, max: Any): LuaModifier {
        modifier = modifier.widthIn(resolveDp(min), resolveDp(max))
        return this
    }

    fun widthIn(table: Any): LuaModifier {
        val unwrapped = ComposeBridge.unwrapAny(table)
        if (unwrapped is Map<*, *>) {
            val min = unwrapped["min"] ?: unwrapped[1.0] ?: unwrapped[1]
            val max = unwrapped["max"] ?: unwrapped[2.0] ?: unwrapped[2]
            val minDp = if (min != null) resolveDp(min) else Dp.Unspecified
            val maxDp = if (max != null) resolveDp(max) else Dp.Unspecified
            modifier = modifier.widthIn(min = minDp, max = maxDp)
        } else {
            modifier = modifier.widthIn(min = resolveDp(unwrapped))
        }
        return this
    }


    fun heightIn(): LuaModifier {
        modifier = modifier.heightIn()
        return this
    }

    fun heightIn(min: Any, max: Any): LuaModifier {
        modifier = modifier.heightIn(resolveDp(min), resolveDp(max))
        return this
    }

    fun heightIn(table: Any): LuaModifier {
        val unwrapped = ComposeBridge.unwrapAny(table)
        if (unwrapped is Map<*, *>) {
            val min = unwrapped["min"] ?: unwrapped[1.0] ?: unwrapped[1]
            val max = unwrapped["max"] ?: unwrapped[2.0] ?: unwrapped[2]
            val minDp = if (min != null) resolveDp(min) else Dp.Unspecified
            val maxDp = if (max != null) resolveDp(max) else Dp.Unspecified
            modifier = modifier.heightIn(min = minDp, max = maxDp)
        } else {
            modifier = modifier.heightIn(min = resolveDp(unwrapped))
        }
        return this
    }


    fun sizeIn(): LuaModifier {
        modifier = modifier.sizeIn()
        return this
    }

    fun sizeIn(
        minWidth: Any,
        minHeight: Any,
        maxWidth: Any,
        maxHeight: Any
    ): LuaModifier {
        modifier = modifier.sizeIn(
            resolveDp(minWidth),
            resolveDp(minHeight),
            resolveDp(maxWidth),
            resolveDp(maxHeight),
        )
        return this
    }

    fun sizeIn(table: Any): LuaModifier {
        val unwrapped = ComposeBridge.unwrapAny(table)
        if (unwrapped is Map<*, *>) {
            val minWidth = unwrapped["minWidth"] ?: unwrapped["min"] ?: unwrapped[1.0] ?: unwrapped[1]
            val minHeight = unwrapped["minHeight"] ?: unwrapped["min"] ?: unwrapped[2.0] ?: unwrapped[2]
            val maxWidth = unwrapped["maxWidth"] ?: unwrapped["max"] ?: unwrapped[3.0] ?: unwrapped[3]
            val maxHeight = unwrapped["maxHeight"] ?: unwrapped["max"] ?: unwrapped[4.0] ?: unwrapped[4]

            val minWidthDp = if (minWidth != null) resolveDp(minWidth) else Dp.Unspecified
            val minHeightDp = if (minHeight != null) resolveDp(minHeight) else Dp.Unspecified
            val maxWidthDp = if (maxWidth != null) resolveDp(maxWidth) else Dp.Unspecified
            val maxHeightDp = if (maxHeight != null) resolveDp(maxHeight) else Dp.Unspecified

            modifier = modifier.sizeIn(
                minWidth = minWidthDp,
                minHeight = minHeightDp,
                maxWidth = maxWidthDp,
                maxHeight = maxHeightDp
            )
        } else {
            modifier = modifier.sizeIn(minWidth = resolveDp(unwrapped), minHeight = resolveDp(unwrapped))
        }
        return this
    }


    // compose.ui.draw
    fun scale(scale: Float): LuaModifier {
        modifier = modifier.scale(scale); return this
    }

    fun scale(scaleX: Float, scaleY: Float): LuaModifier {
        modifier = modifier.scale(scaleX, scaleY); return this
    }





}
