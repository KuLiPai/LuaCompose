package com.kulipai.luacompose.compose

import com.kulipai.luacompose.annotations.LuaBridgePackage

@LuaBridgePackage(packageName = "androidx.compose.foundation", category = "foundation")
@LuaBridgePackage(packageName = "androidx.compose.foundation.layout", category = "foundation.layout")
@LuaBridgePackage(packageName = "androidx.compose.material3", category = "material3")
@LuaBridgePackage(packageName = "androidx.compose.ui", category = "ui")
@com.kulipai.luacompose.annotations.LuaBridgeClass(targetClass = androidx.compose.ui.graphics.Color::class, category = "ui.graphics")
@com.kulipai.luacompose.annotations.LuaBridgeClass(targetClass = androidx.compose.ui.unit.Dp::class, category = "ui.unit")
@com.kulipai.luacompose.annotations.LuaBridgeModifiers(packageName = "androidx.compose.foundation.layout")
class LuaComposeConfig
