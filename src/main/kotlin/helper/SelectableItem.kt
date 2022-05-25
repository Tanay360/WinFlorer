@file:Suppress("FunctionName")

package helper

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SelectableItem(isSelected: Boolean, text: String, runOnClick: (() -> Unit)? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(if (isSelected) Color(0xFF2059c9) else Color.Transparent)
            .clickable {
                if (!isSelected) {
                    onClick()
                }
                runOnClick?.let { it() }
            }
            .padding(vertical = 4.dp, horizontal = 8.dp)
    ) {
        Text(text = text, fontSize = 12.sp)
    }
}