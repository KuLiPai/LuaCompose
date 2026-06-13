package com.kulipai.luacompose.compose.material3.progressindicator


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

internal fun registerProgressindicatorComponents(map: MutableMap<String, @Composable (props: Map<String, Any?>, childScope: ComposeScope?) -> Unit>) {
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
}
