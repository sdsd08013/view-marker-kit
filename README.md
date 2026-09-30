# ViewMarkerKit

Google Maps の上に、本物の Android View をマーカーとして表示するためのライブラリです。

`AdvancedMarkerOptions.iconView` は View を画像化して表示しますが、ViewMarkerKit は View をそのまま地図に重ねてカメラに同期させます。マーカー内でアニメーションを動かしたり、マーカーの一部だけをタップ可能にしたりできます。

持つのは「View を載せる / 外す」「スクリーン座標をカメラに追従させる」「位置を滑らかに動かす」だけです。どのマーカーを載せるか（クラスタリング・間引き・focus など）は利用側が決めます。

- View ベースのマーカー表示とカメラ同期（カメラ移動中はデルタ計算で追従）
- 画面外マーカーの端寄せ表示（`EdgeMode.Clamp`）
- 位置のなめらかな移動
- View への一過性イベントの配送

> 開発中です。API はまだ安定していません。

## 導入

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

## 使い方

```kotlin
// 1. マーカーの契約を実装する（id / 位置 / View の一辺の dp）
class Pin(override val id: Long, override var location: LatLng) : ViewMarker {
    override val sizeInDp = 48
}

// 2. GoogleMap と同じ領域に MarkerOverlayView を重ね、View の作り方を渡す
overlay.viewFactory = MarkerViewFactory.sync { marker, _ -> PinView(context) }

// 3. layer を作り、カメラのコールバックを繋ぐ
val layer = ViewMarkerLayer<Pin>(activity, viewLifecycleOwner, googleMap, overlay)
layer.attachCameraListeners()

// 4. 載せる / 外す / 動かす
layer.show(pin)
layer.moveSmoothly(pin, to = newLocation)
layer.hide(pin)
```

`ClusterManager` は必要ありません。クラスタリングや間引きをしたい場合は、その結果に応じて `show` / `hide` を呼びます。

非同期に inflate したい場合は `MarkerViewFactory { marker, edge, onCreated -> ... }` で `onCreated` を後から呼びます。
GoogleMap のカメラリスナーを自前で持つ場合は `attachCameraListeners()` の代わりに、そのリスナーから `layer.onCameraMove()` / `layer.onCameraIdle()` を呼びます。

## 公開 API

| 型 | 役割 |
|---|---|
| `ViewMarker` / `MarkerIdentity` | マーカーの契約（位置・大きさ・識別子）。識別子は既定で `id` がそのまま使われる |
| `MarkerOverlayView` / `MarkerViewFactory` | View の載せ先と、View の生成（同期なら `MarkerViewFactory.sync`） |
| `ViewMarkerLayer` / `ViewMarkerLayer.Listener` | 描画層の本体 |
| `EdgeMode` / `MarkerEdge` | 画面外マーカーの扱い（そのまま / 画面端へ寄せる） |
| `MarkerPositionDescriptor` / `Alignable` / `ScreenPoint` | 配置情報と、端へ寄せたときの View への通知 |
| `MarkerEvent` / `MarkerEventReceiver` | `MarkerOverlayView.dispatchEvent` で View へ一過性イベントを渡す |

## 動作要件

- minSdk 26 / compileSdk 36
- 依存: play-services-maps, androidx.lifecycle, androidx.core, kotlinx-coroutines

## ライセンス

Apache License 2.0。詳細は [LICENSE](LICENSE) を参照。
