package com.kulipai.luacompose.compose.plugins


import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kulipai.luacompose.compose.LuaBridge
import com.kulipai.luacompose.compose.LuaComposeRegistry
import com.kulipai.luacompose.compose.LuaDrawScope
import com.kulipai.luacompose.compose.LuaScope
import com.kulipai.luacompose.compose.LuaScopeComponent
import com.kulipai.luacompose.compose.resolveDp
import com.kulipai.luacompose.compose.resolveModifier
import org.luaj.LuaFunction
import org.luaj.LuaTable
import org.luaj.LuaValue
import org.luaj.lib.OneArgFunction
import org.luaj.lib.TwoArgFunction

class FoundationPlugin : LuaComposePlugin {
    override val namespace: String? = null

    override fun getComponents(): Map<String, @Composable (props: Map<String, Any?>, childScope: LuaScope?) -> Unit> {
        val map =
            mutableMapOf<String, @Composable (props: Map<String, Any?>, childScope: LuaScope?) -> Unit>()

        map["Column"] = { props, childScope ->
            val modifier = resolveModifier(props["modifier"])
            Column(
                modifier = modifier,
                verticalArrangement = LuaComposeRegistry.resolveVerticalArrangement(props["verticalArrangement"]),
                horizontalAlignment = LuaComposeRegistry.resolveHorizontalAlignment(props["horizontalAlignment"])
            ) {
                childScope?.let { LuaScopeComponent(it, this) }
            }
        }

        map["Row"] = { props, childScope ->
            val modifier = resolveModifier(props["modifier"])
            Row(
                modifier = modifier,
                horizontalArrangement = LuaComposeRegistry.resolveHorizontalArrangement(props["horizontalArrangement"]),
                verticalAlignment = LuaComposeRegistry.resolveVerticalAlignment(props["verticalAlignment"])
            ) {
                childScope?.let { LuaScopeComponent(it, this) }
            }
        }

        map["Box"] = { props, childScope ->
            val modifier = resolveModifier(props["modifier"])
            Box(
                modifier = modifier,
                contentAlignment = LuaComposeRegistry.resolveAlignment(props["contentAlignment"])
            ) {
                childScope?.let { LuaScopeComponent(it, this) }
            }
        }

        map["Canvas"] = { props, _ ->
            val modifier = resolveModifier(props["modifier"])
            val onDraw = props["onDraw"] as? LuaFunction

            Canvas(modifier = modifier) {
                if (onDraw != null) {
                    val luaDrawScope = LuaDrawScope(this)
                    try {
                        onDraw.call(luaDrawScope)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        map["ScrollableColumn"] = { props, childScope ->
            val modifier = resolveModifier(props["modifier"]).verticalScroll(rememberScrollState())
            Column(
                modifier = modifier,
                verticalArrangement = LuaComposeRegistry.resolveVerticalArrangement(props["verticalArrangement"]),
                horizontalAlignment = LuaComposeRegistry.resolveHorizontalAlignment(props["horizontalAlignment"])
            ) {
                childScope?.let { LuaScopeComponent(it, this) }
            }
        }

        map["ScrollableRow"] = { props, childScope ->
            val modifier =
                resolveModifier(props["modifier"]).horizontalScroll(rememberScrollState())
            Row(
                modifier = modifier,
                horizontalArrangement = LuaComposeRegistry.resolveHorizontalArrangement(props["horizontalArrangement"]),
                verticalAlignment = LuaComposeRegistry.resolveVerticalAlignment(props["verticalAlignment"])
            ) {
                childScope?.let { LuaScopeComponent(it, this) }
            }
        }

        map["Spacer"] = { props, _ ->
            Spacer(modifier = resolveModifier(props["modifier"]))
        }

        map["LazyColumn"] = { props, _ ->
            val itemsList = props["items"] as? List<*> ?: emptyList<Any?>()
            val itemContent = props["itemContent"] as? LuaFunction
            val modifier = resolveModifier(props["modifier"])

            LazyColumn(
                modifier = modifier,
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(itemsList.size) { index ->
                    val item = itemsList[index]
                    if (itemContent != null) {
                        val itemScope = remember(index) { LuaScope(itemContent) }
                        val luaInstance = LuaBridge.javaToLuaValue(item)
                        val indexValue = LuaValue.valueOf(index + 1)
                        LuaScopeComponent(itemScope, this, luaInstance, indexValue)
                    }
                }
            }
        }

        return map
    }

    override fun injectGlobals(luaTable: LuaTable) {
        luaTable.set("RoundedCornerShape", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val radius = resolveDp(LuaBridge.luaValueToJava(arg))
                return LuaBridge.javaToLuaValue(RoundedCornerShape(radius))
            }
        })
        luaTable.set("CircleShape", LuaBridge.javaToLuaValue(CircleShape))


        // -------------- Arrangement ------------------
        val arrangementTable = LuaTable()
        arrangementTable.set("Top", LuaBridge.javaToLuaValue(Arrangement.Top))
        arrangementTable.set("Center", LuaBridge.javaToLuaValue(Arrangement.Center))
        arrangementTable.set("Start", LuaBridge.javaToLuaValue(Arrangement.Start))
        arrangementTable.set("Start", LuaBridge.javaToLuaValue(Arrangement.Start))
        arrangementTable.set("Bottom", LuaBridge.javaToLuaValue(Arrangement.Bottom))
        arrangementTable.set("SpaceAround", LuaBridge.javaToLuaValue(Arrangement.SpaceAround))
        arrangementTable.set("SpaceBetween", LuaBridge.javaToLuaValue(Arrangement.SpaceBetween))
        arrangementTable.set("SpaceEvenly", LuaBridge.javaToLuaValue(Arrangement.SpaceEvenly))
        arrangementTable.set("spacedBy", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val space = resolveDp(LuaBridge.luaValueToJava(arg))
                return LuaBridge.javaToLuaValue(Arrangement.spacedBy(space))
            }
        })

        // [WARN]: 未测试
        arrangementTable.set("aligned", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val space = LuaBridge.luaValueToJava(arg)
                if (space is Alignment.Horizontal) {
                    return LuaBridge.javaToLuaValue(Arrangement.aligned(space))
                } else if (space is Alignment.Vertical) {
                    return LuaBridge.javaToLuaValue(Arrangement.aligned(space))
                }
                return NIL
            }
        })

        luaTable.set("Arrangement", arrangementTable)

        // -------------- Alignment ------------------
        val alignmentTable = LuaTable()
        alignmentTable.set("TopStart", LuaBridge.javaToLuaValue(Alignment.TopStart))
        alignmentTable.set("TopCenter", LuaBridge.javaToLuaValue(Alignment.TopCenter))
        alignmentTable.set("TopEnd", LuaBridge.javaToLuaValue(Alignment.TopEnd))
        alignmentTable.set("CenterStart", LuaBridge.javaToLuaValue(Alignment.CenterStart))
        alignmentTable.set("Center", LuaBridge.javaToLuaValue(Alignment.Center))
        alignmentTable.set("CenterEnd", LuaBridge.javaToLuaValue(Alignment.CenterEnd))
        alignmentTable.set("BottomStart", LuaBridge.javaToLuaValue(Alignment.BottomStart))
        alignmentTable.set("BottomCenter", LuaBridge.javaToLuaValue(Alignment.BottomCenter))
        alignmentTable.set("BottomEnd", LuaBridge.javaToLuaValue(Alignment.BottomEnd))

        // 1D Alignment.Verticals.
        alignmentTable.set("Top", LuaBridge.javaToLuaValue(Alignment.Top))
        alignmentTable.set("CenterVertically", LuaBridge.javaToLuaValue(Alignment.CenterVertically))
        alignmentTable.set("Bottom", LuaBridge.javaToLuaValue(Alignment.Bottom))

        // 1D Alignment.Horizontals.
        alignmentTable.set("Start", LuaBridge.javaToLuaValue(Alignment.Start))
        alignmentTable.set(
            "CenterHorizontally",
            LuaBridge.javaToLuaValue(Alignment.CenterHorizontally)
        )
        alignmentTable.set("End", LuaBridge.javaToLuaValue(Alignment.End))

        luaTable.set("Alignment", alignmentTable)

        // -------------- FontWeight ------------------

        val fontWeightCompanionTable = LuaTable()

        /** [Thin] */
        fontWeightCompanionTable.set("W100", LuaBridge.javaToLuaValue(FontWeight.W100))
        /** [ExtraLight] */
        fontWeightCompanionTable.set("W200", LuaBridge.javaToLuaValue(FontWeight.W200))
        /** [Light] */
        fontWeightCompanionTable.set("W300", LuaBridge.javaToLuaValue(FontWeight.W300))
        /** [Normal] / regular / plain */
        fontWeightCompanionTable.set("W400", LuaBridge.javaToLuaValue(FontWeight.W400))
        /** [Medium] */
        fontWeightCompanionTable.set("W500", LuaBridge.javaToLuaValue(FontWeight.W500))
        /** [SemiBold] */
        fontWeightCompanionTable.set("W600", LuaBridge.javaToLuaValue(FontWeight.W600))
        /** [Bold] */
        fontWeightCompanionTable.set("W700", LuaBridge.javaToLuaValue(FontWeight.W700))
        /** [ExtraBold] */
        fontWeightCompanionTable.set("W800", LuaBridge.javaToLuaValue(FontWeight.W800))
        /** [Black] */
        fontWeightCompanionTable.set("W900", LuaBridge.javaToLuaValue(FontWeight.W900))
        /** Alias for [W100] */
        fontWeightCompanionTable.set("Thin", LuaBridge.javaToLuaValue(FontWeight.Thin))
        /** Alias for [W200] */
        fontWeightCompanionTable.set("ExtraLight", LuaBridge.javaToLuaValue(FontWeight.ExtraLight))
        /** Alias for [W300] */
        fontWeightCompanionTable.set("Light", LuaBridge.javaToLuaValue(FontWeight.Light))
        /** The default font weight - alias for [W400] */
        fontWeightCompanionTable.set("Normal", LuaBridge.javaToLuaValue(FontWeight.Normal))
        /** Alias for [W500] */
        fontWeightCompanionTable.set("Medium", LuaBridge.javaToLuaValue(FontWeight.Medium))
        /** Alias for [W600] */
        fontWeightCompanionTable.set("SemiBold", LuaBridge.javaToLuaValue(FontWeight.SemiBold))
        /** A commonly used font weight that is heavier than normal - alias for [W700] */
        fontWeightCompanionTable.set("Bold", LuaBridge.javaToLuaValue(FontWeight.Bold))
        /** Alias for [W800] */
        fontWeightCompanionTable.set("ExtraBold", LuaBridge.javaToLuaValue(FontWeight.ExtraBold))
        /** Alias for [W900] */
        fontWeightCompanionTable.set("Black", LuaBridge.javaToLuaValue(FontWeight.Black))

        val fontWeightTableMeta = LuaTable()
        fontWeightTableMeta.set("__index", object : TwoArgFunction() {
            override fun call(table: LuaValue, key: LuaValue): LuaValue {
                return fontWeightCompanionTable[key.checkjstring()]
            }
        })
        fontWeightTableMeta.set("__call", object : TwoArgFunction() {
            override fun call(table: LuaValue, params: LuaValue): LuaValue {
                return LuaBridge.javaToLuaValue(FontWeight(params.checkint()))
            }
        })
        val fontWeightTable = LuaTable()
        fontWeightTable.setmetatable(fontWeightTableMeta)


        luaTable.set("FontWeight", fontWeightTable)

        // -------------- Color ------------------
        val colorCompanionTable = LuaTable()
        colorCompanionTable.set("Black", LuaBridge.javaToLuaValue(Color.Black))
        colorCompanionTable.set("DarkGray", LuaBridge.javaToLuaValue(Color.DarkGray))
        colorCompanionTable.set("Gray", LuaBridge.javaToLuaValue(Color.Gray))
        colorCompanionTable.set("LightGray", LuaBridge.javaToLuaValue(Color.LightGray))
        colorCompanionTable.set("White", LuaBridge.javaToLuaValue(Color.White))
        colorCompanionTable.set("Red", LuaBridge.javaToLuaValue(Color.Red))
        colorCompanionTable.set("Green", LuaBridge.javaToLuaValue(Color.Green))
        colorCompanionTable.set("Blue", LuaBridge.javaToLuaValue(Color.Blue))
        colorCompanionTable.set("Yellow", LuaBridge.javaToLuaValue(Color.Yellow))
        colorCompanionTable.set("Cyan", LuaBridge.javaToLuaValue(Color.Cyan))
        colorCompanionTable.set("Magenta", LuaBridge.javaToLuaValue(Color.Magenta))
        colorCompanionTable.set("Transparent", LuaBridge.javaToLuaValue(Color.Transparent))
        colorCompanionTable.set("Unspecified", LuaBridge.javaToLuaValue(Color.Unspecified))

        val colorInstanceMeta = LuaTable()
        val colorMethods = LuaTable()
        colorMethods.set("luminance", object : OneArgFunction() {
            override fun call(self: LuaValue): LuaValue {
                val colorJava = self.checkuserdata(Color::class.java) as Color
                return LuaBridge.javaToLuaValue(colorJava.luminance())
            }
        })

        val colorTableMeta = LuaTable()
        colorTableMeta.set("__index", object : TwoArgFunction() {
            override fun call(table: LuaValue, key: LuaValue): LuaValue {
                return colorCompanionTable[key.checkjstring()]
            }
        })
        colorTableMeta.set("__call", object : TwoArgFunction() {
            override fun call(table: LuaValue, params: LuaValue): LuaValue {
                val instance = LuaBridge.javaToLuaValue(Color(params.checkint()))

                val oldMeta = instance.getmetatable()
                val newMeta = LuaTable()
                newMeta.set("__index", object : TwoArgFunction() {
                    override fun call(obj: LuaValue, key: LuaValue): LuaValue {
                        // 1. 先找自定义方法
                        val custom = colorMethods.get(key)
                        if (!custom.isnil()) return custom

                        // 2. 自定义没有，直接从旧元表里 get 这个 key
                        if (oldMeta != null) {
                            return oldMeta.get(key) // 或者写成 oldMeta[key]
                        }

                        return LuaValue.NIL
                    }
                })

                instance.setmetatable(newMeta)
                return instance
            }
        })
        val colorTable = LuaTable()
        colorTable.setmetatable(colorTableMeta)
        luaTable.set("Color", colorTable)


        
        // ------------------- TransformOrigin ---------------
        val transformOriginMethods = LuaTable()
        transformOriginMethods.set("copy", object : org.luaj.lib.VarArgFunction() {
            override fun invoke(args: org.luaj.Varargs): org.luaj.Varargs {
                val self = args.arg1().checkuserdata() as androidx.compose.ui.graphics.TransformOrigin
                val pivotFractionX = args.arg(2).optdouble(self.pivotFractionX.toDouble()).toFloat()
                val pivotFractionY = args.arg(3).optdouble(self.pivotFractionY.toDouble()).toFloat()
                val newInstance = LuaBridge.javaToLuaValue(androidx.compose.ui.graphics.TransformOrigin(pivotFractionX, pivotFractionY))
                newInstance.setmetatable(args.arg1().getmetatable())
                return newInstance
            }
        })
        
        fun wrapTransformOrigin(to: androidx.compose.ui.graphics.TransformOrigin): LuaValue {
            val instance = LuaBridge.javaToLuaValue(to)
            val oldMeta = instance.getmetatable()
            val newMeta = LuaTable()
            newMeta.set("__index", object : org.luaj.lib.TwoArgFunction() {
                override fun call(obj: LuaValue, key: LuaValue): LuaValue {
                    val realObj = obj.checkuserdata() as androidx.compose.ui.graphics.TransformOrigin
                    val k = key.tojstring()
                    if (k == "pivotFractionX") return LuaValue.valueOf(realObj.pivotFractionX.toDouble())
                    if (k == "pivotFractionY") return LuaValue.valueOf(realObj.pivotFractionY.toDouble())
                    
                    val custom = transformOriginMethods.get(key)
                    if (!custom.isnil()) return custom
                    
                    if (oldMeta != null) return oldMeta.get(key)
                    return LuaValue.NIL
                }
            })
            instance.setmetatable(newMeta)
            return instance
        }
        
        val transformOriginMeta = LuaTable()
        transformOriginMeta.set("__index", object : org.luaj.lib.TwoArgFunction() {
            override fun call(table: LuaValue, key: LuaValue): LuaValue {
                if (key.tojstring() == "Center") {
                    return wrapTransformOrigin(androidx.compose.ui.graphics.TransformOrigin.Center)
                }
                return LuaValue.NIL
            }
        })
        transformOriginMeta.set("__call", object : org.luaj.lib.VarArgFunction() {
            override fun invoke(args: org.luaj.Varargs): org.luaj.Varargs {
                val pivotFractionX = args.arg(2).optdouble(0.5).toFloat()
                val pivotFractionY = args.arg(3).optdouble(0.5).toFloat()
                return wrapTransformOrigin(androidx.compose.ui.graphics.TransformOrigin(pivotFractionX, pivotFractionY))
            }
        })
        
        val transformOriginTable = LuaTable()
        transformOriginTable.setmetatable(transformOriginMeta)
        luaTable.set("TransformOrigin", transformOriginTable)



    }
}
