package com.kulipai.luacompose

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentComposer
import androidx.compose.ui.Modifier

@Composable
fun TestComposeReflection() {
    val clazz = Class.forName("androidx.compose.material3.TextKt")
    // Find the Text method that takes String
    val method = clazz.declaredMethods.find { 
        it.name == "Text" && it.parameterTypes[0] == String::class.java 
    }
    
    if (method != null) {
        val composer = currentComposer
        val paramsCount = method.parameterTypes.size
        val args = arrayOfNulls<Any>(paramsCount)
        
        args[0] = "Hello from Reflection!" // text
        
        // Find Composer index
        val composerIndex = method.parameterTypes.indexOfFirst { it.name == "androidx.compose.runtime.Composer" }
        args[composerIndex] = composer
        
        // Fill defaults for everything else
        for (i in 1 until composerIndex) {
            val type = method.parameterTypes[i]
            args[i] = when (type) {
                Boolean::class.javaPrimitiveType -> false
                Int::class.javaPrimitiveType -> 0
                Float::class.javaPrimitiveType -> 0f
                Long::class.javaPrimitiveType -> 0L
                Double::class.javaPrimitiveType -> 0.0
                Short::class.javaPrimitiveType -> 0.toShort()
                Byte::class.javaPrimitiveType -> 0.toByte()
                Char::class.javaPrimitiveType -> '\u0000'
                else -> null
            }
        }
        
        // Fill changed and default bitmasks
        for (i in (composerIndex + 1) until paramsCount) {
            val isDefault = i >= composerIndex + 1 + Math.ceil((composerIndex) / 10.0).toInt()
            if (isDefault) {
                // Set all bits to 1 to tell Compose to use defaults for all optional parameters
                args[i] = -1 // all 1s
            } else {
                args[i] = 0 // force recompose
            }
        }
        
        try {
            method.isAccessible = true
            method.invoke(null, *args)
        } catch(e: Exception) {
            e.printStackTrace()
        }
    }
}
