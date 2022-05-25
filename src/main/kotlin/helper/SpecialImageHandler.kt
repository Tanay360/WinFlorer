@file:Suppress("FunctionName")

package helper

import androidx.compose.foundation.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.swing.filechooser.FileSystemView

@Composable
fun ExeImage(file: File, modifier: Modifier) {
    val icon =
        FileSystemView.getFileSystemView()?.getSystemIcon(file)?.toImage()?.toBufferedImage()?.toComposeImageBitmap()
    if (icon != null) {
        Image(bitmap = icon, contentDescription = file.name, modifier = modifier)
    } else {
        Image(
            painter = painterResource("exe_file_icon.svg"),
            contentDescription = file.name,
            modifier = modifier
        )
    }
}

@Composable
fun ShortcutImage(file: File, modifier: Modifier) {
    var path by remember { mutableStateOf(null as String?) }
    if (path != null) {
        val absFile = File(file.parentFile, path!!)
        val icon =
            FileSystemView.getFileSystemView()?.getSystemIcon(absFile)?.toImage()?.toBufferedImage()?.toComposeImageBitmap()
        if (icon != null) {
            Image(bitmap = icon, contentDescription = file.name, modifier = modifier)
        } else {
            Image(
                painter = painterResource("lnk_file_icon.svg"),
                contentDescription = file.name,
                modifier = modifier
            )
        }
    } else {
        Image(
            painter = painterResource("lnk_file_icon.svg"),
            contentDescription = file.name,
            modifier = modifier
        )
    }
    LaunchedEffect(file) {
        withContext(Dispatchers.IO) {
            runCatching {
                path = WindowsShortcut(file).relativePath
            }
        }
    }

}
