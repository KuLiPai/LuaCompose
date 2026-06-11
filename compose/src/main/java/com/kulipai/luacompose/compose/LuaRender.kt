package com.kulipai.luacompose.compose

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import org.luaj.LuaValue

@Composable
fun LuaScopeComponent(scope: LuaScope, parentComposeScope: Any? = null, vararg args: LuaValue) {
    scope.coroutineScope = rememberCoroutineScope()
    scope.context = LocalContext.current
    scope.density = LocalDensity.current
    scope.configuration = LocalConfiguration.current

    val version by scope.recomposeVersion
    val nodes = remember(version, *args) { scope.execute(*args) }

    for (node in nodes) {
        LuaNodeRenderer(node, parentComposeScope)
    }
}

@Composable
fun LuaNodeRenderer(node: LuaNode, parentComposeScope: Any? = null) {
    val childModifier = resolveModifier(node.props["modifier"])
    val alignmentStr = (node.props["modifier"] as? LuaModifier)?.alignmentStr
    val weightVal = (node.props["modifier"] as? LuaModifier)?.weightVal

    var finalModifier = if (alignmentStr != null && parentComposeScope != null) {
        when (parentComposeScope) {
            is androidx.compose.foundation.layout.BoxScope -> {
                val alignment = LuaComposeRegistry.resolveBoxAlignment(alignmentStr)
                with(parentComposeScope) { childModifier.align(alignment) }
            }
            is androidx.compose.foundation.layout.ColumnScope -> {
                val alignment = LuaComposeRegistry.resolveColumnAlignment(alignmentStr)
                with(parentComposeScope) { childModifier.align(alignment) }
            }
            is androidx.compose.foundation.layout.RowScope -> {
                val alignment = LuaComposeRegistry.resolveRowAlignment(alignmentStr)
                with(parentComposeScope) { childModifier.align(alignment) }
            }
            else -> childModifier
        }
    } else {
        childModifier
    }

    if (weightVal != null && parentComposeScope != null) {
        finalModifier = when (parentComposeScope) {
            is androidx.compose.foundation.layout.ColumnScope -> with(parentComposeScope) { finalModifier.weight(weightVal) }
            is androidx.compose.foundation.layout.RowScope -> with(parentComposeScope) { finalModifier.weight(weightVal) }
            else -> finalModifier
        }
    }

    val finalProps = node.props.toMutableMap()
    finalProps["modifier"] = finalModifier

    val renderer = LuaComposeRegistry.components[node.type]
    if (renderer != null) {
        renderer(finalProps, node.childScope)
    } else {
        Text("组件 [${node.type}] 未在注册表注册", color = Color.Red, modifier = Modifier.padding(8.dp))
    }
}
