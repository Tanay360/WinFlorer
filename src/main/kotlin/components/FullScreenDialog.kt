@file:Suppress("FunctionName")

package components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import helper.GlassyContainer
import helper.TooltipTarget

@Composable
fun FullScreenDialog(modifier: Modifier = Modifier, title: String, onDismissRequest: () -> Unit, content: @Composable BoxScope.(dismissDialog: () -> Unit) -> Unit) {
    Column(modifier = modifier.fillMaxSize().background(Color(0xFF121212))) {
        GlassyContainer(modifier = Modifier.fillMaxWidth().height(50.dp), background = Color.Transparent) {
            Box(modifier = it.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Text(title, modifier = Modifier.align(Alignment.CenterStart))
                TooltipTarget(tip = "Close Dialog", modifier = Modifier.align(Alignment.CenterEnd)) {
                    IconButton(onClick = onDismissRequest) {
                        Icon(
                            modifier = Modifier.size(20.dp),
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }
            }
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            content(onDismissRequest)
        }
    }
}