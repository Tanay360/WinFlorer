@file:Suppress("FunctionName")

package components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import db.ArrayDB
import helper.*
import kotlinx.coroutines.*
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ExcludeFileFilter
import net.lingala.zip4j.model.ZipParameters
import java.io.File
import java.util.*

@Composable
fun HandlerBar(
    paths: SnapshotStateList<String>,
    removedPaths: SnapshotStateList<String>,
    showHiddenFiles: MutableState<Boolean>,
    currentSortBy: MutableState<SortBy>,
    db: ArrayDB,
    showSnackBar: (String) -> Unit,
    refresh: MutableState<Boolean>,
    searchResults: SnapshotStateList<String>,
    searched: MutableState<Boolean>,
    searchValue: MutableState<String>,
    searchDone: MutableState<Boolean>,
    job: MutableState<Job>,
    displayOptions: MutableState<DisplayOptions>,
    selectionMode: MutableState<SelectionMode>,
    selectedFiles: SnapshotStateList<File>
) {
    Box(
        modifier = Modifier.fillMaxWidth().height(40.dp).padding(horizontal = 16.dp).padding(bottom = 4.dp)
    ) {
        Row(
            modifier = Modifier.align(Alignment.CenterStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LeftButton(
                enabled = paths.size > 1
            ) {
                val r = paths.removeLast()
                val ll = mutableListOf(r).also { ll -> ll.addAll(removedPaths.toList()) }
                removedPaths.clear()
                removedPaths.addAll(ll)
            }
            Spacer(Modifier.size(2.dp))
            RightButton(
                enabled = removedPaths.size > 0
            ) {
                paths.add(removedPaths.removeFirst())
            }

            if (selectionMode.value != SelectionMode.NONE) {
                Spacer(modifier = Modifier.width(50.dp))
                if (selectionMode.value == SelectionMode.SELECT) {
                    CopyButton(selectedFiles, selectionMode)
                    Spacer(modifier = Modifier.width(5.dp))
                    CutButton(selectedFiles, selectionMode)
                } else {
                    PasteButton(selectedFiles, selectionMode, paths)
                }
            } else {
                Spacer(modifier = Modifier.width(50.dp))
                DisplaySelector(displayOptions = displayOptions)

                Spacer(modifier = Modifier.width(5.dp))
                SortByButton(currentSortBy = currentSortBy)

                if (paths.size > 1) {
                    Spacer(modifier = Modifier.width(5.dp))
                    ZipButton(paths, showSnackBar, showHiddenFiles)
                }

                Spacer(modifier = Modifier.width(5.dp))
                LabelButton(paths, db = db, showSnackBar = showSnackBar)
            }

            Spacer(modifier = Modifier.width(5.dp))
            ShowHideButton(showHiddenFiles)

            Spacer(modifier = Modifier.width(5.dp))
            RefreshButton {
                refresh.value = true
            }

            if (selectionMode.value == SelectionMode.NONE) {
                Spacer(modifier = Modifier.width(5.dp))
                SelectButton(selectionMode)

                Spacer(modifier = Modifier.width(5.dp))
                NewFileButton(File(paths.joinToString("/")).absolutePath, showSnackBar, refresh)

                Spacer(modifier = Modifier.width(5.dp))
                TerminalButton(paths)
            }

        }
        if (selectionMode.value == SelectionMode.NONE) {
            SearchBar(
                modifier = Modifier.width(250.dp).align(Alignment.CenterEnd),
                paths = paths,
                searched = searched,
                searchResults = searchResults,
                showHiddenFiles = showHiddenFiles,
                searchValue = searchValue,
                searchDone = searchDone,
                job = job
            )
        }
    }
    Divider()
}

@Composable
private fun SelectButton(selectionMode: MutableState<SelectionMode>) {
    TooltipTarget("Select Files") {
        Box(
            modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFF5a5b5d)).clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = true),
                onClick = {
                    selectionMode.value = SelectionMode.SELECT
                }
            ).padding(vertical = 5.dp, horizontal = 7.dp)
        ) {
            Icon(
                modifier = Modifier.size(20.dp),
                tint = Color.White,
                painter = painterResource("multiselect.svg"),
                contentDescription = "Select"
            )
        }
    }
}

