@file:Suppress("FunctionName")

package components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import helper.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mslinks.ShellLink
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ExcludeFileFilter
import net.lingala.zip4j.model.ZipParameters
import org.apache.commons.io.FileUtils
import java.awt.Desktop
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.text.SimpleDateFormat
import java.util.*
import kotlin.io.path.isDirectory

@Composable
fun FileDropdown(
    modifier: Modifier = Modifier,
    file: File,
    paths: SnapshotStateList<String>,
    refresh: MutableState<Boolean>,
    expanded: MutableState<Pair<Boolean, Offset?>>,
    showSnackBar: (String) -> Unit,
    showHiddenFiles: MutableState<Boolean>
) {
    val coroutineScope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showPermanentDeleteDialog by remember { mutableStateOf(false) }
    var showPropertiesDialog by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    DropdownMenu(
        offset = expanded.value.second?.run { DpOffset(with(density) { x.toDp() }, with(density) { y.toDp() }) }
            ?: DpOffset(0.dp, 0.dp),
        expanded = expanded.value.first,
        onDismissRequest = { expanded.value = false to null },
        modifier = modifier
    ) {
        DropdownMenuItem(onClick = {
            if (file.isDirectory) {
                paths.clear()
                paths.addAll(file.absolutePath.split("/", "\\").filter { it.isNotBlank() }
                    .let { listOf(it.first() + "\\", *it.subList(1, it.size).toTypedArray()) })
            } else {
                runCatching {
                    Desktop.getDesktop().open(file)
                }
            }
            expanded.value = false to null
        }) {
            Text("Open", fontSize = 14.sp)
        }

        if (file.isDirectory) {
            DropdownMenuItem(onClick = {
                runCatching {
                    Desktop.getDesktop().open(file)
                }
                expanded.value = false to null
            }) {
                Text("Open in Explorer", fontSize = 14.sp)
            }
            DropdownMenuItem(onClick = {
                expanded.value = false to null
                showSnackBar("Starting to zip folder")
                coroutineScope.launch {
                    withContext(Dispatchers.IO) {
                        runCatching {
                            val path = file.absolutePath
                            var i = 0
                            var zipper = File("$path.zip")
                            while (zipper.exists()) {
                                zipper = File("$path (${++i}).zip")
                            }
                            val zipFile = ZipFile(zipper)
                            val zipParameters = ZipParameters()
                            if (!showHiddenFiles.value) {
                                val excludeFileFilter = ExcludeFileFilter { it?.isHidden == true }
                                zipParameters.excludeFileFilter = excludeFileFilter
                            }
                            zipFile.addFolder(File(path), zipParameters)
                        }.onSuccess {
                            showSnackBar("Created zip file!")
                            refresh.value = true
                        }.onFailure {
                            showSnackBar("Could not create zip!")
                        }
                    }
                }
            }) {
                Text("Zip Folder", fontSize = 14.sp)
            }
        }

        DropdownMenuItem(onClick = {
            expanded.value = false to null
            showDeleteDialog = true
        }) {
            Text("Delete", fontSize = 14.sp)
        }

        DropdownMenuItem(onClick = {
            expanded.value = false to null
            showRenameDialog = true
        }) {
            Text("Rename", fontSize = 14.sp)
        }

        if (file.extension.lowercase() != ".lnk") {
            DropdownMenuItem(onClick = {
                @Suppress("DEPRECATION")
                ShellLink.createLink(
                    file.absolutePath,
                    File(file.parentFile, "${file.nameWithoutExtension}.lnk").absolutePath
                )
                refresh.value = true
            }) {
                Text("Create Shortcut", fontSize = 14.sp)
            }
        }



        DropdownMenuItem(onClick = {
            expanded.value = false to null
            showPropertiesDialog = true
        }) {
            Text("Properties", fontSize = 14.sp)
        }
    }
    if (showDeleteDialog) {
        NOPAlertDialog(
            backgroundColor = Color(0xFF1D1D1D),
            onDismissRequest = { showDeleteDialog = false },
            confirmButton = {},
            title = "Delete ${file.name}",
            text = {
                Box(
                    modifier = Modifier.padding(8.dp).fillMaxSize(),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Do you really want to delete ${file.name}",
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp
                        )
                        Text("Hit continue to confirm!", textAlign = TextAlign.Center, fontSize = 14.sp)
                    }
                    Row(
                        modifier = Modifier.padding(top = 8.dp).align(Alignment.BottomEnd),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MButton {
                            content {
                                Text("Cancel", color = Color.White, fontSize = 14.sp)
                            }
                            onClick {
                                showDeleteDialog = false
                            }
                        }
                        MButton {
                            content {
                                Text("Continue", color = Color.White, fontSize = 14.sp)
                            }
                            onClick {
                                showDeleteDialog = false
                                showSnackBar("Moving file to recycle bin...")
                                coroutineScope.launch {
                                    runCatching {
                                        if (!file.exists()) {
                                            showSnackBar("File does not exist!")
                                            refresh.value = true
                                            return@runCatching
                                        }
                                        if (Desktop.getDesktop().moveToTrash(file)) {
                                            showSnackBar("Moved file to recycle bin!")
                                            refresh.value = true
                                        } else {
                                            showPermanentDeleteDialog = true
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

            })
    }
    if (showRenameDialog) {
        NOPAlertDialog(
            backgroundColor = Color(0xFF1D1D1D),
            onDismissRequest = { showRenameDialog = false },
            confirmButton = {},
            title = "Rename ${file.name}",
            text = {
                var newName by remember { mutableStateOf("") }
                Box(modifier = Modifier.padding(8.dp).fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            "Enter a new name for the file:",
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp
                        )
                        MTextField(
                            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 35.dp),
                            value = newName,
                            singleLine = true,
                            textColor = Color.White,
                            labelColor = Color.LightGray,
                            onValueChange = {
                                newName =
                                    it.filter { c -> c.isLetterOrDigit() || c.isWhitespace() || c == '(' || c == ')' || c == '[' || c == ']' || c == '_' || c == '-' || c == ',' || c == '.' }
                            },
                            label = file.name
                        )
                    }

                    Row(
                        modifier = Modifier.padding(top = 8.dp).align(Alignment.BottomEnd),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MButton {
                            content {
                                Text("Cancel", color = Color.White, fontSize = 14.sp)
                            }
                            onClick {
                                showRenameDialog = false
                            }
                        }
                        MButton {
                            content {
                                Text("Rename", color = Color.White, fontSize = 14.sp)
                            }
                            onClick {
                                val newFile = File(file.parentFile, newName)
                                if (newFile.exists()) {
                                    showSnackBar("File exists!")
                                } else {
                                    file.renameTo(newFile)
                                    refresh.value = true
                                    showRenameDialog = false
                                    showSnackBar("File renamed to $newName")
                                }
                            }
                        }
                    }
                }
            }
        )
    }
    if (showPermanentDeleteDialog) {
        NOPAlertDialog(
            backgroundColor = Color(0xFF1D1D1D),
            onDismissRequest = { showPermanentDeleteDialog = false },
            confirmButton = {},
            title = "Permanently delete ${file.name}",
            text = {
                Box(
                    modifier = Modifier.padding(8.dp).fillMaxSize(),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Could not move ${file.name} to recycle bin. Delete it permanently?",
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp
                        )
                        Text("Hit continue to confirm!", textAlign = TextAlign.Center, fontSize = 14.sp)
                    }
                    Row(
                        modifier = Modifier.padding(top = 8.dp).align(Alignment.BottomEnd),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MButton {
                            content {
                                Text("Cancel", color = Color.White, fontSize = 14.sp)
                            }
                            onClick {
                                showPermanentDeleteDialog = false
                            }
                        }
                        MButton {
                            content {
                                Text("Continue", color = Color.White, fontSize = 14.sp)
                            }
                            onClick {
                                showPermanentDeleteDialog = false
                                showSnackBar("Permanently deleting file")
                                coroutineScope.launch {
                                    runCatching {
                                        if (!file.exists()) {
                                            showSnackBar("File does not exist!")
                                            refresh.value = true
                                        }
                                        if (file.isDirectory) {
                                            FileUtils.deleteDirectory(file)
                                        } else {
                                            FileUtils.forceDelete(file)
                                        }
                                    }.onSuccess {
                                        showSnackBar("Deleted file permanently!")
                                    }.onFailure {
                                        showSnackBar("Could not delete file!")
                                    }
                                }
                            }
                        }
                    }
                }

            })
    }
    if (showPropertiesDialog) {
        var size by remember { mutableStateOf(null as String?) }
        var filesAndFolder by remember { mutableStateOf(null as FilesAndFolder?) }
        NOPAlertDialog(
            backgroundColor = Color(0xFF1D1D1D),
            onDismissRequest = { showPropertiesDialog = false },
            confirmButton = {},
            title = "${file.name} Properties",
            text = {
                Box(modifier = Modifier.fillMaxSize().padding(end = 10.dp)) {
                    val fullState = rememberScrollState(0)
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(fullState).padding(8.dp)
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val scrollState = rememberScrollState(0)
                            Image(
                                painter = painterResource(FileIconGeneratorListColumn.getIcon(file)),
                                contentDescription = file.name,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                SelectionContainer(
                                    modifier = Modifier.horizontalScroll(scrollState).fillMaxWidth().clip(
                                        RoundedCornerShape(4.dp)
                                    ).background(Color.DarkGray).padding(4.dp)
                                ) {
                                    Text(text = file.name, fontSize = 14.sp)
                                }
                                HorizontalScrollbar(
                                    adapter = rememberScrollbarAdapter(scrollState),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Divider()

                        Column(
                            modifier = Modifier.padding(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text("Type: ${file.mimeType}", fontSize = 13.sp)
                            SelectionContainer {
                                Text("Location: ${file.absolutePath}", fontSize = 13.sp)
                            }
                            Text("Size: ${size ?: "Loading Size..."}", fontSize = 13.sp)
                            if (file.isDirectory && filesAndFolder != null) {
                                val (files, folders) = filesAndFolder!!
                                Text("Contains: $files File(s), $folders Folder(s)", fontSize = 13.sp)
                            }
                        }

                        Divider()
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            val created = file.createdDate
                            if (created != null) {
                                Text(
                                    "Created: ${SimpleDateFormat("dd-MM-yyyy HH:mm").format(Date(created))}",
                                    fontSize = 13.sp
                                )
                            }
                            Text(
                                "Date Modified: ${SimpleDateFormat("dd-MM-yyyy HH:mm").format(Date(file.lastModified()))}",
                                fontSize = 13.sp
                            )
                        }

                        LaunchedEffect(showPropertiesDialog) {
                            withContext(Dispatchers.IO) {
                                runCatching {
                                    size = file.sizeInHumanReadableFormat
                                    filesAndFolder = file.filesAndFoldersNum
                                }
                            }
                        }
                    }
                    VerticalScrollbar(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        adapter = rememberScrollbarAdapter(fullState)
                    )
                }
            }
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun DropdownMenuItem(onClick: () -> Unit, content: @Composable BoxScope.() -> Unit) {
    var isHovered by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier.fillMaxWidth()
            .background(color = if (isHovered) Color.DarkGray else Color.Transparent)
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
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        content()
    }
}

private val File.createdDate: Long?
    get() = run {
        runCatching {
            val attr = Files.readAttributes(toPath(), BasicFileAttributes::class.java)
            attr.creationTime().toMillis()
        }.getOrNull()
    }

private val File.filesAndFoldersNum: FilesAndFolder?
    get() = runCatching {
        val files = Files.list(toPath())
        var filesNum = 0
        var foldersNum = 0

        files.forEach { path ->
            if (path.isDirectory()) {
                foldersNum++
            } else {
                filesNum++
            }
        }
        return FilesAndFolder(files = filesNum, folders = foldersNum)
    }.getOrNull()

data class FilesAndFolder(val files: Int, val folders: Int)

private val File.mimeType: String
    get() = run {
        return if (isDirectory) {
            "Folder"
        } else {
            if (extension.lowercase() == "lnk") {
                return "ShortCut"
            }
            runCatching {
                val path: Path = toPath() ?: return "File"
                Files.probeContentType(path)
            }.getOrDefault("File")
        }
    }

private val File.sizeInHumanReadableFormat: String get() = humanReadableByteCountSI(size)

private val File.size: Long
    get() = run {
        return if (!isDirectory) {
            length()
        } else {
            folderSize(toPath())
        }
    }

private fun folderSize(path: Path): Long {
    return Files.walk(path)
        .filter { p: Path -> p.toFile().let { it.exists() && it.isFile } }
        .mapToLong { p: Path -> p.toFile().length() }
        .sum()
}