@file:Suppress("FunctionName")

package components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import helper.MButton
import helper.MTextField
import helper.TooltipTarget
import java.io.File
import kotlin.math.max

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PathsBreadCrumbs(modifier: Modifier = Modifier, paths: SnapshotStateList<String>, showSnackBar: (String) -> Unit) {
    var editOpen by remember { mutableStateOf(false) }
    var tempFile by remember { mutableStateOf(File(paths.joinToString("/")).absolutePath) }
    val editDone = {
        val file = File(tempFile)
        if (!file.exists()) {
            showSnackBar("File does not exist!")
            tempFile = File(paths.joinToString("/")).absolutePath
        } else if (file.isDirectory) {
            paths.clear()
            paths.addAll(file.absolutePath.split("/", "\\").filter { it.isNotBlank() }
                .let { listOf(it.first() + "\\", *it.subList(1, it.size).toTypedArray()) })
        } else {
            showSnackBar("File is not a directory!")
            tempFile = File(paths.joinToString("/")).absolutePath
        }
        editOpen = false
    }
    if (!editOpen) {
        BoxWithConstraints(modifier) {
            val boxWithConstraintsScope = this
            val pathSize = 86.dp

            val numberPaths = max(
                0,
                boxWithConstraintsScope.maxWidth.div(pathSize).toInt()
            )
            Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    for (index in paths.take(numberPaths).indices) {
                        TooltipTarget(tip = paths.subList(0, index + 1).joinToString("/")) {
                            Row(
                                modifier = Modifier.clickable(
                                    interactionSource = MutableInteractionSource(),
                                    indication = null,
                                    onClick = {
                                        if (index != paths.lastIndex) {
                                            paths.removeRange(index + 1, paths.size)
                                        }
                                    }
                                ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    modifier = Modifier.padding(end = 2.dp).size(18.dp),
                                    painter = painterResource("folder-icon.svg"),
                                    contentDescription = ""
                                )
                                Text(
                                    modifier = Modifier.widthIn(max = 50.dp),
                                    text = paths[index],
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (index != paths.lastIndex) {
                                    Icon(
                                        modifier = Modifier.padding(horizontal = 4.dp).size(8.dp),
                                        painter = painterResource("arrow-right.svg"),
                                        contentDescription = ""
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
                MButton(
                    background = Color(0xFF5a5b5d),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    content {
                        Icon(
                            modifier = Modifier.size(18.dp),
                            imageVector = Icons.Default.Edit,
                            tint = Color.White,
                            contentDescription = ""
                        )
                    }
                    onClick {
                        editOpen = true
                    }
                }
            }
        }
    } else {
        MTextField(
            background = Color.Transparent,
            singleLine = true,
            modifier = modifier,
            value = tempFile,
            onValueChange = { tempFile = it },
            onEnter = editDone
        )
    }

    LaunchedEffect(paths.toList()) {
        tempFile = File(paths.joinToString("/")).absolutePath
    }
}