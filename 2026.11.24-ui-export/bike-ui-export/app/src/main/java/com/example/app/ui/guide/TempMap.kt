package com.example.app.ui.guide

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.ui.theme.Soft
import kotlin.math.cos

/**
 * 임시 지도 (실제 지도 SDK 붙이기 전).
 * 화면 비율(0~1) 좌표로 길을 그리고, 위도·경도와 화면 위치를 서로 바꿔 줌.
 * 실제 지도를 붙이면 [toScreen] 대신 지도 SDK 의 변환을 쓰면 됨 (예: 네이버 `naverMap.projection.toScreenLocation`).
 */
object TempMap {
    // 화면 왼쪽 위가 (NORTH, WEST) 이고, 화면 가로가 WIDTH_M, 세로가 HEIGHT_M 인 것으로 침
    private const val NORTH = 37.5320
    private const val WEST = 126.9170
    private const val WIDTH_M = 600.0
    private const val HEIGHT_M = 1330.0
    private val latSpan = HEIGHT_M / METERS_PER_DEGREE
    private val lngSpan = WIDTH_M / (METERS_PER_DEGREE * cos(Math.toRadians(NORTH)))

    /** 위도·경도 → 지도 영역 안 화면 위치 (px) */
    fun toScreen(point: GeoPoint, mapSize: Size): Offset = Offset(
        ((point.longitude - WEST) / lngSpan * mapSize.width).toFloat(),
        ((NORTH - point.latitude) / latSpan * mapSize.height).toFloat(),
    )

    /** 화면 비율 위치 (0~1) → 위도·경도 */
    fun toGeo(fraction: Offset): GeoPoint = GeoPoint(NORTH - fraction.y * latSpan, WEST + fraction.x * lngSpan)

    // ── 길 모양 (화면 비율 좌표, 점들을 부드럽게 잇는 곡선) ──
    /** 큰길: 왼쪽 위에서 오른쪽으로 휘었다가 갈림길(5번 점)을 지나 아래로 */
    internal val MainRoad = listOf(
        Offset(-0.08f, 0.125f), Offset(0.2f, 0.13f), Offset(0.42f, 0.185f), Offset(0.545f, 0.29f), Offset(0.52f, 0.4f),
        Offset(0.3f, 0.51f), Offset(0.2f, 0.61f), Offset(0.27f, 0.72f), Offset(0.44f, 0.82f), Offset(0.4f, 0.93f), Offset(0.26f, 1.06f),
    )
    /** 갈림길에서 오른쪽으로 가는 길 (3번 점에서 오른쪽 골목과 만남) */
    internal val Branch = listOf(Offset(0.3f, 0.51f), Offset(0.42f, 0.565f), Offset(0.62f, 0.595f), Offset(0.845f, 0.605f), Offset(1.08f, 0.61f))
    internal val Streets = listOf(
        listOf(Offset(0.86f, -0.05f), Offset(0.85f, 0.25f), Offset(0.83f, 0.45f), Offset(0.845f, 0.605f), Offset(0.88f, 0.8f), Offset(0.9f, 1.05f)),
        listOf(Offset(-0.05f, 0.335f), Offset(0.2f, 0.345f), Offset(0.36f, 0.39f), Offset(0.52f, 0.4f)),   // 큰길 가운데에서 끝남
        listOf(Offset(-0.05f, 0.8f), Offset(0.18f, 0.79f), Offset(0.42f, 0.815f), Offset(0.62f, 0.8f), Offset(0.84f, 0.785f), Offset(1.05f, 0.77f)),
    )
    internal val Parks = listOf(Rect(0.62f, 0.25f, 0.78f, 0.41f), Rect(-0.06f, 0.655f, 0.15f, 0.765f))

