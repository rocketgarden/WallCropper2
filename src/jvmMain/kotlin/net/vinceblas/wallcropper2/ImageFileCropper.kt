package net.vinceblas.wallcropper2

import androidx.compose.ui.geometry.Offset
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.JpegWriter
import com.sksamuel.scrimage.nio.PngWriter
import java.io.File

class ImageFileCropper {

    companion object {
        const val CROPPED_DIR = "cropped"
    }

    fun cropImage(outfile: File, outputBaseDir: String, bytes: ByteArray, offset: Offset, ratio: Double) {
        val (writer, name) = if (outfile.extension.equals("png", ignoreCase = true)) {
            PngWriter.MinCompression to outfile.name
            // Compression is hecka slow for minimal size reduction.
            // Can recompress in batches using another tool if necessary
        } else {
            JpegWriter.compression(99) to "${outfile.nameWithoutExtension}.jpg" // rename ".jpeg" to ".jpg" if needed
        }

        val outDir = File(outputBaseDir, CROPPED_DIR)
        outDir.mkdirs()

        val outFile = File(outDir, name)

        try {
            val oldImage = ImmutableImage.loader().fromBytes(bytes)

            // Get desired size for cropping
            val desiredSize = getDesiredSizeForRatio(oldImage.width, oldImage.height, ratio)
            // todo will need to change this if zooming/shrinking crop area is added

            // Calculate trim parameters
            val left = offset.x.toInt()
            val top = offset.y.toInt()
            val right = oldImage.width - (left + desiredSize.width.toInt())
            val bottom = oldImage.height - (top + desiredSize.height.toInt())

            // Make sure parameters are within image bounds
            if (left < 0 || right < 0 || top < 0 || bottom < 0) {
                throw IllegalArgumentException("Crop area is outside image bounds")
            }

            println("Cropping ${outfile.name} to left=$left, top=$top, right=$right, bottom=$bottom")

            // Crop image and write it to the output file
            oldImage.trim(left, top, right, bottom).output(writer, outFile) // todo put this in coroutine, maybe return the async reference?
            println("Finished cropping ${outfile.name}")
        } catch (e: Exception) {
            e.printStackTrace()
            System.err.println("Couldn't crop image ${outfile.name}")
        }
    }

}