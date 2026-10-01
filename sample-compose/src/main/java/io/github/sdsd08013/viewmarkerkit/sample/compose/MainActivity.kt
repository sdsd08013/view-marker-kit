package io.github.sdsd08013.viewmarkerkit.sample.compose

import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import io.github.sdsd08013.viewmarkerkit.EdgeMode
import io.github.sdsd08013.viewmarkerkit.MarkerOverlayView
import io.github.sdsd08013.viewmarkerkit.MarkerViewFactory
import io.github.sdsd08013.viewmarkerkit.ViewMarkerLayer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MapScreen()
            }
        }
    }
}

private val pins = listOf(
    Pin(1, LatLng(35.6812, 139.7671), "Tokyo", Color(0xFFE53935)),
    Pin(2, LatLng(35.6586, 139.7454), "Tower", Color(0xFF1E88E5)),
    Pin(3, LatLng(35.7101, 139.8107), "Skytree", Color(0xFF43A047)),
    Pin(4, LatLng(35.6938, 139.7034), "Shinjuku", Color(0xFFFB8C00)),
    Pin(5, LatLng(35.6595, 139.7005), "Shibuya", Color(0xFF8E24AA)),
)

@Composable
private fun MapScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }
    var clamp by remember { mutableStateOf(false) }

    // The map and the overlay must share one View hierarchy so touches on empty overlay
    // areas fall through to the map, so both are hosted by a single AndroidView.
    val mapView = remember { MapView(context) }
    val overlay = remember {
        MarkerOverlayView(context).apply {
            // The overlay creates a view for each marker it is asked to show
            viewFactory = MarkerViewFactory.sync { marker, _ ->
                PinView(context, marker as Pin) { pin -> dispatchEvent(pin.identity, Bounce) }
            }
        }
    }
    val container = remember {
        FrameLayout(context).apply {
            val match = FrameLayout.LayoutParams.MATCH_PARENT
            addView(mapView, FrameLayout.LayoutParams(match, match))
            addView(overlay, FrameLayout.LayoutParams(match, match))
        }
    }

    // Forward the lifecycle to the MapView
    DisposableEffect(lifecycleOwner) {
        mapView.onCreate(null)
        mapView.getMapAsync { map ->
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(35.68, 139.75), 12f))
            googleMap = map
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    // One layer per (map, edge mode). Creating a layer on the overlay closes the previous one.
    val map = googleMap
    val layer = remember(map, clamp) {
        map?.let {
            val edgeMode = if (clamp) EdgeMode.Clamp(marginPx = (40 * context.resources.displayMetrics.density).toInt()) else EdgeMode.None
            ViewMarkerLayer<Pin>(lifecycleOwner, it, overlay, edgeMode = edgeMode)
        }
    }
    DisposableEffect(layer) {
        layer?.let { l ->
            l.attachCameraListeners()
            pins.forEach { l.show(it) }
        }
        onDispose { layer?.close() }
    }

    // Move one pin around in a small circle to show updatePosition
    LaunchedEffect(layer) {
        val l = layer ?: return@LaunchedEffect
        val pin = pins[0]
        val center = LatLng(35.6812, 139.7671)
        val start = withFrameMillis { it }
        while (true) {
            val angle = withFrameMillis { (it - start) % 20_000 / 20_000.0 * 2 * Math.PI }
            pin.location = LatLng(center.latitude + 0.01 * Math.sin(angle), center.longitude + 0.01 * Math.cos(angle))
            l.updatePosition(pin)
        }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(factory = { container }, modifier = Modifier.fillMaxSize())

        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            shadowElevation = 4.dp,
        ) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Clamp to edge", Modifier.padding(end = 8.dp))
                Switch(checked = clamp, onCheckedChange = { clamp = it })
            }
        }
    }
}