@Composable
private fun CutButton(selectedFiles: SnapshotStateList<File>, selectionMode: MutableState<SelectionMode>) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(if (selectedFiles.size > 0) 0xFF5a5b5d else 0xFF48484a)).clickable(
            interactionSource = remember { MutableInteractionSource() },
            enabled = selectedFiles.size > 0,
            indication = rememberRipple(bounded = true),
            onClick = {
                selectionMode.value = SelectionMode.CUT
            }
        ).padding(vertical = 5.dp, horizontal = 7.dp)
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            tint = Color.White,
            painter = painterResource("scissors-solid.svg"),
            contentDescription = "Cut"
        )
    }
}

@Composable
private fun CopyButton(selectedFiles: SnapshotStateList<File>, selectionMode: MutableState<SelectionMode>) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(if (selectedFiles.size > 0) 0xFF5a5b5d else 0xFF48484a)).clickable(
            interactionSource = remember { MutableInteractionSource() },
            enabled = selectedFiles.size > 0,
            indication = rememberRipple(bounded = true),
            onClick = {
                selectionMode.value = SelectionMode.COPY
            }
        ).padding(vertical = 5.dp, horizontal = 7.dp)
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            tint = Color.White,
            painter = painterResource("copy-solid.svg"),
            contentDescription = "Copy"
        )
    }
}

@Composable
private fun PasteButton(selectedFiles: SnapshotStateList<File>, selectionMode: MutableState<SelectionMode>, paths: SnapshotStateList<String>) {
    val coroutineScope = rememberCoroutineScope()
    Box(
        modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(if (selectedFiles.size > 0) 0xFF5a5b5d else 0xFF48484a)).clickable(
            interactionSource = remember { MutableInteractionSource() },
            enabled = selectedFiles.size > 0,
            indication = rememberRipple(bounded = true),
            onClick = {
                coroutineScope.launch {
                    runCatching {
                        val targetFolder = File(paths.joinToString("/")) // Target folder
                        selectedFiles.forEach { file ->
                            if (selectionMode.value == SelectionMode.COPY) {
                                var targetFile = File(targetFolder,"${file.nameWithoutExtension} - copy.${file.extension}")
                                var i = 0
                                while (targetFile.exists()) {
                                    targetFile = File(targetFolder, "${file.nameWithoutExtension} - copy(${++i}).${file.extension}")
                                }
                                file.copyTo(targetFile)
                            } else if (selectionMode.value == SelectionMode.CUT) {
                                if (file.parentFile.absolutePath != targetFolder.absolutePath) {
                                    var targetFile = File(targetFolder, file.name)
                                    var i = 0
                                    while (targetFile.exists()) {
                                        targetFile = File(targetFolder,"${file.nameWithoutExtension} (${++i}).${file.extension}")
                                    }
                                    file.renameTo(targetFile)
                                }
                            }
                        }
                    }.getOrNull().let {
                        selectedFiles.clear()
                        selectionMode.value = SelectionMode.NONE
                    }
                }
            }
        ).padding(vertical = 5.dp, horizontal = 7.dp)
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            tint = Color.White,
            painter = painterResource("clipboard-solid.svg"),
            contentDescription = "Cut"
        )
    }
}


@Composable
private fun RefreshButton(onClick: () -> Unit) {
    TooltipTarget(tip = "Refresh") {
        Box(
            modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFF5a5b5d)).clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = true),
                onClick = onClick
            ).padding(vertical = 5.dp, horizontal = 7.dp)
        ) {
            Icon(
                modifier = Modifier.size(20.dp),
                painter = painterResource("refresh-icon.svg"),
                contentDescription = "Refresh"
            )
        }
    }
}

@Composable
private fun ShowHideButton(showHiddenFiles: MutableState<Boolean>) {
    TooltipTarget(tip = if (showHiddenFiles.value) "Don't show Hidden Items" else "Show Hidden Items") {
        Box(
            modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFF5a5b5d)).clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = true),
                onClick = {
                    showHiddenFiles.value = !showHiddenFiles.value
                }
            ).padding(vertical = 5.dp, horizontal = 7.dp)
        ) {
            Image(
                modifier = Modifier.size(20.dp),
                painter = painterResource(if (showHiddenFiles.value) "show-icon.svg" else "hide-icon.svg"),
                contentDescription = ""
            )
        }
    }
}

