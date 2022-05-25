@file:Suppress("FunctionName")

package components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
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
import kotlin.io.path.exists
import kotlin.io.path.pathString

@Composable
fun FolderColumnViewer(
    modifier: Modifier = Modifier,
    paths: SnapshotStateList<String>,
    removedPaths: SnapshotStateList<String>,
    showHiddenFiles: MutableState<Boolean>,
    currentSortBy: MutableState<SortBy>,
    refresh: MutableState<Boolean>,
    showSnackBar: (String) -> Unit
) {
    val refresher = remember { mutableStateOf(0 to false) }
    Row(
        modifier = modifier
    ) {
        for ((index, pathIndex) in (paths.size - 3 until paths.size).withIndex().toList()) {
            if (pathIndex > -1) {
                FolderColumn(
                    modifier = Modifier.fillMaxWidth(1f / (3 - index)).fillMaxHeight(),
                    paths = paths,
                    removedPaths = removedPaths,
                    currentPath = paths[pathIndex],
                    pathIndex = pathIndex,
                    showHiddenFiles = showHiddenFiles,
                    currentSortBy = currentSortBy,
                    refresher = refresher,
                    showSnackBar = showSnackBar,
                    refresh = refresh
                )
            }
        }
    }
    LaunchedEffect(refresh.value) {
        if (refresh.value) {
            refresher.value = 3 to true
        }
    }
    LaunchedEffect(refresher.value) {
        if (!refresher.value.second && refresh.value) {
            refresh.value = false
        }
    }
}

@Composable
fun FolderColumn(
    modifier: Modifier = Modifier,
    paths: SnapshotStateList<String>,
    removedPaths: SnapshotStateList<String>,
    currentPath: String,
    pathIndex: Int,
    showHiddenFiles: MutableState<Boolean>,
    currentSortBy: MutableState<SortBy>,
    refresher: MutableState<Pair<Int, Boolean>>,
    showSnackBar: (String) -> Unit,
    refresh: MutableState<Boolean>
) {
    val stateVertical = rememberLazyListState()
    var scrollToPosition by remember { mutableStateOf(0) }
    var files by remember { mutableStateOf(listOf<File>()) }
    var isLoading by remember { mutableStateOf(true) }
    val nextSelected = if (pathIndex < paths.size - 1) File(paths.subList(0, pathIndex + 2).joinToString("/")) else null
    Box(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
                .padding(end = 13.dp),
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
                            modifier = Modifier.size(200.dp),
                            painter = painterResource("no_files_here.svg"),
                            contentDescription = "No Files here"
                        )
                        Text(text = "There are no files here!", fontSize = 14.sp)
                    }
                }
            }

            items(files.size, key = { files[it].name }) { index ->
                val file = files[index]
                val isSelected = file == nextSelected
                if (isSelected && !file.exists()) {
                    paths.removeRange(pathIndex + 1, paths.size)
                }
                FileItem(
                    paths = paths,
                    removedPaths = removedPaths,
                    refresh = refresh,
                    showSnackBar = showSnackBar,
                    showHiddenFiles = showHiddenFiles,
                    parentIndex = pathIndex,
                    painter = painterResource(FileIconGeneratorListColumn.getIcon(file)),
                    file = file,
                    isSelected = isSelected,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxHeight().align(Alignment.CenterEnd).width(13.dp)
        ) {
            VerticalScrollbar(
                modifier = Modifier.fillMaxHeight(),
                adapter = rememberScrollbarAdapter(scrollState = stateVertical)
            )
            Divider(modifier = Modifier.width(1.dp).fillMaxHeight())
        }
        LaunchedEffect(scrollToPosition) {
            stateVertical.scrollToItem(scrollToPosition)
        }

        val loadFiles: CoroutineScope.() -> Unit = {
            runCatching {
                isLoading = true
                val folder: Path = Paths.get(paths.subList(0, pathIndex + 1).joinToString("/"))
                if (!folder.exists()) {
                    showSnackBar("${folder.pathString} does not exist!")
                    paths.removeRange(pathIndex, paths.size)
                }
                Files.newDirectoryStream(
                    folder
                ) { entry: Path? -> entry != null }.use { stream ->
                    var pos = 0
                    val filesNew =
                        stream.map { it.toFile() }.filter { it.exists() && (showHiddenFiles.value || !it.isHidden) }
                            .let {
                                return@let (when (currentSortBy.value) {
                                    SortBy.NAME_ASCENDING -> it.sortedBy { f -> f.name }
                                    SortBy.NAME_DESCENDING -> it.sortedByDescending { f -> f.name }
                                    SortBy.DATE_ASCENDING -> it.sortedBy { f -> f.lastModified() }
                                    SortBy.DATE_DESCENDING -> it.sortedByDescending { f -> f.lastModified() }
                                    SortBy.SIZE_ASCENDING -> it.sortedBy { f -> f.length() }
                                    SortBy.SIZE_DESCENDING -> it.sortedByDescending { f -> f.length() }
                                }).also { f ->
                                    f.forEachIndexed { index, file ->
                                        if (file.absolutePath == nextSelected?.absolutePath) {
                                            pos = index
                                        }
                                    }
                                }
                            }
                    files = filesNew
                    scrollToPosition = pos
                    stream.close()
                }
            }.getOrNull().let {
                isLoading = false
            }
        }

        LaunchedEffect(refresher.value) {
            var (times, refreshone) = refresher.value
            if (refreshone) {
                loadFiles()
                if (paths.size == 1) {
                    times = 0
                    refreshone = false
                } else {
                    if (times == 1) {
                        times = 0
                        refreshone = false
                    } else {
                        times -= 1
                    }
                }
                refresher.value = times to refreshone
            }
        }

        LaunchedEffect(currentPath, pathIndex, currentSortBy.value, showHiddenFiles.value) {
            loadFiles()
        }
    }
}


