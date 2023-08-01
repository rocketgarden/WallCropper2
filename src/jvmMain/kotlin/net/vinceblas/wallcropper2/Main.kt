package net.vinceblas.wallcropper2

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.singleWindowApplication
import net.vinceblas.wallcropper2.components.CropPreviewImage


@Composable
@Preview
fun App() {
    @Suppress("UNUSED_VARIABLE") val widePic = "picdir/test169wide.png"
    @Suppress("UNUSED_VARIABLE") val bigpic = "picdir/weirdeagle.jpg"

    var text by remember { mutableStateOf("Hello, World!") }

    MaterialTheme {
        Column {
            CropPreviewImage(bigpic)
            Button(onClick = {
                text = "Hello, Desktop!"
            }) {
                Text(text)
            }
        }
    }
}

fun main() {
    singleWindowApplication(
        state = WindowState(width = 1500.dp, height = 1300.dp)
    ) {
        App()
    }
}
