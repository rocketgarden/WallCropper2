package net.vinceblas.wallcropper2.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.onDrag
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.DrawModifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException


data class CropState(
    // all dimensions are relative to the image, not the preview on screen
    val ratio: Double = 16.0 / 9.0,
    val imageSize: IntSize = IntSize.Zero,
    val cropImageOffset: Offset = Offset.Zero,
    val scaleFactor: Float = 1.0f
    // multiply by scaleFactor to go from image -> screen dimensions
    // divide for reverse
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CropPreviewImage(filepath: String) {

    var state by remember { mutableStateOf(CropState()) }

    Column {
        AsyncImage(
            load = { loadImageBitmap(File(filepath)) },
            afterLoad = { bitmap -> state = onImageLoaded(state, bitmap) },
            painterFor = { remember { BitmapPainter(it) } },
            modifier = Modifier.onSizeChanged { renderSize ->
                if (renderSize.width > 0 && renderSize.height > 0) {
                    state = state.copy(scaleFactor = (renderSize.height.toFloat() / state.imageSize.height.toFloat()))
//                println("Scalefactor is ${state.scaleFactor}, ${renderSize.height} / ${state.imageSize.height}")
                }
            }
                .then(CropPreviewOverlay(state))
                .onDrag { dragOffset ->
                    state = state.copy(cropImageOffset = getNewOffset(state, dragOffset))
                }
        )
        Text("Raw Imagesize ${state.imageSize}")
        Text("Desired size ${getDesiredSizeForRatio(state.imageSize, state.ratio)}")

        Text("Crop Offset ${state.cropImageOffset}")
        Text("Scalefactor ${state.scaleFactor}")

        Text("Desired scaled size ${getDesiredSizeForRatio(state.imageSize, state.ratio) * state.scaleFactor}")
        Text("Scaled Imagesize ${state.imageSize.toSize() * state.scaleFactor}")
    }
}

private fun getNewOffset(state: CropState, dragOffset: Offset): Offset {
    val size = state.imageSize
    if (size == IntSize.Zero) return state.cropImageOffset

    val desiredSize = getDesiredSizeForRatio(state.imageSize, state.ratio)

    val xMax = (size.width - desiredSize.width)// maximum play in x direction
    val yMax = (size.height - desiredSize.height)

    val imagespaceDrag = dragOffset / state.scaleFactor // scale drag offset up into imagespace

    println("$xMax, $yMax")

    val newRawOffset = state.cropImageOffset + imagespaceDrag
    val finalOffset = Offset(newRawOffset.x.coerceIn(0f, xMax), newRawOffset.y.coerceIn(0f, yMax))

    println("$finalOffset")

    return finalOffset
}

private fun onImageLoaded(state: CropState, bitmap: ImageBitmap): CropState {
    val imageSize = IntSize(bitmap.width, bitmap.height)
    val desiredSize = getDesiredSizeForRatio(imageSize, state.ratio)

    //init crop window to center of image
    val cropOffset = Offset((imageSize.width - desiredSize.width) / 2f, (imageSize.height - desiredSize.height) / 2f)

    return state.copy(imageSize = imageSize, cropImageOffset = cropOffset)
}

class CropPreviewOverlay(val state: CropState) : DrawModifier {
    override fun ContentDrawScope.draw() {
        drawContent()

        if (state.cropImageOffset == Offset.Unspecified) return

        val cropRectSize = (getDesiredSizeForRatio(state.imageSize, ratio = state.ratio)) * state.scaleFactor
        val previewOffset = state.cropImageOffset * state.scaleFactor

        drawRect(Color.Cyan, topLeft = previewOffset, size = cropRectSize, alpha = 0.4f)
    }
}

@Composable
fun <ImageBitmap> AsyncImage(
    load: suspend () -> ImageBitmap,
    afterLoad: (ImageBitmap) -> Unit = {},
    painterFor: @Composable (ImageBitmap) -> Painter,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    val image: ImageBitmap? by produceState<ImageBitmap?>(null) {
        value = withContext(Dispatchers.IO) {
            try {
                load()
            } catch (e: IOException) {
                // instead of printing to console, you can also write this to log,
                // or show some error placeholder
                e.printStackTrace()
                null
            }
        }
        value?.let(afterLoad)
    }

    if (image != null) {
        Image(
            painter = painterFor(image!!),
            contentDescription = null,
            contentScale = contentScale,
            modifier = modifier
        )
    }
}

private fun loadImageBitmap(file: File): ImageBitmap =
    file.inputStream().buffered().use(::loadImageBitmap)

private fun getDesiredSizeForRatio(size: IntSize, ratio: Double): Size {
    if (size == IntSize.Zero) return Size.Zero
    return if (size.width / size.height > ratio) { // too wide, base size on height
        Size(width = (size.height * ratio).toFloat(), height = size.height.toFloat())
    } else {
        Size(width = size.width.toFloat(), height = (size.width / ratio).toFloat())
    }
}