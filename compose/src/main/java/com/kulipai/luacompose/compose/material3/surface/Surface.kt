package com.kulipai.luacompose.compose.material3.surface


import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.kulipai.luacompose.compose.runtime.ComposeScope
import com.kulipai.luacompose.compose.script.ScriptFunction
import com.kulipai.luacompose.compose.ui.graphics.ComposeScopeComponent
import com.kulipai.luacompose.compose.ui.resolveColor
import com.kulipai.luacompose.compose.ui.resolveDp
import com.kulipai.luacompose.compose.ui.resolveModifier
import com.kulipai.luacompose.compose.ui.resolveShape

internal fun registerSurfaceComponents(map: MutableMap<String, @Composable (props: Map<String, Any?>, childScope: ComposeScope?) -> Unit>) {
    map["Surface"] = { props, childScope ->
        val modifier = resolveModifier(props["modifier"])
        val shape = resolveShape(props["shape"]) ?: RectangleShape
        val color = resolveColor(props["color"], MaterialTheme.colorScheme.surface)
        val contentColor = resolveColor(props["contentColor"], contentColorFor(color))
        val tonalElevation = props["tonalElevation"]?.let { resolveDp(it) } ?: 0.dp
        val shadowElevation = props["shadowElevation"]?.let { resolveDp(it) } ?: 0.dp
        val onClick = props["onClick"] as? ScriptFunction

        if (onClick != null) {
            val enabled = props["enabled"] as? Boolean ?: true
            Surface(
                onClick = { onClick.call() },
                modifier = modifier,
                enabled = enabled,
                shape = shape,
                color = color,
                contentColor = contentColor,
                tonalElevation = tonalElevation,
                shadowElevation = shadowElevation
            ) {
                childScope?.let { ComposeScopeComponent(it, null) }
            }
        } else {
            Surface(
                modifier = modifier,
                shape = shape,
                color = color,
                contentColor = contentColor,
                tonalElevation = tonalElevation,
                shadowElevation = shadowElevation
            ) {
                childScope?.let { ComposeScopeComponent(it, null) }
            }
        }
    }
}
