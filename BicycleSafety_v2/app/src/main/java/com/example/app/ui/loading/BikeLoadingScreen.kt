package com.example.app.ui.loading

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.bicyclesafety.R
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer

private val BgBlue = Color(0xFF1A5FD6)
private val SkyBlue = Color(0xFF87CEFA)

private val EaseOutEnter = CubicBezierEasing(0.2f, 0.7f, 0.3f, 1f)
private val EaseInExit = CubicBezierEasing(0.6f, 0f, 0.9f, 0.4f)

private const val CYCLE_MS = 3600
private const val ENTER_END = 1200   // 왼쪽에서 등장 완료
private const val HOLD_END = 2100    // 대기 끝
private const val EXIT_END = 3240    // 오른쪽으로 퇴장 완료 (이후 잠깐 빈 화면)

// 자전거 좌표계 (SVG 단위). 앞바퀴 허브 = (700, 600), 바퀴 반지름 = 170
private const val FRONT_HUB_X = 700f
private const val REAR_HUB_X = 120f
private const val HUB_Y = 600f
private const val WHEEL_R = 170f

// 화면 배치: 앞바퀴 허브가 화면 (22%, 63%) 지점, 자전거 960단위 = 화면 너비의 175%
private const val HUB_FRACTION_X = 0.22f
private const val HUB_FRACTION_Y = 0.63f
private const val UNIT_PER_WIDTH = 1.75f / 960f

// 한 번만 만들어 두고 재사용 (매 프레임 새로 만들지 않음)
private val BikeStroke = Stroke(width = 9f, cap = StrokeCap.Round, join = StrokeJoin.Round)
private val FramePath = framePath()
private val SpokesPath = spokesPath()

/**
 * 로딩화면: 파란 바탕 + 우하단 하늘색 그라데이션,
 * 흰색 자전거가 왼쪽에서 들어와 잠시 멈췄다가 오른쪽으로 사라짐.
 *
 * [isLoading] 이 true 인 동안 계속 반복하고, false 가 되면
 * 지금 지나가는 자전거가 화면 밖으로 나간 뒤 [onFinished] 를 호출.
 *
 * 성능: 자전거 그림은 처음 한 번만 그려 두고, 애니메이션 중에는
 * 레이어의 이동(translationX)·회전(rotationZ) 값만 바꿈.
 * → 매 프레임 재구성·재배치·다시 그리기 없음.
 */
@Composable
fun BikeLoadingScreen(
    modifier: Modifier = Modifier,
    isLoading: Boolean = true,
    onFinished: () -> Unit = {},
) {
    val time = remember { Animatable(0f) }
    val loading by rememberUpdatedState(isLoading)
    val finished by rememberUpdatedState(onFinished)

    LaunchedEffect(Unit) {
        while (true) {
            time.snapTo(0f)
            // 등장 → 대기 → 퇴장
            time.animateTo(EXIT_END.toFloat(), tween(EXIT_END, easing = LinearEasing))
            if (!loading) break
            // 빈 화면 잠깐 → 다음 바퀴
            time.animateTo(CYCLE_MS.toFloat(), tween(CYCLE_MS - EXIT_END, easing = LinearEasing))
        }
        finished()
    }

    Box(
        modifier
            .fillMaxSize()
            // 배경: 그라데이션 브러시는 화면 크기가 바뀔 때만 새로 만듦
            .drawWithCache {
                val glow = Brush.radialGradient(
                    colors = listOf(SkyBlue.copy(alpha = 0.55f), SkyBlue.copy(alpha = 0f)),
                    center = Offset(size.width, size.height),
                    radius = maxOf(size.width * 1.2f, size.height * 0.6f),
                )
                onDrawBehind {
                    drawRect(BgBlue)
                    drawRect(glow)
                }
            }
    ) {
        // Final identity asset from UI/UX team. Animation remains unchanged.
        Image(
            painter = painterResource(R.drawable.bike_logo_white),
            contentDescription = "자전거 안전 로고",
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding()
                .padding(top = 42.dp).size(88.dp),
        )
        // 자전거 전체: 가로 이동은 레이어 속성으로만 처리
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { translationX = bikeShift(time.value) * size.width }
        ) {
            // 프레임·바퀴 테두리 (움직이지 않는 부분)
            Spacer(
                Modifier
                    .fillMaxSize()
                    .drawBike {
                        drawPath(FramePath, Color.White, style = BikeStroke)
                        drawRimAndHub(REAR_HUB_X)
                        drawRimAndHub(FRONT_HUB_X)
                    }
            )
            WheelSpokes(REAR_HUB_X) { wheelAngle(time.value) }
            WheelSpokes(FRONT_HUB_X) { wheelAngle(time.value) }
        }
    }
}

