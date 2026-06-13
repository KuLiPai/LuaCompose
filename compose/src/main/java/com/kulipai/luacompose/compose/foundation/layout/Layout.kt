package com.kulipai.luacompose.compose.foundation.layout


import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kulipai.luacompose.compose.runtime.ComposeBridge
import com.kulipai.luacompose.compose.LuaComposeRegistry
import com.kulipai.luacompose.compose.createComposeDrawScope
import com.kulipai.luacompose.compose.runtime.ComposeScope
import com.kulipai.luacompose.compose.ui.graphics.ComposeScopeComponent
import com.kulipai.luacompose.compose.runtime.ComposeScriptPlugin
import com.kulipai.luacompose.compose.ui.resolveDp
import com.kulipai.luacompose.compose.ui.resolveModifier
import com.kulipai.luacompose.compose.script.ScriptFunction
import com.kulipai.luacompose.compose.script.ScriptTable
import com.kulipai.luacompose.compose.script.ScriptValue

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
