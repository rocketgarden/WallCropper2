package net.vinceblas.wallcropper2

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.singleWindowApplication
import net.vinceblas.wallcropper2.components.CropPreviewImage


@OptIn(ExperimentalFoundationApi::class)
@Composable
@Preview
fun App() {
    val widePic: String = "picdir/test169wide.png"
    val bigpic: String = "picdir/weirdeagle.jpg"

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
        state = WindowState(width = 1600.dp, height = 1200.dp)
    ) {
        App()
    }
}
