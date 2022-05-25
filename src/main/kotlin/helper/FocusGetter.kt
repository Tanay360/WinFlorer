package helper

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun Modifier.focusGetter(focusRequester: FocusRequester = FocusRequester()): Modifier {
    var state by remember { mutableStateOf(false) }

    LaunchedEffect(state) {
        if (state) {
            focusRequester.requestFocus()
            state = false
        }
    }
    return then(
        Modifier.focusTarget()
            .focusRequester(focusRequester)
            .pointerInput(Unit) {
                detectTapGestures {
                    state = true
                }
            }
    )
}