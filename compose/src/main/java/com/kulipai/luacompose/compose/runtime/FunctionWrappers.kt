package com.kulipai.luacompose.compose.runtime

import androidx.compose.runtime.Composable
import com.kulipai.luacompose.compose.ui.graphics.ComposeScopeComponent

object FunctionWrappers {
    fun wrap(childScope: ComposeScope, paramTypeName: String): Any {
        return when (paramTypeName) {
            "kotlin.jvm.functions.Function2" -> {
                val func: @Composable () -> Unit = { ComposeScopeComponent(childScope, null) }
                func
            }
            "kotlin.jvm.functions.Function3" -> {
                val func: @Composable (Any?) -> Unit = { p1 -> ComposeScopeComponent(childScope, p1) }
                func
            }
            "kotlin.jvm.functions.Function4" -> {
                val func: @Composable (Any?, Any?) -> Unit = { p1, p2 -> ComposeScopeComponent(childScope, p1) }
                func
            }
            "kotlin.jvm.functions.Function5" -> {
                val func: @Composable (Any?, Any?, Any?) -> Unit = { p1, p2, p3 -> ComposeScopeComponent(childScope, p1) }
                func
            }
            "kotlin.jvm.functions.Function6" -> {
                val func: @Composable (Any?, Any?, Any?, Any?) -> Unit = { p1, p2, p3, p4 -> ComposeScopeComponent(childScope, p1) }
                func
            }
            else -> {
                val func: @Composable () -> Unit = { ComposeScopeComponent(childScope, null) }
                func
            }
        }
    }
}
