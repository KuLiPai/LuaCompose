package com.kulipai.luacompose.compose

import kotlin.reflect.KType

object TypeResolver {
    fun resolve(value: Any?, type: Class<*>): Any? {
        if (value == null) return null
        
        if (type == String::class.java) {
            return value.toString()
        }
        
        if (type.name == "androidx.compose.ui.text.AnnotatedString") {
            return androidx.compose.ui.text.AnnotatedString(value.toString())
        }

        if (value is Number) {
            when (type) {
                Int::class.javaPrimitiveType, Int::class.javaObjectType -> return value.toInt()
                Float::class.javaPrimitiveType, Float::class.javaObjectType -> return value.toFloat()
                Double::class.javaPrimitiveType, Double::class.javaObjectType -> return value.toDouble()
                Long::class.javaPrimitiveType, Long::class.javaObjectType -> return value.toLong()
                Short::class.javaPrimitiveType, Short::class.javaObjectType -> return value.toShort()
                Byte::class.javaPrimitiveType, Byte::class.javaObjectType -> return value.toByte()
            }
        }
        
        // Return original value if no conversion is matched
        return value
    }
}
