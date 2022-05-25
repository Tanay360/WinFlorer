@file:Suppress("FunctionName")

package components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.awtEventOrNull
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import javax.swing.filechooser.FileSystemView

@Composable
fun SelectableFolderLarge(folder: Folder, paths: SnapshotStateList<String>, modifier: Modifier = Modifier) {
    val (icon, title) = folder
    SelectableFolderLarge(title, icon, paths, modifier)
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SelectableFolderLarge(title: String, icon: String = "folder-icon.svg", paths: SnapshotStateList<String>, modifier: Modifier = Modifier) {
    val focusRequester = FocusRequester()
    var isTapped by remember { mutableStateOf(false) }
    var isDoubleTap by remember { mutableStateOf(false) }
    var color by remember { mutableStateOf(Color.Transparent) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(5.dp).focusRequester(focusRequester)
            .onFocusChanged { color = if (it.isFocused) Color(0xFF2059c9) else Color.Transparent }
            .focusTarget()
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = title,
            modifier = Modifier.width(122.525f.dp).height(97.175f.dp).clip(RoundedCornerShape(4.dp))
                .background(if (color != Color.Transparent) Color(0xFF313334) else color).padding(16.dp)
                .onPointerEvent(PointerEventType.Press) {
                    when {
                        it.buttons.isPrimaryPressed -> when (it.awtEventOrNull?.clickCount) {
                            1 -> {
                                isTapped = true
                            }
                            2 -> {
                                isDoubleTap = true
                            }
                        }
                    }
                }
        )
        Spacer(modifier = Modifier.height(4.dp))
        Column(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(color).padding(3.dp).pointerInput(Unit) {
            detectTapGestures {
                isTapped = true
            }
        }) {
            Text(
                text = title,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
    LaunchedEffect(isTapped) {
        if (isTapped) {
            focusRequester.requestFocus()
            isTapped = false
        }
    }

    LaunchedEffect(isDoubleTap) {
        if (isDoubleTap) {
            val file = when (title) {
                "Desktop" -> File(System.getProperty("user.home") + "/Desktop")
                "Documents" -> File(FileSystemView.getFileSystemView().defaultDirectory.path)
                "Downloads" -> File(System.getProperty("user.home") + "/Downloads")
                "Videos" -> File(System.getProperty("user.home") + "/Videos")
                "Music" -> File(System.getProperty("user.home") + "/Music")
                "Pictures" -> File(System.getProperty("user.home") + "/Pictures")
                else -> null
            }
            if (file != null) {
                paths.clear()
                paths.addAll(file.absolutePath.split("/", "\\").filter { it.isNotBlank() }
                    .let { listOf(it.first() + "\\", *it.subList(1, it.size).toTypedArray()) })
            }
            isDoubleTap = false
        }
    }
}

data class Folder(val icon: String, val title: String)

infix fun String.folder(title: String): Folder = Folder(this, title)