@Composable
private fun MoreOptionsButton() {
    TooltipTarget(tip = "More") {
        Box(
            modifier = Modifier.clip(RoundedCornerShape(4.dp)).clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = true),
                onClick = {

                }
            ).padding(vertical = 5.dp, horizontal = 7.dp)
        ) {
            Icon(
                modifier = Modifier.size(20.dp),
                painter = painterResource("more_options.svg"),
                contentDescription = "More"
            )
        }
    }
}

val sortOptions = listOf(
    "Name Ascending" to SortBy.NAME_ASCENDING,
    "Name Descending" to SortBy.NAME_DESCENDING,
    "Date Ascending" to SortBy.DATE_ASCENDING,
    "Date Descending" to SortBy.DATE_DESCENDING,
    "Size Ascending" to SortBy.SIZE_ASCENDING,
    "Size Descending" to SortBy.SIZE_DESCENDING,
)

@Composable
private fun SortByButton(currentSortBy: MutableState<SortBy>) {
    val (sortByDropShown, setSortByDropDownShown) = remember { mutableStateOf(false) }

    TooltipTarget(tip = "Sort By") {
        Box(
            modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFF5a5b5d)).clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = true),
                onClick = {
                    setSortByDropDownShown(true)
                }
            ).padding(vertical = 5.dp, horizontal = 7.dp)
        ) {
            Icon(
                modifier = Modifier.size(20.dp),
                painter = painterResource("sortby.svg"),
                contentDescription = "Sort By"
            )
            DropdownMenu(
                expanded = sortByDropShown,
                onDismissRequest = { setSortByDropDownShown(false) }
            ) {
                Column {
                    for ((option, value) in sortOptions) {
                        SelectableItem(
                            text = option,
                            isSelected = value == currentSortBy.value,
                            runOnClick = {
                                setTimeOut(200) {
                                    setSortByDropDownShown(false)
                                }
                            }
                        ) {
                            currentSortBy.value = value
                        }
                    }
                }
            }

        }
    }
}


@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun ZipButton(
    paths: SnapshotStateList<String>,
    showSnackBar: (String) -> Unit,
    showHiddenFiles: MutableState<Boolean>
) {
    val coroutineScope = rememberCoroutineScope()
    var showAlert by remember { mutableStateOf(false) }
    TooltipTarget(tip = "Zip folder") {
        Box(
            modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFF5a5b5d)).clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = true),
                onClick = {
                    showAlert = true
                }
            ).padding(vertical = 3.dp, horizontal = 5.dp)
        ) {
            Icon(
                modifier = Modifier.size(30.dp),
                painter = painterResource("zip-dwn-icon.svg"),
                contentDescription = "Zip Folder",
                tint = Color.Unspecified
            )
        }
    }
    if (showAlert) {
        NOPAlertDialog(
            backgroundColor = Color(0xFF1D1D1D),
            onDismissRequest = { showAlert = false },
            confirmButton = {},
            title = "Create Archive",
            text = {
                Box(
                    modifier = Modifier.padding(8.dp).fillMaxSize(),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Starting to create zip of folder...", textAlign = TextAlign.Center, fontSize = 14.sp)
                        Text("Hit continue to confirm!", textAlign = TextAlign.Center, fontSize = 14.sp)
                    }
                    Row(
                        modifier = Modifier.padding(top = 8.dp).align(Alignment.BottomEnd),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MButton {
                            content {
                                Text("Cancel", color = Color.White, fontSize = 12.sp)
                            }
                            onClick {
                                showAlert = false
                            }
                        }
                        MButton {
                            content {
                                Text("Continue", color = Color.White, fontSize = 12.sp)
                            }
                            onClick {
                                showAlert = false
                                showSnackBar("Starting to zip folder")
                                coroutineScope.launch {
                                    runCatching {
                                        val path = paths.joinToString("/")
                                        val name = paths.last()
                                        var i = 0
                                        var file = File("$path/${name}.zip")
                                        while (file.exists()) {
                                            file = File("$path/${name} (${++i}).zip")
                                        }
                                        val zipFile = ZipFile(file)
                                        val zipParameters = ZipParameters()
                                        if (!showHiddenFiles.value) {
                                            val excludeFileFilter = ExcludeFileFilter { it?.isHidden == true }
                                            zipParameters.excludeFileFilter = excludeFileFilter
                                        }
                                        zipFile.addFolder(File(path), zipParameters)
                                    }.onSuccess {
                                        showSnackBar("Created zip file!")
                                    }.onFailure {
                                        showSnackBar("Could not create zip!")
                                    }
                                }
                            }
                        }
                    }
                }

            }
        )
    }
}

