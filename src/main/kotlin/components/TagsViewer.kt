@file:Suppress("FunctionName")

package components

import androidx.compose.foundation.Image
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.awtEventOrNull
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import db.ArrayDB
import helper.FileIconGeneratorListColumn
import helper.TooltipTarget
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TagsViewer(
    modifier: Modifier = Modifier,
    tag: String,
    paths: SnapshotStateList<String>,
    db: ArrayDB,
    showSnackBar: (String) -> Unit,
    showHiddenFiles: MutableState<Boolean>
) {
    var files by remember { mutableStateOf(arrayOf<String>()) }
    Box(modifier = modifier) {
        val verticalState = rememberLazyListState()
        Column(modifier = Modifier.fillMaxSize()) {
            Text(tag, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp, start = 8.dp))
            LazyColumn(
                state = verticalState,
                modifier = Modifier.fillMaxSize().padding(end = 10.dp)
            ) {
                item {
                    if (files.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Image(
                                modifier = Modifier.size(300.dp),
                                painter = painterResource("no_items_here.svg"),
                                contentDescription = "No Items"
                            )
                            Text("No Items Here", fontSize = 16.sp)
                        }
                    }
                }
                items(files, key = { it }) { filePath ->
                    val file = File(filePath)
                    FileTagItem(
                        file = file,
                        paths = paths,
                        showSnackBar = showSnackBar,
                        showHiddenFiles = showHiddenFiles,
                        refresh = remember { mutableStateOf(false) }
                    )
                    Divider()
                }
            }
        }
        VerticalScrollbar(
            modifier = Modifier.fillMaxHeight().width(10.dp).align(Alignment.CenterEnd),
            adapter = rememberScrollbarAdapter(scrollState = verticalState)
        )
    }
    LaunchedEffect(tag) {
        files = db.getAllItems(tag)
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun FileTagItem(
    file: File,
    modifier: Modifier = Modifier,
    paths: SnapshotStateList<String>,
    showSnackBar: (String) -> Unit,
    refresh: MutableState<Boolean>,
    showHiddenFiles: MutableState<Boolean>
) {
    val focusRequester = remember { FocusRequester() }
    var isDoubleTap by remember { mutableStateOf(false) }
    var isTapped by remember { mutableStateOf(false) }
    var color by remember { mutableStateOf(Color.Transparent) }
    val expanded = remember { mutableStateOf(false to null as Offset?) }
    var y by remember { mutableStateOf(0f) }

    Row(modifier = modifier.fillMaxWidth().background(color = color)
        .padding(bottom = 2.dp, start = 8.dp, end = 8.dp).focusRequester(focusRequester)
        .onFocusChanged {
            color = if (it.isFocused) Color(0xFF126def) else Color.Transparent
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
                    isTapped = true
                    expanded.value = true to Offset(x, y)
                }
            }
        }
        .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                modifier = Modifier.size(20.dp),
                painter = painterResource(FileIconGeneratorListColumn.getIcon(file)),
                contentDescription = file.name
            )
            Column(modifier = Modifier.padding(start = 6.dp).fillMaxWidth(0.7f)) {
                Text(text = file.name, fontSize = 14.sp)
                TooltipTarget(file.absolutePath) {
                    Text(text = file.absolutePath, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
                }
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Date Modified: ${SimpleDateFormat("dd-MM-yyyy HH:mm").format(Date(file.lastModified()))}",
                fontSize = 12.sp
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

    LaunchedEffect(isTapped) {
        if (isTapped) {
            focusRequester.requestFocus()
            isTapped = false
        }
    }
    LaunchedEffect(isDoubleTap) {
        if (isDoubleTap) {
            if (file.isDirectory) {
                paths.clear()
                paths.addAll(file.absolutePath.split("/", "\\").filter { it.isNotBlank() }
                    .let { listOf(it.first() + "\\", *it.subList(1, it.size).toTypedArray()) })
            }
            isDoubleTap = false
        }
    }

}