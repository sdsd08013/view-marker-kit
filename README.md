# ViewMarkerKit

Render real Android views as markers on Google Maps.

`AdvancedMarkerOptions.iconView` rasterizes a view into an image. ViewMarkerKit instead keeps the
view itself on top of the map and moves it with the camera, so the marker can run animations and
have individually tappable parts.

The library only attaches and detaches views, follows the camera and animates moves. Which markers
to show (clustering, decluttering, focus, ...) is up to you.

- View-based markers synced with the camera (delta calculation while panning)
- Off-screen markers clamped to the screen edge (`EdgeMode.Clamp`)
- Smooth moves between locations
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
val layer = ViewMarkerLayer<Pin>(activity, viewLifecycleOwner, googleMap, overlay)
layer.attachCameraListeners()

// 4. Show, move and hide markers
layer.show(pin)
layer.moveSmoothly(pin, to = newLocation)
layer.hide(pin)
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

## Requirements

- minSdk 26 / compileSdk 36
- Dependencies: play-services-maps, androidx.lifecycle, androidx.core, kotlinx-coroutines

## License

Apache License 2.0. See [LICENSE](LICENSE).