@Composable
private fun LabelButton(
    paths: SnapshotStateList<String>,
    db: ArrayDB,
    showSnackBar: (String) -> Unit
) {
    val (labelDropShown, setLabelDropShown) = remember { mutableStateOf(false) }
    TooltipTarget(tip = "Label") {
        Column {
            Box(
                modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFF5a5b5d)).clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(bounded = true),
                    onClick = {
                        setLabelDropShown(true)
                    }
                ).padding(vertical = 5.dp, horizontal = 7.dp)
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource("tag-solid.svg"),
                    contentDescription = "Label"
                )
            }
            DropdownMenu(
                expanded = labelDropShown,
                onDismissRequest = { setLabelDropShown(false) }
            ) {
                Row(
                    modifier = Modifier.padding(4.dp)
                ) {
                    LabelColor(
                        text = "Red",
                        tint = Color.Red,
                        paths = paths,
                        db = db,
                        showSnackBar = showSnackBar,
                        setLabelDropShown = setLabelDropShown
                    )
                    LabelColor(
                        text = "Orange",
                        tint = Color(0xFFFFA500),
                        paths = paths,
                        db = db,
                        showSnackBar = showSnackBar,
                        setLabelDropShown = setLabelDropShown
                    )
                    LabelColor(
                        text = "Yellow",
                        tint = Color.Yellow,
                        paths = paths,
                        db = db,
                        showSnackBar = showSnackBar,
                        setLabelDropShown = setLabelDropShown
                    )
                    LabelColor(
                        text = "Green",
                        tint = Color.Green,
                        paths = paths,
                        db = db,
                        showSnackBar = showSnackBar,
                        setLabelDropShown = setLabelDropShown
                    )
                    LabelColor(
                        text = "Blue",
                        tint = Color.Blue,
                        paths = paths,
                        db = db,
                        showSnackBar = showSnackBar,
                        setLabelDropShown = setLabelDropShown
                    )
                    LabelColor(
                        text = "Purple",
                        tint = Color(0xFFA020F0),
                        paths = paths,
                        db = db,
                        showSnackBar = showSnackBar,
                        setLabelDropShown = setLabelDropShown
                    )
                    LabelColor(
                        text = "Gray",
                        tint = Color.Gray,
                        paths = paths,
                        db = db,
                        showSnackBar = showSnackBar,
                        setLabelDropShown = setLabelDropShown
                    )
                }
            }

        }
    }
}

@Composable
private fun LabelColor(
    text: String,
    tint: Color,
    modifier: Modifier = Modifier,
    paths: SnapshotStateList<String>,
    db: ArrayDB,
    showSnackBar: (String) -> Unit,
    setLabelDropShown: (Boolean) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    Icon(
        modifier = modifier.size(20.dp).padding(end = 3.dp).pointerInput(Unit) {
            detectTapGestures {
                coroutineScope.launch {
                    if (db.insertItem(text, paths.joinToString("/"))) {
                        showSnackBar("${paths.last()} added to tag $text")
                        setLabelDropShown(false)
                    }
                }
            }
        },
        painter = painterResource("circle.svg"),
        tint = tint,
        contentDescription = ""
    )
}

