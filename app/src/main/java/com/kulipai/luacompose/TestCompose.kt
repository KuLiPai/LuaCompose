package com.kulipai.luacompose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import android.util.Log

@Composable
fun TestPointerInput() {
    Box(
        modifier = Modifier
            .size(80.dp)
            .background(Color.Red)
            .pointerInput(Unit) {
                Log.e("LUAMODIFIER_LOG", "KOTLIN POINTER INPUT EXECUTING!!")
            }
    )
}
