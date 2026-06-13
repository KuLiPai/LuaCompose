import org.luaj.LuaTable;
import org.luaj.LuaValue;
import org.luaj.lib.VarArgFunction;
import org.luaj.Varargs;

public class TestLuaj {
    public static void main(String[] args) {
        LuaTable mt = new LuaTable();
        mt.set("__add", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                System.out.println("__add invoked!");
                return LuaValue.valueOf("added!");
            }
        });
        
        LuaTable t1 = new LuaTable();
        t1.setmetatable(mt);
        LuaTable t2 = new LuaTable();
        t2.setmetatable(mt);
        
        LuaValue result = t1.add(t2);
        System.out.println("Result of t1 + t2: " + result);
    }
}
