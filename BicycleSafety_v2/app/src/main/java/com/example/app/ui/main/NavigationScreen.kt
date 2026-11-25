package com.example.app.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.app.ui.NavUiState
import com.example.app.ui.RouteUiState
import com.example.app.ui.guide.NavIcons
import com.example.app.ui.theme.Soft
import com.example.app.ui.theme.softInset
import com.example.app.ui.theme.softPressable
import com.example.app.ui.theme.softRaised
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/*
 * 길안내 화면 · 경로 미리보기 카드.
 * 디자인 팀 bike-ui-export 의 소프트 UI(GuideBanner · TurnBox · NavIcons) 모양에 맞춤:
 * 바탕과 같은 색 + 볼록/오목 그림자, 강조색은 Soft.Accent, 화살표는 NavIcons.
 * 사고 위험·경로 이탈처럼 바로 알아봐야 하는 것만 빨강·주황 글자로 표시.
 */

private val NavCardShape = RoundedCornerShape(18.dp)
private val ArrowWellShape = RoundedCornerShape(20.dp)   // 디자인: 위쪽 배너 화살표 칸
private val SmallWellShape = RoundedCornerShape(12.dp)
private val NavPillShape = RoundedCornerShape(percent = 50)
private val ARROW_WELL = 72.dp
private val BANNER_ICON = 48.dp
private val NavDanger = Color(0xFFD93A3A)
private val NavCaution = Color(0xFFE0921A)
private const val PRESS_MS = 110

// ───────────────────────── 경로 미리보기 (경로 분석 후, 경로 설정 창을 닫았을 때) ─────────────────────────

/**
 * 지도에 경로가 그려져 있을 때 아래에 띄우는 요약 카드.
 * 안내 시작 버튼은 경로 설정 창에 있으므로 여기서는 요약과 '경로 설정 열기'만 둠.
 */
@Composable
fun RoutePreviewPanel(
    state: RouteUiState,
    onEditRoute: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val minutes = ((state.durationSeconds ?: 0) + 59) / 60
    Column(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(20.dp)
            .softRaised(NavCardShape)
            .clip(NavCardShape)
            .background(Soft.Base)
            .padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                state.modeLabel.ifEmpty { "자전거 경로" },
                color = Soft.Accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            NavIconButton(onClick = onClose, size = 36.dp) {
                Icon(Icons.Filled.Close, contentDescription = "경로 지우기", tint = Soft.TextSub, modifier = Modifier.size(18.dp))
            }
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text("$minutes", color = Soft.Text, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Text("분", color = Soft.Text, fontSize = 18.sp, modifier = Modifier.padding(start = 2.dp, bottom = 5.dp))
            Spacer(Modifier.width(14.dp))
            Text(distanceText(state.distanceMeters ?: 0), color = Soft.TextSub, fontSize = 17.sp,
                modifier = Modifier.padding(bottom = 5.dp))
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
            color = if ((hazards ?: 0) > 0) NavDanger else Soft.TextHint,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp, end = 8.dp),
        )
        Spacer(Modifier.height(14.dp))
        NavWideButton("경로 설정 열기 · 안내 시작", Modifier.padding(end = 8.dp), onClick = onEditRoute)
    }
}

// ───────────────────────── 길안내 화면 ─────────────────────────

/**
 * 안내 시작 후 지도 위에 겹치는 길안내 화면.
 * 위: 디자인 팀 GuideBanner 모양의 전체 폭 배너 (오목한 칸 안 화살표 · "80m 후 우회전" · 그다음 안내 · 도착까지)
 * 왼쪽: 경로 이탈·사고 위험 알림, 현재 속도 / 오른쪽: 진행 막대 / 아래: 도착 예정 시각 · 남은 거리 · 재탐색 · 종료
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
        Column(Modifier.align(Alignment.TopStart).fillMaxWidth()) {
            GuideBannerNav(state)
            Column(
                Modifier.padding(start = 20.dp, top = 14.dp, end = 64.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                when {
                    state.rerouting -> StatusCard("경로를 다시 찾는 중…", NavCaution)
                    state.offRoute -> StatusCard("경로를 벗어났어요", NavCaution)
                }
                proximityWarning?.let { StatusCard(it, NavDanger) }
                SpeedBadge(state.speedKmh)
            }
        }

        RouteProgressBar(
            progress = state.progress,
            hazardMarks = state.hazardMarks,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 20.dp)
                .width(14.dp)
                .height(260.dp),
        )

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            toast?.let {
                Text(
                    it,
                    color = Soft.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .padding(bottom = 14.dp)
                        .softRaised(NavPillShape, offset = 3.dp, blur = 6.dp)
                        .clip(NavPillShape)
                        .background(Soft.Base)
                        .padding(horizontal = 22.dp, vertical = 11.dp),
                )
            }
            BottomInfoCard(state, onReroute = onReroute, onEnd = { confirmEnd = true })
        }
    }

    if (confirmEnd) EndGuideDialog(onDismiss = { confirmEnd = false }, onEnd = { confirmEnd = false; onEnd() })
}

/** NavigationTracker.Maneuver 이름 → 디자인 팀 화살표 (살짝 꺾는 방향은 같은 쪽 화살표, 도착은 핀) */
private fun maneuverIcon(maneuver: String?): ImageVector = when (maneuver) {
    "LEFT", "SLIGHT_LEFT" -> NavIcons.TurnLeft
    "RIGHT", "SLIGHT_RIGHT" -> NavIcons.TurnRight
    "UTURN" -> NavIcons.UTurn
    "ARRIVE" -> NavIcons.Pin
    else -> NavIcons.Straight
}

