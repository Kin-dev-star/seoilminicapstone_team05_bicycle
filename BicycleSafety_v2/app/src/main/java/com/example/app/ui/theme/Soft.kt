package com.example.app.ui.theme

import android.graphics.BlurMaskFilter
import androidx.compose.foundation.background
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.roundToInt

/** 테마 하나의 색 묶음 */
class SoftPalette(
    val base: Color,        // 바탕
    val light: Color,       // 밝은 그림자 (왼쪽 위)
    val dark: Color,        // 어두운 그림자 (오른쪽 아래)
    val darkAlpha: Float,   // 어두운 그림자 진하기 (볼록)
    val insetAlpha: Float,  // 어두운 그림자 진하기 (오목)
    val text: Color,
    val textSub: Color,
    val textHint: Color,
    val accent: Color,
    val mapGrid: Color,
    val mapRoad: Color,
    val mapPark: Color,
)

private val LightPalette = SoftPalette(
    base = Color(0xFFF0F0F3), light = Color(0xFFFFFFFF), dark = Color(0xFFAEAEC0),
    darkAlpha = 0.5f, insetAlpha = 0.6f,
    text = Color(0xFF2B2D33), textSub = Color(0xFF6E7383), textHint = Color(0xFFA2A7B3),
    accent = Color(0xFF1A5FD6),
    mapGrid = Color(0xFFE4E5EA), mapRoad = Color(0xFFFFFFFF), mapPark = Color(0xFFE1EAE2),
)

private val BlackPalette = SoftPalette(
    base = Color(0xFF1A1B1F), light = Color(0xFF2C2F36), dark = Color(0xFF040405),
    darkAlpha = 0.9f, insetAlpha = 0.85f,
    text = Color(0xFFE8E9ED), textSub = Color(0xFF9CA1AE), textHint = Color(0xFF6C717D),
    accent = Color(0xFF4D8DFF),
    mapGrid = Color(0xFF222429), mapRoad = Color(0xFF2B2E35), mapPark = Color(0xFF1B2620),
)

/**
 * 소프트 UI (뉴모피즘) 스타일.
 * 바탕과 같은 색의 요소에 왼쪽 위는 밝은 그림자, 오른쪽 아래는 어두운 그림자를 넣어
 * 바탕에서 살짝 튀어나오거나(볼록) 눌려 들어간(오목) 것처럼 보이게 함.
 * [isBlack] 을 바꾸면 이 색들을 읽는 화면이 자동으로 다시 그려짐.
 */
object Soft {
    /** 블랙 테마 여부 (설정 화면에서 바꿈) */
    var isBlack by mutableStateOf(false)
    private val p: SoftPalette get() = if (isBlack) BlackPalette else LightPalette

    val Base: Color get() = p.base
    val Light: Color get() = p.light
    val Dark: Color get() = p.dark
    val DarkAlpha: Float get() = p.darkAlpha
    val InsetAlpha: Float get() = p.insetAlpha
    val Text: Color get() = p.text
    val TextSub: Color get() = p.textSub
    val TextHint: Color get() = p.textHint
    val Accent: Color get() = p.accent
    val MapGrid: Color get() = p.mapGrid
    val MapRoad: Color get() = p.mapRoad
    val MapPark: Color get() = p.mapPark

    val RaiseOffset = 5.dp             // 볼록 그림자 거리
    val RaiseBlur = 10.dp              // 볼록 그림자 흐림
    val InsetOffset = 3.dp             // 오목 그림자 거리
    val InsetBlur = 5.dp               // 오목 그림자 흐림
}

/** 볼록하게 튀어나온 모양 (크기가 고정된 요소용) */
fun Modifier.softRaised(shape: Shape, offset: Dp = Soft.RaiseOffset, blur: Dp = Soft.RaiseBlur): Modifier = this
    .dropShadow(shape, Shadow(radius = blur, color = Soft.Light, offset = DpOffset(-offset, -offset)))
    .dropShadow(shape, Shadow(radius = blur, color = Soft.Dark, offset = DpOffset(offset, offset), alpha = Soft.DarkAlpha))

/** 오목하게 들어간 모양 (입력칸 등). clip·background 뒤에 붙임 */
fun Modifier.softInset(shape: Shape, offset: Dp = Soft.InsetOffset, blur: Dp = Soft.InsetBlur): Modifier = this
    .innerShadow(shape, Shadow(radius = blur, color = Soft.Dark, offset = DpOffset(offset, offset), alpha = Soft.InsetAlpha))
    .innerShadow(shape, Shadow(radius = blur, color = Soft.Light, offset = DpOffset(-offset, -offset)))

/**
 * 누르면 들어가는 버튼 모양 (크기가 고정된 요소용).
 * [press] 0 = 볼록, 1 = 눌려서 오목. 값은 그리기 단계에서만 읽음.
 */
fun Modifier.softPressable(shape: Shape, press: () -> Float): Modifier = this
    .dropShadow(shape) {
        radius = Soft.RaiseBlur.toPx()
        color = Soft.Light
        offset = Offset(-Soft.RaiseOffset.toPx(), -Soft.RaiseOffset.toPx())
        alpha = 1f - press()
    }
    .dropShadow(shape) {
        radius = Soft.RaiseBlur.toPx()
        color = Soft.Dark
        offset = Offset(Soft.RaiseOffset.toPx(), Soft.RaiseOffset.toPx())
        alpha = Soft.DarkAlpha * (1f - press())
    }
    .clip(shape)
    .background(Soft.Base)
    .softPressedInset(shape, press)

