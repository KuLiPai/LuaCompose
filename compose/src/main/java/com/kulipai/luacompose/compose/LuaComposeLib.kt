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
    init {
        ComposeBridge.converters[androidx.compose.ui.unit.Dp::class.java] = { obj ->
            val dp = obj as androidx.compose.ui.unit.Dp
            val table = ComposeBridge.engine.createTable()
            table.set("value", ComposeBridge.engine.createValue(dp.value.toDouble()))
            table.set("toPx", ComposeBridge.engine.createFunction { _ ->
                val density = android.content.res.Resources.getSystem().displayMetrics.density
                ComposeBridge.engine.createValue((dp.value * density).toDouble())
            })
            table.set("_javaDp", ComposeBridge.engine.createUserdata(dp))
            table
        }
    }

    var rootContentFunc: ScriptFunction? = null
    var globalEnv: ScriptTable? = null

    private fun ensureChildTable(parent: ScriptTable, key: String): ScriptTable {
        val existing = parent.rawget(key)
        if (!existing.isNil() && existing.isTable()) {
            return existing.asTable()
        }
        val child = ComposeBridge.engine.createTable()
        parent.set(key, child)
        return child
    }

    private fun setNestedValue(root: ScriptTable, path: String, value: ScriptValue) {
        val parts = path.split(".")
        if (parts.size == 1) {
            root.set(path, value)
            return
        }
        var current = root
        for (part in parts.dropLast(1)) {
            current = ensureChildTable(current, part)
        }
        current.set(parts.last(), value)
    }

    private fun hasNestedValue(root: ScriptTable, path: String): Boolean {
        val parts = path.split(".")
        var current = root
        for ((index, part) in parts.withIndex()) {
            val existing = current.rawget(part)
            if (existing.isNil()) return false
            if (index == parts.lastIndex) return true
            if (!existing.isTable()) return false
            current = existing.asTable()
        }
        return false
    }

    fun clearRuntimeState() {
        rootContentFunc = null
        globalEnv = null
    }

    fun inject(env: ScriptTable): ScriptTable {
        clearRuntimeState()
        globalEnv = env
        val composeTable = ComposeBridge.createLazyNamespace("androidx.compose")
        env.set("compose", composeTable)

        composeTable.set("delay", ComposeBridge.engine.createFunction { args ->
            val ms = args[0].toLong()
            kotlinx.coroutines.runBlocking {
                kotlinx.coroutines.delay(ms)
            }
            ComposeBridge.engine.createNil()
        })

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
            if (args.isEmpty()) throw RuntimeException("compose.remember() requires at least a calculation block")
            val func = args.last()
            if (!func.isFunction()) throw RuntimeException("The last argument to compose.remember() must be a function")
            
            val keys = args.dropLast(1).map { ComposeBridge.scriptToJava(it) }
            scope.getOrCreateRemember(func.asFunction(), keys)
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
                    activeScope.coroutineScope?.let { coroutineScope ->
                        val coroutineCreate = env.get("coroutine").asTable().get("create").asFunction()
                        val coroutineResume = env.get("coroutine").asTable().get("resume").asFunction()
                        val coroutineStatus = env.get("coroutine").asTable().get("status").asFunction()
                        
                        val luaThread = coroutineCreate.call(effectFunc)
                        
                        fun resumeLoop(arg: ScriptValue) {
                            coroutineScope.launch {
                                try {
                                    val result = coroutineResume.call(luaThread, arg)
                                    if (result.isBoolean() && result.toBoolean()) {
                                        val status = coroutineStatus.call(luaThread)
                                        if (status.isString() && status.toStringValue() == "suspended") {
                                            androidx.compose.runtime.withFrameNanos { frameTime ->
                                                resumeLoop(ComposeBridge.engine.createValue(frameTime.toDouble()))
                                            }
                                        }
                                    } else {
                                        System.err.println("Coroutine error")
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                        
                        resumeLoop(ComposeBridge.engine.createNil())
                    }
                }
            }
            ComposeBridge.engine.createNil()
        })

        composeTable.set("withFrameNanos", ComposeBridge.engine.createFunction { args ->
            val callback = args[0]
            if (callback.isFunction()) {
                val yieldFunc = env.get("coroutine").asTable().get("yield").asFunction()
                val yieldRes = yieldFunc.call(ComposeBridge.engine.createValue("requestFrameNanos"))
                val frameTime = yieldRes.toDouble()
                callback.asFunction().call(ComposeBridge.engine.createValue(frameTime))
            }
            ComposeBridge.engine.createNil()
        })

        composeTable.set("with", ComposeBridge.engine.createFunction { args ->
            val receiver = ComposeBridge.scriptToJava(args.getOrNull(0))
            val block = args.getOrNull(1)
                ?: throw RuntimeException("compose.with(receiver, block) requires a block")
            if (!block.isFunction()) {
                throw RuntimeException("compose.with(receiver, block) block must be a function")
            }

            if (receiver != null) {
                ComposeBridge.pushContextReceiver(receiver)
                try {
                    block.asFunction().call()
                } finally {
                    ComposeBridge.popContextReceiver()
                }
            } else {
                block.asFunction().call()
            }
        })

        composeTable.set("key", ComposeBridge.engine.createFunction { args ->
            val scope = ComposeBridge.getActiveScope()
                ?: throw RuntimeException("compose.key() 必须在 Compose 上下文中调用")
            
            val numArgs = args.size
            if (numArgs == 0) return@createFunction ComposeBridge.engine.createNil()
            
            val contentFunc = args[numArgs - 1]
            if (!contentFunc.isFunction()) return@createFunction ComposeBridge.engine.createNil()
            
            val keys = mutableListOf<Any>()
            for (i in 0 until numArgs - 1) {
                keys.add(ComposeBridge.scriptToJava(args[i]) ?: "nil")
            }
            val stringKey = "key_${keys.joinToString("_")}"
            
            val childScope = scope.getOrCreateChildScope(contentFunc.asFunction(), stringKey)
            
            ComposeBridge.pushActiveScope(childScope)
            childScope.statesCount = 0
            childScope.remembersCount = 0
            childScope.childScopesCount = 0
            childScope.accessedStates.clear()
            childScope.accessedRemembers.clear()
            childScope.accessedChildScopes.clear()
            
            var result: ScriptValue = ComposeBridge.engine.createNil()
            try {
                result = contentFunc.asFunction().call()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                ComposeBridge.popActiveScope()
            }
            
            childScope.states.keys.retainAll(childScope.accessedStates)
            childScope.remembers.keys.retainAll(childScope.accessedRemembers)
            childScope.childScopes.keys.retainAll(childScope.accessedChildScopes)
            
            result
        })

        val contextMeta = ComposeBridge.engine.createTable()
        contextMeta.set("__index", ComposeBridge.engine.createFunction { args ->
            val key = args[1].toStringValue()
            if (key == "current") {
                val scope = ComposeBridge.getActiveScope()
                return@createFunction scope?.context?.let { ComposeBridge.engine.coerceJavaToScript(it) } ?: ComposeBridge.engine.createNil()
            }
            ComposeBridge.engine.createNil()
        })
        val localContextTable = ComposeBridge.engine.createTable()
        localContextTable.setMetatable(contextMeta)
        composeTable.set("LocalContext", localContextTable)

        val densityMeta = ComposeBridge.engine.createTable()
        densityMeta.set("__index", ComposeBridge.engine.createFunction { args ->
            val key = args[1].toStringValue()
            if (key == "current") {
                val scope = ComposeBridge.getActiveScope()
                return@createFunction scope?.density?.let { ComposeBridge.engine.coerceJavaToScript(it) } ?: ComposeBridge.engine.createNil()
            }
            ComposeBridge.engine.createNil()
        })
        val localDensityTable = ComposeBridge.engine.createTable()
        localDensityTable.setMetatable(densityMeta)
        composeTable.set("LocalDensity", localDensityTable)

        val configurationMeta = ComposeBridge.engine.createTable()
        configurationMeta.set("__index", ComposeBridge.engine.createFunction { args ->
            val key = args[1].toStringValue()
            if (key == "current") {
                val scope = ComposeBridge.getActiveScope()
                return@createFunction scope?.configuration?.let { ComposeBridge.engine.coerceJavaToScript(it) } ?: ComposeBridge.engine.createNil()
            }
            ComposeBridge.engine.createNil()
        })
        val localConfigurationTable = ComposeBridge.engine.createTable()
        localConfigurationTable.setMetatable(configurationMeta)
        composeTable.set("LocalConfiguration", localConfigurationTable)

        composeTable.set("rememberCoroutineScope", ComposeBridge.engine.createFunction {
            val scope = ComposeBridge.getActiveScope()
            val coroutineScope = scope?.coroutineScope ?: kotlinx.coroutines.GlobalScope
            val table = ComposeBridge.engine.createTable()
            table.set("launch", ComposeBridge.engine.createFunction { args ->
                val isMethodCall = args.getOrNull(0) == table
                val block = if (isMethodCall) args.getOrNull(1) else args.getOrNull(0)
                if (block != null && block.isFunction()) {
                    val coroutineCreate = env.get("coroutine").asTable().get("create").asFunction()
                    val coroutineResume = env.get("coroutine").asTable().get("resume").asFunction()
                    val coroutineStatus = env.get("coroutine").asTable().get("status").asFunction()
                    val luaThread = coroutineCreate.call(block)
                    
                    fun resumeLoop(arg: com.kulipai.luacompose.compose.script.ScriptValue) {
                        coroutineScope.launch {
                            try {
                                val result = coroutineResume.call(luaThread, arg)
                                if (result.isBoolean() && result.toBoolean()) {
                                    val status = coroutineStatus.call(luaThread)
                                    if (status.isString() && status.toStringValue() == "suspended") {
                                        androidx.compose.runtime.withFrameNanos { frameTime ->
                                            resumeLoop(ComposeBridge.engine.createValue(frameTime.toDouble()))
                                        }
                                    }
                                } else {
                                    System.err.println("Coroutine error in scope.launch")
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                    resumeLoop(ComposeBridge.engine.createNil())
                }
                ComposeBridge.engine.createNil()
            })
            table
        })

        composeTable.set("LaunchedEffect", ComposeBridge.engine.createFunction { args ->
            val scope = ComposeBridge.getActiveScope() ?: return@createFunction ComposeBridge.engine.createNil()
            val numArgs = args.size
            if (numArgs == 0) return@createFunction ComposeBridge.engine.createNil()
            
            val effectFunc = args[numArgs - 1]
            if (effectFunc.isFunction()) {
                val keys = mutableListOf<Any?>()
                for (i in 0 until numArgs - 1) {
                    keys.add(ComposeBridge.scriptToJava(args[i]))
                }
                val effectKey = "launched_effect_${keys.hashCode()}"
                if (scope.effectStates[effectKey] == null) {
                    scope.effectStates[effectKey] = true
                    scope.coroutineScope?.let { coroutineScope ->
                        val coroutineCreate = env.get("coroutine").asTable().get("create").asFunction()
                        val coroutineResume = env.get("coroutine").asTable().get("resume").asFunction()
                        val coroutineStatus = env.get("coroutine").asTable().get("status").asFunction()
                        
                        val luaThread = coroutineCreate.call(effectFunc)
                        
                        fun resumeLoop(arg: ScriptValue) {
                            coroutineScope.launch {
                                try {
                                    val result = coroutineResume.call(luaThread, arg)
                                    if (result.isBoolean() && result.toBoolean()) {
                                        val status = coroutineStatus.call(luaThread)
                                        if (status.isString() && status.toStringValue() == "suspended") {
                                            androidx.compose.runtime.withFrameNanos { frameTime ->
                                                resumeLoop(ComposeBridge.engine.createValue(frameTime.toDouble()))
                                            }
                                        }
                                    } else {
                                        System.err.println("Coroutine error")
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                        
                        resumeLoop(ComposeBridge.engine.createNil())
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
                var currentPackage = "androidx.compose"
                for (part in parts) {
                    currentPackage = "$currentPackage.$part"
                    var nextTable = currentTable.get(part)
                    if (nextTable.isNil()) {
                        nextTable = ComposeBridge.createLazyNamespace(currentPackage)
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
                if (!hasNestedValue(targetTable, componentName)) {
                    setNestedValue(targetTable, componentName, func)
                }
            }
        }

        return composeTable
    }
}
