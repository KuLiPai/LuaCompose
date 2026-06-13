#!/bin/bash
cp app/src/main/assets/main.lua /sdcard/Android/data/com.kulipai.luacompose/files/main.lua
adb logcat -c
am start -n com.kulipai.luacompose/.MainActivity
sleep 2
adb logcat -d | grep -E "AndroidRuntime|LUA_ANIM|Exception|Error"
