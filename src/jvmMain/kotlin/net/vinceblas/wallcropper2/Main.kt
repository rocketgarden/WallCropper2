package net.vinceblas.wallcropper2

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import net.vinceblas.wallcropper2.components.CropPreviewImage
import net.vinceblas.wallcropper2.components.CropState
import net.vinceblas.wallcropper2.components.ImageLoader
import java.io.File

data class LoadedImageState( // todo sealed class with error/empty/finished(/loading?) states
    val fileHandle: File,
    @Suppress("ArrayInDataClass") val bytes: ByteArray
    // no need to override equals since we only load bytes once; compare by reference is fine
)

@Composable
@Preview
fun App(imageLoader: ImageLoader, baseDirectoryFlow: MutableStateFlow<File>, windowTitle: MutableState<String>) {
    val composableScope = rememberCoroutineScope()
    val imageFileCropper = ImageFileCropper()

    val cropStateFlow = remember { MutableStateFlow<CropState?>(null) }
    // not actually sure if remember is necessary here since we'd be feeding a new initial state in on recompose anyways? maybe?

    // feed loaded images into PreviewImage state
    composableScope.launch {
        imageLoader.currentImageStateFlow.collect { loadedImageState ->
            loadedImageState?.let {
                cropStateFlow.value = loadedImageState.let { CropState.buildInitialState(it.bytes) }
                println("Loaded ${loadedImageState.fileHandle}")
                windowTitle.value = loadedImageState.fileHandle.name
            }
        }
    }

    MaterialTheme {
        Column {
            Box(modifier = Modifier.weight(1f, true)) {
                // nb: weight modifier used bc it subtracts fixed-size elements from the allocatable space
                // vs default unmodified behavior which expands to maximum size possible (and pushes buttons offscreen)
                // wrapped in box so variable image size doesn't bound layout around
                CropPreviewImage(cropStateFlow)
                // todo show error/etc message here if cropStateFlow is null (image loading broken)
            }
            Row {
                Button(onClick = {
                    val loadedState = imageLoader.currentImageStateFlow.value
                    val cropState = cropStateFlow.value

                    if (loadedState != null && cropState != null) {
                        imageLoader.archive() // Load the next image
                        imageFileCropper.cropImage(
                            outfile = loadedState.fileHandle,
                            outputBaseDir = baseDirectoryFlow.value.name,
                            bytes = loadedState.bytes,
                            offset = cropState.cropImageOffset,
                            ratio = cropState.desiredRatio
                        )
                    }
                }) {
                    Text("Crop")
                }

                Button(onClick = {
                    // move file to skipped folder, no crop
                    imageLoader.skip()
                }) {
                    Text("Skip")
                }
                Button(onClick = {
                    // move file to trash folder
                    imageLoader.delete()
                }) {
                    Text("Discard")
                }
            }
        }
    }
}


fun main() {
    val windowTitle = mutableStateOf("Loading...")

    val baseDirectoryFlow = MutableStateFlow(File("picdir")) // todo inject
    val imageLoader = ImageLoader(baseDirectoryFlow)

    application {
        Window(
            ::exitApplication,
            state = WindowState(width = 1500.dp, height = 1300.dp),
            title = windowTitle.value
        ) {
            App(imageLoader, baseDirectoryFlow, windowTitle)
        }
    }
}