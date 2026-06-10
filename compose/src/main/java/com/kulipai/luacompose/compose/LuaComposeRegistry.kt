package com.kulipai.luacompose.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.luaj.LuaFunction
import org.luaj.LuaValue
import org.luaj.lib.jse.CoerceJavaToLua

object LuaComposeRegistry {
    val components = mutableMapOf<String, @Composable (props: Map<String, Any?>, children: List<LuaNode>) -> Unit>()

    init {
        // --- 1. 基础排版组件 ---
        
        // Text
        register("Text") { props, _ ->
            val textVal = when (val textProp = props["text"]) {
                is LuaState -> textProp.get()?.toString() ?: ""
                else -> textProp?.toString() ?: ""
            }
            Text(
                text = textVal,
                modifier = resolveModifier(props["modifier"]),
                color = resolveColor(props["color"])
            )
        }

        // Column
        register("Column") { props, children ->
            val modifier = resolveModifier(props["modifier"])
            Column(
                modifier = modifier,
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start
            ) {
                renderChildren(children, this)
            }
        }

        // Row
        register("Row") { props, children ->
            val modifier = resolveModifier(props["modifier"])
            Row(
                modifier = modifier,
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.Top
            ) {
                renderChildren(children, this)
            }
        }

        // Box
        register("Box") { props, children ->
            val modifier = resolveModifier(props["modifier"])
            Box(
                modifier = modifier,
                contentAlignment = Alignment.TopStart
            ) {
                renderChildren(children, this)
            }
        }

        // Spacer
        register("Spacer") { props, _ ->
            Spacer(modifier = resolveModifier(props["modifier"]))
        }

        // Divider
        register("Divider") { props, _ ->
            HorizontalDivider(
                modifier = resolveModifier(props["modifier"]),
                color = resolveColor(props["color"], DividerDefaults.color)
            )
        }

        // --- 2. 按钮类组件 (Material 3) ---

        // Button
        register("Button") { props, children ->
            val onClick = props["onClick"] as? LuaFunction
            Button(
                onClick = { onClick?.call() },
                modifier = resolveModifier(props["modifier"])
            ) {
                children.forEach { LuaNodeRenderer(it) }
            }
        }

        // ElevatedButton
        register("ElevatedButton") { props, children ->
            val onClick = props["onClick"] as? LuaFunction
            ElevatedButton(
                onClick = { onClick?.call() },
                modifier = resolveModifier(props["modifier"])
            ) {
                children.forEach { LuaNodeRenderer(it) }
            }
        }

        // FilledTonalButton
        register("FilledTonalButton") { props, children ->
            val onClick = props["onClick"] as? LuaFunction
            FilledTonalButton(
                onClick = { onClick?.call() },
                modifier = resolveModifier(props["modifier"])
            ) {
                children.forEach { LuaNodeRenderer(it) }
            }
        }

        // OutlinedButton
        register("OutlinedButton") { props, children ->
            val onClick = props["onClick"] as? LuaFunction
            OutlinedButton(
                onClick = { onClick?.call() },
                modifier = resolveModifier(props["modifier"])
            ) {
                children.forEach { LuaNodeRenderer(it) }
            }
        }

        // TextButton
        register("TextButton") { props, children ->
            val onClick = props["onClick"] as? LuaFunction
            TextButton(
                onClick = { onClick?.call() },
                modifier = resolveModifier(props["modifier"])
            ) {
                children.forEach { LuaNodeRenderer(it) }
            }
        }

        // IconButton
        register("IconButton") { props, children ->
            val onClick = props["onClick"] as? LuaFunction
            IconButton(
                onClick = { onClick?.call() },
                modifier = resolveModifier(props["modifier"])
            ) {
                children.forEach { LuaNodeRenderer(it) }
            }
        }

        // --- 3. 卡片类组件 (Material 3) ---

        // Card
        register("Card") { props, children ->
            Card(
                modifier = resolveModifier(props["modifier"])
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    children.forEach { LuaNodeRenderer(it) }
                }
            }
        }

