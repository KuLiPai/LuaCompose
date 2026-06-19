package com.kulipai.luacompose

import org.junit.Test
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue

class TestTableKeys {
    @Test
    fun testKeys() {
        val table = LuaTable()
        println("Keys before: ")
        var k: LuaValue = LuaValue.NIL
        while (true) {
            val next = table.next(k)
            k = next.arg1()
            if (k.isnil()) break
            println(k)
        }
        
        table.get("_isState")
        
        println("Keys after: ")
        k = LuaValue.NIL
        while (true) {
            val next = table.next(k)
            k = next.arg1()
            if (k.isnil()) break
            println(k)
        }
    }
}
