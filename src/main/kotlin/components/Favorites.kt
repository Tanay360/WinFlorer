@file:Suppress("FunctionName")

package components

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import helper.GlassyContainer
import helper.TooltipTarget
import java.io.File
import javax.swing.filechooser.FileSystemView

@Composable
fun Favorites(
    modifier: Modifier = Modifier,
    paths: SnapshotStateList<String>,
    removedPaths: SnapshotStateList<String>,
    setTag: (String?) -> Unit
) {
    val setTagItem: (String) -> Unit = { str ->
        paths.clear()
        removedPaths.clear()
        setTag(str)
    }
    GlassyContainer(
        modifier = modifier
            .fillMaxHeight()
            .fillMaxWidth(0.2f)
    ) {
        Box(
            modifier = it
        ) {
            val verticalScroll = rememberScrollState(0)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .verticalScroll(verticalScroll)
            ) {
                Text("Favorites", fontSize = 12.sp)
                Column(
                    modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth()
                ) {
                    FavItem(text = "Desktop", iconRes = "desktop.svg") { f -> openFolder(f, paths) }
                    FavItem(text = "Documents", iconRes = "documents.svg") { f -> openFolder(f, paths) }
                    FavItem(text = "Downloads", iconRes = "downloads.svg") { f -> openFolder(f, paths) }
                    FavItem(text = "Music", iconRes = "music.svg") { f -> openFolder(f, paths) }
                    FavItem(text = "Pictures", iconRes = "pictures.svg") { f -> openFolder(f, paths) }
                    FavItem(text = "Videos", iconRes = "videos.svg") { f -> openFolder(f, paths) }
                }

                Text("Drives", fontSize = 12.sp)
                val disks = File.listRoots().toList()

                Column(
                    modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    for (disk in disks) {
                        DiskItem(disk, paths)
                    }
                }

                Text("Tags", fontSize = 12.sp)
                Column(
                    modifier = Modifier.padding(top = 8.dp).fillMaxWidth()
                ) {
                    TagItem(text = "Red", color = Color.Red, setTagItem)
                    TagItem(text = "Orange", color = Color(0xFFFFA500), setTagItem)
                    TagItem(text = "Yellow", color = Color.Yellow, setTagItem)
                    TagItem(text = "Green", color = Color.Green, setTagItem)
                    TagItem(text = "Blue", color = Color.Blue, setTagItem)
                    TagItem(text = "Purple", color = Color(0xFFA020F0), setTagItem)
                    TagItem(text = "Gray", color = Color.Gray, setTagItem)
                }
            }
            VerticalScrollbar(
                modifier = Modifier.align(Alignment.CenterEnd)
                    .fillMaxHeight(),
                adapter = rememberScrollbarAdapter(verticalScroll)
            )
        }
    }
}

private fun openFolder(title: String, paths: SnapshotStateList<String>) {
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

}

@Composable
private fun DiskItem(disk: File, paths: SnapshotStateList<String>) {
    val freeSpace = bytesToGb(disk.freeSpace)
    val totalSpace = bytesToGb(disk.totalSpace)
    TooltipTarget(tip = "Space free: ${freeSpace.toInt()} GB \nTotal Size: ${totalSpace.toInt()} GB") {
        FavItem(text = "Disk (${disk.absolutePath.split("\\")[0]})", onClick = {
            paths.clear()
            paths.add(disk.absolutePath)
        }, painter = painterResource("disk.svg"), isIcon = false)
    }
}

@Composable
private fun FavItem(text: String, iconRes: String, onClick: (title: String) -> Unit) {
    FavItem(text, painterResource(iconRes), onClick = onClick, isIcon = false)
}

@Composable
private fun TagItem(text: String, color: Color, onClick: (title: String) -> Unit) {
    FavItem(text = text, painter = painterResource("circle.svg"), onClick = onClick, tint = color)
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun FavItem(
    text: String,
    painter: Painter,
    onClick: (title: String) -> Unit,
    isIcon: Boolean = true,
    tint: Color? = null
) {
    var isHovered by remember { mutableStateOf(false) }
    val focusRequester = FocusRequester()
    var isTapped by remember { mutableStateOf(false) }
    var color by remember { mutableStateOf(Color.Transparent) }
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp))
            .background(color = if (color == Color.Transparent && isHovered) Color.DarkGray else color)
            .padding(bottom = 2.dp, start = 8.dp, end = 8.dp).focusRequester(focusRequester)
            .pointerMoveFilter(
                onEnter = {
                    isHovered = true
                    true
                },
                onExit = {
                    isHovered = false
                    true
                }
            )
            .onFocusChanged { color = if (it.isFocused) Color(0xFF126def) else Color.Transparent }
            .focusTarget()
            .pointerInput(Unit) {
                detectTapGestures {
                    onClick(text)
                    isTapped = true
                }
            }
    ) {
        if (isIcon) {
            Icon(
                painter = painter,
                contentDescription = text,
                modifier = Modifier.size(20.dp).padding(end = 3.dp),
                tint = tint ?: LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
            )
        } else {
            Image(
                painter = painter,
                contentDescription = text,
                modifier = Modifier.size(20.dp).padding(end = 3.dp)
            )
        }
        Text(text = text, fontSize = 14.sp)
    }
    LaunchedEffect(isTapped) {
        if (isTapped) {
            focusRequester.requestFocus()
            isTapped = false
        }
    }
}