/** "80m 후 우회전" 처럼 거리 뒤에 붙는 말 */
private fun afterText(maneuver: String?, label: String): String = when (maneuver) {
    "STRAIGHT", "START", null -> " 직진"
    "ARRIVE" -> " 후 도착"
    else -> " 후 $label"
}

/** 디자인 팀 GuideBanner 모양: 위쪽 전체 폭, 바탕색 배너 + 아래 그림자, 왼쪽 오목한 칸에 화살표 */
@Composable
private fun GuideBannerNav(state: NavUiState) {
    val detail = when {
        !state.hasFix -> "GPS 신호를 기다리고 있어요"
        state.nextManeuver != null ->
            "그다음 ${distanceText(state.nextDistance)}${afterText(state.nextManeuver, labelOf(state.nextManeuver))}"
        state.instruction.isNotBlank() && state.instruction != state.maneuverLabel -> state.instruction
        else -> null
    }
    val remaining = if (state.hasFix) {
        "도착까지 ${distanceText(state.remainingMeters)} · 약 ${(state.remainingSeconds + 59) / 60}분"
    } else null
    Row(
        Modifier
            .fillMaxWidth()
            .softRaised(RectangleShape)
            .background(Soft.Base)
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 18.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "길안내: ${distanceText(state.distanceToNext)}${afterText(state.maneuver, state.maneuverLabel)}"
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(ARROW_WELL)
                .clip(ArrowWellShape)
                .background(Soft.Base)
                .softInset(ArrowWellShape),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = state.maneuver,
                transitionSpec = {
                    (fadeIn(tween(200, delayMillis = 60)) +
                        scaleIn(tween(320, delayMillis = 60, easing = FastOutSlowInEasing), initialScale = 0.6f)) togetherWith
                        (fadeOut(tween(140)) + scaleOut(tween(140), targetScale = 1.2f)) using
                        SizeTransform(clip = false)
                },
                label = "bannerTurn",
            ) { m ->
                Icon(maneuverIcon(m), contentDescription = null, tint = Soft.Accent, modifier = Modifier.size(BANNER_ICON))
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            if (!state.hasFix) {
                Text("현재 위치 확인 중…", color = Soft.Text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            } else {
                Row {
                    Text(distanceText(state.distanceToNext), color = Soft.Text, fontSize = 28.sp,
                        fontWeight = FontWeight.Bold, modifier = Modifier.alignByBaseline())
                    Text(afterText(state.maneuver, state.maneuverLabel), color = Soft.Text, fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold, modifier = Modifier.alignByBaseline())
                }
            }
            detail?.let {
                Spacer(Modifier.size(2.dp))
                Text(it, color = Soft.TextSub, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            remaining?.let {
                Spacer(Modifier.size(2.dp))
                Text(it, color = Soft.TextHint, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** 경로 이탈 · 사고 위험 알림: 볼록한 카드, 왼쪽 오목한 칸 안 경고 아이콘, 글자는 상태 색 */
@Composable
private fun StatusCard(text: String, color: Color) {
    Row(
        Modifier
            .widthIn(max = 320.dp)
            .softRaised(NavCardShape, offset = 3.dp, blur = 6.dp)
            .clip(NavCardShape)
            .background(Soft.Base)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(30.dp).clip(SmallWellShape).background(Soft.Base).softInset(SmallWellShape, offset = 2.dp, blur = 3.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = color, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(text, color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** 현재 속도: 볼록한 동그라미 안 오목한 동그라미 (디자인 팀 로그인 화면 아이콘 모양) */
@Composable
private fun SpeedBadge(speedKmh: Int) {
    Box(
        Modifier.size(68.dp).softRaised(CircleShape).clip(CircleShape).background(Soft.Base),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.size(54.dp).clip(CircleShape).background(Soft.Base).softInset(CircleShape, offset = 2.dp, blur = 4.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(if (speedKmh >= 0) "$speedKmh" else "–", color = Soft.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("km/h", color = Soft.TextSub, fontSize = 9.sp)
        }
    }
}

/** 아래 카드: 재탐색 · 도착지 / 도착 예정 시각 · 남은 거리 · 종료 */
@Composable
private fun BottomInfoCard(state: NavUiState, onReroute: () -> Unit, onEnd: () -> Unit) {
    val arrival = remember(state.remainingSeconds) {
        SimpleDateFormat("a h:mm", Locale.KOREA).format(Date(System.currentTimeMillis() + state.remainingSeconds * 1000L))
    }
    Row(
        Modifier
            .fillMaxWidth()
            .softRaised(NavCardShape)
            .clip(NavCardShape)
            .background(Soft.Base)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavIconButton(onClick = { if (!state.rerouting) onReroute() }) {
            Icon(Icons.Filled.Refresh, contentDescription = "경로 재탐색",
                tint = if (state.rerouting) Soft.TextHint else Soft.Accent, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(NavIcons.Pin, contentDescription = null, tint = Soft.Accent, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(3.dp))
                Text(state.destination.ifEmpty { "목적지" }, color = Soft.TextSub, fontSize = 13.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(arrival, color = Soft.Text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(" 도착", color = Soft.TextSub, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
                Spacer(Modifier.width(12.dp))
                Text(distanceText(state.remainingMeters), color = Soft.Text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
        NavIconButton(onClick = onEnd) {
            Icon(Icons.Filled.Close, contentDescription = "안내 종료", tint = Soft.TextSub, modifier = Modifier.size(22.dp))
        }
    }
}

/**
 * 오른쪽 세로 진행 막대: 오목한 길 위에 남은 경로를 강조색으로 (아래가 출발, 위가 도착).
 * 빨간 점은 경로 주변 과거 사고다발지역.
 */
@Composable
private fun RouteProgressBar(progress: Float, hazardMarks: List<Float>, modifier: Modifier = Modifier) {
    Box(modifier.clip(NavPillShape).background(Soft.Base).softInset(NavPillShape, offset = 2.dp, blur = 3.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val p = progress.coerceIn(0f, 1f)
            fun y(f: Float) = h * (1f - f)
            val accent = Soft.Accent
            drawLine(accent, Offset(w / 2, w / 2), Offset(w / 2, (y(p) - w / 2).coerceAtLeast(w / 2)),
                strokeWidth = w * 0.45f, cap = StrokeCap.Round)
            hazardMarks.forEach { f ->
                if (f >= p) drawCircle(NavDanger, radius = w * 0.32f, center = Offset(w / 2, y(f).coerceIn(w / 2, h - w / 2)))
            }
            val c = Offset(w / 2, y(p).coerceIn(w / 2, h - w / 2))
            drawCircle(Soft.Base, radius = w * 0.62f, center = c)
            drawCircle(accent, radius = w * 0.38f, center = c)
        }
    }
}

/** 안내 종료 확인: 디자인 팀 팝업과 같은 바탕 · 둥근 모서리 */
@Composable
private fun EndGuideDialog(onDismiss: () -> Unit, onEnd: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Soft.Base).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text("길안내를 끝낼까요?", color = Soft.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("지금까지 따라온 경로 안내가 종료돼요.", color = Soft.TextSub, fontSize = 14.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text("계속 안내", color = Soft.TextSub, fontSize = 14.sp,
                    modifier = Modifier.clickable(onClick = onDismiss).padding(10.dp))
                Text("종료", color = Soft.Accent, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(onClick = onEnd).padding(10.dp))
            }
        }
    }
}

// ───────────────────────── 작은 부품 ─────────────────────────

/** 누르면 오목하게 들어가는 동그란 버튼 (디자인 팀 SoftIconButton 과 같은 모양) */
@Composable
private fun NavIconButton(onClick: () -> Unit, size: Dp = 44.dp, content: @Composable () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 1f else 0f, tween(PRESS_MS), label = "press")
    Box(
        Modifier
            .size(size)
            .softPressable(CircleShape) { press }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** 가로로 꽉 찬 버튼 (디자인 팀 SoftWideButton 과 같은 모양) */
@Composable
private fun NavWideButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 1f else 0f, tween(PRESS_MS), label = "press")
    val shape: Shape = RoundedCornerShape(16.dp)
    Box(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .softPressable(shape) { press }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Soft.Accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** NavigationTracker.labelOf 와 같은 짧은 안내 (그다음 안내 글자용) */
private fun labelOf(maneuver: String?): String = when (maneuver) {
    "LEFT" -> "좌회전"
    "RIGHT" -> "우회전"
    "SLIGHT_LEFT" -> "왼쪽 방향"
    "SLIGHT_RIGHT" -> "오른쪽 방향"
    "UTURN" -> "유턴"
    "ARRIVE" -> "목적지"
    else -> "직진"
}

/** 거리 글자 (디자인 팀 formatDistance 와 같은 규칙): 47m · 120m · 1.2km */
private fun distanceText(meters: Int): String {
    val m = meters.coerceAtLeast(0)
    return when {
        m >= 1000 -> "${m / 1000}.${m % 1000 / 100}km"
        m >= 50 -> "${(m + 5) / 10 * 10}m"
        else -> "${m}m"
    }
}
