package com.kulipai.luacompose.compose.androidx.compose.ui.graphics

import androidx.compose.ui.graphics.Color as ComposeColor

class Color  {
    companion object {

        @get:JvmName("getBlack")
        @JvmStatic
        val Black = ComposeColor.Black

        @get:JvmName("getDarkGray")
        @JvmStatic
        val DarkGray = ComposeColor.DarkGray


        @get:JvmName("getGray")
        @JvmStatic
        val Gray = ComposeColor.Gray

        @get:JvmName("getLightGray")
        @JvmStatic
        val LightGray = ComposeColor.LightGray


        @get:JvmName("getWhite")
        @JvmStatic
        val White = ComposeColor.White

        @get:JvmName("getRed")
        @JvmStatic
        val Red = ComposeColor.Red

        @get:JvmName("getGreen")
        @JvmStatic
        val Green = ComposeColor.Green


        @get:JvmName("getBlue")
        @JvmStatic
        val Blue = ComposeColor.Blue


        @get:JvmName("getYellow")
        @JvmStatic
        val Yellow = ComposeColor.Yellow


        @get:JvmName("getCyan")
        @JvmStatic
        val Cyan = ComposeColor.Cyan


        @get:JvmName("getMagenta")
        @JvmStatic
        val Magenta = ComposeColor.Magenta


        @get:JvmName("getTransparent")
        @JvmStatic
        val Transparent = ComposeColor.Transparent


        @get:JvmName("getUnspecified")
        @JvmStatic
        val Unspecified = ComposeColor.Unspecified


    }
}


//@Composable
//fun a() {
//        val Cyan : ComposeColor = ComposeColor.Cyan
//    Text(
//        text = "",
//        color = Cyan
//    )
//    Text(
//        text = "",
//        color = ComposeColor(0xffffffff),
//    )
//
//}