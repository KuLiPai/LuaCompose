package com.kulipai.luacompose.compose.plugins

import androidx.compose.runtime.Composable
import com.kulipai.luacompose.compose.LuaScope
import org.luaj.LuaTable

interface LuaComposePlugin {
    /**
     * The namespace prefix in Lua (e.g. "material3"). 
     * If null, components are registered to the global `compose` table.
     */
    val namespace: String?

    /**
     * Defines the components to register.
     * The key is the component name (e.g. "Text"), and the value is the Composable renderer.
     */
    fun getComponents(): Map<String, @Composable (props: Map<String, Any?>, childScope: LuaScope?) -> Unit>
    
    /**
     * Defines utility functions/tables to expose under this namespace.
     * For example, material3.CardDefaults or compose.RoundedCornerShape.
     */
    fun injectGlobals(luaTable: LuaTable)
}
