@file:Suppress("FunctionName")

package components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import helper.TooltipTarget
import setWindowCentered
import java.awt.Frame
import java.awt.GraphicsEnvironment
import java.awt.event.WindowEvent
import javax.swing.JFrame

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ToolBar(window: JFrame) {
    val maximized = remember { mutableStateOf(false) }
    val isMinimizeHovered = remember { mutableStateOf(false) }
    val isMaximizeHovered = remember { mutableStateOf(false) }
    val isCloseHovered = remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth().padding(start = 8.dp)) {
        Row(modifier = Modifier.align(Alignment.CenterStart), verticalAlignment = Alignment.CenterVertically) {
            Image(
                modifier = Modifier.size(25.dp).padding(end = 4.dp),
                painter = painterResource("finder.svg"),
                contentDescription = "Logo"
            )
            Text("WinFlorer", fontSize = 14.sp)
        }
        Row(modifier = Modifier.align(Alignment.TopEnd)) {
            TooltipTarget(tip = "Minimize") {
                Button(colors = ButtonDefaults.textButtonColors(backgroundColor = if (!isMinimizeHovered.value) Color.Transparent else Color.DarkGray),
                    onClick = {
                        window.state = Frame.ICONIFIED
                    },
                    modifier = Modifier.pointerMoveFilter(
                        onEnter = {
                            isMinimizeHovered.value = true
                            false
                        },
                        onExit = {
                            isMinimizeHovered.value = false
                            false
                        }
                    )
                )
                {
                    Icon(
                        painter = painterResource("dash.svg"),
                        contentDescription = "Minimize",
                        tint = Color.Unspecified
                    )
                }
            }

            TooltipTarget(tip = if (maximized.value) "Restore" else "Maximize") {
                Button(colors = ButtonDefaults.textButtonColors(if (!isMaximizeHovered.value) Color.Transparent else Color.DarkGray),
                    onClick = {
                        if (maximized.value) {
                            window.apply {
                                setSize(800, 600)
                                setWindowCentered()
                            }
                            maximized.value = false
                        } else {
                            val rect = GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds
                            window.apply {
                                setSize(rect.width, rect.height)
                                setLocation(rect.x, rect.y)
                            }
                            maximized.value = true
                        }
                    },

                    modifier = Modifier.pointerMoveFilter(
                        onEnter = {
                            isMaximizeHovered.value = true
                            false
                        },
                        onExit = {
                            isMaximizeHovered.value = false
                            false
                        }
                    )
                )
                {
                    Icon(
                        modifier = if (maximized.value) Modifier else Modifier.size(10.dp),
                        painter = if (maximized.value) painterResource("maxpop.svg") else painterResource("restore.svg"),
                        contentDescription = "",
                        tint = Color.Unspecified
                    )
                }
            }

            TooltipTarget(tip = "Close") {
                Button(
                    colors = if (isCloseHovered.value) ButtonDefaults.textButtonColors(
                        contentColor = Color.Black,
                        backgroundColor = Color.Red
                    ) else ButtonDefaults.textButtonColors(contentColor = Color.White),
                    modifier = Modifier.pointerMoveFilter(
                        onEnter = {
                            isCloseHovered.value = true
                            false
                        },
                        onExit = {
                            isCloseHovered.value = false
                            false
                        }
                    ),
                    onClick = {
                        window.dispatchEvent(WindowEvent(window, WindowEvent.WINDOW_CLOSING))
                    })
                {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        }
    }
//    Divider()
}