package com.kulipai.luacompose.compose.foundation.lazy


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import com.kulipai.luacompose.compose.runtime.ComposeBridge
import com.kulipai.luacompose.compose.runtime.ComposeScope
import com.kulipai.luacompose.compose.script.ScriptFunction
import com.kulipai.luacompose.compose.ui.graphics.ComposeScopeComponent
import com.kulipai.luacompose.compose.ui.resolveModifier

internal fun registerLazyComponents(map: MutableMap<String, @Composable (props: Map<String, Any?>, childScope: ComposeScope?) -> Unit>) {
    map["LazyColumn"] = { props, _ ->
        val itemsList = props["items"] as? List<*> ?: emptyList<Any?>()
        val itemContent = props["itemContent"] as? ScriptFunction
        val modifier = resolveModifier(props["modifier"])

        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(itemsList.size) { index ->
                val item = itemsList[index]
                if (itemContent != null) {
                    val itemScope = remember(index) { ComposeScope(itemContent) }
                    val luaInstance = ComposeBridge.javaToScript(item)
                    val indexValue = ComposeBridge.engine.createValue(index + 1)
                    ComposeScopeComponent(itemScope, this, luaInstance, indexValue)
                }
            }
        }
    }
}
