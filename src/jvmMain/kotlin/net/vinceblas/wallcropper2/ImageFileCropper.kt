package net.vinceblas.wallcropper2

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.JpegWriter
import com.sksamuel.scrimage.nio.PngWriter
import java.awt.Rectangle
import java.io.File
import java.nio.file.Files
import kotlin.math.roundToInt

class ImageFileCropper {

    var outputDir: File = File("./")

    companion object {
        const val LATER_DIR = "skipped"
        const val TRASH_DIR = "trash"
        const val BACKUP_DIR = "backup"
        const val ERROR_DIR = "backup"
        const val CROPPED_DIR = "cropped"
    }

    fun cropImage(file: File, rect: Rectangle) {

        val (writer, name) = if (file.extension.equals("png", ignoreCase = true)) {
            PngWriter.MinCompression to
                    "${file.nameWithoutExtension}.png"
            // Max compression is slow as hell for only ~20% space savings (based on minimal testing)
            // If space is a concern, should postprocess/convert images in another tool afterward
        } else {
            JpegWriter.compression(98) to
                    "${file.nameWithoutExtension}.jpg"
        }

        val outFile = File(outputDir, "$CROPPED_DIR\\$name")
        outFile.parentFile.mkdirs()
        println("Cropping ${file.name} to ${rect.minX}, ${rect.minY} by ${rect.maxX}, ${rect.maxY}")
        // begin slow computation block
            val success = try {
                val canvas = ImmutableImage.create(rect.width, rect.height)
                val oldImage = ImmutableImage.loader().fromFile(file)
                canvas.overlay(oldImage, -rect.minX.roundToInt(), -rect.minY.roundToInt()).output(writer, outFile)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            } finally {

            }

            if (success) {
                println("Finished cropping ${file.name}")
                backupImage(file)
            } else {
                System.err.println("Couldn't crop image ${file.name}")
                errorImage(file)
            }

    }

    fun setAsideImage(file: File): Boolean {
        return moveFile(file, outputDir, LATER_DIR)
    }

    fun trashImage(file: File): Boolean {
        return moveFile(file, outputDir, TRASH_DIR)
    }

    fun skipCropProcessing(file: File): Boolean {
        println("${file.name} unchanged, skipped cropping")
        val ret = copyFile(file, outputDir, CROPPED_DIR)
        backupImage(file)

        return ret
    }

    private fun errorImage(file: File): Boolean {
        return moveFile(file, outputDir, ERROR_DIR)
    }

    private fun backupImage(file: File): Boolean {
        return moveFile(file, outputDir, BACKUP_DIR)
    }

    private fun moveFile(file: File, baseDir: File, folderName: String): Boolean {
        val dest = File(baseDir, "$folderName\\${file.name}")
        if (dest.exists()) { //rename fails if target exists. Overwriting is desired behavior
            dest.delete()
        }
        dest.parentFile.mkdirs()
        val moved = file.renameTo(dest)
        if (moved) {
//            println("Moved ${file.name} to $dest")
        } else {
            println("Warning: Couldn't move file ${file.path} to $dest")
        }
        return moved
    }

    private fun copyFile(file: File, baseDir: File, folderName: String): Boolean {
        val dest = File(baseDir, "$folderName\\${file.name}")
        if (dest.exists()) { //rename fails if target exists. Overwriting is desired behavior
            dest.delete()
        }
        dest.parentFile.mkdirs()
        val copied = try {
            Files.copy(file.toPath(), dest.toPath())
            true
        } catch (e: Throwable) {
            false
        }
//        println("Copied ${file.name} to $dest")
        return copied
    }
}