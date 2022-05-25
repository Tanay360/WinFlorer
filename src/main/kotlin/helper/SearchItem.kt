@file:Suppress("FunctionName")

package helper

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import components.FileDropdown
import java.awt.Desktop
import java.io.File
import java.text.CharacterIterator
import java.text.SimpleDateFormat
import java.text.StringCharacterIterator
import java.util.*

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SearchItem(
    modifier: Modifier = Modifier,
    file: File,
    paths: SnapshotStateList<String>,
    dismissDialog: () -> Unit,
    showSnackBar: (String) -> Unit,
    refresh: MutableState<Boolean>,
    showHiddenFiles: MutableState<Boolean>
) {
    val focusRequester = remember { FocusRequester() }
    var isDoubleTap by remember { mutableStateOf(false) }
    var isTapped by remember { mutableStateOf(false) }
    var color by remember { mutableStateOf(Color.Transparent) }
    val expanded = remember { mutableStateOf(false to null as Offset?) }
    var isHovered by remember { mutableStateOf(false) }
    var y by remember { mutableStateOf(0f) }

    Row(modifier = modifier.fillMaxWidth().background(color = if (color == Color.Transparent && isHovered) Color.DarkGray else color)
        .padding(bottom = 2.dp, start = 8.dp, end = 8.dp).focusRequester(focusRequester)
        .onFocusChanged {
            color = if (it.isFocused) Color(0xFF126def) else Color.Transparent
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
        .onGloballyPositioned { layoutCoordinates ->
            y = layoutCoordinates.positionInParent().y
        }
        .focusTarget()
        .onPointerEvent(PointerEventType.Press) {
            val (x) = it.changes.first().position

            when {
                it.buttons.isPrimaryPressed -> when (it.awtEventOrNull?.clickCount) {
                    1-> {
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
        .padding(horizontal = 16.dp, vertical = 8.dp)
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
            Text("Size: ${humanReadableByteCountSI(file.length())}", fontSize = 14.sp)
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
                dismissDialog()
            } else {
                runCatching {
                    Desktop.getDesktop().open(file)
                }
            }
            isDoubleTap = false
        }
    }
}