/** 바퀴살: 따로 레이어에 그려 두고, 허브를 중심으로 레이어째 회전 */
@Composable
private fun WheelSpokes(hubX: Float, angle: () -> Float) {
    val pivot = remember(hubX) {
        TransformOrigin(HUB_FRACTION_X + (hubX - FRONT_HUB_X) * UNIT_PER_WIDTH, HUB_FRACTION_Y)
    }
    Spacer(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                transformOrigin = pivot
                rotationZ = angle()
            }
            .drawBike {
                translate(hubX, HUB_Y) {
                    drawPath(SpokesPath, Color.White, style = BikeStroke)
                }
            }
    )
}

/** 자전거 좌표계(SVG 단위)로 그리기. 배율은 화면 크기가 바뀔 때만 다시 계산 */
private fun Modifier.drawBike(block: DrawScope.() -> Unit): Modifier = drawWithCache {
    val unit = size.width * UNIT_PER_WIDTH
    val originX = size.width * HUB_FRACTION_X - FRONT_HUB_X * unit
    val originY = size.height * HUB_FRACTION_Y - HUB_Y * unit
    onDrawBehind {
        withTransform({
            translate(originX, originY)
            scale(unit, unit, pivot = Offset.Zero)
        }, block)
    }
}

private fun DrawScope.drawRimAndHub(hubX: Float) {
    val hub = Offset(hubX, HUB_Y)
    drawCircle(Color.White, WHEEL_R, hub, style = BikeStroke)
    drawCircle(Color.White, 9f, hub, style = BikeStroke)
}

/** 시간(ms) → 화면 너비 대비 가로 위치 */
private fun bikeShift(t: Float): Float = when {
    t < ENTER_END -> -1.10f + 1.10f * EaseOutEnter.transform(t / ENTER_END)
    t < HOLD_END -> 0f
    t < EXIT_END -> 2.30f * EaseInExit.transform((t - HOLD_END) / (EXIT_END - HOLD_END))
    else -> 2.30f
}

/** 시간(ms) → 바퀴 회전 각도 (이동 거리에 맞춤, 멈춰 있을 땐 정지) */
private fun wheelAngle(t: Float): Float = when {
    t < ENTER_END -> 200f * EaseOutEnter.transform(t / ENTER_END)
    t < HOLD_END -> 200f
    t < EXIT_END -> 200f + 420f * EaseInExit.transform((t - HOLD_END) / (EXIT_END - HOLD_END))
    else -> 620f
}

/** 허브 (0,0) 기준의 곡선 바퀴살 */
private fun spokesPath() = Path().apply {
    moveTo(0f, -30f);   relativeQuadraticTo(-8f, -55f, 18f, -110f)
    moveTo(28f, 12f);   relativeQuadraticTo(50f, 18f, 92f, 62f)
    moveTo(-20f, 20f);  relativeQuadraticTo(-35f, 40f, -45f, 95f)
    moveTo(-28f, -10f); relativeQuadraticTo(-50f, -12f, -95f, -55f)
    moveTo(15f, 32f);   relativeQuadraticTo(5f, 50f, -8f, 100f)
}

/** 프레임 + 안장 + 핸들바 */
private fun framePath() = Path().apply {
    fun line(x1: Float, y1: Float, x2: Float, y2: Float) { moveTo(x1, y1); lineTo(x2, y2) }

    line(700f, 600f, 640f, 260f) // 포크
    line(640f, 260f, 615f, 95f)  // 헤드튜브/스템
    line(630f, 210f, 330f, 230f) // 탑튜브
    line(636f, 245f, 390f, 600f) // 다운튜브
    line(390f, 600f, 317f, 170f) // 시트튜브 + 시트포스트 (안장 밑면까지)
    line(390f, 600f, 120f, 600f) // 체인스테이
    line(330f, 230f, 120f, 600f) // 시트스테이
    addOval(Rect(Offset(390f, 600f), 38f)) // 체인링

    // 안장
    moveTo(255f, 160f)
    quadraticTo(300f, 148f, 375f, 160f)
    quadraticTo(340f, 172f, 290f, 170f)
    close()

    // 드롭 핸들바 (왼쪽 끝 → 바 → 오른쪽 끝을 한 획으로)
    moveTo(512f, 137f)
    quadraticTo(488f, 135f, 488f, 117f)
    quadraticTo(490f, 95f, 520f, 95f)
    lineTo(720f, 95f)
    quadraticTo(750f, 95f, 752f, 117f)
    quadraticTo(752f, 135f, 726f, 139f)
}