@Composable
private fun SearchBar(
    modifier: Modifier = Modifier,
    paths: SnapshotStateList<String>,
    searchResults: SnapshotStateList<String>,
    searched: MutableState<Boolean>,
    showHiddenFiles: MutableState<Boolean>,
    searchValue: MutableState<String>,
    searchDone: MutableState<Boolean>,
    job: MutableState<Job>
) {
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    MTextField(
        modifier = modifier.focusRequester(focusRequester),
        value = searchValue.value,
        onValueChange = { searchValue.value = it },
        singleLine = true,
        label = "Search in ${paths.last()}",
        prefixIcon = {
            Icon(
                painter = painterResource("search.svg"),
                contentDescription = "",
                modifier = Modifier.size(20.dp).padding(end = 4.dp)
            )
        },
        onEnter = {
            searched.value = true
            val file = File(paths.joinToString("/"))
            searchResults.clear()
            job.value = coroutineScope.launch(Dispatchers.IO) {
                getFiles(this, searchResults, file, showHiddenFiles, searchValue)
            }.also {
                it.invokeOnCompletion {
                    searchDone.value = true
                }
            }
        }
    )

    LaunchedEffect(Unit) {
        focusManager.clearFocus(true)
    }
}

private fun getFiles(
    coroutineScope: CoroutineScope,
    searchResults: SnapshotStateList<String>,
    file: File,
    showHiddenFiles: MutableState<Boolean>,
    searchValue: MutableState<String>
) {
    val stack = mutableListOf(file)
    while (stack.isNotEmpty() && coroutineScope.isActive) {
        val currentFile = stack.removeLast()
        currentFile.listFiles()?.forEach {
            if (!coroutineScope.isActive) {
                return
            }
            if (it != null && it.exists() && (showHiddenFiles.value || !it.isHidden)) {
                if (it.isDirectory) {
                    if (it.name.contains(searchValue.value, ignoreCase = true)) {
                        searchResults.add(it.absolutePath)
                    }
                    stack.add(it)
                } else if (it.isFile && it.name.contains(searchValue.value, ignoreCase = true)) {
                    searchResults.add(it.absolutePath)
                }
            }
        }
    }
}

@Composable
private fun TerminalButton(paths: SnapshotStateList<String>) {
    val (newTerminalDropdown, setNewTerminalDropdown) = remember { mutableStateOf(false) }
    TooltipTarget(tip = "Open Terminal") {
        Column {
            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFF333333)).clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = true),
                onClick = {
                    setNewTerminalDropdown(true)
                }
            ).padding(vertical = 5.dp, horizontal = 7.dp)) {
                Image(
                    painter = painterResource("terminal.svg"),
                    contentDescription = "Open Terminal",
                    modifier = Modifier.size(25.dp)
                )
            }

            DropdownMenu(
                expanded = newTerminalDropdown,
                onDismissRequest = { setNewTerminalDropdown(false) }
            ) {
                DropdownMenuItem(onClick = {
                    setNewTerminalDropdown(false)
                    val p = Runtime.getRuntime().exec("cmd /c start cmd.exe /k cd ${paths.joinToString("/")}")
                    p.waitFor()
                }) {
                    Text("cmd", fontSize = 13.sp)
                }

                DropdownMenuItem(onClick = {
                    setNewTerminalDropdown(false)
                    val p = Runtime.getRuntime()
                        .exec("cmd /c start powershell.exe -noexit -command \"cd ${paths.joinToString("/")}\"")
                    p.waitFor()
                }) {
                    Text("Powershell", fontSize = 13.sp)
                }

            }
        }
    }
}

