package net.vinceblas.wallcropper2.components

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import net.vinceblas.wallcropper2.LoadedImageState
import net.vinceblas.wallcropper2.getValidImages
import java.io.File

class ImageLoader(currentDirectoryFlow: MutableStateFlow<File>) {

    companion object {
        const val SKIP_DIR = "skipped"
        const val DELETE_DIR = "trash"
        const val BACKUP_DIR = "backup"
        const val ERROR_DIR = "error"
        const val CROPPED_DIR = "cropped"
    }

    // todo maybe add internal map or something to track which files we've seen/acted on already
    // only use case for this so far is avoiding reloading the last image in a directory?
    // Might be useful for dealing with failed ops too
    private var imageFiles = currentDirectoryFlow.value.getValidImages() // todo filter this based on file type/extension. jpg and png only
    private var nextFileIndex = 0

    private var loaderJob: Job? = null

    val currentImageStateFlow: MutableStateFlow<LoadedImageState?> = MutableStateFlow(null)
    // todo expose as StateFlow (not mutable)

    init {
        MainScope().launch(Dispatchers.IO) {// IO might be overkill here but w/e
            currentDirectoryFlow.collect { dir ->
                imageFiles = dir.getValidImages()
                nextFileIndex = 0
                println(imageFiles)
                loadNextImage()
            }
        }
    }

    fun skip() {
        // TODO skip current image
        loadNextImage()
    }

    fun delete() {
        // TODO delete the current image
        loadNextImage()
    }

    fun archive() {
        // TODO archive current image
        loadNextImage()
    }

    private fun loadNextImage() {
        loaderJob?.cancel()
        loaderJob = null

        // Load the next image and update the currentImageStateFlow
        if (nextFileIndex < imageFiles.size) {
            loaderJob = MainScope().launch(Dispatchers.IO) {
                val file = imageFiles[nextFileIndex]
                val bytes = file.readBytes()
                if(isActive) { // if we're cancelled, discard loaded data and don't update any state
                    currentImageStateFlow.value = LoadedImageState(file, bytes)
                    nextFileIndex++
                }
            }
        } else {
            currentImageStateFlow.value = null
        }
    }

    private fun moveFile(file: File, baseDir: File, folderName: String): Boolean {
        // Check if the file exists
        if (!file.exists()) return false

        // Check/create the base and sub directories
        val subDir = File(baseDir, folderName)
        if (!subDir.exists() && !subDir.mkdirs()) return false

        // Check if a file with the same name already exists in the destination directory
        val destinationFile = File(subDir, file.name)
        if (destinationFile.exists() && !destinationFile.delete()) return false

        // Move the file
        return file.renameTo(destinationFile)
    }
}
