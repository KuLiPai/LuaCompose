package com.kulipai.luacompose.compose.ui.geometry

import com.kulipai.luacompose.compose.runtime.ComposeScriptPlugin
import com.kulipai.luacompose.compose.runtime.ComposeScope
import com.kulipai.luacompose.compose.script.ScriptTable
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.kulipai.luacompose.compose.runtime.ComposeBridge

class UiGeometryPlugin : ComposeScriptPlugin {
    override val namespace: String = "ui.geometry"
    override fun getComponents(): Map<String, @Composable (props: Map<String, Any?>, childScope: ComposeScope?) -> Unit> = emptyMap()

    override fun injectGlobals(scriptTable: ScriptTable) {
        val sizeMeta = ComposeBridge.engine.createTable()
        sizeMeta.set("__call", ComposeBridge.engine.createFunction { args ->
            val w = args.getOrNull(1)?.let { if (!it.isNil()) it.toFloat() else 0f } ?: 0f
            val h = args.getOrNull(2)?.let { if (!it.isNil()) it.toFloat() else 0f } ?: 0f
            val table = ComposeBridge.engine.createTable()
            table.set("width", ComposeBridge.engine.createValue(w.toDouble()))
            table.set("height", ComposeBridge.engine.createValue(h.toDouble()))
            table.set("_javaSize", ComposeBridge.engine.createUserdata(Size(w, h)))
            table
        })
        val sizeTable = ComposeBridge.engine.createTable()
        sizeTable.setMetatable(sizeMeta)
        scriptTable.set("Size", sizeTable)

        val offsetMeta = ComposeBridge.engine.createTable()
        offsetMeta.set("__call", ComposeBridge.engine.createFunction { args ->
            val x = args.getOrNull(1)?.let { if (!it.isNil()) it.toFloat() else 0f } ?: 0f
            val y = args.getOrNull(2)?.let { if (!it.isNil()) it.toFloat() else 0f } ?: 0f
            val table = ComposeBridge.engine.createTable()
            table.set("x", ComposeBridge.engine.createValue(x.toDouble()))
            table.set("y", ComposeBridge.engine.createValue(y.toDouble()))
            table.set("_javaOffset", ComposeBridge.engine.createUserdata(Offset(x, y)))
            table
        })
        val offsetTable = ComposeBridge.engine.createTable()
        offsetTable.setMetatable(offsetMeta)
        scriptTable.set("Offset", offsetTable)
    }
}