        // ElevatedButton
        register("ElevatedCard") { props, children ->
            ElevatedCard(
                modifier = resolveModifier(props["modifier"])
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    children.forEach { LuaNodeRenderer(it) }
                }
            }
        }

        // OutlinedCard
        register("OutlinedCard") { props, children ->
            OutlinedCard(
                modifier = resolveModifier(props["modifier"])
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    children.forEach { LuaNodeRenderer(it) }
                }
            }
        }

        // --- 4. 输入与选择类组件 ---

        // TextField
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

        // OutlinedTextField
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

        // Checkbox
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

        // Switch
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

        // RadioButton
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

        // CircularProgressIndicator
        register("CircularProgressIndicator") { props, _ ->
            CircularProgressIndicator(
                modifier = resolveModifier(props["modifier"]),
                color = resolveColor(props["color"], ProgressIndicatorDefaults.circularColor)
            )
        }

        // LinearProgressIndicator
        register("LinearProgressIndicator") { props, _ ->
            LinearProgressIndicator(
                modifier = resolveModifier(props["modifier"]),
                color = resolveColor(props["color"], ProgressIndicatorDefaults.linearColor)
            )
        }

        // --- 6. 图标类组件 ---

        // Icon
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

        // LazyColumn
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
                        // 在渲染项内部，动态构建虚拟节点
                        val luaActiveScope = LuaBridge.getActiveScope()
                        val nodesVal = if (luaActiveScope != null) {
                            // 执行 Lua 模板函数并收集它产生的虚拟节点
                            val luaInstance = CoerceJavaToLua.coerce(item)
                            val indexValue = LuaValue.valueOf(index + 1) // 转换为 Lua 的 1 进制索引
                            itemContent.call(luaInstance, indexValue)
                            
                            // 针对单项生成虚拟节点列表
                            val resTable = luaActiveScope.execute()
                            resTable
                        } else {
                            emptyList()
                        }
                        
                        // 渲染每一项的虚拟节点
                        nodesVal.forEach { node ->
                            LuaNodeRenderer(node)
                        }
                    }
                }
            }
        }
    }

    fun register(name: String, renderer: @Composable (props: Map<String, Any?>, children: List<LuaNode>) -> Unit) {
        components[name] = renderer
    }

    // 解析 Box 内子项的对齐
    private fun resolveBoxAlignment(alignStr: String): Alignment {
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
    private fun resolveColumnAlignment(alignStr: String): Alignment.Horizontal {
        return when (alignStr.lowercase()) {
            "center", "centerhorizontally" -> Alignment.CenterHorizontally
            "start" -> Alignment.Start
            "end" -> Alignment.End
            else -> Alignment.Start
        }
    }

    // 解析 Row 内子项的纵向对齐
    private fun resolveRowAlignment(alignStr: String): Alignment.Vertical {
        return when (alignStr.lowercase()) {
            "center", "centervertically" -> Alignment.CenterVertically
            "top" -> Alignment.Top
            "bottom" -> Alignment.Bottom
            else -> Alignment.Top
        }
    }

    // 辅助方法：结合 Scope 自动应用子组件的 Alignment 修饰符
    @Composable
    private fun renderChildren(children: List<LuaNode>, scope: Any) {
        children.forEach { child ->
            val childModifier = resolveModifier(child.props["modifier"])
            val alignmentStr = (child.props["modifier"] as? LuaModifier)?.alignmentStr

            val finalModifier = if (alignmentStr != null) {
                when (scope) {
                    is BoxScope -> {
                        val alignment = resolveBoxAlignment(alignmentStr)
                        with(scope) { childModifier.align(alignment) }
                    }
                    is ColumnScope -> {
                        val alignment = resolveColumnAlignment(alignmentStr)
                        with(scope) { childModifier.align(alignment) }
                    }
                    is RowScope -> {
                        val alignment = resolveRowAlignment(alignmentStr)
                        with(scope) { childModifier.align(alignment) }
                    }
                    else -> childModifier
                }
            } else {
                childModifier
            }

            // 更新临时的渲染修饰符
            val tempNode = child.copy(props = child.props.toMutableMap().apply {
                this["modifier"] = finalModifier
            })
            LuaNodeRenderer(tempNode)
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
