@file:Suppress("FunctionName")

package helper

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MTextField(
    modifier: Modifier = Modifier,
    value: String,
    textColor: Color = Color.LightGray,
    onValueChange: (String) -> Unit,
    singleLine: Boolean = false,
    label: String? = null,
    labelColor: Color = Color(0xFF969EBD),
    labelFontSize: TextUnit = 14.sp,
    background: Color = Color(0xFF5a5b5d),
    onEnter: (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    prefixIcon: (@Composable () -> Unit)? = null
) {
    val customTextSelectionColors = TextSelectionColors(
        handleColor = Color.Gray,
        backgroundColor = Color.Gray.copy(alpha = 0.4f)
    )
    CompositionLocalProvider(LocalTextSelectionColors provides customTextSelectionColors) {
        BasicTextField(
            modifier = modifier.clip(RoundedCornerShape(4.dp)).background(background).padding(4.dp).let { mod ->
                if (onEnter == null) {
                    return@let mod
                }
                return@let mod.onKeyEvent {
                    if (it.key == Key.Enter) {
                        onEnter()
                        true
                    } else {
                        false
                    }
                }
            },
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                color = textColor,
            ),
            keyboardOptions = if (onEnter == null)  keyboardOptions else KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = if (onEnter == null) keyboardActions else KeyboardActions(
                onDone = {
                    onEnter()
                }
            ),

            singleLine = singleLine,
            cursorBrush = SolidColor(Color.White),
            decorationBox = { innerTextField ->
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (prefixIcon != null) {
                        prefixIcon()
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Box {
                        innerTextField()
                        if (value.isEmpty() && label != null) {
                            Text(
                                text = label,
                                color = labelColor,
                                fontSize = labelFontSize
                            )
                        }
                    }
                }
            }
        )
    }
}