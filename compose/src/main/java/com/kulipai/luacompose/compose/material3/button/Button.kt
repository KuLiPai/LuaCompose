package com.kulipai.luacompose.compose.material3.button


import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.kulipai.luacompose.compose.runtime.ComposeScope
import com.kulipai.luacompose.compose.script.ScriptFunction
import com.kulipai.luacompose.compose.ui.graphics.ComposeScopeComponent
import com.kulipai.luacompose.compose.ui.resolveModifier

internal fun registerButtonComponents(map: MutableMap<String, @Composable (props: Map<String, Any?>, childScope: ComposeScope?) -> Unit>) {
    map["Button"] = { props, childScope ->
        val onClick = props["onClick"] as? ScriptFunction
        Button(
            onClick = { onClick?.call() },
            modifier = resolveModifier(props["modifier"])
        ) {
            childScope?.let { ComposeScopeComponent(it, this) }
        }
    }
    map["ElevatedButton"] = { props, childScope ->
        val onClick = props["onClick"] as? ScriptFunction
        ElevatedButton(
            onClick = { onClick?.call() },
            modifier = resolveModifier(props["modifier"])
        ) {
            childScope?.let { ComposeScopeComponent(it, this) }
        }
    }
    map["FilledTonalButton"] = { props, childScope ->
        val onClick = props["onClick"] as? ScriptFunction
        FilledTonalButton(
            onClick = { onClick?.call() },
            modifier = resolveModifier(props["modifier"])
        ) {
            childScope?.let { ComposeScopeComponent(it, this) }
        }
    }
    map["OutlinedButton"] = { props, childScope ->
        val onClick = props["onClick"] as? ScriptFunction
        OutlinedButton(
            onClick = { onClick?.call() },
            modifier = resolveModifier(props["modifier"])
        ) {
            childScope?.let { ComposeScopeComponent(it, this) }
        }
    }
    map["TextButton"] = { props, childScope ->
        val onClick = props["onClick"] as? ScriptFunction
        TextButton(
            onClick = { onClick?.call() },
            modifier = resolveModifier(props["modifier"])
        ) {
            childScope?.let { ComposeScopeComponent(it, this) }
        }
    }
    map["IconButton"] = { props, childScope ->
        val onClick = props["onClick"] as? ScriptFunction
        IconButton(
            onClick = { onClick?.call() },
            modifier = resolveModifier(props["modifier"])
        ) {
            childScope?.let { ComposeScopeComponent(it, null) }
        }
    }
}
