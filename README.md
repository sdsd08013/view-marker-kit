# ViewMarkerKit

[![CI](https://github.com/sdsd08013/view-marker-kit/actions/workflows/ci.yml/badge.svg)](https://github.com/sdsd08013/view-marker-kit/actions/workflows/ci.yml)

Render real Android views as markers on Google Maps.

`AdvancedMarkerOptions.iconView` rasterizes a view into an image. ViewMarkerKit instead keeps the
view itself on top of the map and moves it with the camera, so the marker can run animations and
have individually tappable parts.

The library only attaches and detaches views and follows the camera. Which markers to show
(clustering, decluttering, focus, ...) is up to you.

- View-based markers synced with the camera (delta calculation while panning)
- Off-screen markers clamped to the screen edge (`EdgeMode.Clamp`)
- One-shot events delivered to marker views

> Work in progress. The API is not stable yet.

## Setup

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://jitpack.io") }
    }
}

// build.gradle.kts
dependencies {
    implementation("com.github.sdsd08013:view-marker-kit:<tag>")
}
```

## Usage

```kotlin
// 1. Implement the marker contract: id, location and the view size in dp
class Pin(override val id: Long, override var location: LatLng) : ViewMarker {
    override val sizeInDp = 48
}

// 2. Place a MarkerOverlayView over the map, covering the same area, and tell it how to create views
overlay.viewFactory = MarkerViewFactory.sync { marker, _ -> PinView(context) }

// 3. Create the layer and hook up the camera callbacks
val layer = ViewMarkerLayer<Pin>(viewLifecycleOwner, googleMap, overlay)
layer.attachCameraListeners()

// 4. Show, move and hide markers
layer.show(pin)
pin.location = newLocation
layer.updatePosition(pin)
layer.hide(pin)

// 5. Release the layer when the screen goes away
layer.close()
```

To animate a move, update the location on every animation frame (`SphericalUtil` is from
android-maps-utils; any interpolation works):

```kotlin
ValueAnimator.ofFloat(0f, 1f).apply {
    duration = 2000
    addUpdateListener {
        pin.location = SphericalUtil.interpolate(from, to, animatedFraction.toDouble())
        layer.updatePosition(pin)
    }
}.start()
```

`ClusterManager` is not required. To cluster or declutter, call `show` / `hide` based on your own logic.

Use `MarkerViewFactory { marker, edge, onCreated -> ... }` to inflate views asynchronously and call
`onCreated` later. If you keep your own camera listeners, call `layer.onCameraMove()` /
`layer.onCameraIdle()` from them instead of `attachCameraListeners()`.

## Public API

| Type | Role |
|---|---|
| `ViewMarker` / `MarkerIdentity` | Marker contract: location, size and identity (defaults to `id`) |
| `MarkerOverlayView` / `MarkerViewFactory` | Container for marker views and how to create them (`MarkerViewFactory.sync` for synchronous creation) |
| `ViewMarkerLayer` / `ViewMarkerLayer.Listener` | The layer itself |
| `EdgeMode` / `MarkerEdge` | How off-screen markers are handled: keep as is, or clamp to the edge |
| `MarkerPositionDescriptor` / `Alignable` / `ScreenPoint` | Placement info, and how a view learns about it when clamped |
| `MarkerEvent` / `MarkerEventReceiver` | One-shot events sent to a view via `MarkerOverlayView.dispatchEvent` |

## Sample

[`sample-views`](sample-views) shows pins over Tokyo, one of them moving, tap to bounce,
and a switch for `EdgeMode.Clamp`. It uses an XML layout with `MapView` and `MarkerOverlayView`;
marker views are plain Android views.

The map and the overlay must share one View hierarchy so that touches on empty overlay areas
fall through to the map. A Compose-native overlay is being developed as a separate library.

To run it, put a Google Maps API key in `local.properties` (it is read into the manifest at build time and never committed):

```
MAPS_API_KEY=your_key
```

## Requirements

- minSdk 26 / compileSdk 36
- Dependencies: play-services-maps, androidx.lifecycle, androidx.core, kotlinx-coroutines

## License

Apache License 2.0. See [LICENSE](LICENSE).
