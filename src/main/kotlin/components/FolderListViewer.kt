@file:Suppress("FunctionName")

package components

import androidx.compose.foundation.Image
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
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
import androidx.compose.ui.awt.awtEventOrNull
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import helper.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*
import kotlin.io.path.exists
import kotlin.io.path.pathString

@Composable
fun FolderListViewer(
    modifier: Modifier = Modifier,
    paths: SnapshotStateList<String>,
    removedPaths: SnapshotStateList<String>,
    showHiddenFiles: MutableState<Boolean>,
    currentSortBy: MutableState<SortBy>,
    refresh: MutableState<Boolean>,
    showSnackBar: (String) -> Unit
) {
    val stateVertical = rememberLazyListState()
    var files by remember { mutableStateOf(listOf<File>()) }
    var isLoading by remember { mutableStateOf(true) }

    Box(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(end = 10.dp)
        ) {
            if (files.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(bottom = 4.dp, start = 8.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(modifier = Modifier.fillMaxWidth(0.6f), text = "Name", fontSize = 14.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            modifier = Modifier.fillMaxWidth(0.6f),
                            text = "Date Modified",
                            fontSize = 12.sp
                        )
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = "Size",
                            fontSize = 12.sp
                        )
                    }
                }
            }
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 2.dp),
                state = stateVertical
            ) {
                item {
                    if (files.isEmpty() && !isLoading) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Image(
                                modifier = Modifier.size(250.dp),
                                painter = painterResource("no_files_here.svg"),
                                contentDescription = "No Files here"
                            )
                            Text(text = "There are no files here!", fontSize = 14.sp)
                        }
                    }
                }
                items(files.size, key = { files[it].name }) { index ->
                    val file = files[index]
                    FileItem(
                        file = file,
                        paths = paths,
                        removedPaths = removedPaths,
                        showHiddenFiles = showHiddenFiles,
                        showSnackBar = showSnackBar,
                        refresh = refresh,
                        painter = painterResource(FileIconGeneratorListColumn.getIcon(file)),
                    )
                }
            }
        }
        VerticalScrollbar(
            modifier = Modifier.fillMaxHeight().width(10.dp).align(Alignment.CenterEnd),
            adapter = rememberScrollbarAdapter(scrollState = stateVertical)
        )
    }

    val loadFiles: CoroutineScope.() -> Unit = {
        runCatching {
            isLoading = true
            val folder: Path = Paths.get(paths.joinToString("/"))
            if (!folder.exists()) {
                showSnackBar("${folder.pathString} does not exist!")
                paths.removeLastOrNull()
            }
            Files.newDirectoryStream(
                folder
            ) { entry: Path? -> entry != null }.use { stream ->
                val filesNew =
                    stream.map { it.toFile() }.filter { it.exists() && (showHiddenFiles.value || !it.isHidden) }.let {
                        return@let (when (currentSortBy.value) {
                            SortBy.NAME_ASCENDING -> it.sortedBy { f -> f.name }
                            SortBy.NAME_DESCENDING -> it.sortedByDescending { f -> f.name }
                            SortBy.DATE_ASCENDING -> it.sortedBy { f -> f.lastModified() }
                            SortBy.DATE_DESCENDING -> it.sortedByDescending { f -> f.lastModified() }
                            SortBy.SIZE_ASCENDING -> it.sortedBy { f -> f.length() }
                            SortBy.SIZE_DESCENDING -> it.sortedByDescending { f -> f.length() }
                        })
                    }
                files = filesNew
                stream.close()
            }
        }.getOrNull().let {
            isLoading = false
        }
    }

    LaunchedEffect(refresh.value) {
        if (refresh.value) {
            loadFiles()
            refresh.value = false
        }
    }
    LaunchedEffect(paths.toList(), currentSortBy.value, showHiddenFiles.value) {
        loadFiles()
        stateVertical.scrollToItem(0)
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun FileItem(
    modifier: Modifier = Modifier,
    paths: SnapshotStateList<String>,
    removedPaths: SnapshotStateList<String>,
    painter: Painter,
    file: File,
    showSnackBar: (String) -> Unit,
    refresh: MutableState<Boolean>,
    showHiddenFiles: MutableState<Boolean>,
    isIcon: Boolean = false,
    tint: Color? = null
) {
    val focusRequester = remember { FocusRequester() }
    var isDoubleTap by remember { mutableStateOf(false) }
    var isTapped by remember { mutableStateOf(false) }
    val expanded = remember { mutableStateOf(false to null as Offset?) }
    var color by remember { mutableStateOf(Color.Transparent) }
    var isHovered by remember { mutableStateOf(false) }
    var isShortCut by remember { mutableStateOf(false) }

    var y by remember { mutableStateOf(0f) }
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(color = if (color == Color.Transparent && isHovered) Color.DarkGray else color)
            .padding(bottom = 2.dp, start = 8.dp, end = 8.dp)
            .pointerMoveFilter(
                onEnter = {
                    isHovered = true
                    false
                },
                onExit = {
                    isHovered = false
                    false
                }
            )
            .onGloballyPositioned { layoutCoordinates ->
                y = layoutCoordinates.positionInParent().y
            }
            .focusRequester(focusRequester)
            .onFocusChanged {
                color = if (it.isFocused) Color(0xFF126def) else Color.Transparent
            }
            .focusTarget()
            .onPointerEvent(PointerEventType.Press) {
                val (x) = it.changes.first().position
                when {
                    it.buttons.isPrimaryPressed -> when (it.awtEventOrNull?.clickCount) {
                        1 -> {
                            isTapped = true
                        }
                        2 -> {
                            isDoubleTap = true
                        }
                    }
                    it.buttons.isSecondaryPressed -> {
                        isTapped = true
                        expanded.value = true to Offset(x, y)
                    }
                }
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(0.6f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (file.extension != "exe" && !isShortCut) {
                if (isIcon) {
                    Icon(
                        painter = painter,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp).padding(end = 3.dp),
                        tint = tint ?: LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
                    )
                } else {
                    Image(
                        painter = painter,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp).padding(end = 3.dp)
                    )
                }
            } else {
                if (file.extension == "exe") {
                    ExeImage(file, modifier = Modifier.size(20.dp).padding(end = 3.dp))
                } else {
                    ShortcutImage(file, modifier = Modifier.size(20.dp).padding(end = 3.dp))
                }
            }
            Text(text = file.name, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(0.6f),
                text = SimpleDateFormat("dd-MM-yyyy HH:mm").format(Date(file.lastModified())),
                fontSize = 12.sp
            )
            if (!file.isDirectory) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = humanReadableByteCountSI(file.length()),
                    fontSize = 12.sp
                )
            }
        }
    }

    FileDropdown(
        file = file,
        showSnackBar = showSnackBar,
        paths = paths,
        refresh = refresh,
        showHiddenFiles = showHiddenFiles,
        expanded = expanded
    )
    LaunchedEffect(isTapped) {
        if (isTapped) {
            focusRequester.requestFocus()
            isTapped = false
        }
    }
    LaunchedEffect(isDoubleTap) {
        if (isDoubleTap) {
            if (!file.isDirectory) {
                runCatching {
                    Desktop.getDesktop().open(file)
                }
            } else {
                paths.add(file.name)
                removedPaths.clear()
            }
            isDoubleTap = false
        }
    }

    LaunchedEffect(file) {
        withContext(Dispatchers.IO) {
            runCatching {
                isShortCut = file.extension.lowercase() == "lnk" && WindowsShortcut.isPotentialValidLink(file)
            }
        }
    }

}