/** 눌렸을 때만 보이는 오목 그림자 */
fun Modifier.softPressedInset(shape: Shape, press: () -> Float): Modifier = this
    .innerShadow(shape) {
        radius = Soft.InsetBlur.toPx() * 1.6f
        color = Soft.Dark
        offset = Offset(Soft.InsetOffset.toPx(), Soft.InsetOffset.toPx())
        alpha = Soft.InsetAlpha * 1.15f * press()
    }
    .innerShadow(shape) {
        radius = Soft.InsetBlur.toPx() * 1.6f
        color = Soft.Light
        offset = Offset(-Soft.InsetOffset.toPx(), -Soft.InsetOffset.toPx())
        alpha = press()
    }

/**
 * 둥근 네모의 볼록 그림자를 9조각으로 늘려 그리는 도구.
 * [base] 크기(모서리 [corner]) 그림자를 한 번만 그려 두고 가운데 한 줄을 늘려서 더 큰 사각형에도 씀.
 * → 크기가 매 프레임 바뀌는 카드에도 그림자 비트맵을 다시 만들지 않음.
 * 흐림 계산은 Compose 의 dropShadow 와 같아서, 같은 크기에서는 모양이 똑같음.
 */
@Stable
class SoftShadow(density: Density, base: Dp, corner: Dp) {
    private val offset: Float
    private val extent: Int
    private val bitmap: ImageBitmap
    private val half: Int
    private var filterBlack: Boolean? = null
    private var lightFilter = ColorFilter.tint(Soft.Light)
    private var darkFilter = ColorFilter.tint(Soft.Dark)

    /** 테마가 바뀌었으면 그림자 색 필터를 새로 만듦 (그리기 중에 읽으므로 테마가 바뀌면 다시 그려짐) */
    private fun syncTheme() {
        val black = Soft.isBlack
        if (black != filterBlack) {
            filterBlack = black
            lightFilter = ColorFilter.tint(Soft.Light)
            darkFilter = ColorFilter.tint(Soft.Dark)
        }
    }

    init {
        with(density) {
            offset = Soft.RaiseOffset.toPx()
            val blur = Soft.RaiseBlur.toPx()
            extent = ceil(blur * 2f).toInt()        // 흐림이 퍼지는 거리
            var s = base.roundToPx()
            if (s % 2 == 0) s += 1                  // 늘릴 가운데 1px 줄이 정확히 가운데 오도록
            val n = s + extent * 2
            bitmap = ImageBitmap(n, n)
            val paint = Paint().apply { color = Color.White }
            paint.asFrameworkPaint().maskFilter = BlurMaskFilter(blur, BlurMaskFilter.Blur.NORMAL)
            val r = corner.toPx()
            Canvas(bitmap).drawRoundRect(
                extent.toFloat(), extent.toFloat(), (extent + s).toFloat(), (extent + s).toFloat(), r, r, paint,
            )
            half = (n - 1) / 2
        }
    }

    /** [rect](요소 영역)의 볼록 그림자: 왼쪽 위 밝게, 오른쪽 아래 어둡게 */
    fun drawRaised(scope: DrawScope, rect: Rect, alpha: Float = 1f) {
        if (alpha <= 0f) return
        syncTheme()
        drawSliced(scope, rect, -offset, -offset, lightFilter, alpha)
        drawSliced(scope, rect, offset, offset, darkFilter, alpha * Soft.DarkAlpha)
    }

    /** 화면 아래에서 올라온 판(바닥 창): 위쪽 가장자리를 따라 은은한 그림자 */
    fun drawLifted(scope: DrawScope, rect: Rect, alpha: Float = 1f) {
        if (alpha <= 0f) return
        syncTheme()
        drawSliced(scope, rect, 0f, -offset * 0.6f, darkFilter, alpha * (if (Soft.isBlack) 0.8f else 0.45f))
    }

    private fun drawSliced(scope: DrawScope, rect: Rect, shiftX: Float, shiftY: Float, filter: ColorFilter, alpha: Float) {
        val l = (rect.left + shiftX).roundToInt() - extent
        val t = (rect.top + shiftY).roundToInt() - extent
        val r = (rect.right + shiftX).roundToInt() + extent
        val b = (rect.bottom + shiftY).roundToInt() + extent
        val hx = min(half, (r - l) / 2)
        val hy = min(half, (b - t) / 2)
        val n = bitmap.width
        for (col in 0..2) {
            val sx = when (col) { 0 -> 0; 1 -> half; else -> n - half }
            val sw = if (col == 1) 1 else half
            val dx = when (col) { 0 -> l; 1 -> l + hx; else -> r - hx }
            val dw = if (col == 1) r - l - hx * 2 else hx
            if (dw <= 0) continue
            for (row in 0..2) {
                val sy = when (row) { 0 -> 0; 1 -> half; else -> n - half }
                val sh = if (row == 1) 1 else half
                val dy = when (row) { 0 -> t; 1 -> t + hy; else -> b - hy }
                val dh = if (row == 1) b - t - hy * 2 else hy
                if (dh <= 0) continue
                scope.drawImage(
                    image = bitmap,
                    srcOffset = IntOffset(sx, sy),
                    srcSize = IntSize(sw, sh),
                    dstOffset = IntOffset(dx, dy),
                    dstSize = IntSize(dw, dh),
                    alpha = alpha,
                    colorFilter = filter,
                )
            }
        }
    }
}

val LocalSoftShadow = staticCompositionLocalOf<SoftShadow> { error("SoftShadow 가 제공되지 않았어요") }
