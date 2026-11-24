package com.example.app.ui.guide

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.ui.theme.Soft
import com.example.app.ui.theme.softInset
import com.example.app.ui.theme.softRaised

private val PIN = 46.dp                    // 핀 아이콘 크기 (핀 끝은 아이콘 위에서 22/24 지점)
private const val MOVE_MS = 900            // 위치가 바뀔 때 핀이 옮겨 가는 시간 (GPS 는 보통 1초마다)
private const val JUMP_M = 300.0           // 이보다 멀리 바뀌면 옮겨 가지 않고 바로 그 자리에

private val TURN_BOX = 88.dp
private val TURN_ICON = 44.dp
private val TurnShape = RoundedCornerShape(24.dp)

/** 핀이 지금 그려지는 자리: [from] → [to] 로 [progress] 만큼 */
private class PinMotion {
    var from by mutableStateOf<GeoPoint?>(null)
    var to by mutableStateOf<GeoPoint?>(null)
    val progress = Animatable(1f)

    fun current(): GeoPoint? {
        val t = to ?: return null
        val f = from ?: return t
        val p = progress.value.toDouble()
        return GeoPoint(f.latitude + (t.latitude - f.latitude) * p, f.longitude + (t.longitude - f.longitude) * p)
    }
}

/**
 * 내 위치 핀: 핀 끝이 내 위치. [location] 이 바뀌면 그 자리로 부드럽게 옮겨 감, null 이면 숨김.
 * [toScreen] 은 위도·경도를 지도 영역 안 화면 위치로 바꾸는 함수 (임시 지도는 [TempMap.toScreen]).
 * [pulse] 가 0 → 1 로 가는 동안 바닥에서 고리가 퍼짐 (현재 위치 버튼). 위치·고리는 그리기 단계에서만 읽음.
 */
@Composable
fun MyLocationPin(
    location: () -> GeoPoint?,
    toScreen: (GeoPoint, Size) -> Offset?,
    pulse: () -> Float,
    modifier: Modifier = Modifier,
) {
    val pin = rememberVectorPainter(NavIcons.Pin)
    val motion = remember { PinMotion() }
    val target = location()
    LaunchedEffect(target) {
        val now = motion.current()
        if (target == null || now == null || distanceMeters(now, target) > JUMP_M) {
            motion.from = null
            motion.to = target
            motion.progress.snapTo(1f)
        } else {
            motion.from = now
            motion.to = target
            motion.progress.snapTo(0f)
            motion.progress.animateTo(1f, tween(MOVE_MS, easing = LinearEasing))
        }
    }
    Spacer(
        modifier
            .graphicsLayer()
            .drawWithCache {
                val s = PIN.toPx()
                val tipFromTopLeft = Offset(s / 2f, s * 22f / 24f)
                val hole = Offset(s * 12f / 24f, s * 9.5f / 24f)
                val range = 30.dp.toPx()
                val ring0 = 8.dp.toPx()
                val ringGrow = 46.dp.toPx()
                val shadowR = 11.dp.toPx()
                onDrawBehind {
                    val here = motion.current() ?: return@onDrawBehind
                    val tip = toScreen(here, size) ?: return@onDrawBehind
                    val p = pulse()
                    val accent = Soft.Accent
                    drawCircle(accent.copy(alpha = 0.10f), radius = range, center = tip)
                    if (p < 1f) drawCircle(accent.copy(alpha = 0.30f * (1f - p)), radius = ring0 + ringGrow * p, center = tip)
                    // 바닥 그림자: 납작한 타원
                    scale(1f, 0.38f, pivot = tip) {
                        drawCircle(
                            Brush.radialGradient(
                                listOf(Soft.Dark.copy(alpha = if (Soft.isBlack) 0.9f else 0.55f), Color.Transparent),
                                center = tip, radius = shadowR,
                            ),
                            radius = shadowR, center = tip,
                        )
                    }
                    val topLeft = tip - tipFromTopLeft
                    drawCircle(Color.White, radius = s * 3.4f / 24f, center = topLeft + hole)   // 핀 가운데 흰 점
                    translate(topLeft.x, topLeft.y) {
                        with(pin) { draw(Size(s, s), colorFilter = ColorFilter.tint(accent)) }
                    }
                }
            }
    )
}

/**
 * 화살표 칸: 다음 안내의 방향 화살표와 남은 거리. [next] 가 null 이면 사라짐.
 * 방향이 바뀌면 새 화살표가 커지며 나타나고, 거리는 숫자만 바뀜.
 */
