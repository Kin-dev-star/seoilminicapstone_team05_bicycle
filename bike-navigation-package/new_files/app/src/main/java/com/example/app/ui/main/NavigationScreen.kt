package com.example.app.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.ui.NavUiState
import com.example.app.ui.RouteUiState
import com.example.app.ui.theme.Soft
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.hypot
import kotlin.math.roundToInt

// 카카오맵·네이버지도 길 안내 화면을 참고한 색
private val NavBlue = Color(0xFF1E6EF5)
private val NavBlueDark = Color(0xFF1557D0)
private val NavRed = Color(0xFFD93A3A)
private val NavAmber = Color(0xFFF2A516)
private val NavCard = RoundedCornerShape(18.dp)

// ───────────────────────── 경로 미리보기 (경로 분석 직후) ─────────────────────────

/**
 * 경로 분석이 끝나면 지도에 전체 경로를 보여주고 아래에 띄우는 요약 카드.
 * 카카오맵의 "실시간 추천 25분 · 7.2km · [안내시작]" 화면과 같은 역할.
 */
@Composable
fun RoutePreviewPanel(
    state: RouteUiState,
    onStartGuidance: () -> Unit,
    onEditRoute: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val minutes = ((state.durationSeconds ?: 0) + 59) / 60
    val (distNumber, distUnit) = formatDistance(state.distanceMeters ?: 0)
    Column(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(12.dp)
            .shadow(14.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(Soft.Base)
            .padding(start = 20.dp, end = 12.dp, top = 14.dp, bottom = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                state.modeLabel.ifEmpty { "자전거 경로" },
                color = NavBlue, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.Filled.Close, contentDescription = "경로 지우기", tint = Soft.TextSub,
                modifier = Modifier.clip(CircleShape).clickable(onClick = onClose).padding(8.dp).size(20.dp),
            )
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text("$minutes", color = Soft.Text, fontSize = 40.sp, fontWeight = FontWeight.Bold)
            Text("분", color = Soft.Text, fontSize = 20.sp, modifier = Modifier.padding(start = 2.dp, bottom = 6.dp))
            Spacer(Modifier.width(14.dp))
            Text("$distNumber$distUnit", color = Soft.TextSub, fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 6.dp))
        }
        if (state.startName.isNotEmpty() || state.endName.isNotEmpty()) {
            Text(
                "${state.startName} → ${state.endName}",
                color = Soft.TextSub, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 8.dp),
            )
        }
        val hazards = state.routeHazards
        Text(
            when {
                hazards == null || hazards < 0 -> "사고 데이터 준비 중 · 실제 사고 회피 계산은 적용되지 않았어요"
                hazards == 0 -> "경로 주변 150m 이내 과거 사고다발지역 없음"
                else -> "경로 주변 150m 이내 과거 사고다발지역 ${hazards}곳 · 회피 경로 아님"
            },
            color = if ((hazards ?: 0) > 0) NavRed else Soft.TextHint,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp, end = 8.dp),
        )
        Spacer(Modifier.height(14.dp))
        Row(Modifier.padding(end = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF6B7280))
                    .clickable(onClick = onEditRoute),
                contentAlignment = Alignment.Center,
            ) {
                Text("경로 변경", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            Row(
                Modifier
                    .weight(1.6f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(NavBlue)
                    .clickable(onClick = onStartGuidance),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ManeuverArrow("STRAIGHT", Color.White, 22.dp)
                Spacer(Modifier.width(8.dp))
                Text("안내 시작", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ───────────────────────── 길 안내 화면 ─────────────────────────

/**
 * 안내 시작 후 지도 위에 겹치는 내비게이션 화면.
 * 왼쪽 위: 다음 회전 카드(화살표·남은 거리) + 그다음 안내, 현재 속도
 * 오른쪽: 진행 막대(사고다발지역 표시), 아래: 도착 예정 시각·남은 거리·재탐색·종료
 */
@Composable
fun NavigationOverlay(
    state: NavUiState,
    proximityWarning: String?,
    onEnd: () -> Unit,
    onReroute: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmEnd by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(state.messageId) {
        val message = state.message
        if (message != null) {
            toast = message
            delay(2500)
            toast = null
        }
    }
    BackHandler { confirmEnd = true }

    Box(modifier.fillMaxSize()) {
        Column(
            Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 12.dp, top = 8.dp, end = 56.dp)
        ) {
            TurnCard(state)
            if (state.hasFix && state.nextManeuver != null) {
                Row(
                    Modifier
                        .padding(start = 0.dp)
                        .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                        .background(NavBlueDark)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("그다음", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    Spacer(Modifier.width(8.dp))
                    ManeuverArrow(state.nextManeuver ?: "STRAIGHT", Color.White, 26.dp)
                    Spacer(Modifier.width(6.dp))
                    val (n, u) = formatDistance(state.nextDistance)
                    Text("$n$u", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
            when {
                state.rerouting -> StatusBanner("경로를 다시 찾는 중…", NavAmber)
                state.offRoute -> StatusBanner("경로를 벗어났어요", NavAmber)
            }
            proximityWarning?.let { StatusBanner(it, NavRed, warningIcon = true) }
            Spacer(Modifier.height(12.dp))
            SpeedBadge(state.speedKmh)
        }

        RouteProgressBar(
            progress = state.progress,
            hazardMarks = state.hazardMarks,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 14.dp)
                .width(12.dp)
                .height(280.dp),
        )

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            toast?.let {
                Text(
                    it,
                    color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xE6202226))
                        .padding(horizontal = 22.dp, vertical = 12.dp),
                )
            }
            BottomInfoCard(state, onReroute = onReroute, onEnd = { confirmEnd = true })
        }
    }

    if (confirmEnd) {
        AlertDialog(
            onDismissRequest = { confirmEnd = false },
            title = { Text("길 안내를 종료할까요?") },
            confirmButton = {
                TextButton(onClick = { confirmEnd = false; onEnd() }) { Text("종료") }
            },
            dismissButton = {
                TextButton(onClick = { confirmEnd = false }) { Text("계속 안내") }
            },
        )
    }
}

/** 왼쪽 위 파란 카드: 회전 화살표 + 남은 거리 + 안내 문구 */
@Composable
private fun TurnCard(state: NavUiState) {
    Row(
        Modifier
            .widthIn(min = 200.dp, max = 320.dp)
            .shadow(10.dp, NavCard)
            .clip(NavCard)
            .background(NavBlue)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .semantics { contentDescription = "다음 안내 ${state.maneuverLabel} ${state.distanceToNext}미터" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ManeuverArrow(state.maneuver, Color.White, 60.dp)
        Spacer(Modifier.width(14.dp))
        Column {
            if (!state.hasFix) {
                Text("현재 위치 확인 중…", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("GPS 신호를 기다리고 있어요", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
            } else {
                val (n, u) = formatDistance(state.distanceToNext)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(n, color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Bold)
                    Text(u, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 2.dp, bottom = 6.dp))
                }
                Text(state.maneuverLabel, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                if (state.instruction.isNotBlank() && state.instruction != state.maneuverLabel) {
                    Text(state.instruction, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun StatusBanner(text: String, color: Color, warningIcon: Boolean = false) {
    Row(
        Modifier
            .padding(top = 8.dp)
            .widthIn(max = 320.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (warningIcon) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** 현재 속도 (km/h). 모르면 "–" */
@Composable
private fun SpeedBadge(speedKmh: Int) {
    Column(
        Modifier
            .size(64.dp)
            .shadow(6.dp, CircleShape)
            .clip(CircleShape)
            .background(Color.White)
            .border(3.dp, NavBlue, CircleShape),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(if (speedKmh >= 0) "$speedKmh" else "–", color = Color(0xFF1B1D22), fontSize = 24.sp,
            fontWeight = FontWeight.Bold)
        Text("km/h", color = Color(0xFF6E7383), fontSize = 9.sp)
    }
}

/** 아래 카드: 재탐색 · 도착지/도착 예정 시각/남은 거리 · 종료 */
@Composable
private fun BottomInfoCard(state: NavUiState, onReroute: () -> Unit, onEnd: () -> Unit) {
    val arrival = remember(state.remainingSeconds) {
        SimpleDateFormat("a h:mm", Locale.KOREA).format(Date(System.currentTimeMillis() + state.remainingSeconds * 1000L))
    }
    val (n, u) = formatDistance(state.remainingMeters)
    Row(
        Modifier
            .fillMaxWidth()
            .shadow(14.dp, RoundedCornerShape(22.dp))
            .clip(RoundedCornerShape(22.dp))
            .background(Soft.Base)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.Refresh, contentDescription = "경로 재탐색", tint = Soft.Text,
            modifier = Modifier.clip(CircleShape).clickable(enabled = !state.rerouting, onClick = onReroute)
                .padding(12.dp).size(26.dp),
        )
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocationOn, contentDescription = null, tint = NavRed, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(2.dp))
                Text(state.destination.ifEmpty { "목적지" }, color = Soft.TextSub, fontSize = 14.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(arrival, color = Soft.Text, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text(" 도착", color = Soft.TextSub, fontSize = 13.sp, modifier = Modifier.padding(bottom = 4.dp))
                Spacer(Modifier.width(14.dp))
                Text(n, color = Soft.Text, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text(u, color = Soft.Text, fontSize = 15.sp, modifier = Modifier.padding(bottom = 3.dp))
            }
            Text("남은 시간 약 ${(state.remainingSeconds + 59) / 60}분", color = Soft.TextHint, fontSize = 11.sp)
        }
        Icon(
            Icons.Filled.Close, contentDescription = "안내 종료", tint = Soft.Text,
            modifier = Modifier.clip(CircleShape).clickable(onClick = onEnd).padding(12.dp).size(26.dp),
        )
    }
}

/**
 * 오른쪽 세로 진행 막대. 아래가 출발, 위가 도착.
 * 파란 부분은 남은 경로, 빨간 점은 경로 주변 과거 사고다발지역.
 */
@Composable
private fun RouteProgressBar(progress: Float, hazardMarks: List<Float>, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val p = progress.coerceIn(0f, 1f)
        fun y(f: Float) = h * (1f - f)
        drawRoundRect(Color(0xFFD5D8DE), size = Size(w, h), cornerRadius = CornerRadius(w / 2, w / 2))
        drawRoundRect(
            NavBlue,
            topLeft = Offset(0f, 0f),
            size = Size(w, y(p)),
            cornerRadius = CornerRadius(w / 2, w / 2),
        )
        hazardMarks.forEach { f ->
            if (f >= p) {
                drawCircle(Color.White, radius = w * 0.75f, center = Offset(w / 2, y(f)))
                drawCircle(NavRed, radius = w * 0.55f, center = Offset(w / 2, y(f)))
            }
        }
        drawCircle(NavBlue, radius = w * 1.05f, center = Offset(w / 2, y(p)))
        drawCircle(Color.White, radius = w * 0.6f, center = Offset(w / 2, y(p)))
    }
}

// ───────────────────────── 회전 화살표 ─────────────────────────

/** NavigationTracker.Maneuver 이름에 맞는 화살표를 그립니다 (아이콘 파일 없이 직접 그림). */
@Composable
fun ManeuverArrow(maneuver: String, color: Color, size: Dp) {
    Canvas(Modifier.size(size)) {
        val s = this.size.minDimension
        val stroke = Stroke(width = s * 0.13f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun pt(x: Float, y: Float) = Offset(x * s, y * s)
        fun same(x: Float) = x
        fun mirror(x: Float) = 1f - x
        when (maneuver) {
            "LEFT", "RIGHT" -> {
                val m: (Float) -> Float = if (maneuver == "LEFT") ::same else ::mirror
                val path = Path().apply {
                    moveTo(m(0.62f) * s, 0.95f * s)
                    lineTo(m(0.62f) * s, 0.50f * s)
                    quadraticTo(m(0.62f) * s, 0.32f * s, m(0.44f) * s, 0.32f * s)
                    lineTo(m(0.32f) * s, 0.32f * s)
                }
                drawPath(path, color, style = stroke)
                arrowHead(pt(m(0.08f), 0.32f), if (maneuver == "LEFT") -1f else 1f, 0f, s * 0.24f, color)
            }
            "SLIGHT_LEFT", "SLIGHT_RIGHT" -> {
                val left = maneuver == "SLIGHT_LEFT"
                val m: (Float) -> Float = if (left) ::same else ::mirror
                val path = Path().apply {
                    moveTo(m(0.60f) * s, 0.95f * s)
                    lineTo(m(0.60f) * s, 0.60f * s)
                    lineTo(m(0.36f) * s, 0.33f * s)
                }
                drawPath(path, color, style = stroke)
                arrowHead(pt(m(0.20f), 0.15f), if (left) -0.4f else 0.4f, -0.45f, s * 0.24f, color)
            }
            "UTURN" -> {
                val path = Path().apply {
                    moveTo(0.68f * s, 0.95f * s)
                    lineTo(0.68f * s, 0.42f * s)
                    cubicTo(0.68f * s, 0.12f * s, 0.30f * s, 0.12f * s, 0.30f * s, 0.42f * s)
                    lineTo(0.30f * s, 0.55f * s)
                }
                drawPath(path, color, style = stroke)
                arrowHead(pt(0.30f, 0.80f), 0f, 1f, s * 0.24f, color)
            }
            "ARRIVE" -> {
                drawLine(color, pt(0.30f, 0.92f), pt(0.30f, 0.10f), strokeWidth = s * 0.11f, cap = StrokeCap.Round)
                val flag = Path().apply {
                    moveTo(0.30f * s, 0.12f * s)
                    lineTo(0.80f * s, 0.28f * s)
                    lineTo(0.30f * s, 0.46f * s)
                    close()
                }
                drawPath(flag, color)
            }
            else -> { // STRAIGHT, START
                drawLine(color, pt(0.5f, 0.95f), pt(0.5f, 0.30f), strokeWidth = s * 0.13f, cap = StrokeCap.Round)
                arrowHead(pt(0.5f, 0.06f), 0f, -1f, s * 0.24f, color)
            }
        }
    }
}

/** tip 위치에서 (dx, dy) 방향을 가리키는 채워진 삼각형 화살촉 */
private fun DrawScope.arrowHead(tip: Offset, dx: Float, dy: Float, length: Float, color: Color) {
    val len = hypot(dx, dy).takeIf { it > 0f } ?: 1f
    val ux = dx / len
    val uy = dy / len
    val base = Offset(tip.x - ux * length, tip.y - uy * length)
    val half = length * 0.78f
    val path = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(base.x - uy * half, base.y + ux * half)
        lineTo(base.x + uy * half, base.y - ux * half)
        close()
    }
    drawPath(path, color)
}

/** 거리 표시: 1km 미만은 m(100m 이상은 10m 단위), 이상은 km 소수 한 자리 */
private fun formatDistance(meters: Int): Pair<String, String> = when {
    meters >= 10_000 -> Pair("${(meters / 1000.0).roundToInt()}", "km")
    meters >= 1000 -> Pair(String.format(Locale.KOREA, "%.1f", meters / 1000.0), "km")
    meters >= 100 -> Pair("${(meters / 10.0).roundToInt() * 10}", "m")
    else -> Pair("${meters.coerceAtLeast(0)}", "m")
}
