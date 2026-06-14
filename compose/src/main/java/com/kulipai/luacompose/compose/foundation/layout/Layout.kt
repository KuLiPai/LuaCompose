package com.kulipai.luacompose.compose.foundation.layout


import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import com.kulipai.luacompose.compose.LuaComposeRegistry
import com.kulipai.luacompose.compose.runtime.ComposeScope
import com.kulipai.luacompose.compose.ui.graphics.ComposeScopeComponent
import com.kulipai.luacompose.compose.ui.resolveModifier

internal fun registerLayoutComponents(map: MutableMap<String, @Composable (props: Map<String, Any?>, childScope: ComposeScope?) -> Unit>) {
    map["Column"] = { props, childScope ->
        val modifier = resolveModifier(props["modifier"])
        Column(
            modifier = modifier,
            verticalArrangement = LuaComposeRegistry.resolveVerticalArrangement(props["verticalArrangement"]),
            horizontalAlignment = LuaComposeRegistry.resolveHorizontalAlignment(props["horizontalAlignment"])
        ) {
            childScope?.let { ComposeScopeComponent(it, this) }
        }
    }
    map["Row"] = { props, childScope ->
        val modifier = resolveModifier(props["modifier"])
        Row(
            modifier = modifier,
            horizontalArrangement = LuaComposeRegistry.resolveHorizontalArrangement(props["horizontalArrangement"]),
            verticalAlignment = LuaComposeRegistry.resolveVerticalAlignment(props["verticalAlignment"])
        ) {
            childScope?.let { ComposeScopeComponent(it, this) }
        }
    }
    map["Box"] = { props, childScope ->
        val modifier = resolveModifier(props["modifier"])
        Box(
            modifier = modifier,
            contentAlignment = LuaComposeRegistry.resolveAlignment(props["contentAlignment"])
        ) {
            childScope?.let { ComposeScopeComponent(it, this) }
        }
    }
    map["BoxWithConstraints"] = { props, childScope ->
        val modifier = resolveModifier(props["modifier"])
        androidx.compose.foundation.layout.BoxWithConstraints(
            modifier = modifier,
            contentAlignment = LuaComposeRegistry.resolveAlignment(props["contentAlignment"])
        ) {
            childScope?.let { 
                val engine = com.kulipai.luacompose.compose.runtime.ComposeBridge.engine
                val constraintsTable = engine.createTable()
                constraintsTable.set("minWidth", engine.createValue(minWidth.value.toDouble()))
                constraintsTable.set("maxWidth", engine.createValue(maxWidth.value.toDouble()))
                constraintsTable.set("minHeight", engine.createValue(minHeight.value.toDouble()))
                constraintsTable.set("maxHeight", engine.createValue(maxHeight.value.toDouble()))
                ComposeScopeComponent(it, this, constraintsTable) 
            }
        }
    }
    map["ScrollableColumn"] = { props, childScope ->
        val modifier = resolveModifier(props["modifier"]).verticalScroll(rememberScrollState())
        Column(
            modifier = modifier,
            verticalArrangement = LuaComposeRegistry.resolveVerticalArrangement(props["verticalArrangement"]),
            horizontalAlignment = LuaComposeRegistry.resolveHorizontalAlignment(props["horizontalAlignment"])
        ) {
            childScope?.let { ComposeScopeComponent(it, this) }
        }
    }
    map["ScrollableRow"] = { props, childScope ->
        val modifier =
            resolveModifier(props["modifier"]).horizontalScroll(rememberScrollState())
        Row(
            modifier = modifier,
            horizontalArrangement = LuaComposeRegistry.resolveHorizontalArrangement(props["horizontalArrangement"]),
            verticalAlignment = LuaComposeRegistry.resolveVerticalAlignment(props["verticalAlignment"])
        ) {
            childScope?.let { ComposeScopeComponent(it, this) }
        }
    }
    map["Spacer"] = { props, _ ->
        Spacer(modifier = resolveModifier(props["modifier"]))
    }
}
