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


        val alignmentTable = LuaTable()
        alignmentTable.set("TopStart",LuaBridge.javaToLuaValue(Alignment.TopStart))
        alignmentTable.set("TopCenter",LuaBridge.javaToLuaValue(Alignment.TopCenter))
        alignmentTable.set("TopEnd",LuaBridge.javaToLuaValue(Alignment.TopEnd))
        alignmentTable.set("CenterStart",LuaBridge.javaToLuaValue(Alignment.CenterStart))
        alignmentTable.set("Center",LuaBridge.javaToLuaValue(Alignment.Center))
        alignmentTable.set("CenterEnd",LuaBridge.javaToLuaValue(Alignment.CenterEnd))
        alignmentTable.set("BottomStart",LuaBridge.javaToLuaValue(Alignment.BottomStart))
        alignmentTable.set("BottomCenter",LuaBridge.javaToLuaValue(Alignment.BottomCenter))
        alignmentTable.set("BottomEnd",LuaBridge.javaToLuaValue(Alignment.BottomEnd))

        // 1D Alignment.Verticals.
        alignmentTable.set("Top",LuaBridge.javaToLuaValue(Alignment.Top))
        alignmentTable.set("CenterVertically",LuaBridge.javaToLuaValue(Alignment.CenterVertically))
        alignmentTable.set("Bottom",LuaBridge.javaToLuaValue(Alignment.Bottom))

        // 1D Alignment.Horizontals.
        alignmentTable.set("Start",LuaBridge.javaToLuaValue(Alignment.Start))
        alignmentTable.set("CenterHorizontally",LuaBridge.javaToLuaValue(Alignment.CenterHorizontally))
        alignmentTable.set("End",LuaBridge.javaToLuaValue(Alignment.End))


        luaTable.set("Alignment", alignmentTable)




    }
}
