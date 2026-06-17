package com.kulipai.luacompose

import com.kulipai.luacompose.adapter.LuajEngine
import com.kulipai.luacompose.compose.LuaComposeLib
import com.kulipai.luacompose.compose.runtime.ComposeBridge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.luaj.Globals
import org.luaj.lib.jse.JsePlatform

class LuaComposeLibExportTest {
    @Test
    fun foundationLayoutComponentsAreExportedToLua() {
        val globals = Globals()
        ComposeBridge.engine = LuajEngine
        ComposeBridge.luaValueUnwrapper = { value -> value }

        val env = LuajEngine.wrap(globals).asTable()
        LuaComposeLib.inject(env)

        val compose = env.get("compose").asTable()
        val foundation = compose.get("foundation").asTable()
        val layout = foundation.get("layout").asTable()
        val material3 = compose.get("material3").asTable()

        assertTrue(foundation.get("Canvas").isFunction())
        assertTrue(layout.get("Column").isFunction())
        assertTrue(layout.get("Row").isFunction())
        assertTrue(material3.get("Text").isFunction())
    }

    @Test
    fun injectClearsPreviousRootContentState() {
        val globals = Globals()
        ComposeBridge.engine = LuajEngine
        ComposeBridge.luaValueUnwrapper = { value -> value }

        LuaComposeLib.rootContentFunc = LuajEngine.createFunction { LuajEngine.createNil() }
        LuaComposeLib.globalEnv = LuajEngine.createTable()

        val env = LuajEngine.wrap(globals).asTable()
        LuaComposeLib.inject(env)

        assertNull(LuaComposeLib.rootContentFunc)
        assertEquals(env, LuaComposeLib.globalEnv)
    }

    @Test
    fun clearRuntimeStateResetsGlobalScriptReferences() {
        ComposeBridge.engine = LuajEngine
        ComposeBridge.luaValueUnwrapper = { value -> value }

        LuaComposeLib.rootContentFunc = LuajEngine.createFunction { LuajEngine.createNil() }
        LuaComposeLib.globalEnv = LuajEngine.createTable()

        LuaComposeLib.clearRuntimeState()

        assertNull(LuaComposeLib.rootContentFunc)
        assertNull(LuaComposeLib.globalEnv)
    }

    @Test
    fun luaScriptCanReadFoundationLayoutColumn() {
        val globals = JsePlatform.standardGlobals()
        ComposeBridge.engine = LuajEngine
        ComposeBridge.luaValueUnwrapper = { value ->
            if (value is org.luaj.LuaValue) {
                ComposeBridge.scriptToJava(LuajEngine.wrap(value))
            } else {
                value
            }
        }

        val env = LuajEngine.wrap(globals).asTable()
        LuaComposeLib.inject(env)

        val result = globals.load(
            """
            local compose = compose
            local Column = compose.foundation.layout.Column
            return Column ~= nil
            """.trimIndent(),
            "main.lua"
        ).call()

        assertEquals(true, result.toboolean())
    }

    @Test
    fun luaScriptCanReadFoundationCanvas() {
        val globals = JsePlatform.standardGlobals()
        ComposeBridge.engine = LuajEngine
        ComposeBridge.luaValueUnwrapper = { value ->
            if (value is org.luaj.LuaValue) {
                ComposeBridge.scriptToJava(LuajEngine.wrap(value))
            } else {
                value
            }
        }

        val env = LuajEngine.wrap(globals).asTable()
        LuaComposeLib.inject(env)

        val result = globals.load(
            """
            local compose = compose
            local Canvas = compose.foundation.Canvas
            return Canvas ~= nil
            """.trimIndent(),
            "main.lua"
        ).call()

        assertEquals(true, result.toboolean())
    }
}