@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun FileItem(
    modifier: Modifier = Modifier,
    paths: SnapshotStateList<String>,
    removedPaths: SnapshotStateList<String>,
    parentIndex: Int,
    painter: Painter,
    file: File,
    isIcon: Boolean = false,
    isSelected: Boolean = false,
    tint: Color? = null,
    showSnackBar: (String) -> Unit,
    showHiddenFiles: MutableState<Boolean>,
    refresh: MutableState<Boolean>
) {
    val focusRequester = remember { FocusRequester() }
    var isDoubleTap by remember { mutableStateOf(false) }
    var isTapped by remember { mutableStateOf(false) }
    var color by remember { mutableStateOf(if (!isSelected) Color.Transparent else Color(0xFF126def)) }
    var isShortCut by remember { mutableStateOf(false) }
    var y by remember { mutableStateOf(0f) }
    var isHovered by remember { mutableStateOf(false) }
    val expanded = remember { mutableStateOf(false to null as Offset?) }

    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp))
            .background(color = if (color == Color.Transparent && isHovered) Color.DarkGray else color)
            .padding(bottom = 2.dp, start = 8.dp, end = 8.dp).focusRequester(focusRequester)
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
            .onFocusChanged {
                color = if ((it.isFocused && !file.isDirectory) || isSelected) Color(0xFF126def) else Color.Transparent
            }
            .onGloballyPositioned { layoutCoordinates ->
                y = layoutCoordinates.positionInParent().y
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
                        expanded.value = true to Offset(x, y)
                        if (file.isFile) {
                            isTapped = true
                        }
                    }
                }
            }
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
    if (paths.lastIndex == parentIndex) {
        FileDropdown(
            file = file,
            showSnackBar = showSnackBar,
            paths = paths,
            refresh = refresh,
            showHiddenFiles = showHiddenFiles,
            expanded = expanded
        )
    }
    LaunchedEffect(isSelected) {
        color = if (isSelected) Color(0xFF126def) else Color.Transparent
    }
    LaunchedEffect(isTapped) {
        if (isTapped) {
            focusRequester.requestFocus()
            if (file.isDirectory && !isSelected) {
                if (paths.getOrNull(parentIndex + 1) != null) {
                    paths.removeRange(parentIndex + 1, paths.size)
                    paths.add(file.name)
                    removedPaths.clear()
                } else {
                    paths.add(file.name)
                    removedPaths.clear()
                }
            } else if (!file.isDirectory) {
                if (paths.getOrNull(parentIndex + 1) != null) {
                    paths.removeRange(parentIndex + 1, paths.size)
                }
            }
            isTapped = false
        }
    }
    LaunchedEffect(isDoubleTap) {
        if (isDoubleTap) {
            if (!file.isDirectory) {
                runCatching {
                    Desktop.getDesktop().open(file)
                }
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