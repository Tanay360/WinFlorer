@file:Suppress("FunctionName")

package components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.awt.awtEventOrNull
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import helper.TooltipTarget
import java.io.File
import kotlin.math.pow

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainFolders(modifier: Modifier = Modifier, paths: SnapshotStateList<String>) {
    Column(
        modifier = modifier
    ) {
        val folders = listOf(
            "desktop-folder.svg" folder "Desktop",
            "documents-folder.svg" folder "Documents",
            "downloads-folder.svg" folder "Downloads",
            "videos-folder.svg" folder "Videos",
            "music-folder.svg" folder "Music",
            "pictures-folder.svg" folder "Pictures"
        )
        Text(text = "Folders(${folders.size})", fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp, start = 8.dp))

        LazyVerticalGrid(
            modifier = Modifier.fillMaxWidth(),
            cells = GridCells.Fixed(6)
        ) {
            items(folders.size) { folderIndex ->
                SelectableFolderLarge(folders[folderIndex], paths)
            }
        }

        Divider()
        val disks = File.listRoots().toList()
        Text(text = "Drives(${disks.size})", fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp, start = 8.dp))

        LazyVerticalGrid(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(8.dp),
            cells = GridCells.Fixed(2)
        ) {
            items(disks.size) { index: Int ->
                Disk(paths, disks[index])
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun Disk(paths: SnapshotStateList<String>, disk: File) {
    val focusRequester = remember { FocusRequester() }
    var color by remember { mutableStateOf(Color.Transparent) }
    var tapped by remember { mutableStateOf(false) }
    var isDoubleTap by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxSize().focusRequester(focusRequester)
            .onPointerEvent(PointerEventType.Press) {
                when {
                    it.buttons.isPrimaryPressed -> when (it.awtEventOrNull?.clickCount) {
                        1 -> {
                            tapped = true
                        }
                        2 -> {
                            isDoubleTap = true
                        }
                    }
                }
            }
            .onFocusChanged { color = if (it.isFocused) Color(0xFF313334) else Color.Transparent }
            .focusTarget(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.background(color).padding(4.dp)
        ) {

            Image(
                painter = painterResource("disk.svg"),
                contentDescription = "Disk",
                modifier = Modifier.size(50.dp).padding(end = 4.dp)
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val freeSpace = bytesToGb(disk.freeSpace)
                val totalSpace = bytesToGb(disk.totalSpace)
                Text("Disk (${disk.absolutePath.split("\\")[0]})", fontSize = 12.sp)
                TooltipTarget(tip = "Space free: ${freeSpace.toInt()} GB \nTotal Size: ${totalSpace.toInt()} GB") {
                    LinearProgressIndicator(
                        progress = ((totalSpace - freeSpace) / totalSpace).toFloat(),
                        modifier = Modifier.height(15.dp).clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFF1AA7EC),
                        backgroundColor = Color.White
                    )
                }
                Text(text = "${freeSpace.toInt()} GB free of ${totalSpace.toInt()} GB", fontSize = 12.sp)
            }
        }

    }

    LaunchedEffect(tapped) {
        if (tapped) {
            focusRequester.requestFocus()
            tapped = false
        }
    }

    LaunchedEffect(isDoubleTap) {
        if (isDoubleTap) {
            paths.clear()
            paths.add(disk.absolutePath)
            isDoubleTap = false
        }
    }


}

internal fun bytesToGb(bytes: Long): Double = bytes / 1024.0.pow(3)