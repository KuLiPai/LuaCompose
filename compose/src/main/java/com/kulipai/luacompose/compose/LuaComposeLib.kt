package com.kulipai.luacompose.compose

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.kulipai.luacompose.compose.script.ScriptFunction
import com.kulipai.luacompose.compose.script.ScriptTable
import com.kulipai.luacompose.compose.script.ScriptValue
import com.kulipai.luacompose.compose.runtime.ComposeBridge
import com.kulipai.luacompose.compose.runtime.ComposeNode
import com.kulipai.luacompose.compose.ui.resolveDp
import com.kulipai.luacompose.compose.ui.resolveSp

object LuaComposeLib {
    var rootContentFunc: ScriptFunction? = null

    fun inject(env: ScriptTable): ScriptTable {
        val composeTable = ComposeBridge.engine.createTable()
        env.set("compose", composeTable)

        composeTable.set("dp", ComposeBridge.engine.createFunction { args ->
            ComposeBridge.javaToScript(resolveDp(ComposeBridge.scriptToJava(args[0])))
        })
        
        composeTable.set("sp", ComposeBridge.engine.createFunction { args ->
            ComposeBridge.javaToScript(resolveSp(ComposeBridge.scriptToJava(args[0])))
        })

        composeTable.set("setContent", ComposeBridge.engine.createFunction { args ->
            rootContentFunc = args[0].asFunction()
            ComposeBridge.engine.createNil()
        })

        composeTable.set("state", ComposeBridge.engine.createFunction { args ->
            val scope = ComposeBridge.getActiveScope()
                ?: throw RuntimeException("compose.state() 必须在 Compose 上下文中调用")
            scope.getOrCreateState(args[0])
        })

        composeTable.set("remember", ComposeBridge.engine.createFunction { args ->
            val scope = ComposeBridge.getActiveScope()
                ?: throw RuntimeException("compose.remember() 必须在 Compose 上下文中调用")
            scope.getOrCreateRemember(args[0].asFunction())
        })

        composeTable.set("derivedStateOf", ComposeBridge.engine.createFunction { args ->
            val scope = ComposeBridge.getActiveScope()
                ?: throw RuntimeException("compose.derivedStateOf() 必须在 Compose 上下文中调用")
            scope.getOrCreateDerivedState(args[0].asFunction())
        })

        composeTable.set("LaunchedEffect", ComposeBridge.engine.createFunction { args ->
            val effectFunc = args[0]
            val activeScope = ComposeBridge.getActiveScope()
            if (activeScope != null && effectFunc.isFunction()) {
                val key = "effect_${effectFunc.hashCode()}"
                if (activeScope.effectStates[key] == null) {
                    activeScope.effectStates[key] = true
                    activeScope.coroutineScope?.launch(Dispatchers.Default) {
                        try {
                            effectFunc.asFunction().call()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
            ComposeBridge.engine.createNil()
        })

        composeTable.set("DisposableEffect", ComposeBridge.engine.createFunction { args ->
            val effectFunc = args[0]
            val activeScope = ComposeBridge.getActiveScope()
            if (activeScope != null && effectFunc.isFunction()) {
                val key = "effect_${effectFunc.hashCode()}"
                if (activeScope.effectStates[key] == null) {
                    activeScope.effectStates[key] = true
                    effectFunc.asFunction().call()
                }
            }
            ComposeBridge.engine.createNil()
        })

        LuaComposeRegistry.plugins.forEach { plugin ->
            val targetTable = if (plugin.namespace != null) {
                val parts = plugin.namespace!!.split(".")
                var currentTable = composeTable
                for (part in parts) {
                    var nextTable = currentTable.get(part)
                    if (nextTable.isNil()) {
                        nextTable = ComposeBridge.engine.createTable()
                        currentTable.set(part, nextTable)
                    }
                    currentTable = nextTable.asTable()
                }
                currentTable
            } else {
                composeTable
            }

            plugin.injectGlobals(targetTable)

            plugin.getComponents().forEach { (componentName, _) ->
                val fullTypeName =
                    if (plugin.namespace != null) "${plugin.namespace}.$componentName" else componentName
                
                val func = ComposeBridge.engine.createFunction { args ->
                    val arg = args.getOrNull(0) ?: ComposeBridge.engine.createNil()
                    val props = mutableMapOf<String, Any?>()
                    var contentFunc: ScriptFunction? = null
                    if (arg.isFunction()) {
                        contentFunc = arg.asFunction()
                    } else if (arg.isTable()) {
                        val scriptTable = arg.asTable()
                        props.putAll(ComposeBridge.scriptTableToMap(scriptTable))
                        val content = scriptTable.get("content")
                        if (content.isFunction()) {
                            contentFunc = content.asFunction()
                            props.remove("content")
                        }
                    }

                    val activeScope = ComposeBridge.getActiveScope()
                    val childScope = if (contentFunc != null && activeScope != null) {
                        activeScope.getOrCreateChildScope(contentFunc)
                    } else null

                    val node = ComposeNode(fullTypeName, props, childScope)
                    ComposeBridge.getActiveNodeList()?.add(node)
                    ComposeBridge.engine.createNil()
                }
                targetTable.set(componentName, func)
            }
        }

        return composeTable
    }
}
