@file:Suppress("FunctionName")

package components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.loadSvgPainter
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderLargeViewer(
    modifier: Modifier = Modifier,
    paths: SnapshotStateList<String>,
    removedPaths: SnapshotStateList<String>,
    showHiddenFiles: MutableState<Boolean>,
    currentSortBy: MutableState<SortBy>,
    refresh: MutableState<Boolean>,
    showSnackBar: (String) -> Unit
) {
    val listState = rememberLazyListState()
    var files by remember { mutableStateOf(listOf<File>()) }
    var isLoading by remember { mutableStateOf(true) }

    Box(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(end = 10.dp),
            state = listState
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

            gridItems(files, columnCount = 5, modifier = Modifier.fillMaxSize()) { file ->
                FileItem(
                    paths = paths,
                    removedPaths = removedPaths,
                    file = file,
                    showSnackBar = showSnackBar,
                    showHiddenFiles = showHiddenFiles,
                    refresh = refresh
                )
            }
        }

        VerticalScrollbar(
            modifier = Modifier.fillMaxHeight().width(10.dp).align(Alignment.CenterEnd),
            adapter = rememberScrollbarAdapter(scrollState = listState)
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
    }
}

private val BITMAPS_EXT = listOf("png", "jpeg", "jpg", "ico", "jfif", "gif")

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun FileItem(
    modifier: Modifier = Modifier,
    paths: SnapshotStateList<String>,
    removedPaths: SnapshotStateList<String>,
    file: File,
    showSnackBar: (String) -> Unit,
    refresh: MutableState<Boolean>,
    showHiddenFiles: MutableState<Boolean>,
) {
    val focusRequester = remember { FocusRequester() }
    var isDoubleTap by remember { mutableStateOf(false) }
    var isTapped by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    var isShortCut by remember { mutableStateOf(false) }
    var isHovered by remember { mutableStateOf(false) }

    var y by remember { mutableStateOf(0f) }

    val expanded = remember { mutableStateOf(false to null as Offset?) }

    TooltipTarget(tip = file.name) {
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            val ext = file.extension.lowercase()
            val isBmp = BITMAPS_EXT.indexOf(ext) > -1
            Box(
                modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    .onGloballyPositioned { layoutCoordinates ->
                        y = layoutCoordinates.positionInParent().y - 20
                    }
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
                    .focusRequester(focusRequester)
                    .onFocusChanged {
                        isFocused = it.isFocused
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
                                expanded.value = true to Offset(x,y)
                            }
                        }
                    }
                    .background(if (isFocused) Color(0xFF4d97f7) else if (isHovered) Color.DarkGray else Color.Transparent)
                    .padding(horizontal = 5.dp, vertical = 10.dp)
            ) {
                if (!isBmp && ext != "svg" && !isShortCut && ext != "exe") {
                    Image(
                        modifier = Modifier.size(60.dp),
                        painter = FileIconGeneratorLargeIcon.getIcon(file),
                        contentDescription = file.name
                    )
                } else {
                    when {
                        ext == "exe" -> {
                            ExeImage(file, modifier = Modifier.size(60.dp))
                        }
                        isShortCut -> {
                            ShortcutImage(file = file, modifier = Modifier.size(60.dp))
                        }
                        isBmp -> {
                            BitmapImage(file = file, modifier = Modifier.size(60.dp))
                        }
                        else -> {
                            SvgImage(file = file, modifier = Modifier.size(60.dp))
                        }
                    }
                }
            }
            Text(
                file.name,
                fontSize = 14.sp,
                modifier = Modifier.clip(
                    RoundedCornerShape(4.dp)
                ).background(if (isFocused) Color(0xFF126def) else if (isHovered) Color.DarkGray else Color.Transparent).padding(2.dp)
                    .focusRequester(focusRequester)
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
                        isFocused = it.isFocused
                    }
                    .focusTarget()
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
                    },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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

    LaunchedEffect(file) {
        withContext(Dispatchers.IO) {
            runCatching {
                isShortCut = file.extension.lowercase() == "lnk" && WindowsShortcut.isPotentialValidLink(file)
            }
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

}

@Composable
fun SvgImage(
    file: File,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var image by remember { mutableStateOf(null as Painter?) }

    if (image != null) {
        Image(
            painter = image!!,
            contentDescription = file.name,
            modifier = modifier
        )
    } else {
        Image(
            painter = painterResource("image-icon.svg"),
            contentDescription = file.name,
            modifier = modifier
        )
    }

    LaunchedEffect(file) {
        withContext(Dispatchers.IO) {
            runCatching {
                image = loadSvgPainter(file.inputStream(), density)
            }
        }
    }

}

@Composable
fun BitmapImage(
    file: File,
    modifier: Modifier = Modifier
) {
    var image by remember { mutableStateOf(null as ImageBitmap?) }

    if (image != null) {
        Image(
            bitmap = image!!,
            contentDescription = file.name,
            modifier = modifier,
        )
    } else {
        Image(
            painter = painterResource("image-icon.svg"),
            contentDescription = file.name,
            modifier = modifier
        )
    }

    LaunchedEffect(file) {
        withContext(Dispatchers.IO) {
            runCatching {
                image = org.jetbrains.skia.Image.makeFromEncoded(file.readBytes()).toComposeImageBitmap()
            }
        }
    }
}