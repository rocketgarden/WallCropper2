package net.vinceblas.wallcropper2

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize
import java.io.File

fun getDesiredSizeForRatio(size: IntSize, ratio: Double): Size {
    return getDesiredSizeForRatio(size.width, size.height, ratio)
}

fun getDesiredSizeForRatio(width: Int, height: Int, ratio: Double): Size {
    if (width == 0 || height == 0) return Size.Zero
    return if (width / height.toDouble() > ratio) { // too wide, base size on height
        Size(width = (height * ratio).toFloat(), height = height.toFloat())
    } else { // vice versa
        Size(width = width.toFloat(), height = (width / ratio).toFloat())
    }
}

fun File.getValidImages(): List<File> {
    return if (this.isDirectory) {
        this.listFiles()?.filter { isValidImage(it) } ?: emptyList()
    } else {
        emptyList()
    }
}

fun isValidImage(file: File): Boolean {
    return file.isFile &&
            (file.extension.equals("jpg", true)
                    || file.extension.equals("png", true)
                    || file.extension.equals("jpeg", true))
}