@Composable
fun TurnBox(next: () -> Maneuver?, modifier: Modifier = Modifier) {
    val current = next()
    val last = remember { arrayOfNulls<Maneuver>(1) }      // 사라지는 동안 보여 줄 마지막 안내
    if (current != null) last[0] = current
    val shown by animateFloatAsState(if (current != null) 1f else 0f, tween(260, easing = FastOutSlowInEasing), label = "turnBox")
    val m = current ?: last[0]
    if (shown <= 0f || m == null) return
    Column(
        modifier
            .graphicsLayer {
                alpha = shown
                scaleX = 0.85f + 0.15f * shown
                scaleY = 0.85f + 0.15f * shown
                compositingStrategy = CompositingStrategy.ModulateAlpha   // 흐려지는 동안 그림자가 잘리지 않게
            }
            .size(TURN_BOX)
            .softRaised(TurnShape)
            .clip(TurnShape)
            .background(Soft.Base)
            .semantics { contentDescription = "다음 안내: ${m.distanceText} 후 ${m.turn.label}" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AnimatedContent(
            targetState = m.turn,
            transitionSpec = {
                (fadeIn(tween(200, delayMillis = 60)) +
                    scaleIn(tween(320, delayMillis = 60, easing = FastOutSlowInEasing), initialScale = 0.6f)) togetherWith
                    (fadeOut(tween(140)) + scaleOut(tween(140), targetScale = 1.2f)) using
                    SizeTransform(clip = false)
            },
            label = "turn",
        ) { turn ->
            Icon(turn.icon, contentDescription = null, tint = Soft.Accent, modifier = Modifier.size(TURN_ICON))
        }
        Spacer(Modifier.size(2.dp))
        Text(m.distanceText, color = Soft.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ───────────────────────── 위쪽 배너 ─────────────────────────

/** 길안내를 어디에 보여 줄지: 위쪽 배너 (화살표 + 자세한 안내) 또는 오른쪽 위 작은 칸 (화살표 + 거리) */
enum class GuideStyle { Banner, Box }

private val BannerShape = RectangleShape                // 모서리 둥글림 없음
private val ArrowWellShape = RoundedCornerShape(20.dp)
private val ARROW_WELL = 72.dp
private val BANNER_ICON = 48.dp

/**
 * 위쪽 길안내 배너: 왼쪽에 방향 화살표, 오른쪽에 자세한 안내
 * (남은 거리와 방향 · 들어갈 길 이름 또는 그다음 안내 · 도착까지 거리와 시간).
 * [guide] 에 다음 안내가 없으면 위로 들어가며 사라짐.
 */
@Composable
fun GuideBanner(guide: GuideState, modifier: Modifier = Modifier) {
    val current = guide.next
    val last = remember { arrayOfNulls<Maneuver>(1) }      // 사라지는 동안 보여 줄 마지막 안내
    if (current != null) last[0] = current
    val shown by animateFloatAsState(if (current != null) 1f else 0f, tween(340, easing = FastOutSlowInEasing), label = "banner")
    val m = current ?: last[0]
    if (shown <= 0f || m == null) return
    val detail = m.road?.let { "$it 방면" } ?: guide.following?.let { "그다음 ${it.text}" }
    val remaining = guide.remainingMeters?.let { "도착까지 ${formatDistance(it)} · 약 ${guide.remainingMinutes}분" }
    Row(
        modifier
            .fillMaxWidth()
            .graphicsLayer { translationY = -size.height * 1.2f * (1f - shown) }
            .dropShadow(
                BannerShape,
                Shadow(radius = 18.dp, color = Soft.Dark, offset = DpOffset(0.dp, 6.dp), alpha = if (Soft.isBlack) 0.9f else 0.45f),
            )
            .clip(BannerShape)
            .background(Soft.Base)
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 18.dp)
            .semantics(mergeDescendants = true) { contentDescription = "길안내: ${m.text}" },
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
                targetState = m.turn,
                transitionSpec = {
                    (fadeIn(tween(200, delayMillis = 60)) +
                        scaleIn(tween(320, delayMillis = 60, easing = FastOutSlowInEasing), initialScale = 0.6f)) togetherWith
                        (fadeOut(tween(140)) + scaleOut(tween(140), targetScale = 1.2f)) using
                        SizeTransform(clip = false)
                },
                label = "bannerTurn",
            ) { turn ->
                Icon(turn.icon, contentDescription = null, tint = Soft.Accent, modifier = Modifier.size(BANNER_ICON))
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Row {
                Text(m.distanceText, color = Soft.Text, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.alignByBaseline())
                Text(
                    if (m.turn == Turn.Straight) " 직진" else " 후 ${m.turn.label}",
                    color = Soft.Text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.alignByBaseline(),
                )
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