@Composable
private fun NewFileButton(currentPath: String, showSnackBar: (String) -> Unit, refresh: MutableState<Boolean>) {
    val (newFileDropdownShown, setNewFileDropdownShown) = remember { mutableStateOf(false) }
    var showNewFileDialog by remember { mutableStateOf(NewFiler.NONE) }
    TooltipTarget(tip = "New File/Folder") {
        Column {
            Box(
                modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFF5a5b5d)).clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(bounded = true),
                    onClick = {
                        setNewFileDropdownShown(true)
                    }
                ).padding(vertical = 5.dp, horizontal = 7.dp)
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    imageVector = Icons.Default.Add,
                    contentDescription = "New File/Folder"
                )
            }
            DropdownMenu(
                expanded = newFileDropdownShown,
                onDismissRequest = { setNewFileDropdownShown(false) }
            ) {
                DropdownMenuItem(onClick = {
                    showNewFileDialog = NewFiler.FILE
                    setNewFileDropdownShown(false)
                }) {
                    Text("New File", fontSize = 13.sp)
                }

                DropdownMenuItem(onClick = {
                    showNewFileDialog = NewFiler.FOLDER
                    setNewFileDropdownShown(false)
                }) {
                    Text("New Folder", fontSize = 13.sp)
                }

            }
        }
    }
    if (showNewFileDialog != NewFiler.NONE) {
        val fileOrFolder = if (showNewFileDialog == NewFiler.FILE) "File" else "Folder"
        NOPAlertDialog(
            onDismissRequest = { showNewFileDialog = NewFiler.NONE },
            confirmButton = {},
            backgroundColor = Color(0xFF1D1D1D),
            title = "New $fileOrFolder in $currentPath",
            text = {
                var fileName by remember { mutableStateOf("") }
                Box(modifier = Modifier.padding(8.dp).fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            "Enter $fileOrFolder name:",
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp
                        )
                        MTextField(
                            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 35.dp),
                            value = fileName,
                            textColor = Color.White,
                            singleLine = true,
                            labelColor = Color.LightGray,
                            onValueChange = {
                                fileName =
                                    it.filter { c -> c.isLetterOrDigit() || c.isWhitespace() || c == '(' || c == ')' || c == '[' || c == ']' || c == '_' || c == '-' || c == ',' || c == '.' }
                            },
                            label = "$fileOrFolder name"
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
                                showNewFileDialog = NewFiler.NONE
                            }
                        }
                        MButton {
                            content {
                                Text("Create $fileOrFolder", color = Color.White, fontSize = 14.sp)
                            }
                            onClick {
                                val newFile = File(currentPath, fileName)
                                if (newFile.exists()) {
                                    showSnackBar("File already exists!")
                                } else {
                                    if (showNewFileDialog == NewFiler.FILE) {
                                        newFile.createNewFile()
                                    } else {
                                        newFile.mkdir()
                                    }
                                    showSnackBar("Created $fileOrFolder")
                                    refresh.value = true
                                    showNewFileDialog = NewFiler.NONE
                                }
                            }
                        }
                    }
                }
            }
        )
    }
}

private enum class NewFiler {
    NONE,
    FILE,
    FOLDER
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


@Composable
private fun DisplaySelector(displayOptions: MutableState<DisplayOptions>) {
    val items = listOf(
        "icons.svg" to DisplayOptions.GRID,
        "list.svg" to DisplayOptions.LIST,
        "column.svg" to DisplayOptions.COLUMN
    )
    Row(
        modifier = Modifier.clip(RoundedCornerShape(4.dp)).height(30.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for ((icon, title) in items) {
            TooltipTarget(tip = "Display as ${title.value}") {
                Box(
                    modifier = Modifier.background(
                        if (title == displayOptions.value) Color(0xFF323334) else Color(
                            0xFF5a5b5d
                        )
                    )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = rememberRipple(bounded = true),
                            onClick = {
                                displayOptions.value = title
                            }
                        ).padding(vertical = 6.dp, horizontal = 8.dp)
                ) {
                    Icon(
                        modifier = Modifier.size(16.dp),
                        painter = painterResource(icon),
                        contentDescription = title.value
                    )
                }
            }
            Divider(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
            )
        }
    }
}

@Composable
private fun LeftButton(enabled: Boolean, onClick: () -> Unit) {
    ArrowButton(enabled = enabled, icon = "left.svg", onClick = onClick)
}

@Composable
private fun RightButton(enabled: Boolean, onClick: () -> Unit) {
    ArrowButton(enabled = enabled, icon = "right.svg", onClick = onClick)
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ArrowButton(enabled: Boolean, icon: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(if (enabled) 0xFF5a5b5d else 0xFF48484a))
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = true),
                onClick = onClick
            ).padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = icon,
            modifier = Modifier.height(20.dp).width(10.dp),
            tint = Color.LightGray
        )
    }
}