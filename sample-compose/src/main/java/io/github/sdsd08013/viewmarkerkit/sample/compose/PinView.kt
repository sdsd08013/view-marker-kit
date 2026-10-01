package io.github.sdsd08013.viewmarkerkit.sample.compose

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.sdsd08013.viewmarkerkit.Alignable
import io.github.sdsd08013.viewmarkerkit.MarkerEdge
import io.github.sdsd08013.viewmarkerkit.MarkerEvent
import io.github.sdsd08013.viewmarkerkit.MarkerEventReceiver
import io.github.sdsd08013.viewmarkerkit.MarkerPositionDescriptor
import kotlinx.coroutines.delay

/**
 * The view rendered for a [Pin]. The content is Compose; the [AbstractComposeView] host is what
 * the layer places on the map. Events from the layer are turned into Compose state.
 */
class PinView(context: Context, private val pin: Pin, private val onClick: (Pin) -> Unit) :
    AbstractComposeView(context), MarkerEventReceiver, Alignable {
    private var bounces by mutableIntStateOf(0)
    private var atEdge by mutableStateOf(false)

    @Composable
    override fun Content() {
        PinContent(pin, bounces, atEdge) { onClick(pin) }
    }

    override fun receive(event: MarkerEvent) {
        if (event === Bounce) bounces++
    }

    override fun align(descriptor: MarkerPositionDescriptor) {
        atEdge = descriptor.currentEdge != MarkerEdge.NONE
    }
}

@Composable
private fun PinContent(pin: Pin, bounces: Int, atEdge: Boolean, onClick: () -> Unit) {
    var bouncing by remember { mutableStateOf(false) }
    LaunchedEffect(bounces) {
        if (bounces == 0) return@LaunchedEffect
        bouncing = true
        delay(120)
        bouncing = false
    }
    val scale by animateFloatAsState(if (bouncing) 1.4f else 1f, spring(), label = "bounce")

    Box(
        modifier = Modifier.size(PIN_SIZE_DP.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(pin.color.copy(alpha = if (atEdge) 0.6f else 1f))
                .border(3.dp, Color.White, CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(pin.label, color = Color.White, fontSize = 11.sp, maxLines = 1)
        }
    }
}
