package com.example.app.ui.guide

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * 길안내 아이콘 (24×24, 한 가지 색 → `Icon(NavIcons.TurnLeft, null, tint = ...)` 처럼 색을 입혀 씀).
 * nav-icons.zip 의 ic_nav_*.xml · svg · png 와 같은 모양. 지도 SDK 마커처럼 이미지 파일이 필요하면 그쪽을 쓰기.
 */
object NavIcons {
    /** 핀 (현재 위치 표시) */
    val Pin: ImageVector by lazy { navIcon("pin", fill = "M4.5,9.5 A7.5,7.5 0 0 1 19.5,9.5 C19.5,14.5 14.5,19 12.9,21.3 Q12,22.59 11.1,21.3 C9.5,19 4.5,14.5 4.5,9.5 Z M15,9.5 A3,3 0 0 0 9,9.5 A3,3 0 0 0 15,9.5 Z") }
    /** 좌회전 */
    val TurnLeft: ImageVector by lazy { navIcon("turn_left", shaft = "M19.5,20.5 V12 A4.5,4.5 0 0 0 15,7.5 H10", head = "M4,7.5 L10,3 L10,12 Z") }
    /** 우회전 */
    val TurnRight: ImageVector by lazy { navIcon("turn_right", shaft = "M4.5,20.5 V12 A4.5,4.5 0 0 1 9,7.5 H14", head = "M20,7.5 L14,12 L14,3 Z") }
    /** 직진 */
    val Straight: ImageVector by lazy { navIcon("straight", shaft = "M12,20.5 V9", head = "M12,3 L16.5,9 L7.5,9 Z") }
    /** 유턴 */
    val UTurn: ImageVector by lazy { navIcon("uturn", shaft = "M18.5,20.5 V8 A4.5,4.5 0 0 0 9.5,8 V15", head = "M9.5,21 L5,15 L14,15 Z") }
}

/** [shaft] 굵은 줄기 (둥근 끝), [head] 둥근 모서리 화살촉, [fill] 채운 모양 */
private fun navIcon(name: String, shaft: String? = null, head: String? = null, fill: String? = null): ImageVector {
    val black = SolidColor(Color.Black)
    return ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        fill?.let { addPath(addPathNodes(it), fill = black) }
        shaft?.let { addPath(addPathNodes(it), stroke = black, strokeLineWidth = 3f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) }
        head?.let { addPath(addPathNodes(it), fill = black, stroke = black, strokeLineWidth = 2f, strokeLineJoin = StrokeJoin.Round) }
    }.build()
}
