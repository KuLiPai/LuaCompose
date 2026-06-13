package com.kulipai.luacompose.compose.material3.switch


import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import com.kulipai.luacompose.compose.runtime.ComposeBridge
import com.kulipai.luacompose.compose.runtime.ComposeScope
import com.kulipai.luacompose.compose.runtime.ComposeState
import com.kulipai.luacompose.compose.script.ScriptFunction
import com.kulipai.luacompose.compose.ui.resolveModifier

internal fun registerSwitchComponents(map: MutableMap<String, @Composable (props: Map<String, Any?>, childScope: ComposeScope?) -> Unit>) {
    map["Switch"] = { props, _ ->
        val checkedProp = props["checked"]
        val checked = when (checkedProp) {
            is ComposeState -> checkedProp.get() as? Boolean ?: false
            else -> checkedProp as? Boolean ?: false
        }
        val onCheckedChange = props["onCheckedChange"] as? ScriptFunction
        Switch(
            checked = checked,
            onCheckedChange = { newVal ->
                onCheckedChange?.call(
                    ComposeBridge.engine.createValue(
                        newVal
                    )
                )
            },
            modifier = resolveModifier(props["modifier"])
        )
    }
}
