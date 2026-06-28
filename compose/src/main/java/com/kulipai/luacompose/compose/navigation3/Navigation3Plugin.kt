package com.kulipai.luacompose.compose.navigation3

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.navigation3.runtime.NavEntry
import com.kulipai.luacompose.compose.script.ScriptValue
import com.kulipai.luacompose.compose.script.ScriptTable
import com.kulipai.luacompose.compose.runtime.ComposeBridge
import com.kulipai.luacompose.compose.runtime.ComposeScriptPlugin
import com.kulipai.luacompose.compose.runtime.ComposeScope

class Navigation3Plugin : ComposeScriptPlugin {
    override val namespace: String = "navigation3"
    
    override fun getComponents(): Map<String, @Composable (props: Map<String, Any?>, childScope: ComposeScope?) -> Unit> {
        return emptyMap()
    }
    
    override fun injectGlobals(scriptTable: ScriptTable) {
        scriptTable.set("entryProvider", ComposeBridge.engine.createFunction { args ->
            val firstArg = args.getOrNull(0) as? ScriptValue
            val luaTable = firstArg?.asTable() ?: error("entryProvider expects a Lua Table")
            val provider: (Any) -> NavEntry<*> = { key ->
                val routeName = if (key is String) {
                    key
                } else if (key is ScriptTable) {
                    val r = key.get("route")
                    if (r.isString()) r.toStringValue() else error("Key table must have a 'route' string field")
                } else {
                    error("Invalid Lua NavKey: $key. Expected String or Table with 'route'.")
                }
                
                val luaFunc = luaTable.get(routeName)
                if (!luaFunc.isFunction()) {
                    error("No Composable function found for route: $routeName")
                }
                
                NavEntry(key) {
                    val wrappedKey = ComposeBridge.javaToScript(key)
                    luaFunc.asFunction().call(wrappedKey)
                }
            }
            ComposeBridge.engine.createUserdata(provider)
        })

        scriptTable.set("rememberNavBackStack", ComposeBridge.engine.createFunction { args ->
            val initialKeysArg = args.getOrNull(0) as? ScriptValue
            val initialKeys = mutableListOf<Any>()
            
            if (initialKeysArg != null && initialKeysArg.isTable()) {
                val table = initialKeysArg.asTable()
                val len = table.length()
                for (i in 1..len) {
                    val v = table.get(i)
                    if (v.isString()) {
                        initialKeys.add(v.toStringValue())
                    } else {
                        initialKeys.add(v)
                    }
                }
            } else if (initialKeysArg != null && initialKeysArg.isString()) {
                initialKeys.add(initialKeysArg.toStringValue())
            }

            val list = mutableStateListOf<Any>()
            list.addAll(initialKeys)
            ComposeBridge.engine.createUserdata(list)
        })
    }
}
