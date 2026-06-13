package com.kulipai.luacompose.compose.material3


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
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
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
import com.kulipai.luacompose.compose.runtime.ComposeBridge
import com.kulipai.luacompose.compose.runtime.ComposeScope
import com.kulipai.luacompose.compose.ui.graphics.ComposeScopeComponent
import com.kulipai.luacompose.compose.runtime.ComposeState
import com.kulipai.luacompose.compose.runtime.ComposeScriptPlugin
import com.kulipai.luacompose.compose.ui.resolveColor
import com.kulipai.luacompose.compose.ui.resolveModifier
import com.kulipai.luacompose.compose.ui.resolveSp
import com.kulipai.luacompose.compose.ui.resolveShape
import com.kulipai.luacompose.compose.ui.resolveDp
import androidx.compose.ui.graphics.RectangleShape
import com.kulipai.luacompose.compose.script.ScriptFunction
import com.kulipai.luacompose.compose.script.ScriptTable
import com.kulipai.luacompose.compose.script.ScriptValue

class Material3Plugin : ComposeScriptPlugin {
    override val namespace: String? = "material3"

    override fun getComponents(): Map<String, @Composable (props: Map<String, Any?>, childScope: ComposeScope?) -> Unit> {
        val map =
            mutableMapOf<String, @Composable (props: Map<String, Any?>, childScope: ComposeScope?) -> Unit>()
        com.kulipai.luacompose.compose.material3.text.registerTextComponents(map)
        com.kulipai.luacompose.compose.material3.card.registerCardComponents(map)
        com.kulipai.luacompose.compose.material3.button.registerButtonComponents(map)
        com.kulipai.luacompose.compose.material3.textfield.registerTextfieldComponents(map)
        com.kulipai.luacompose.compose.material3.checkbox.registerCheckboxComponents(map)
        com.kulipai.luacompose.compose.material3.switch.registerSwitchComponents(map)
        com.kulipai.luacompose.compose.material3.radiobutton.registerRadiobuttonComponents(map)
        com.kulipai.luacompose.compose.material3.progressindicator.registerProgressindicatorComponents(map)
        com.kulipai.luacompose.compose.material3.icon.registerIconComponents(map)
        com.kulipai.luacompose.compose.material3.divider.registerDividerComponents(map)
        com.kulipai.luacompose.compose.material3.surface.registerSurfaceComponents(map)

        


        

        

        

        

        

        

        

        

        

        

        

        

        

        

        

        

        

        

        

        return map
    }

