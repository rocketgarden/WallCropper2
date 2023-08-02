package net.vinceblas.wallcropper2.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.onDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.DrawModifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.flow.MutableStateFlow
import net.vinceblas.wallcropper2.getDesiredSizeForRatio
import org.jetbrains.skia.Image

data class CropState(
    // all dimensions are relative to the image, not the preview on screen
    val imageBitmap: ImageBitmap, // todo move bitmap out of cropstate and into image load state? Then can resize on load into sane dimensions
    val desiredRatio: Double,
    val imageSize: IntSize = IntSize.Zero,
    val cropImageOffset: Offset = Offset.Zero,
    val scaleFactor: Float = Float.NaN
    // multiply by scaleFactor to go from image -> screen dimensions
    // divide for reverse

) {
    companion object {
        // mostly just encapsulating crop offset calculation
        fun buildInitialState(byteArray: ByteArray, ratio: Double = 16.0 / 9.0): CropState {
            val bitmap = Image.makeFromEncoded(byteArray).toComposeImageBitmap()

            val imageSize = IntSize(bitmap.width, bitmap.height)
            val desiredSize = getDesiredSizeForRatio(imageSize, ratio)
            val cropOffset =
                Offset((imageSize.width - desiredSize.width) / 2f, (imageSize.height - desiredSize.height) / 2f)

            return CropState(bitmap, ratio, imageSize, cropOffset)
        }
    }

}

const val PREVIEW_DEBUG = false

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CropPreviewImage(stateFlow: MutableStateFlow<CropState?>, modifier: Modifier = Modifier) {
    val state = stateFlow.collectAsState().value
    state?.let { cropState ->
        Box(modifier = modifier) {
            Image( // todo use drawWithCache to render image instead so we don't have to redraw entire bitmap when overlay changes
                bitmap = cropState.imageBitmap,
                contentDescription = null,
                modifier = Modifier.onSizeChanged { renderSize ->
                    // capture scaleFactor after render or resize
                    if (renderSize.width > 0 && renderSize.height > 0) {
                        stateFlow.value =
                            cropState.copy(scaleFactor = (renderSize.height.toFloat() / cropState.imageSize.height.toFloat()))
                    }
                }
                    .then( // awkward conditional modifiers
                        if (cropState.scaleFactor.isFinite())
                        // don't draw overlay until image is measured and we know scaleFactor
                            CropPreviewOverlayModifier(cropState)
                                .onDrag { dragOffset ->
                                    stateFlow.value?.let {state ->
                                        // reference most recent state in case multiple drag events come through before recompose
                                        stateFlow.value =
                                            state.copy(cropImageOffset = getNewOffset(state, dragOffset))
                                    }
                                }
                        else Modifier
                    )
            )
            if (PREVIEW_DEBUG) {
                Column(Modifier.background(Color(200, 200, 200, 120))) {
                    Text("Raw Imagesize ${cropState.imageSize}")
                    Text("Desired size ${getDesiredSizeForRatio(cropState.imageSize, cropState.desiredRatio)}")

                    Text("Crop Offset ${cropState.cropImageOffset}")
                    Text("Scalefactor ${cropState.scaleFactor}")

                    Text(
                        "Desired scaled size ${
                            getDesiredSizeForRatio(
                                cropState.imageSize,
                                cropState.desiredRatio
                            ) * cropState.scaleFactor
                        }"
                    )
                    Text("Scaled Imagesize ${cropState.imageSize.toSize() * cropState.scaleFactor}")
                }
            }
        }
    }
}

/**
 * Convert screenSpace drag offset to imageSpace offset for crop window, bounded by image edges
 */
private fun getNewOffset(state: CropState, dragOffset: Offset): Offset {
    val size = state.imageSize
    if (size == IntSize.Zero) return state.cropImageOffset

    val desiredSize = getDesiredSizeForRatio(state.imageSize, state.desiredRatio)

    val xMax = (size.width - desiredSize.width)// maximum play in x direction
    val yMax = (size.height - desiredSize.height)

    val imagespaceDrag = dragOffset / state.scaleFactor // scale drag offset up into imagespace

    val newRawOffset = state.cropImageOffset + imagespaceDrag
    val finalOffset = Offset(newRawOffset.x.coerceIn(0f, xMax), newRawOffset.y.coerceIn(0f, yMax))

    return finalOffset.round().toOffset() // snap to underlying pixel bounds
    // rounding on each drag event means preview pane is always rendered in a "snapped" state
}

var lastDrawTime = System.currentTimeMillis()

class CropPreviewOverlayModifier(private val state: CropState) : DrawModifier {
    override fun ContentDrawScope.draw() {
        if (PREVIEW_DEBUG) {
            val time = System.currentTimeMillis()
            println("frame time: ${time - lastDrawTime}")
            lastDrawTime = time
        }

        drawContent()

        if (state.cropImageOffset == Offset.Unspecified) return
        if (size.isUnspecified || size == Size.Zero) return

        val cropRectSize = (getDesiredSizeForRatio(state.imageSize, ratio = state.desiredRatio)) * state.scaleFactor
        val previewOffset = state.cropImageOffset * state.scaleFactor
        val alpha = 0.8f

        // left/right pillarboxes

        val pillarboxLeft = Size(previewOffset.x, size.height)
        val pillarboxRight = Size(size.width - (cropRectSize.width + previewOffset.x), size.height)

        // conditionals because rounding errors can create small negative dimensions at boundaries
        if (pillarboxLeft.width >= 1) drawRect(Color.Green, topLeft = Offset.Zero, size = pillarboxLeft, alpha = alpha)
        if (pillarboxRight.width >= 1) drawRect(
            Color.Red,
            topLeft = Offset(cropRectSize.width + pillarboxLeft.width, 0f),
            size = pillarboxRight,
            alpha = alpha
        )

        // top/bottom letterboxes

        // could use size.width instead of cropRectSize.width, but this covers potential future case
        // where cropRect can shrink and isn't edge-to-edge, so we don't double-draw the corners
        val letterboxTop = Size(cropRectSize.width, previewOffset.y)
        val letterboxBottom = Size(cropRectSize.width, size.height - (cropRectSize.height + previewOffset.y))
        if (letterboxTop.height >= 1f) drawRect(
            Color.Cyan,
            topLeft = previewOffset.copy(y = 0f),
            size = letterboxTop,
            alpha = alpha
        )
        if (letterboxBottom.height >= 1f) drawRect(
            Color.Blue,
            topLeft = previewOffset.copy(y = cropRectSize.height + letterboxTop.height),
            size = letterboxBottom,
            alpha = alpha
        )
    }
}

