package com.kulipai.luacompose.compose.ui

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.AbstractComposeView
import com.kulipai.luacompose.compose.runtime.ComposeScope
import com.kulipai.luacompose.compose.ui.graphics.ComposeScopeComponent
import com.kulipai.luacompose.compose.script.ScriptFunction

/**
 * A standard Android View that can render a Lua Compose function.
 * This allows integrating Lua Compose into standard Android XML layouts or View hierarchies.
 */
class LuaComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {

    private var composeScope: ComposeScope? = null

    /**
     * Sets the Lua function to be rendered by this view.
     */
    fun setContent(luaFunction: ScriptFunction) {
        // Reuse scope if the function is the same, or create a new one
        if (composeScope?.contentFunc != luaFunction) {
            composeScope = ComposeScope(luaFunction)
        }
        // Force recomposition
        disposeComposition()
        requestLayout()
    }

    @Composable
    override fun Content() {
        composeScope?.let { scope ->
            ComposeScopeComponent(scope)
        }
    }
}
