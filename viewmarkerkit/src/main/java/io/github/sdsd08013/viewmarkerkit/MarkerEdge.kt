package io.github.sdsd08013.viewmarkerkit

/**
 * マーカーが画面の端にある位置を表すenum
 */
enum class MarkerEdge {
    LEFT,        // 左端
    RIGHT,       // 右端
    TOP,         // 上端
    BOTTOM,      // 下端
    TOP_LEFT,    // 左上角
    TOP_RIGHT,   // 右上角
    BOTTOM_LEFT, // 左下角
    BOTTOM_RIGHT,// 右下角
    NONE,        // 端にない（中央）
}