    override fun injectGlobals(scriptTable: ScriptTable) {
        val cardDefaultsTable = ComposeBridge.engine.createTable()
        cardDefaultsTable.set("cardColors", ComposeBridge.engine.createFunction { args ->
            val arg = args[0]
            val table = ComposeBridge.engine.createTable()
            table.set("_isCardColors", ComposeBridge.engine.createValue(true))
            if (arg.isTable()) {
                val luaMap = arg.asTable()
                val containerColor = luaMap.get("containerColor")
                val contentColor = luaMap.get("contentColor")
                if (!containerColor.isNil()) table.set("containerColor", containerColor)
                if (!contentColor.isNil()) table.set("contentColor", contentColor)
            }
            table
        })
        scriptTable.set("CardDefaults", cardDefaultsTable)

        val mtTable = ComposeBridge.engine.createTable()

        // --- ColorScheme ---
        val colorSchemeTable = ComposeBridge.engine.createTable()
        val colorSchemeMeta = ComposeBridge.engine.createTable()
        colorSchemeMeta.set("__index", ComposeBridge.engine.createFunction { args ->
            val key = args[1]
            val scope = ComposeBridge.getActiveScope()
            val cs = scope?.colorScheme ?: androidx.compose.material3.lightColorScheme()
            when (key.toStringValue()) {
                "primary" -> ComposeBridge.javaToScript(cs.primary)
                "onPrimary" -> ComposeBridge.javaToScript(cs.onPrimary)
                "primaryContainer" -> ComposeBridge.javaToScript(cs.primaryContainer)
                "onPrimaryContainer" -> ComposeBridge.javaToScript(cs.onPrimaryContainer)
                "secondary" -> ComposeBridge.javaToScript(cs.secondary)
                "onSecondary" -> ComposeBridge.javaToScript(cs.onSecondary)
                "secondaryContainer" -> ComposeBridge.javaToScript(cs.secondaryContainer)
                "onSecondaryContainer" -> ComposeBridge.javaToScript(cs.onSecondaryContainer)
                "tertiary" -> ComposeBridge.javaToScript(cs.tertiary)
                "onTertiary" -> ComposeBridge.javaToScript(cs.onTertiary)
                "tertiaryContainer" -> ComposeBridge.javaToScript(cs.tertiaryContainer)
                "onTertiaryContainer" -> ComposeBridge.javaToScript(cs.onTertiaryContainer)
                "error" -> ComposeBridge.javaToScript(cs.error)
                "errorContainer" -> ComposeBridge.javaToScript(cs.errorContainer)
                "onError" -> ComposeBridge.javaToScript(cs.onError)
                "onErrorContainer" -> ComposeBridge.javaToScript(cs.onErrorContainer)
                "background" -> ComposeBridge.javaToScript(cs.background)
                "onBackground" -> ComposeBridge.javaToScript(cs.onBackground)
                "surface" -> ComposeBridge.javaToScript(cs.surface)
                "onSurface" -> ComposeBridge.javaToScript(cs.onSurface)
                "surfaceVariant" -> ComposeBridge.javaToScript(cs.surfaceVariant)
                "onSurfaceVariant" -> ComposeBridge.javaToScript(cs.onSurfaceVariant)
                "outline" -> ComposeBridge.javaToScript(cs.outline)
                "inverseOnSurface" -> ComposeBridge.javaToScript(cs.inverseOnSurface)
                "inverseSurface" -> ComposeBridge.javaToScript(cs.inverseSurface)
                "inversePrimary" -> ComposeBridge.javaToScript(cs.inversePrimary)
                "surfaceTint" -> ComposeBridge.javaToScript(cs.surfaceTint)
                "outlineVariant" -> ComposeBridge.javaToScript(cs.outlineVariant)
                "scrim" -> ComposeBridge.javaToScript(cs.scrim)
                else -> ComposeBridge.engine.createNil()
            }
        })
        colorSchemeTable.setMetatable(colorSchemeMeta)
        mtTable.set("colorScheme", colorSchemeTable)
        mtTable.set("colors", colorSchemeTable) // Alias

        // --- Typography ---
        val typographyTable = ComposeBridge.engine.createTable()
        val typographyMeta = ComposeBridge.engine.createTable()
        typographyMeta.set("__index", ComposeBridge.engine.createFunction { args ->
            val key = args[1]
            val scope = ComposeBridge.getActiveScope()
            val typo = scope?.typography ?: androidx.compose.material3.Typography()
            when (key.toStringValue()) {
                "displayLarge" -> ComposeBridge.javaToScript(typo.displayLarge)
                "displayMedium" -> ComposeBridge.javaToScript(typo.displayMedium)
                "displaySmall" -> ComposeBridge.javaToScript(typo.displaySmall)
                "headlineLarge" -> ComposeBridge.javaToScript(typo.headlineLarge)
                "headlineMedium" -> ComposeBridge.javaToScript(typo.headlineMedium)
                "headlineSmall" -> ComposeBridge.javaToScript(typo.headlineSmall)
                "titleLarge" -> ComposeBridge.javaToScript(typo.titleLarge)
                "titleMedium" -> ComposeBridge.javaToScript(typo.titleMedium)
                "titleSmall" -> ComposeBridge.javaToScript(typo.titleSmall)
                "bodyLarge" -> ComposeBridge.javaToScript(typo.bodyLarge)
                "bodyMedium" -> ComposeBridge.javaToScript(typo.bodyMedium)
                "bodySmall" -> ComposeBridge.javaToScript(typo.bodySmall)
                "labelLarge" -> ComposeBridge.javaToScript(typo.labelLarge)
                "labelMedium" -> ComposeBridge.javaToScript(typo.labelMedium)
                "labelSmall" -> ComposeBridge.javaToScript(typo.labelSmall)
                else -> ComposeBridge.engine.createNil()
            }
        })
        typographyTable.setMetatable(typographyMeta)
        mtTable.set("typography", typographyTable)

        // --- Shapes ---
        val shapesTable = ComposeBridge.engine.createTable()
        val shapesMeta = ComposeBridge.engine.createTable()
        shapesMeta.set("__index", ComposeBridge.engine.createFunction { args ->
            val key = args[1]
            val scope = ComposeBridge.getActiveScope()
            val sh = scope?.shapes ?: androidx.compose.material3.Shapes()
            when (key.toStringValue()) {
                "extraSmall" -> ComposeBridge.javaToScript(sh.extraSmall)
                "small" -> ComposeBridge.javaToScript(sh.small)
                "medium" -> ComposeBridge.javaToScript(sh.medium)
                "large" -> ComposeBridge.javaToScript(sh.large)
                "extraLarge" -> ComposeBridge.javaToScript(sh.extraLarge)
                else -> ComposeBridge.engine.createNil()
            }
        })
        shapesTable.setMetatable(shapesMeta)
        mtTable.set("shapes", shapesTable)
        mtTable.set("shape", shapesTable) // Alias

        scriptTable.set("MaterialTheme", mtTable)
    }

    internal fun resolveIcon(name: String): ImageVector {
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
