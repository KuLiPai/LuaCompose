package com.kulipai.luacompose.compose.runtime

import androidx.compose.runtime.Composable
import com.kulipai.luacompose.compose.script.ScriptFunction
import com.kulipai.luacompose.compose.ui.graphics.ComposeScopeComponent

object FunctionWrappers {
    fun wrap(childScope: ComposeScope?, scriptFunction: ScriptFunction?, paramTypeName: String, isComposable: Boolean): Any {
        if (isComposable) {
            return when (paramTypeName) {
                "kotlin.jvm.functions.Function2" -> {
                    val func: @Composable () -> Unit = { childScope?.let { ComposeScopeComponent(it, null) } }
                    func
                }
                "kotlin.jvm.functions.Function3" -> {
                    val func: @Composable (Any?) -> Unit = { p1 -> childScope?.let { ComposeScopeComponent(it, p1, ScopeWrappers.wrap(p1)) } }
                    func
                }
                "kotlin.jvm.functions.Function4" -> {
                    val func: @Composable (Any?, Any?) -> Unit = { p1, p2 -> childScope?.let { ComposeScopeComponent(it, p1, ScopeWrappers.wrap(p1), ScopeWrappers.wrap(p2)) } }
                    func
                }
                "kotlin.jvm.functions.Function5" -> {
                    val func: @Composable (Any?, Any?, Any?) -> Unit = { p1, p2, p3 -> childScope?.let { ComposeScopeComponent(it, p1, ScopeWrappers.wrap(p1), ScopeWrappers.wrap(p2), ScopeWrappers.wrap(p3)) } }
                    func
                }
                "kotlin.jvm.functions.Function6" -> {
                    val func: @Composable (Any?, Any?, Any?, Any?) -> Unit = { p1, p2, p3, p4 -> childScope?.let { ComposeScopeComponent(it, p1, ScopeWrappers.wrap(p1), ScopeWrappers.wrap(p2), ScopeWrappers.wrap(p3), ScopeWrappers.wrap(p4)) } }
                    func
                }
                else -> {
                    val func: @Composable () -> Unit = { childScope?.let { ComposeScopeComponent(it, null) } }
                    func
                }
            }
        } else {
            return when (paramTypeName) {
                "kotlin.jvm.functions.Function0" -> {
                    val func: () -> Unit = { scriptFunction?.call() }
                    func
                }
                "kotlin.jvm.functions.Function1" -> {
                    val func: (Any?) -> Unit = { p1 -> 
                        scriptFunction?.call(ScopeWrappers.wrap(p1))
                    }
                    func
                }
                "kotlin.jvm.functions.Function2" -> {
                    val func: (Any?, Any?) -> Unit = { p1, p2 -> 
                        scriptFunction?.call(
                            ScopeWrappers.wrap(p1),
                            ScopeWrappers.wrap(p2)
                        )
                    }
                    func
                }
                "kotlin.jvm.functions.Function3" -> {
                    val func: (Any?, Any?, Any?) -> Unit = { p1, p2, p3 -> 
                        scriptFunction?.call(
                            ScopeWrappers.wrap(p1),
                            ScopeWrappers.wrap(p2),
                            ScopeWrappers.wrap(p3)
                        )
                    }
                    func
                }
                "kotlin.jvm.functions.Function4" -> {
                    val func: (Any?, Any?, Any?, Any?) -> Unit = { p1, p2, p3, p4 -> 
                        scriptFunction?.call(
                            ScopeWrappers.wrap(p1),
                            ScopeWrappers.wrap(p2),
                            ScopeWrappers.wrap(p3),
                            ScopeWrappers.wrap(p4)
                        )
                    }
                    func
                }
                else -> {
                    val func: () -> Unit = { scriptFunction?.call() }
                    func
                }
            }
        }
    }
}
