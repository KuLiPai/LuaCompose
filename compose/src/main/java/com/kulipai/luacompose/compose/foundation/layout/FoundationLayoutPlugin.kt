package com.kulipai.luacompose.compose.foundation.layout

import androidx.compose.runtime.Composable
import com.kulipai.luacompose.compose.runtime.ComposeScope
import com.kulipai.luacompose.compose.runtime.ComposeScriptPlugin
import com.kulipai.luacompose.compose.script.ScriptTable

class FoundationLayoutPlugin : ComposeScriptPlugin {
    override val namespace: String = "foundation.layout"

    override fun getComponents(): Map<String, @Composable (props: Map<String, Any?>, childScope: ComposeScope?) -> Unit> {
        val map =
            mutableMapOf<String, @Composable (props: Map<String, Any?>, childScope: ComposeScope?) -> Unit>()
        registerLayoutComponents(map)
        return map
    }

    override fun injectGlobals(scriptTable: ScriptTable) = Unit
}