    /**
     * 데모 값 채우기 (지도·GPS 붙이기 전 임시).
     * 경로: 아래쪽 큰길 → 갈림길에서 우회전 → 오른쪽 골목에서 좌회전. 내 위치: 갈림길 80m 전
     */
    fun fillDemo(guide: GuideState) {
        val path = sampleCurve(MainRoad, from = 5, to = 10).reversed() +
            sampleCurve(Branch, from = 0, to = 3) +
            sampleCurve(Streets[0], from = 0, to = 3).reversed()
        guide.startRoute(path.map(::toGeo))
        val route = guide.route ?: return
        val firstTurn = route.turns.firstOrNull()?.at ?: return
        guide.updateLocation(route.pointAt(firstTurn - 80.0))
    }
}

/** 임시 지도 그림: 큰길, 갈림길, 골목, 공원 */
@Composable
fun TempMapView(modifier: Modifier = Modifier) {
    Box(modifier) {
        // 별도 레이어에 그려 둠 → 메뉴 애니메이션 중에도 지도는 다시 그리지 않음. 길 모양은 크기가 바뀔 때만 계산
        Spacer(
            Modifier
                .fillMaxSize()
                .graphicsLayer()
                .drawWithCache {
                    val w = size.width
                    val h = size.height
                    fun px(points: List<Offset>) = smoothPath(points.map { Offset(it.x * w, it.y * h) })
                    val parks = TempMap.Parks.map { Rect(it.left * w, it.top * h, it.right * w, it.bottom * h) }
                    val parkCorner = CornerRadius(22.dp.toPx())
                    val edge = 1.5.dp.toPx()          // 길 테두리 (바탕과 길 사이 옅은 선)
                    val roads = listOf(px(TempMap.MainRoad) to 20.dp.toPx(), px(TempMap.Branch) to 15.dp.toPx()) +
                        TempMap.Streets.map { px(it) to 7.dp.toPx() }
                    fun stroke(width: Float) = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    val edges = roads.map { (path, width) -> path to stroke(width + edge * 2) }
                    val fills = roads.map { (path, width) -> path to stroke(width) }
                    onDrawBehind {
                        drawRect(Soft.Base)
                        parks.forEach { drawRoundRect(Soft.MapPark, it.topLeft, it.size, parkCorner) }
                        // 테두리를 먼저 다 그리고 길을 덮어야 길끼리 만나는 곳이 자연스럽게 이어짐
                        edges.forEach { (path, s) -> drawPath(path, Soft.MapGrid, style = s) }
                        fills.forEach { (path, s) -> drawPath(path, Soft.MapRoad, style = s) }
                    }
                }
        )
        Text(
            "임시 지도",
            color = Soft.TextHint,
            fontSize = 13.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 20.dp, top = 16.dp),
        )
    }
}

/** 점 i → i+1 사이 곡선의 베지어 조절점 (캣멀-롬 곡선: 모든 점을 부드럽게 지남) */
private fun controls(points: List<Offset>, i: Int): Pair<Offset, Offset> {
    val p0 = points[maxOf(i - 1, 0)]
    val p1 = points[i]
    val p2 = points[i + 1]
    val p3 = points[minOf(i + 2, points.size - 1)]
    return (p1 + (p2 - p0) / 6f) to (p2 - (p3 - p1) / 6f)
}

private fun smoothPath(points: List<Offset>): Path = Path().apply {
    moveTo(points[0].x, points[0].y)
    for (i in 0 until points.size - 1) {
        val (c1, c2) = controls(points, i)
        cubicTo(c1.x, c1.y, c2.x, c2.y, points[i + 1].x, points[i + 1].y)
    }
}

/** 곡선 위 점들 (점 [from] ~ [to] 구간, 구간마다 [steps] 개) — 그린 길과 똑같은 곡선을 따라감 */
private fun sampleCurve(points: List<Offset>, from: Int, to: Int, steps: Int = 16): List<Offset> {
    val out = ArrayList<Offset>()
    for (i in from until to) {
        val p1 = points[i]
        val p2 = points[i + 1]
        val (c1, c2) = controls(points, i)
        for (k in 0 until steps) {
            val t = k / steps.toFloat()
            val u = 1f - t
            out += p1 * (u * u * u) + c1 * (3f * u * u * t) + c2 * (3f * u * t * t) + p2 * (t * t * t)
        }
    }
    out += points[to]
    return out
}
