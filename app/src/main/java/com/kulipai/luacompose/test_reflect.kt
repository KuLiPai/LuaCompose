package com.kulipai.luacompose

fun testReflect() {
    try {
        val clazz = Class.forName("androidx.compose.material3.TextKt")
        println("Found class! Methods:")
        clazz.declaredMethods.forEach { println(it.name) }
    } catch(e: Exception) {
        println("Error: $e")
    }
}
