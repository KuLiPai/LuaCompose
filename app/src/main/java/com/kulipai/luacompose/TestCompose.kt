package com.kulipai.luacompose

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineStart

@Composable
fun TestPointerInput() {
    Box(
        modifier = Modifier
            .size(200.dp)
            .background(Color.Red)
            .pointerInput(Unit) {
                Log.e("LUAMODIFIER_LOG", "TestPointerInput attached!")
                coroutineScope {
                    launch(start = CoroutineStart.UNDISPATCHED) {
                        Log.e("LUAMODIFIER_LOG", "TestPointerInput launch block executing!")
                        detectDragGestures { change, dragAmount ->
                            Log.e("LUAMODIFIER_LOG", "TestPointerInput dragged! $dragAmount")
                        }
                    }
                }
            }
    )
}
