package com.kulipai.luacompose.compose.material3.card


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.kulipai.luacompose.compose.runtime.ComposeScope
import com.kulipai.luacompose.compose.ui.graphics.ComposeScopeComponent
import com.kulipai.luacompose.compose.ui.resolveColor
import com.kulipai.luacompose.compose.ui.resolveModifier

internal fun registerCardComponents(map: MutableMap<String, @Composable (props: Map<String, Any?>, childScope: ComposeScope?) -> Unit>) {
    map["Card"] = { props, childScope ->
        val modifier = resolveModifier(props["modifier"])
        val colorsProp = props["colors"]
        val colors = if (colorsProp is Map<*, *> && colorsProp["_isCardColors"] == true) {
            val containerColor = resolveColor(colorsProp["containerColor"], Color.Unspecified)
            val contentColor = resolveColor(colorsProp["contentColor"], Color.Unspecified)
            CardDefaults.cardColors(
                containerColor = containerColor,
                contentColor = contentColor
            )
        } else {
            val containerColor = resolveColor(props["containerColor"], Color.Unspecified)
            if (containerColor != Color.Unspecified) {
                CardDefaults.cardColors(containerColor = containerColor)
            } else CardDefaults.cardColors()
        }

        val shape = props["shape"] as? Shape
            ?: run {
                val shapeProp = props["shape"]?.toString() ?: "rounded"
                val radius = (props["cornerRadius"] as? Number)?.toFloat() ?: 12f
                when (shapeProp.lowercase()) {
                    "circle" -> CircleShape
                    "rounded" -> RoundedCornerShape(radius.dp)
                    else -> androidx.compose.foundation.shape.RoundedCornerShape(radius.dp)
                }
            }

        val elevationVal = (props["elevation"] as? Number)?.toFloat()
        val elevation = if (elevationVal != null) {
            CardDefaults.cardElevation(defaultElevation = elevationVal.dp)
        } else CardDefaults.cardElevation()

        Card(
            modifier = modifier,
            shape = shape,
            colors = colors,
            elevation = elevation
        ) {
            childScope?.let { ComposeScopeComponent(it, this) }
        }
    }
    map["ElevatedCard"] = { props, childScope ->
        ElevatedCard(
            modifier = resolveModifier(props["modifier"])
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                childScope?.let { ComposeScopeComponent(it, this) }
            }
        }
    }
    map["OutlinedCard"] = { props, childScope ->
        OutlinedCard(
            modifier = resolveModifier(props["modifier"])
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                childScope?.let { ComposeScopeComponent(it, this) }
            }
        }
    }
}
