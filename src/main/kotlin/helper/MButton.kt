@file:Suppress("FunctionName")

package helper

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun MButton(modifier: Modifier = Modifier, background: Color = Color(0xFF2059c9), contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 6.dp), content: MButton.() -> MButtonResult) {
    val btn = MButton()
    val result = content(btn)
    Box(modifier = modifier.clip(RoundedCornerShape(4.dp)).background(background).clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = rememberRipple(bounded = true),
        onClick = {
            result.action()
        }
    ).padding(paddingValues = contentPadding)) {
        btn.content()
    }
}

class MButton internal constructor() {
    internal var content: @Composable () -> Unit = {}
    fun content(content: @Composable () -> Unit) {
        this.content = content
    }

    fun onClick(action: () -> Unit) = MButtonResult(action)
}

class MButtonResult(internal val action: () -> Unit)