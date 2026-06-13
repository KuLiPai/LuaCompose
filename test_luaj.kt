import org.luaj.LuaTable
import org.luaj.LuaFunction
import org.luaj.LuaValue
import org.luaj.lib.VarArgFunction
import org.luaj.Varargs

fun main() {
    val mt = LuaTable()
    mt.set("__add", object : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs {
            println("__add invoked!")
            return LuaValue.valueOf("added!")
        }
    })
    
    val t1 = LuaTable()
    t1.setmetatable(mt)
    val t2 = LuaTable()
    t2.setmetatable(mt)
    
    val result = t1.add(t2)
    println("Result of t1 + t2: " + result)
}
