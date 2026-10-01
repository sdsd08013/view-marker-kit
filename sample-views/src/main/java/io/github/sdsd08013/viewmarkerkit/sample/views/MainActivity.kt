package io.github.sdsd08013.viewmarkerkit.sample.views

import android.animation.ValueAnimator
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import io.github.sdsd08013.viewmarkerkit.EdgeMode
import io.github.sdsd08013.viewmarkerkit.MarkerViewFactory
import io.github.sdsd08013.viewmarkerkit.ViewMarkerLayer
import io.github.sdsd08013.viewmarkerkit.sample.views.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var layer: ViewMarkerLayer<Pin>? = null
    private var walker: ValueAnimator? = null

    private val pins = listOf(
        Pin(1, LatLng(35.6812, 139.7671), "Tokyo", 0xFFE53935.toInt()),
        Pin(2, LatLng(35.6586, 139.7454), "Tower", 0xFF1E88E5.toInt()),
        Pin(3, LatLng(35.7101, 139.8107), "Skytree", 0xFF43A047.toInt()),
        Pin(4, LatLng(35.6938, 139.7034), "Shinjuku", 0xFFFB8C00.toInt()),
        Pin(5, LatLng(35.6595, 139.7005), "Shibuya", 0xFF8E24AA.toInt()),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // The overlay creates a view for each marker it is asked to show
        binding.overlay.viewFactory = MarkerViewFactory.sync { marker, _ ->
            PinView(this, marker as Pin) { pin -> binding.overlay.dispatchEvent(pin.identity, Bounce) }
        }

        binding.map.onCreate(savedInstanceState)
        binding.map.getMapAsync { map ->
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(35.68, 139.75), 12f))
            attachLayer(map, clamp = binding.clampSwitch.isChecked)
            binding.clampSwitch.setOnCheckedChangeListener { _, checked -> attachLayer(map, clamp = checked) }
            startWalking(pins[0])
        }
    }

    /** (Re)creates the layer. Creating a layer on an overlay closes the previous one. */
    private fun attachLayer(map: GoogleMap, clamp: Boolean) {
        val edgeMode = if (clamp) EdgeMode.Clamp(marginPx = dp(40)) else EdgeMode.None
        val layer = ViewMarkerLayer<Pin>(this, map, binding.overlay, edgeMode = edgeMode)
        layer.attachCameraListeners()
        map.setOnCameraIdleListener { layer.onCameraIdle(); android.util.Log.d("SampleDebug", "idle ${map.cameraPosition.target}") }
        map.setOnCameraMoveListener { layer.onCameraMove(); android.util.Log.d("SampleDebug", "move ${map.cameraPosition.target} ${map.projection.toScreenLocation(pins[1].location)}") }
        pins.forEach { layer.show(it) }
        this.layer = layer
    }

    /** Moves a pin around in a small circle to show [ViewMarkerLayer.updatePosition]. */
    private fun startWalking(pin: Pin) {
        val center = pin.location
        walker = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 20_000
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                val angle = animatedFraction * 2 * Math.PI
                pin.location = LatLng(
                    center.latitude + 0.01 * Math.sin(angle),
                    center.longitude + 0.01 * Math.cos(angle),
                )
                layer?.updatePosition(pin)
            }
            start()
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()

    override fun onStart() { super.onStart(); binding.map.onStart() }
    override fun onResume() { super.onResume(); binding.map.onResume() }
    override fun onPause() { binding.map.onPause(); super.onPause() }
    override fun onStop() { binding.map.onStop(); super.onStop() }
    override fun onLowMemory() { super.onLowMemory(); binding.map.onLowMemory() }
    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); binding.map.onSaveInstanceState(outState) }

    override fun onDestroy() {
        walker?.cancel()
        layer?.close()
        binding.map.onDestroy()
        super.onDestroy()
    }
}
