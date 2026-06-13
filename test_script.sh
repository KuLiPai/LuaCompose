adb logcat -c
adb shell am force-stop com.kulipai.luacompose
adb shell am start -n com.kulipai.luacompose/.MainActivity
sleep 2
adb logcat -d | grep LUA_ANIM
