#!/bin/bash
sed -i 's/val keys = args.dropLast(1).map { ComposeBridge.unwrapAny(it) }.toTypedArray()/com.kulipai.luacompose.compose.ui.LuaModifierLog.log("pointerInput CALLED! args: " + args.size); val keys = args.dropLast(1).map { ComposeBridge.unwrapAny(it) }.toTypedArray()/g' compose/src/main/java/com/kulipai/luacompose/compose/ui/LuaModifier.kt
