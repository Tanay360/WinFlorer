import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.awtEventOrNull
import androidx.compose.ui.input.key.*

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Modifier.arrowListener(paths: SnapshotStateList<String>, removedPaths: SnapshotStateList<String>, refresh: MutableState<Boolean>): Modifier {
    return onKeyEvent {
        if (it.isAltPressed && it.key == Key.DirectionLeft && it.type == KeyEventType.KeyUp) {
            if (paths.size > 1) {
                val r = paths.removeLast()
                val ll = mutableListOf(r).also { ll -> ll.addAll(removedPaths.toList()) }
                removedPaths.clear()
                removedPaths.addAll(ll)
            }
            true
        } else if (it.isAltPressed && it.key == Key.DirectionRight && it.type == KeyEventType.KeyUp) {
            if (removedPaths.size > 0 && paths.isNotEmpty()) {
                paths.add(removedPaths.removeFirst())
            }
            true
        } else if (it.isCtrlPressed && it.key == Key.R) {
            if (paths.isNotEmpty()) {
                refresh.value = true
            }
            true
        } else {
            false
        }
    }
}