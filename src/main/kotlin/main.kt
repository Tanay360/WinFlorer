import androidx.compose.foundation.Image
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.formdev.flatlaf.FlatDarkLaf
import components.*
import db.ArrayDB
import helper.*
import kotlinx.coroutines.*
import java.awt.Color
import java.awt.Dimension
import java.io.File
import javax.swing.JFrame
import javax.swing.SwingUtilities
import javax.swing.WindowConstants

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    SwingUtilities.invokeLater {
        FlatDarkLaf.setup()
        JFrame.setDefaultLookAndFeelDecorated(true)

        val window = JFrame()
        window.rootPane.putClientProperty("JRootPane.titleBarBackground", Color(18, 18, 18))
        window.rootPane.putClientProperty("JRootPane.titleBarForeground", Color.white)
        window.apply {
            minimumSize = Dimension(1250, 500)
            val composePanel = ComposePanel()
            defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
            iconImage = FinderIcons.main
            title = "WinFlorer"
            contentPane.add(composePanel)
            composePanel.setContent {
                val fFocs = FocusRequester()
                val db = ArrayDB("labels")
                val paths = remember { mutableStateListOf<String>() }
                val showHiddenFiles = remember { mutableStateOf(false) }
                val currentSortBy = remember { mutableStateOf(SortBy.NAME_ASCENDING) }
                val removedPaths = remember { mutableStateListOf<String>() }
                val refresh = remember { mutableStateOf(false) }
                val searched = remember { mutableStateOf(false) }
                val searchValue = remember { mutableStateOf("") }
                val searchResults = remember { mutableStateListOf<String>() }
                val searchDone = remember { mutableStateOf(false) }
                val selectionMode = remember { mutableStateOf(SelectionMode.NONE) }
                val selectedFiles = remember { mutableStateListOf<File>() }
                val job = remember { mutableStateOf<Job>(Job()) }
                val displayOptions = remember { mutableStateOf(DisplayOptions.COLUMN) }
                val (tag, setTag) = remember { mutableStateOf(null as String?) }
                MaterialTheme(colors = darkColors()) {
                    Surface(
                        modifier = Modifier.fillMaxSize().focusGetter(fFocs).arrowListener(paths, removedPaths, refresh)
                    ) {
                        val scaffoldState = rememberScaffoldState()
                        val coroutineScope = rememberCoroutineScope()
                        val keyboardController = LocalSoftwareKeyboardController.current
                        Scaffold(
                            scaffoldState = scaffoldState,
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                keyboardController?.hide()
                            }
                        ) {

                            val showSnackBar: (String) -> Unit = { str ->
                                coroutineScope.launch {
                                    scaffoldState.snackbarHostState.showSnackbar(message = str)
                                }
                            }
                            Column(modifier = Modifier.fillMaxSize()) {
                                if (paths.isNotEmpty()) {
                                    HandlerBar(
                                        paths = paths,
                                        removedPaths = removedPaths,
                                        showHiddenFiles = showHiddenFiles,
                                        currentSortBy = currentSortBy,
                                        db = db,
                                        showSnackBar = showSnackBar,
                                        refresh = refresh,
                                        searched = searched,
                                        searchResults = searchResults,
                                        searchValue = searchValue,
                                        searchDone = searchDone,
                                        job = job,
                                        displayOptions = displayOptions,
                                        selectionMode = selectionMode,
                                        selectedFiles = selectedFiles
                                    )
                                }
                                if (selectionMode.value != SelectionMode.NONE) {
                                    FullScreenDialog(title = "Select Files (${selectedFiles.size})", onDismissRequest = {
                                        selectionMode.value = SelectionMode.NONE
                                        selectedFiles.clear()
                                    }) {
                                        FileSelector(
                                            paths = paths,
                                            showHiddenFiles = showHiddenFiles,
                                            removedPaths = removedPaths,
                                            currentSortBy = currentSortBy,
                                            refresh = refresh,
                                            showSnackBar = showSnackBar,
                                            selectedFiles = selectedFiles,
                                            selectionMode = selectionMode,
                                        )
                                    }
                                } else {
                                    Row(modifier = Modifier.weight(1f)) {
                                        Favorites(paths = paths, removedPaths = removedPaths, setTag = setTag)
                                        if (paths.isNotEmpty()) {
                                            Column(
                                                modifier = Modifier.fillMaxSize()
                                            ) {
                                                when (displayOptions.value) {
                                                    DisplayOptions.GRID -> {
                                                        FolderLargeViewer(
                                                            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.93f),
                                                            paths = paths,
                                                            removedPaths = removedPaths,
                                                            showHiddenFiles = showHiddenFiles,
                                                            currentSortBy = currentSortBy,
                                                            refresh = refresh,
                                                            showSnackBar = showSnackBar
                                                        )
                                                    }
                                                    DisplayOptions.LIST -> {
                                                        FolderListViewer(
                                                            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.93f),
                                                            paths = paths,
                                                            removedPaths = removedPaths,
                                                            showHiddenFiles = showHiddenFiles,
                                                            currentSortBy = currentSortBy,
                                                            refresh = refresh,
                                                            showSnackBar = showSnackBar
                                                        )
                                                    }
                                                    DisplayOptions.COLUMN -> {
                                                        FolderColumnViewer(
                                                            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.93f),
                                                            paths = paths,
                                                            removedPaths = removedPaths,
                                                            showHiddenFiles = showHiddenFiles,
                                                            currentSortBy = currentSortBy,
                                                            refresh = refresh,
                                                            showSnackBar = showSnackBar
                                                        )
                                                    }
                                                }
                                                Divider()
                                                PathsBreadCrumbs(
                                                    modifier = Modifier.weight(1f).fillMaxWidth()
                                                        .padding(horizontal = 9.dp),
                                                    paths = paths,
                                                    showSnackBar = showSnackBar
                                                )
                                            }
                                        } else {
                                            if (tag != null) {
                                                TagsViewer(
                                                    modifier = Modifier.fillMaxSize(),
                                                    tag = tag,
                                                    paths = paths,
                                                    db = db,
                                                    showSnackBar = showSnackBar,
                                                    showHiddenFiles = showHiddenFiles
                                                )
                                            } else {
                                                MainFolders(modifier = Modifier.fillMaxSize(), paths = paths)
                                            }
                                        }
                                    }
                                }
                            }
                            if (searched.value) {
                                var results by remember { mutableStateOf<List<String>>(listOf()) }
                                FullScreenDialog(
                                    title = "Search Results for \"${searchValue.value}\" (${results.size})",
                                    onDismissRequest = {
                                        runBlocking {
                                            if (job.value.isActive) {
                                                job.value.cancelAndJoin()
                                            }
                                            searched.value = false
                                            searchDone.value = false
                                            searchValue.value = ""
                                            searchResults.clear()
                                            results = listOf()
                                        }
                                    }) { dismissDialog ->
                                    val lazyListState = rememberLazyListState()
                                    LazyColumn(
                                        state = lazyListState,
                                        modifier = Modifier.fillMaxSize().padding(end = 4.dp)
                                    ) {
                                        item {
                                            if (results.isEmpty()) {
                                                Box(modifier = Modifier.fillMaxSize().padding(32.dp)) {
                                                    Column(
                                                        modifier = Modifier.align(Alignment.Center),
                                                        horizontalAlignment = Alignment.CenterHorizontally
                                                    ) {
                                                        if (searchDone.value) {
                                                            Image(
                                                                modifier = Modifier.size(200.dp),
                                                                painter = painterResource("no_search_results.svg"),
                                                                contentDescription = ""
                                                            )
                                                            Text("No search result found for \"${searchValue.value}\"")
                                                        } else {
                                                            CircularProgressIndicator(
                                                                modifier = Modifier.size(200.dp), /*color = androidx.compose.ui.graphics.Color.White,*/
                                                                strokeWidth = 8.dp
                                                            )
                                                            Text(
                                                                "Loading results...",
                                                                modifier = Modifier.padding(top = 16.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            } else if (!searchDone.value) {
                                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                            }
                                        }
                                        items(results, key = { it }) { filePath ->
                                            val file = File(filePath)
                                            Column(modifier = Modifier.fillMaxSize()) {
                                                SearchItem(
                                                    file = file,
                                                    paths = paths,
                                                    dismissDialog = dismissDialog,
                                                    showHiddenFiles = showHiddenFiles,
                                                    showSnackBar = showSnackBar,
                                                    refresh = refresh
                                                )

                                                Divider()
                                            }
                                        }
                                    }
                                    VerticalScrollbar(
                                        modifier = Modifier.align(Alignment.CenterEnd)
                                            .fillMaxHeight(),
                                        adapter = rememberScrollbarAdapter(lazyListState)
                                    )
                                }
                                LaunchedEffect(searchResults.toList()) {
                                    if (searched.value) {
                                        withContext(Dispatchers.IO) {
                                            runCatching {
                                                results = searchResults.toSet().toList()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                LaunchedEffect(Unit) {
                    fFocs.requestFocus()
                }
            }
            size = Dimension(800, 600)
            setWindowCentered()
            isVisible = true
        }
    }
}

fun JFrame.setWindowCentered() {
    val screenBounds = graphicsConfiguration.bounds
    val x = (screenBounds.width - width) / 2 + screenBounds.x
    val y = (screenBounds.height - height) / 2 + screenBounds.y
    setLocation(x, y)
}