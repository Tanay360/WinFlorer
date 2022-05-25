package helper

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import java.io.File
import java.lang.IllegalStateException

private val FILES = listOf(
    "bat",
    "cpp",
    "css",
    "c",
    "cs",
    "db",
    "ini",
    "docx",
    "exe",
    "svelte",
    "vue",
    "go",
    "gradle",
    "lnk",
    "html",
    "java",
    "json",
    "dart",
    "yaml",
    "md",
    "js",
    "ts",
    "jsx",
    "tsx",
    "kt",
    "pdf",
    "php",
    "pptx",
    "python",
    "rust",
    "txt",
    "xlsx",
    "xml"
)

object FileIconGeneratorListColumn {

    fun getIcon(file: File): String {
        val ext = file.extension.lowercase()
        return when {
            file.isDirectory -> {
                "folder-icon.svg"
            }
            FILES.indexOf(ext) > -1 -> {
                "${ext}_file_icon.svg"
            }
            else -> {
                when (ext) {
                    "png", "jpeg", "jpg", "ico", "jfif", "gif", "svg" -> "image-icon.svg"
                    "zip", "gzip", "rar", "7z", "jar", "tar" -> "archive-icon.svg"
                    "mp4", "webm", "mov", "wmv", "avi", "mkv" -> "video-icon.svg"
                    "m4a", "flac", "mp3", "wav", "wma", "aac" -> "music-icon.svg"
                    "ttf", "woff", "woff2", "otf" -> "font-icon.svg"
                    else -> "file-icon.svg"
                }
            }
        }
    }
}

object FileIconGeneratorLargeIcon {
    @Composable
    fun getIcon(file: File): Painter {
        val ext = file.extension.lowercase()
        return painterResource(
            when {
                file.isDirectory -> {
                    "folder-icon.svg"
                }
                FILES.indexOf(ext) > -1 -> {
                    "${ext}_file_icon.svg"
                }
                else -> {
                    when (ext) {
                        "png", "jpeg", "jpg", "ico", "jfif", "gif", "svg" -> {
                            throw IllegalStateException("File is an image, show the preview instead")
                        }
                        "zip", "gzip", "rar", "7z", "jar", "tar" -> "archive-icon.svg"
                        "mp4", "webm", "mov", "wmv", "avi", "mkv" -> "video-icon.svg"
                        "m4a", "flac", "mp3", "wav", "wma", "aac" -> "music-icon.svg"
                        "ttf", "woff", "woff2", "otf" -> "font-icon.svg"
                        else -> "file-icon.svg"
                    }
                }
            }
        )
    }

}