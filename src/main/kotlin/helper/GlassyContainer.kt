@file:Suppress("FunctionName")

package helper

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun GlassyContainer(
    modifier: Modifier = Modifier,
    background: Color? = null,
    content: @Composable (modifier: Modifier) -> Unit
) {
    Box(
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.fillMaxSize().blur(13.dp)
                .let { if (background != null) it.background(background) else it })
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Card(
                    modifier = Modifier.fillMaxSize(),
                    backgroundColor = Color(0xFFFFFF),
                    elevation = 0.dp
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .alpha(1f)
                            .blur(
                                radius = 28.dp,
                                edgeTreatment = BlurredEdgeTreatment.Unbounded
                            )
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        Color(0x12FFFFFF),
                                        Color(0xDFFFFFF),
                                    ),
                                    radius = 2200f,
                                    center = Offset.Infinite
                                )
                            )
                    ) {

                    }
                    content(Modifier.fillMaxSize())
                }
            }
        }
    }
}