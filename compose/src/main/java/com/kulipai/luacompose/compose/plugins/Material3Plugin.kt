package com.kulipai.luacompose.compose.plugins


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.kulipai.luacompose.compose.LuaBridge
import com.kulipai.luacompose.compose.LuaScope
import com.kulipai.luacompose.compose.LuaScopeComponent
import com.kulipai.luacompose.compose.LuaState
import com.kulipai.luacompose.compose.resolveColor
import com.kulipai.luacompose.compose.resolveModifier
import com.kulipai.luacompose.compose.resolveSp
import org.luaj.LuaFunction
import org.luaj.LuaTable
import org.luaj.LuaValue
import org.luaj.lib.OneArgFunction

class Material3Plugin : LuaComposePlugin {
    override val namespace: String? = "material3"

    override fun getComponents(): Map<String, @Composable (props: Map<String, Any?>, childScope: LuaScope?) -> Unit> {
        val map =
            mutableMapOf<String, @Composable (props: Map<String, Any?>, childScope: LuaScope?) -> Unit>()

        map["Text"] = { props, _ ->
            val textVal = when (val textProp = props["text"]) {
                is LuaState -> textProp.get()?.toString() ?: ""
                else -> textProp?.toString() ?: ""
            }
            val spVal = resolveSp(props["fontSize"])

            val styleProp = props["style"]
            val style = if (styleProp is TextStyle) {
                styleProp
            } else {
                when (styleProp?.toString()?.lowercase()) {
                    "displaylarge" -> MaterialTheme.typography.displayLarge
                    "displaymedium" -> MaterialTheme.typography.displayMedium
                    "displaysmall" -> MaterialTheme.typography.displaySmall
                    "headlinelarge" -> MaterialTheme.typography.headlineLarge
                    "headlinemedium" -> MaterialTheme.typography.headlineMedium
                    "headlinesmall" -> MaterialTheme.typography.headlineSmall
                    "titlelarge" -> MaterialTheme.typography.titleLarge
                    "titlemedium" -> MaterialTheme.typography.titleMedium
                    "titlesmall" -> MaterialTheme.typography.titleSmall
                    "bodylarge" -> MaterialTheme.typography.bodyLarge
                    "bodymedium" -> MaterialTheme.typography.bodyMedium
                    "bodysmall" -> MaterialTheme.typography.bodySmall
                    "labellarge" -> MaterialTheme.typography.labelLarge
                    "labelmedium" -> MaterialTheme.typography.labelMedium
                    "labelsmall" -> MaterialTheme.typography.labelSmall
                    else -> TextStyle.Default
                }
            }

            val fontWeightProp = props["fontWeight"]?.toString()?.lowercase()
            val fontWeight = when (fontWeightProp) {
                "bold" -> FontWeight.Bold
                "medium" -> FontWeight.Medium
                "light" -> FontWeight.Light
                else -> null
            }

            Text(
                text = textVal,
                modifier = resolveModifier(props["modifier"]),
                color = resolveColor(props["color"]),
                fontSize = if (spVal != TextUnit.Unspecified) spVal else TextUnit.Unspecified,
                style = style,
                fontWeight = fontWeight
            )
        }


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
                childScope?.let { LuaScopeComponent(it, this) }
            }
        }

        map["ElevatedCard"] = { props, childScope ->
            ElevatedCard(
                modifier = resolveModifier(props["modifier"])
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    childScope?.let { LuaScopeComponent(it, this) }
                }
            }
        }

        map["OutlinedCard"] = { props, childScope ->
            OutlinedCard(
                modifier = resolveModifier(props["modifier"])
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    childScope?.let { LuaScopeComponent(it, this) }
                }
            }
        }

        map["Button"] = { props, childScope ->
            val onClick = props["onClick"] as? LuaFunction
            Button(
                onClick = { onClick?.call() },
                modifier = resolveModifier(props["modifier"])
            ) {
                childScope?.let { LuaScopeComponent(it, this) }
            }
        }

        map["ElevatedButton"] = { props, childScope ->
            val onClick = props["onClick"] as? LuaFunction
            ElevatedButton(
                onClick = { onClick?.call() },
                modifier = resolveModifier(props["modifier"])
            ) {
                childScope?.let { LuaScopeComponent(it, this) }
            }
        }

        map["FilledTonalButton"] = { props, childScope ->
            val onClick = props["onClick"] as? LuaFunction
            FilledTonalButton(
                onClick = { onClick?.call() },
                modifier = resolveModifier(props["modifier"])
            ) {
                childScope?.let { LuaScopeComponent(it, this) }
            }
        }

        map["OutlinedButton"] = { props, childScope ->
            val onClick = props["onClick"] as? LuaFunction
            OutlinedButton(
                onClick = { onClick?.call() },
                modifier = resolveModifier(props["modifier"])
            ) {
                childScope?.let { LuaScopeComponent(it, this) }
            }
        }

        map["TextButton"] = { props, childScope ->
            val onClick = props["onClick"] as? LuaFunction
            TextButton(
                onClick = { onClick?.call() },
                modifier = resolveModifier(props["modifier"])
            ) {
                childScope?.let { LuaScopeComponent(it, this) }
            }
        }

        map["IconButton"] = { props, childScope ->
            val onClick = props["onClick"] as? LuaFunction
            IconButton(
                onClick = { onClick?.call() },
                modifier = resolveModifier(props["modifier"])
            ) {
                childScope?.let { LuaScopeComponent(it, this) }
            }
        }

        map["TextField"] = { props, _ ->
            val valueProp = props["value"]
            val onValueChange = props["onValueChange"] as? LuaFunction
            val valueText = when (valueProp) {
                is LuaState -> valueProp.get()?.toString() ?: ""
                else -> valueProp?.toString() ?: ""
            }
            val labelProp = props["label"]?.toString()
            TextField(
                value = valueText,
                onValueChange = { newVal -> onValueChange?.call(LuaValue.valueOf(newVal)) },
                modifier = resolveModifier(props["modifier"]),
                label = labelProp?.let { { Text(it) } }
            )
        }

        map["OutlinedTextField"] = { props, _ ->
            val valueProp = props["value"]
            val onValueChange = props["onValueChange"] as? LuaFunction
            val valueText = when (valueProp) {
                is LuaState -> valueProp.get()?.toString() ?: ""
                else -> valueProp?.toString() ?: ""
            }
            val labelProp = props["label"]?.toString()
            OutlinedTextField(
                value = valueText,
                onValueChange = { newVal -> onValueChange?.call(LuaValue.valueOf(newVal)) },
                modifier = resolveModifier(props["modifier"]),
                label = labelProp?.let { { Text(it) } }
            )
        }

        map["Checkbox"] = { props, _ ->
            val checkedProp = props["checked"]
            val checked = when (checkedProp) {
                is LuaState -> checkedProp.get() as? Boolean ?: false
                else -> checkedProp as? Boolean ?: false
            }
            val onCheckedChange = props["onCheckedChange"] as? LuaFunction
            Checkbox(
                checked = checked,
                onCheckedChange = { newVal -> onCheckedChange?.call(LuaValue.valueOf(newVal)) },
                modifier = resolveModifier(props["modifier"])
            )
        }

        map["Switch"] = { props, _ ->
            val checkedProp = props["checked"]
            val checked = when (checkedProp) {
                is LuaState -> checkedProp.get() as? Boolean ?: false
                else -> checkedProp as? Boolean ?: false
            }
            val onCheckedChange = props["onCheckedChange"] as? LuaFunction
            Switch(
                checked = checked,
                onCheckedChange = { newVal -> onCheckedChange?.call(LuaValue.valueOf(newVal)) },
                modifier = resolveModifier(props["modifier"])
            )
        }

        map["RadioButton"] = { props, _ ->
            val selectedProp = props["selected"]
            val selected = when (selectedProp) {
                is LuaState -> selectedProp.get() as? Boolean ?: false
                else -> selectedProp as? Boolean ?: false
            }
            val onClick = props["onClick"] as? LuaFunction
            RadioButton(
                selected = selected,
                onClick = { onClick?.call() },
                modifier = resolveModifier(props["modifier"])
            )
        }

        map["CircularProgressIndicator"] = { props, _ ->
            CircularProgressIndicator(
                modifier = resolveModifier(props["modifier"]),
                color = resolveColor(props["color"], ProgressIndicatorDefaults.circularColor)
            )
        }

        map["LinearProgressIndicator"] = { props, _ ->
            LinearProgressIndicator(
                modifier = resolveModifier(props["modifier"]),
                color = resolveColor(props["color"], ProgressIndicatorDefaults.linearColor)
            )
        }

        map["Icon"] = { props, _ ->
            val iconName = props["icon"]?.toString() ?: "info"
            val tint = resolveColor(props["tint"], LocalContentColor.current)
            Icon(
                imageVector = resolveIcon(iconName),
                contentDescription = iconName,
                modifier = resolveModifier(props["modifier"]),
                tint = tint
            )
        }

        map["Divider"] = { props, _ ->
            HorizontalDivider(
                modifier = resolveModifier(props["modifier"]),
                color = resolveColor(props["color"], DividerDefaults.color)
            )
        }

        return map
    }

    override fun injectGlobals(luaTable: LuaTable) {
        val cardDefaultsTable = LuaTable()
        cardDefaultsTable.set("cardColors", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val table = LuaTable()
                table.set("_isCardColors", valueOf(true))
                if (arg.istable()) {
                    val luaMap = arg.checktable()
                    val containerColor = luaMap.get("containerColor")
                    val contentColor = luaMap.get("contentColor")
                    if (!containerColor.isnil()) table.set("containerColor", containerColor)
                    if (!contentColor.isnil()) table.set("contentColor", contentColor)
                }
                return table
            }
        })
        luaTable.set("CardDefaults", cardDefaultsTable)

        val mtTable = LuaTable()

        // --- ColorScheme ---
        val colorSchemeTable = LuaTable()
        val colorSchemeMeta = LuaTable()
        colorSchemeMeta.set("__index", object : org.luaj.lib.TwoArgFunction() {
            override fun call(table: LuaValue, key: LuaValue): LuaValue {
                val scope = LuaBridge.getActiveScope()
                val cs = scope?.colorScheme ?: androidx.compose.material3.lightColorScheme()
                return when (key.checkjstring()) {
                    "primary" -> LuaBridge.javaToLuaValue(cs.primary)
                    "onPrimary" -> LuaBridge.javaToLuaValue(cs.onPrimary)
                    "primaryContainer" -> LuaBridge.javaToLuaValue(cs.primaryContainer)
                    "onPrimaryContainer" -> LuaBridge.javaToLuaValue(
                        cs.onPrimaryContainer
                    )

                    "secondary" -> LuaBridge.javaToLuaValue(cs.secondary)
                    "onSecondary" -> LuaBridge.javaToLuaValue(cs.onSecondary)
                    "secondaryContainer" -> LuaBridge.javaToLuaValue(
                        cs.secondaryContainer
                    )

                    "onSecondaryContainer" -> LuaBridge.javaToLuaValue(
                        cs.onSecondaryContainer
                    )

                    "tertiary" -> LuaBridge.javaToLuaValue(cs.tertiary)
                    "onTertiary" -> LuaBridge.javaToLuaValue(cs.onTertiary)
                    "tertiaryContainer" -> LuaBridge.javaToLuaValue(
                        cs.tertiaryContainer
                    )

                    "onTertiaryContainer" -> LuaBridge.javaToLuaValue(
                        cs.onTertiaryContainer
                    )

                    "error" -> LuaBridge.javaToLuaValue(cs.error)
                    "errorContainer" -> LuaBridge.javaToLuaValue(cs.errorContainer)
                    "onError" -> LuaBridge.javaToLuaValue(cs.onError)
                    "onErrorContainer" -> LuaBridge.javaToLuaValue(cs.onErrorContainer)
                    "background" -> LuaBridge.javaToLuaValue(cs.background)
                    "onBackground" -> LuaBridge.javaToLuaValue(cs.onBackground)
                    "surface" -> LuaBridge.javaToLuaValue(cs.surface)
                    "onSurface" -> LuaBridge.javaToLuaValue(cs.onSurface)
                    "surfaceVariant" -> LuaBridge.javaToLuaValue(cs.surfaceVariant)
                    "onSurfaceVariant" -> LuaBridge.javaToLuaValue(cs.onSurfaceVariant)
                    "outline" -> LuaBridge.javaToLuaValue(cs.outline)
                    "inverseOnSurface" -> LuaBridge.javaToLuaValue(cs.inverseOnSurface)
                    "inverseSurface" -> LuaBridge.javaToLuaValue(cs.inverseSurface)
                    "inversePrimary" -> LuaBridge.javaToLuaValue(cs.inversePrimary)
                    "surfaceTint" -> LuaBridge.javaToLuaValue(cs.surfaceTint)
                    "outlineVariant" -> LuaBridge.javaToLuaValue(cs.outlineVariant)
                    "scrim" -> LuaBridge.javaToLuaValue(cs.scrim)
                    else -> org.luaj.LuaValue.NIL
                }
            }
        })
        colorSchemeTable.setmetatable(colorSchemeMeta)
        mtTable.set("colorScheme", colorSchemeTable)
        mtTable.set("colors", colorSchemeTable) // Alias

        // --- Typography ---
        val typographyTable = LuaTable()
        val typographyMeta = LuaTable()
        typographyMeta.set("__index", object : org.luaj.lib.TwoArgFunction() {
            override fun call(table: org.luaj.LuaValue, key: org.luaj.LuaValue): org.luaj.LuaValue {
                val scope = LuaBridge.getActiveScope()
                val typo = scope?.typography ?: androidx.compose.material3.Typography()
                return when (key.checkjstring()) {
                    "displayLarge" -> LuaBridge.javaToLuaValue(typo.displayLarge)
                    "displayMedium" -> LuaBridge.javaToLuaValue(typo.displayMedium)
                    "displaySmall" -> LuaBridge.javaToLuaValue(typo.displaySmall)
                    "headlineLarge" -> LuaBridge.javaToLuaValue(typo.headlineLarge)
                    "headlineMedium" -> LuaBridge.javaToLuaValue(typo.headlineMedium)
                    "headlineSmall" -> LuaBridge.javaToLuaValue(typo.headlineSmall)
                    "titleLarge" -> LuaBridge.javaToLuaValue(typo.titleLarge)
                    "titleMedium" -> LuaBridge.javaToLuaValue(typo.titleMedium)
                    "titleSmall" -> LuaBridge.javaToLuaValue(typo.titleSmall)
                    "bodyLarge" -> LuaBridge.javaToLuaValue(typo.bodyLarge)
                    "bodyMedium" -> LuaBridge.javaToLuaValue(typo.bodyMedium)
                    "bodySmall" -> LuaBridge.javaToLuaValue(typo.bodySmall)
                    "labelLarge" -> LuaBridge.javaToLuaValue(typo.labelLarge)
                    "labelMedium" -> LuaBridge.javaToLuaValue(typo.labelMedium)
                    "labelSmall" -> LuaBridge.javaToLuaValue(typo.labelSmall)
                    else -> org.luaj.LuaValue.NIL
                }
            }
        })
        typographyTable.setmetatable(typographyMeta)
        mtTable.set("typography", typographyTable)

        // --- Shapes ---
        val shapesTable = LuaTable()
        val shapesMeta = LuaTable()
        shapesMeta.set("__index", object : org.luaj.lib.TwoArgFunction() {
            override fun call(table: org.luaj.LuaValue, key: org.luaj.LuaValue): org.luaj.LuaValue {
                val scope = LuaBridge.getActiveScope()
                val sh = scope?.shapes ?: androidx.compose.material3.Shapes()
                return when (key.checkjstring()) {
                    "extraSmall" -> LuaBridge.javaToLuaValue(sh.extraSmall)
                    "small" -> LuaBridge.javaToLuaValue(sh.small)
                    "medium" -> LuaBridge.javaToLuaValue(sh.medium)
                    "large" -> LuaBridge.javaToLuaValue(sh.large)
                    "extraLarge" -> LuaBridge.javaToLuaValue(sh.extraLarge)
                    else -> org.luaj.LuaValue.NIL
                }
            }
        })
        shapesTable.setmetatable(shapesMeta)
        mtTable.set("shapes", shapesTable)
        mtTable.set("shape", shapesTable) // Alias

        luaTable.set("MaterialTheme", mtTable)
    }

    private fun resolveIcon(name: String): ImageVector {
        return when (name.lowercase()) {
            "refresh" -> Icons.Default.Refresh
            "add" -> Icons.Default.Add
            "settings" -> Icons.Default.Settings
            "favorite" -> Icons.Default.Favorite
            "close" -> Icons.Default.Close
            "menu" -> Icons.Default.Menu
            "home" -> Icons.Default.Home
            "search" -> Icons.Default.Search
            "person" -> Icons.Default.Person
            "arrowback", "arrow_back" -> Icons.AutoMirrored.Filled.ArrowBack
            "arrowforward", "arrow_forward" -> Icons.AutoMirrored.Filled.ArrowForward
            "check" -> Icons.Default.Check
            "info" -> Icons.Default.Info
            "star" -> Icons.Default.Star
            "share" -> Icons.Default.Share
            "delete" -> Icons.Default.Delete
            "edit" -> Icons.Default.Edit
            "play" -> Icons.Default.PlayArrow
            else -> Icons.Default.Info
        }
    }
}
