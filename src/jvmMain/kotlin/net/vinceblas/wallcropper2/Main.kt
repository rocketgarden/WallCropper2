package net.vinceblas.wallcropper2

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.flow.MutableStateFlow
import net.vinceblas.wallcropper2.components.CropPreviewImage
import net.vinceblas.wallcropper2.components.CropState
import net.vinceblas.wallcropper2.components.ImageLoader
import java.io.File

sealed class ImageState {
    object Loading : ImageState()
    data class Loaded(
        val fileHandle: File,
        @Suppress("ArrayInDataClass") val bytes: ByteArray
        // no need to override equals since we only load bytes once; compare by reference is fine
    ) : ImageState()

    object Finished : ImageState()
    data class Error(val message: String) : ImageState()
}

@OptIn(ExperimentalUnitApi::class)
@Composable
@Preview
fun App(imageLoader: ImageLoader, baseDirectoryFlow: MutableStateFlow<File>, windowTitle: MutableState<String>) {
    val imageFileCropper = ImageFileCropper()

    val cropStateFlow = remember { MutableStateFlow<CropState?>(null) }
    // not actually sure if remember is necessary here since we'd be feeding a new initial state in on recompose anyways? maybe?

    // feed loaded images into PreviewImage state
    LaunchedEffect(imageLoader) {
        imageLoader.currentImageStateFlow.collect { loadedImageState ->
            if (loadedImageState is ImageState.Loaded) {
                cropStateFlow.value = loadedImageState.let { CropState.buildInitialState(it.bytes) }
                println("Loaded ${loadedImageState.fileHandle}")
                windowTitle.value = loadedImageState.fileHandle.name
            } else {
                cropStateFlow.value = null
                windowTitle.value = "Loading..."
            }
        }
    }

    MaterialTheme {
        Column {
            val imageState = imageLoader.currentImageStateFlow.value
            Box(modifier = Modifier.weight(1f, true).fillMaxWidth()) {
                // nb: weight modifier used bc it subtracts fixed-size elements from the allocatable space
                // vs default unmodified behavior which expands to maximum size possible (and pushes buttons offscreen)
                // wrapped in box so variable image size doesn't bounce layout around
                when (imageState) {
                    is ImageState.Loaded -> {
                        CropPreviewImage(cropStateFlow)
                    }

                    else -> {
                        val message = when (imageState) {
                            ImageState.Finished -> "No More Images"
                            ImageState.Loading -> "Loading!"
                            is ImageState.Error -> "Error: ${imageState.message}"
                            else -> "Unknown Error"
                        }
                        Text(message, Modifier.align(Alignment.Center), fontSize = TextUnit(26f, TextUnitType.Sp))
                    }
                }
            }
            val isLoaded = imageState is ImageState.Loaded
            Row {
                Button(
                    onClick = {
                        val cropState = cropStateFlow.value

                        if (imageState is ImageState.Loaded && cropState != null) {
                            imageLoader.archive() // Load the next image
                            imageFileCropper.cropImage(
                                outfile = imageState.fileHandle,
                                outputBaseDir = baseDirectoryFlow.value.name,
                                bytes = imageState.bytes,
                                offset = cropState.cropImageOffset,
                                ratio = cropState.desiredRatio
                            )
                        }
                    },
                    enabled = isLoaded
                ) {
                    Text("Crop")
                }

                Button(
                    onClick = imageLoader::skip,
                    enabled = isLoaded
                ) {
                    Text("Skip")
                }
                Button(
                    onClick = imageLoader::delete,
                    enabled = isLoaded
                ) {
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
            state = rememberWindowState(size = DpSize(width = 1500.dp, height = 1300.dp)),
            title = windowTitle.value
        ) {
            App(imageLoader, baseDirectoryFlow, windowTitle)
        }
    }
}