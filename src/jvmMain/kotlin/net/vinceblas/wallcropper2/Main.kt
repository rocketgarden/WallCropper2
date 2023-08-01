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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.vinceblas.wallcropper2.components.CropPreviewImage
import net.vinceblas.wallcropper2.components.CropState
import java.io.File

const val widePic = "picdir/test169wide.png"
const val bigPic = "picdir/weirdeagle.jpg"

@Composable
@Preview
fun App(imageFilePathFlow: MutableStateFlow<String>) {
    val composableScope = rememberCoroutineScope()

    val imageByteArrayFlow: StateFlow<ByteArray?> = imageFilePathFlow
        .filter { it.isNotEmpty() }
        .map { filePath ->
            withContext(Dispatchers.IO) {
                File(filePath).readBytes()
            }
        }.stateIn(composableScope, SharingStarted.Eagerly, null)
    // stateIn converts this to a stateFlow so we can remember the last value and access it later when we want to crop/save the image

    val cropStateFlow = remember { MutableStateFlow<CropState?>(null) }
    // not actually sure if remember is necessary here since we'd be feeding a new initial state in on recompose anyways? maybe?

    // feed loaded images into PreviewImage state
    composableScope.launch {
        imageByteArrayFlow.collect { bytes ->
            bytes?.let { cropStateFlow.value = CropState.buildInitialState(bytes) }
        }
    }

    MaterialTheme {
        Column {
            CropPreviewImage(cropStateFlow)

            Button(onClick = {
                // simulate loading next image
                imageFilePathFlow.value = bigPic
            }) {
                Text("Next image")
            }
        }
    }
}


fun main() {
    // declare master flow outside compose scope so it isn't recreated on recompose
    val imageFilePathFlow = MutableStateFlow(widePic)
    singleWindowApplication(
        state = WindowState(width = 1500.dp, height = 1300.dp)
    ) {
        App(imageFilePathFlow)
    }
}
