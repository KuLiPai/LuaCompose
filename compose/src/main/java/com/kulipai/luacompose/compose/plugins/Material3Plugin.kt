package com.kulipai.luacompose.compose.plugins


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
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
import androidx.compose.material3.Typography
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
import kotlin.collections.get

class Material3Plugin : LuaComposePlugin {
    override val namespace: String? = "material3"

    override fun getComponents(): Map<String, @Composable (props: Map<String, Any?>, childScope: LuaScope?) -> Unit> {
        val map = mutableMapOf<String, @Composable (props: Map<String, Any?>, childScope: LuaScope?) -> Unit>()

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
        val typographyTable = LuaTable()
        val defaultTypography = Typography()
        typographyTable.set("displayLarge", LuaBridge.javaToLuaValue(defaultTypography.displayLarge))
        typographyTable.set("displayMedium", LuaBridge.javaToLuaValue(defaultTypography.displayMedium))
        typographyTable.set("displaySmall", LuaBridge.javaToLuaValue(defaultTypography.displaySmall))
        typographyTable.set("headlineLarge", LuaBridge.javaToLuaValue(defaultTypography.headlineLarge))
        typographyTable.set("headlineMedium", LuaBridge.javaToLuaValue(defaultTypography.headlineMedium))
        typographyTable.set("headlineSmall", LuaBridge.javaToLuaValue(defaultTypography.headlineSmall))
        typographyTable.set("titleLarge", LuaBridge.javaToLuaValue(defaultTypography.titleLarge))
        typographyTable.set("titleMedium", LuaBridge.javaToLuaValue(defaultTypography.titleMedium))
        typographyTable.set("titleSmall", LuaBridge.javaToLuaValue(defaultTypography.titleSmall))
        typographyTable.set("bodyLarge", LuaBridge.javaToLuaValue(defaultTypography.bodyLarge))
        typographyTable.set("bodyMedium", LuaBridge.javaToLuaValue(defaultTypography.bodyMedium))
        typographyTable.set("bodySmall", LuaBridge.javaToLuaValue(defaultTypography.bodySmall))
        typographyTable.set("labelLarge", LuaBridge.javaToLuaValue(defaultTypography.labelLarge))
        typographyTable.set("labelMedium", LuaBridge.javaToLuaValue(defaultTypography.labelMedium))
        typographyTable.set("labelSmall", LuaBridge.javaToLuaValue(defaultTypography.labelSmall))
                val defaultColors = androidx.compose.material3.lightColorScheme()
        val colorSchemeTable = LuaTable()
        colorSchemeTable.set("primary", LuaBridge.javaToLuaValue(defaultColors.primary))
        colorSchemeTable.set("onPrimary", LuaBridge.javaToLuaValue(defaultColors.onPrimary))
        colorSchemeTable.set("primaryContainer", LuaBridge.javaToLuaValue(defaultColors.primaryContainer))
        colorSchemeTable.set("onPrimaryContainer", LuaBridge.javaToLuaValue(defaultColors.onPrimaryContainer))
        colorSchemeTable.set("secondary", LuaBridge.javaToLuaValue(defaultColors.secondary))
        colorSchemeTable.set("onSecondary", LuaBridge.javaToLuaValue(defaultColors.onSecondary))
        colorSchemeTable.set("secondaryContainer", LuaBridge.javaToLuaValue(defaultColors.secondaryContainer))
        colorSchemeTable.set("onSecondaryContainer", LuaBridge.javaToLuaValue(defaultColors.onSecondaryContainer))
        colorSchemeTable.set("tertiary", LuaBridge.javaToLuaValue(defaultColors.tertiary))
        colorSchemeTable.set("onTertiary", LuaBridge.javaToLuaValue(defaultColors.onTertiary))
        colorSchemeTable.set("tertiaryContainer", LuaBridge.javaToLuaValue(defaultColors.tertiaryContainer))
        colorSchemeTable.set("onTertiaryContainer", LuaBridge.javaToLuaValue(defaultColors.onTertiaryContainer))
        colorSchemeTable.set("error", LuaBridge.javaToLuaValue(defaultColors.error))
        colorSchemeTable.set("errorContainer", LuaBridge.javaToLuaValue(defaultColors.errorContainer))
        colorSchemeTable.set("onError", LuaBridge.javaToLuaValue(defaultColors.onError))
        colorSchemeTable.set("onErrorContainer", LuaBridge.javaToLuaValue(defaultColors.onErrorContainer))
        colorSchemeTable.set("background", LuaBridge.javaToLuaValue(defaultColors.background))
        colorSchemeTable.set("onBackground", LuaBridge.javaToLuaValue(defaultColors.onBackground))
        colorSchemeTable.set("surface", LuaBridge.javaToLuaValue(defaultColors.surface))
        colorSchemeTable.set("onSurface", LuaBridge.javaToLuaValue(defaultColors.onSurface))
        colorSchemeTable.set("surfaceVariant", LuaBridge.javaToLuaValue(defaultColors.surfaceVariant))
        colorSchemeTable.set("onSurfaceVariant", LuaBridge.javaToLuaValue(defaultColors.onSurfaceVariant))
        colorSchemeTable.set("outline", LuaBridge.javaToLuaValue(defaultColors.outline))
        colorSchemeTable.set("inverseOnSurface", LuaBridge.javaToLuaValue(defaultColors.inverseOnSurface))
        colorSchemeTable.set("inverseSurface", LuaBridge.javaToLuaValue(defaultColors.inverseSurface))
        colorSchemeTable.set("inversePrimary", LuaBridge.javaToLuaValue(defaultColors.inversePrimary))
        colorSchemeTable.set("surfaceTint", LuaBridge.javaToLuaValue(defaultColors.surfaceTint))
        colorSchemeTable.set("outlineVariant", LuaBridge.javaToLuaValue(defaultColors.outlineVariant))
        colorSchemeTable.set("scrim", LuaBridge.javaToLuaValue(defaultColors.scrim))
        mtTable.set("colorScheme", colorSchemeTable)
        mtTable.set("colors", colorSchemeTable) // Alias for convenience

        val defaultShapes = androidx.compose.material3.Shapes()
        val shapesTable = LuaTable()
        shapesTable.set("extraSmall", LuaBridge.javaToLuaValue(defaultShapes.extraSmall))
        shapesTable.set("small", LuaBridge.javaToLuaValue(defaultShapes.small))
        shapesTable.set("medium", LuaBridge.javaToLuaValue(defaultShapes.medium))
        shapesTable.set("large", LuaBridge.javaToLuaValue(defaultShapes.large))
        shapesTable.set("extraLarge", LuaBridge.javaToLuaValue(defaultShapes.extraLarge))
        mtTable.set("shapes", shapesTable)
        mtTable.set("shape", shapesTable) // Alias for convenience
        
        mtTable.set("typography", typographyTable)
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
