package com.kulipai.luacompose.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.luaj.LuaFunction
import org.luaj.LuaTable
import org.luaj.LuaValue
import org.luaj.lib.jse.CoerceJavaToLua

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Path
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.graphics.Color

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
                    cornerRadius = CornerRadius(cornerX, cornerY)
                )
                return NIL
            }
        })
        set("drawCircle", object : org.luaj.lib.OneArgFunction() {
            override fun call(args: LuaValue): LuaValue {
                val color = resolveColor(args.get("color"))
                val radius = args.get("radius").optdouble(50.0).toFloat()
                val cx = args.get("centerX").optdouble(50.0).toFloat()
                val cy = args.get("centerY").optdouble(50.0).toFloat()
                drawScope.drawCircle(
                    color = color,
                    radius = radius,
                    center = Offset(cx, cy)
                )
                return NIL
            }
        })
        set("drawOval", object : org.luaj.lib.OneArgFunction() {
            override fun call(args: LuaValue): LuaValue {
                val color = resolveColor(args.get("color"))
                val x = args.get("x").optdouble(0.0).toFloat()
                val y = args.get("y").optdouble(0.0).toFloat()
                val width = args.get("width").optdouble(100.0).toFloat()
                val height = args.get("height").optdouble(100.0).toFloat()
                drawScope.drawOval(
                    color = color,
                    topLeft = Offset(x, y),
                    size = Size(width, height)
                )
                return NIL
            }
        })
        set("drawArc", object : org.luaj.lib.OneArgFunction() {
            override fun call(args: LuaValue): LuaValue {
                val color = resolveColor(args.get("color"))
                val startAngle = args.get("startAngle").optdouble(0.0).toFloat()
                val sweepAngle = args.get("sweepAngle").optdouble(0.0).toFloat()
                val useCenter = args.get("useCenter").optboolean(true)
                val x = args.get("x").optdouble(0.0).toFloat()
                val y = args.get("y").optdouble(0.0).toFloat()
                val width = args.get("width").optdouble(100.0).toFloat()
                val height = args.get("height").optdouble(100.0).toFloat()
                drawScope.drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = useCenter,
                    topLeft = Offset(x, y),
                    size = Size(width, height)
                )
                return NIL
            }
        })
        set("drawLine", object : org.luaj.lib.OneArgFunction() {
            override fun call(args: LuaValue): LuaValue {
                val color = resolveColor(args.get("color"))
                val startX = args.get("startX").optdouble(0.0).toFloat()
                val startY = args.get("startY").optdouble(0.0).toFloat()
                val endX = args.get("endX").optdouble(0.0).toFloat()
                val endY = args.get("endY").optdouble(0.0).toFloat()
                val strokeWidth = args.get("strokeWidth").optdouble(1.0).toFloat()
                drawScope.drawLine(
                    color = color,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = strokeWidth
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
    val m3ComponentNames = mutableSetOf<String>()

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
            "center" -> Alignment.CenterHorizontally
            else -> Alignment.Start
        }
    }

    fun resolveVerticalAlignment(prop: Any?): Alignment.Vertical {
        return when (prop?.toString()?.lowercase()) {
            "top" -> Alignment.Top
            "bottom" -> Alignment.Bottom
            "center" -> Alignment.CenterVertically
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

    init {
        // --- 0. 生命周期与副作用 ---

        register("LaunchedEffect") { props, _ ->
            val key = props["key"]
            val contentFunc = props["content"] as? LuaFunction
            
            androidx.compose.runtime.LaunchedEffect(key) {
                withContext(Dispatchers.Default) {
                    try {
                        contentFunc?.call()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        register("DisposableEffect") { props, _ ->
            val key = props["key"]
            val contentFunc = props["content"] as? LuaFunction
            
            androidx.compose.runtime.DisposableEffect(key) {
                var onDisposeFunc: LuaFunction? = null
                try {
                    val result = contentFunc?.call()
                    if (result != null && result.isfunction()) {
                        onDisposeFunc = result.checkfunction()
                    } else if (result != null && result.istable() && result.get("onDispose").isfunction()) {
                        onDisposeFunc = result.get("onDispose").checkfunction()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                
                onDispose {
                    try {
                        onDisposeFunc?.call()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        // --- 1. 基础排版组件 ---

        register("Column") { props, childScope ->
            val modifier = resolveModifier(props["modifier"])
            Column(
                modifier = modifier,
                verticalArrangement = resolveVerticalArrangement(props["verticalArrangement"]),
                horizontalAlignment = resolveHorizontalAlignment(props["horizontalAlignment"])
            ) {
                childScope?.let { LuaScopeComponent(it, this) }
            }
        }

        register("Row") { props, childScope ->
            val modifier = resolveModifier(props["modifier"])
            Row(
                modifier = modifier,
                horizontalArrangement = resolveHorizontalArrangement(props["horizontalArrangement"]),
                verticalAlignment = resolveVerticalAlignment(props["verticalAlignment"])
            ) {
                childScope?.let { LuaScopeComponent(it, this) }
            }
        }

        register("Box") { props, childScope ->
            val modifier = resolveModifier(props["modifier"])
            Box(
                modifier = modifier,
                contentAlignment = resolveAlignment(props["contentAlignment"])
            ) {
                childScope?.let { LuaScopeComponent(it, this) }
            }
        }

        register("Canvas") { props, _ ->
            val modifier = resolveModifier(props["modifier"])
            val onDraw = props["onDraw"] as? LuaFunction
            
            Canvas(modifier = modifier) {
                if (onDraw != null) {
                    val luaDrawScope = LuaDrawScope(this)
                    try {
                        onDraw.call(luaDrawScope)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        // --- Material 3 组件 ---
        m3ComponentNames.add("Text")
        register("Text") { props, _ ->
            val textVal = when (val textProp = props["text"]) {
                is LuaState -> textProp.get()?.toString() ?: ""
                else -> textProp?.toString() ?: ""
            }
            val spVal = resolveSp(props["fontSize"])
            
            // Map common string values to MaterialTheme typography
            val styleProp = props["style"]
            val style = if (styleProp is androidx.compose.ui.text.TextStyle) {
                styleProp
            } else {
                when (styleProp?.toString()?.lowercase()) {
                    "displaylarge" -> androidx.compose.material3.MaterialTheme.typography.displayLarge
                    "displaymedium" -> androidx.compose.material3.MaterialTheme.typography.displayMedium
                    "displaysmall" -> androidx.compose.material3.MaterialTheme.typography.displaySmall
                    "headlinelarge" -> androidx.compose.material3.MaterialTheme.typography.headlineLarge
                    "headlinemedium" -> androidx.compose.material3.MaterialTheme.typography.headlineMedium
                    "headlinesmall" -> androidx.compose.material3.MaterialTheme.typography.headlineSmall
                    "titlelarge" -> androidx.compose.material3.MaterialTheme.typography.titleLarge
                    "titlemedium" -> androidx.compose.material3.MaterialTheme.typography.titleMedium
                    "titlesmall" -> androidx.compose.material3.MaterialTheme.typography.titleSmall
                    "bodylarge" -> androidx.compose.material3.MaterialTheme.typography.bodyLarge
                    "bodymedium" -> androidx.compose.material3.MaterialTheme.typography.bodyMedium
                    "bodysmall" -> androidx.compose.material3.MaterialTheme.typography.bodySmall
                    "labellarge" -> androidx.compose.material3.MaterialTheme.typography.labelLarge
                    "labelmedium" -> androidx.compose.material3.MaterialTheme.typography.labelMedium
                    "labelsmall" -> androidx.compose.material3.MaterialTheme.typography.labelSmall
                    else -> androidx.compose.ui.text.TextStyle.Default
                }
            }
            
            val fontWeightProp = props["fontWeight"]?.toString()?.lowercase()
            val fontWeight = when (fontWeightProp) {
                "bold" -> androidx.compose.ui.text.font.FontWeight.Bold
                "medium" -> androidx.compose.ui.text.font.FontWeight.Medium
                "light" -> androidx.compose.ui.text.font.FontWeight.Light
                else -> null
            }
            
            Text(
                text = textVal,
                modifier = resolveModifier(props["modifier"]),
                color = resolveColor(props["color"]),
                fontSize = if (spVal != androidx.compose.ui.unit.TextUnit.Unspecified) spVal else androidx.compose.ui.unit.TextUnit.Unspecified,
                style = style,
                fontWeight = fontWeight
            )
        }

        m3ComponentNames.add("MaterialTheme")
        register("MaterialTheme") { props, childScope ->
            // MaterialTheme context
            androidx.compose.material3.MaterialTheme {
                childScope?.let { LuaScopeComponent(it, this) }
            }
        }

        m3ComponentNames.add("Card")
        register("Card") { props, childScope ->
            val modifier = resolveModifier(props["modifier"])
            // colors
            val colorsProp = props["colors"]
            val colors = if (colorsProp is Map<*, *> && colorsProp["_isCardColors"] == true) {
                val containerColor = resolveColor(colorsProp["containerColor"], Color.Unspecified)
                val contentColor = resolveColor(colorsProp["contentColor"], Color.Unspecified)
                androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = containerColor,
                    contentColor = contentColor
                )
            } else {
                val containerColor = resolveColor(props["containerColor"], Color.Unspecified)
                if (containerColor != Color.Unspecified) {
                    androidx.compose.material3.CardDefaults.cardColors(containerColor = containerColor)
                } else androidx.compose.material3.CardDefaults.cardColors()
            }
            
            // shape
            val shape = props["shape"] as? androidx.compose.ui.graphics.Shape
                ?: run {
                    val shapeProp = props["shape"]?.toString() ?: "rounded"
                    val radius = (props["cornerRadius"] as? Number)?.toFloat() ?: 12f
                    when (shapeProp.lowercase()) {
                        "circle" -> androidx.compose.foundation.shape.CircleShape
                        "rounded" -> androidx.compose.foundation.shape.RoundedCornerShape(radius.dp)
                        else -> androidx.compose.foundation.shape.RoundedCornerShape(radius.dp)
                    }
                }
            
            val elevationVal = (props["elevation"] as? Number)?.toFloat()
            val elevation = if (elevationVal != null) {
                androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = elevationVal.dp)
            } else androidx.compose.material3.CardDefaults.cardElevation()
            
            androidx.compose.material3.Card(
                modifier = modifier,
                shape = shape,
                colors = colors,
                elevation = elevation
            ) {
                childScope?.let { LuaScopeComponent(it, this) }
            }
        }

        register("ScrollableColumn") { props, childScope ->
            val modifier = resolveModifier(props["modifier"]).verticalScroll(rememberScrollState())
            Column(
                modifier = modifier,
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start
            ) {
                childScope?.let { LuaScopeComponent(it, this) }
            }
        }

        register("ScrollableRow") { props, childScope ->
            val modifier = resolveModifier(props["modifier"]).horizontalScroll(rememberScrollState())
            Row(
                modifier = modifier,
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.Top
            ) {
                childScope?.let { LuaScopeComponent(it, this) }
            }
        }

        register("Spacer") { props, _ ->
            Spacer(modifier = resolveModifier(props["modifier"]))
        }

        register("Divider") { props, _ ->
            HorizontalDivider(
                modifier = resolveModifier(props["modifier"]),
                color = resolveColor(props["color"], DividerDefaults.color)
            )
        }

        // --- 2. 按钮类组件 (Material 3) ---

        register("Button") { props, childScope ->
            val onClick = props["onClick"] as? LuaFunction
            Button(
                onClick = { onClick?.call() },
                modifier = resolveModifier(props["modifier"])
            ) {
                childScope?.let { LuaScopeComponent(it, this) }
            }
        }

        register("ElevatedButton") { props, childScope ->
            val onClick = props["onClick"] as? LuaFunction
            ElevatedButton(
                onClick = { onClick?.call() },
                modifier = resolveModifier(props["modifier"])
            ) {
                childScope?.let { LuaScopeComponent(it, this) }
            }
        }

        register("FilledTonalButton") { props, childScope ->
            val onClick = props["onClick"] as? LuaFunction
            FilledTonalButton(
                onClick = { onClick?.call() },
                modifier = resolveModifier(props["modifier"])
            ) {
                childScope?.let { LuaScopeComponent(it, this) }
            }
        }

        register("OutlinedButton") { props, childScope ->
            val onClick = props["onClick"] as? LuaFunction
            OutlinedButton(
                onClick = { onClick?.call() },
                modifier = resolveModifier(props["modifier"])
            ) {
                childScope?.let { LuaScopeComponent(it, this) }
            }
        }

        register("TextButton") { props, childScope ->
            val onClick = props["onClick"] as? LuaFunction
            TextButton(
                onClick = { onClick?.call() },
                modifier = resolveModifier(props["modifier"])
            ) {
                childScope?.let { LuaScopeComponent(it, this) }
            }
        }

        register("IconButton") { props, childScope ->
            val onClick = props["onClick"] as? LuaFunction
            IconButton(
                onClick = { onClick?.call() },
                modifier = resolveModifier(props["modifier"])
            ) {
                childScope?.let { LuaScopeComponent(it, this) }
            }
        }

        // --- 3. 卡片类组件 (Material 3) ---

        register("Card") { props, childScope ->
            Card(
                modifier = resolveModifier(props["modifier"])
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    childScope?.let { LuaScopeComponent(it, this) }
                }
            }
        }

        register("ElevatedCard") { props, childScope ->
            ElevatedCard(
                modifier = resolveModifier(props["modifier"])
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    childScope?.let { LuaScopeComponent(it, this) }
                }
            }
        }

        register("OutlinedCard") { props, childScope ->
            OutlinedCard(
                modifier = resolveModifier(props["modifier"])
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    childScope?.let { LuaScopeComponent(it, this) }
                }
            }
        }

        // --- 4. 输入与选择类组件 ---

        register("TextField") { props, _ ->
            val valueProp = props["value"]
            val onValueChange = props["onValueChange"] as? LuaFunction
            val valueText = when (valueProp) {
                is LuaState -> valueProp.get()?.toString() ?: ""
                else -> valueProp?.toString() ?: ""
            }
            val labelProp = props["label"]?.toString()
            TextField(
                value = valueText,
                onValueChange = { newVal -> onValueChange?.call(LuaValue.valueOf(newVal)) },
                modifier = resolveModifier(props["modifier"]),
                label = labelProp?.let { { Text(it) } }
            )
        }

        register("OutlinedTextField") { props, _ ->
            val valueProp = props["value"]
            val onValueChange = props["onValueChange"] as? LuaFunction
            val valueText = when (valueProp) {
                is LuaState -> valueProp.get()?.toString() ?: ""
                else -> valueProp?.toString() ?: ""
            }
            val labelProp = props["label"]?.toString()
            OutlinedTextField(
                value = valueText,
                onValueChange = { newVal -> onValueChange?.call(LuaValue.valueOf(newVal)) },
                modifier = resolveModifier(props["modifier"]),
                label = labelProp?.let { { Text(it) } }
            )
        }

        register("Checkbox") { props, _ ->
            val checkedProp = props["checked"]
            val checked = when (checkedProp) {
                is LuaState -> checkedProp.get() as? Boolean ?: false
                else -> checkedProp as? Boolean ?: false
            }
            val onCheckedChange = props["onCheckedChange"] as? LuaFunction
            Checkbox(
                checked = checked,
                onCheckedChange = { newVal -> onCheckedChange?.call(LuaValue.valueOf(newVal)) },
                modifier = resolveModifier(props["modifier"])
            )
        }

        register("Switch") { props, _ ->
            val checkedProp = props["checked"]
            val checked = when (checkedProp) {
                is LuaState -> checkedProp.get() as? Boolean ?: false
                else -> checkedProp as? Boolean ?: false
            }
            val onCheckedChange = props["onCheckedChange"] as? LuaFunction
            Switch(
                checked = checked,
                onCheckedChange = { newVal -> onCheckedChange?.call(LuaValue.valueOf(newVal)) },
                modifier = resolveModifier(props["modifier"])
            )
        }

        register("RadioButton") { props, _ ->
            val selectedProp = props["selected"]
            val selected = when (selectedProp) {
                is LuaState -> selectedProp.get() as? Boolean ?: false
                else -> selectedProp as? Boolean ?: false
            }
            val onClick = props["onClick"] as? LuaFunction
            RadioButton(
                selected = selected,
                onClick = { onClick?.call() },
                modifier = resolveModifier(props["modifier"])
            )
        }

        // --- 5. 指示器类组件 ---

        register("CircularProgressIndicator") { props, _ ->
            CircularProgressIndicator(
                modifier = resolveModifier(props["modifier"]),
                color = resolveColor(props["color"], ProgressIndicatorDefaults.circularColor)
            )
        }

        register("LinearProgressIndicator") { props, _ ->
            LinearProgressIndicator(
                modifier = resolveModifier(props["modifier"]),
                color = resolveColor(props["color"], ProgressIndicatorDefaults.linearColor)
            )
        }

        // --- 6. 图标类组件 ---

        register("Icon") { props, _ ->
            val iconName = props["icon"]?.toString() ?: "info"
            val tint = resolveColor(props["tint"], LocalContentColor.current)
            Icon(
                imageVector = resolveIcon(iconName),
                contentDescription = iconName,
                modifier = resolveModifier(props["modifier"]),
                tint = tint
            )
        }

        // --- 7. 动态列表组件 (Lazy Lists) ---

        register("LazyColumn") { props, _ ->
            val itemsList = props["items"] as? List<*> ?: emptyList<Any?>()
            val itemContent = props["itemContent"] as? LuaFunction
            val modifier = resolveModifier(props["modifier"])

            LazyColumn(
                modifier = modifier,
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(itemsList.size) { index ->
                    val item = itemsList[index]
                    if (itemContent != null) {
                        val itemScope = remember(index) { LuaScope(itemContent) }
                        val luaInstance = LuaBridge.javaToLuaValue(item)
                        val indexValue = LuaValue.valueOf(index + 1)
                        LuaScopeComponent(itemScope, this, luaInstance, indexValue)
                    }
                }
            }
        }
    }

    fun register(name: String, renderer: @Composable (props: Map<String, Any?>, childScope: LuaScope?) -> Unit) {
        components[name] = renderer
    }

    // 解析 Box 内子项的对齐
    fun resolveBoxAlignment(alignStr: String): Alignment {
        return when (alignStr.lowercase()) {
            "center" -> Alignment.Center
            "topstart" -> Alignment.TopStart
            "topcenter" -> Alignment.TopCenter
            "topend" -> Alignment.TopEnd
            "centerstart" -> Alignment.CenterStart
            "centerend" -> Alignment.CenterEnd
            "bottomstart" -> Alignment.BottomStart
            "bottomcenter" -> Alignment.BottomCenter
            "bottomend" -> Alignment.BottomEnd
            else -> Alignment.TopStart
        }
    }

    // 解析 Column 内子项的横向对齐
    fun resolveColumnAlignment(alignStr: String): Alignment.Horizontal {
        return when (alignStr.lowercase()) {
            "center", "centerhorizontally" -> Alignment.CenterHorizontally
            "start" -> Alignment.Start
            "end" -> Alignment.End
            else -> Alignment.Start
        }
    }

    // 解析 Row 内子项的纵向对齐
    fun resolveRowAlignment(alignStr: String): Alignment.Vertical {
        return when (alignStr.lowercase()) {
            "center", "centervertically" -> Alignment.CenterVertically
            "top" -> Alignment.Top
            "bottom" -> Alignment.Bottom
            else -> Alignment.Top
        }
    }

    // 解析内置 Icon
    private fun resolveIcon(name: String): ImageVector {
        return when (name.lowercase()) {
            "refresh" -> Icons.Default.Refresh
            "add" -> Icons.Default.Add
            "settings" -> Icons.Default.Settings
            "favorite" -> Icons.Default.Favorite
            "close" -> Icons.Default.Close
            "menu" -> Icons.Default.Menu
            "home" -> Icons.Default.Home
            "search" -> Icons.Default.Search
            "person" -> Icons.Default.Person
            "arrowback", "arrow_back" -> Icons.Default.ArrowBack
            "arrowforward", "arrow_forward" -> Icons.Default.ArrowForward
            "check" -> Icons.Default.Check
            "info" -> Icons.Default.Info
            "star" -> Icons.Default.Star
            "share" -> Icons.Default.Share
            "delete" -> Icons.Default.Delete
            "edit" -> Icons.Default.Edit
            "play" -> Icons.Default.PlayArrow
            else -> Icons.Default.Info
        }
    }
}
