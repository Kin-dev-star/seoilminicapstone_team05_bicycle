package com.example.app.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.ui.theme.LocalSoftShadow
import com.example.app.ui.theme.Soft
import com.example.app.ui.RouteUiState
import java.util.Locale
import com.example.app.ui.theme.SoftShadow
import com.example.app.ui.theme.softInset
import com.example.app.ui.theme.softPressable
import com.example.app.ui.theme.softPressedInset
import com.example.app.ui.theme.softRaised
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.roundToInt

// ── 오른쪽 아래 원통 메뉴 ──
private val BAR_H = 64.dp                // 막대 높이
private val PEEK = 16.dp                 // 뒤쪽 막대가 앞 막대 위로 보이는 높이
private val DROP = 24.dp                 // 앞 막대가 아래로 말려 들어갈 때 내려가는 거리
private val DRUM_STEP = 60.dp            // 한 칸 넘기는 데 필요한 드래그 거리
private const val VISIBLE_BACK = 2f      // 앞 막대 뒤로 보이는 막대 수
private const val BACK_SHRINK = 0.06f    // 뒤로 한 칸 갈 때마다 줄어드는 비율 (오른쪽 아래 기준)
private const val BACK_SHADE = 0.10f     // 뒤로 한 칸 갈 때마다 어두워지는 정도
private const val DRUM_WIDTH = 0.8f      // 막대 너비 (화면 너비 대비)
private val DRUM_H = BAR_H + PEEK * VISIBLE_BACK + 8.dp   // 원통 메뉴 영역 높이
private val LOC_BTN = 40.dp             // 현재 위치 버튼 크기
private val LIST_GAP = 12.dp            // 모아보기에서 막대 사이 간격
private const val OVERVIEW_STAGGER = 0.08f   // 모아보기로 펼칠 때 막대마다 늦게 출발하는 정도
/** 모아보기로 펼쳤을 때 막대 목록 전체 높이 */
private val LIST_H: Dp get() = BAR_H * NavItem.entries.size + LIST_GAP * (NavItem.entries.size - 1)
private const val DRUM_FRICTION = 2f     // 관성 마찰 (작을수록 멀리 돎)
private const val SNAP_SPEED = 4f        // 이 속도(초당 칸)보다 느려지면 가장 가까운 막대로 빠르게 맞춤
private const val MAX_SPIN = 8f          // 한 번 튕길 때 최대로 도는 칸 수 (두 바퀴)

// ── 경로 설정 창 ──
private val ROW = 44.dp                  // 출발/도착 한 줄 높이
private val DIVIDER = 2.dp               // 출발·도착 사이 구분선 (어두운 선 1dp + 밝은 선 1dp)
private val FIELD_H = ROW * 2 + DIVIDER
private val SHEET_MIN = 250.dp           // 화면이 작아도 1/3 창이 이보다 작아지지 않게
private val SHEET_CORNER = 28.dp

private val EDGE = 20.dp                 // 화면 가장자리 여백
private val CORNER = 18.dp
private val CardShape = RoundedCornerShape(CORNER)
private val FieldShape = RoundedCornerShape(16.dp)
private val WellShape = RoundedCornerShape(12.dp)
private val PillShape = RoundedCornerShape(percent = 50)
private val DividerDark: Color get() = Soft.Dark.copy(alpha = if (Soft.isBlack) 0.9f else 0.45f)

// 테마(밝은/블랙)에 따라 바뀌는 글자 스타일
private val FieldTextStyle: TextStyle get() = TextStyle(color = Soft.Text, fontSize = 14.sp)
private val CursorBrush: SolidColor get() = SolidColor(Soft.Accent)
private const val PRESS_MS = 110         // 눌림(볼록 → 오목) 전환 시간

/** 원통 메뉴에 차례대로 도는 항목 */
enum class NavItem(val title: String, val subtitle: String, val pageTitle: String, val icon: ImageVector) {
    Route("경로 설정", "출발지 · 도착지 설정", "경로 설정", Icons.Filled.LocationOn),
    Account("계정", "마이페이지", "마이페이지", Icons.Filled.Person),
    Temp1("임시화면1", "임시 화면", "임시화면 1", Icons.Filled.Star),
    Settings("설정", "앱 설정", "설정", Icons.Filled.Settings),
}

/**
 * 메인 화면 (v2): 전체가 지도 영역, 오른쪽 아래에 원통 모양 메뉴.
 * - 막대 하나에 메뉴 하나. 위아래로 밀면 원통이 돌듯 끝없이 넘어감
 * - 경로 설정: 화면 1/3 창이 열리고, 위로 끌어올리면 상세정보
 * - 나머지: 누른 막대가 커지며 화면을 채우고 해당 화면이 나옴
 */
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    mapContent: @Composable (Modifier) -> Unit = { MapPlaceholder(it) },
    onAnalyzeRoute: (String, String, Boolean) -> Unit = { _, _, _ -> },
    onMyLocation: () -> Unit = {},
    onRouteFormChanged: () -> Unit = {},
    routeUiState: RouteUiState = RouteUiState(),
    onStartGuidance: () -> Unit = {},
    proximityWarning: String? = null,
) {
    val density = LocalDensity.current
    val softShadow = remember(density) { SoftShadow(density, base = BAR_H, corner = CORNER) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    // 위치 계산용 참조 (상태가 아니므로 바뀌어도 재구성되지 않음)
    val rootCoords = remember { arrayOfNulls<LayoutCoordinates>(1) }
    var rootSize by remember { mutableStateOf(IntSize.Zero) }
    val statusTop = WindowInsets.statusBars.getTop(density)

    // 계정/임시화면1/설정: 막대가 커지며 화면을 채움
    var openPage by remember { mutableStateOf<NavItem?>(null) }
    var pageFrom by remember { mutableStateOf(Rect.Zero) }
    val pageProgress = remember { Animatable(0f) }

    // 설정 값 (페이지를 닫았다 열어도 유지, 지금은 화면에서만 바뀌는 임시 값)
    val settings = remember { AppSettings() }
    // 위치 버튼은 Java MainActivity의 실제 위치 권한/GPS 요청과 연결합니다.
    // Route form state is hoisted so text survives closing/reopening the menu.
    var routeStart by rememberSaveable { mutableStateOf("") }
    var routeDestination by rememberSaveable { mutableStateOf("") }
    var routeOption by rememberSaveable { mutableIntStateOf(0) }

    // 메뉴 모아보기: 겹쳐진 막대들을 위로 펼쳐서 한 번에 보여줌
    var overview by remember { mutableStateOf(false) }
    val overviewAnim = animateFloatAsState(
        targetValue = if (overview) 1f else 0f,
        animationSpec = tween(if (overview) 420 else 320, easing = FastOutSlowInEasing),
        label = "overview",
    )
    val overviewShown by remember { derivedStateOf { overviewAnim.value > 0f } }
    val drum = remember { DrumHandle() }

    // 경로 설정: 화면 1/3 창, 위로 끌면 상세정보
    var routeOpen by remember { mutableStateOf(false) }
    var routeFrom by remember { mutableStateOf(Rect.Zero) }
    val routeReveal = remember { Animatable(0f) }   // 0 = 막대, 1 = 창
    val sheetTop = remember { Animatable(0f) }       // 창 윗변 위치 (px)
    val screenH = rootSize.height.toFloat()
    val thirdTop = screenH - max(screenH / 3f, with(density) { SHEET_MIN.toPx() })
    val fullTop = statusTop + with(density) { 12.dp.toPx() }
    val lowestTop = screenH - with(density) { 96.dp.toPx() }

    val selectItem: (NavItem, LayoutCoordinates) -> Unit = { item, bar ->
        val root = rootCoords[0]
        if (root != null && bar.isAttached && openPage == null && !routeOpen) {
            focusManager.clearFocus()
            val rect = root.localBoundingBoxOf(bar)
            if (item == NavItem.Route) {
                routeFrom = rect
                routeOpen = true
                scope.launch {
                    sheetTop.snapTo(thirdTop)
                    routeReveal.snapTo(0f)
                    routeReveal.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
                }
            } else {
                pageFrom = rect
                openPage = item
                scope.launch {
                    pageProgress.snapTo(0f)
                    pageProgress.animateTo(1f, tween(450, easing = FastOutSlowInEasing))
                }
            }
        }
    }
    // 닫힐 때는 지금 앞에 있는 막대 자리로 줄어듦 (모아보기에서 골랐으면 그 막대가 앞에 와 있음)
    val frontRect: () -> Rect? = {
        val root = rootCoords[0]
        val bar = drum.frontCoords()
        if (root != null && bar != null && bar.isAttached) root.localBoundingBoxOf(bar) else null
    }
    val closePage: () -> Unit = {
        if (openPage != null) {
            frontRect()?.let { pageFrom = it }
            scope.launch {
                pageProgress.animateTo(0f, tween(380, easing = FastOutSlowInEasing))
                openPage = null
            }
        }
    }
    val closeRoute: () -> Unit = {
        if (routeOpen) {
            focusManager.clearFocus()
            frontRect()?.let { routeFrom = it }
            scope.launch {
                routeReveal.animateTo(0f, tween(360, easing = FastOutSlowInEasing))
                routeOpen = false
            }
        }
    }
    val moveSheet: (Float) -> Unit = { target ->
        scope.launch { sheetTop.animateTo(target, spring(dampingRatio = 0.9f, stiffness = 500f)) }
    }
    // 손을 뗐을 때: 많이 내리면 닫고, 아니면 1/3 또는 전체 중 가까운 쪽으로
    val settleSheet: (Float) -> Unit = { velocity ->
        val top = sheetTop.value
        val closeLine = thirdTop + (screenH - thirdTop) * 0.35f
        when {
            top > closeLine || (velocity > 1500f && top > thirdTop - 1f) -> closeRoute()
            velocity < -800f -> moveSheet(fullTop)
            velocity > 800f -> moveSheet(thirdTop)
            else -> moveSheet(if (top < (thirdTop + fullTop) / 2f) fullTop else thirdTop)
        }
    }

    // 모아보기에서 고르면: 고른 막대 자리에서 바로 열리고, 모아보기가 접힌 뒤 그 메뉴가 앞으로 옴
    val pickFromOverview: (NavItem, LayoutCoordinates) -> Unit = { item, bar ->
        selectItem(item, bar)
        overview = false
        drum.bringToFront(item)
    }

    BackHandler(enabled = openPage != null || routeOpen || overview) {
        when {
            openPage != null -> closePage()
            routeOpen && sheetTop.value < thirdTop - 1f -> moveSheet(thirdTop)   // 상세정보 보는 중이면 1/3로
            routeOpen -> closeRoute()
            else -> overview = false
        }
    }

    CompositionLocalProvider(LocalSoftShadow provides softShadow) {
        Box(
            modifier
                .fillMaxSize()
                .onPlaced { rootCoords[0] = it }
                .onSizeChanged { rootSize = it }
        ) {
            // Real Kakao MapView from Java MainActivity: map touch/zoom/labels are preserved.
            mapContent(Modifier.fillMaxSize())
            // 모아보기 중에는 뒤를 살짝 어둡게, 빈 곳을 누르면 닫힘
            if (overviewShown) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = overviewAnim.value }
                        .background(Soft.Dark.copy(alpha = if (Soft.isBlack) 0.55f else 0.3f))
                        .pointerInput(Unit) { detectTapGestures { overview = false } }
                )
            }

            // GPS 접근 시 화면 경고. 서버 판별이 아닌 과거 사고 지점과의 근접 테스트입니다.
            proximityWarning?.let { warning ->
                Text(
                    warning,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(CardShape)
                        .background(Color(0xFF502626))
                        .padding(12.dp),
                    color = Color.White,
                    fontSize = 13.sp,
                )
            }

            // 원통 메뉴 오른쪽 위 작은 버튼들: 모아보기 · 현재 위치 (모아보기를 펼치면 목록 위로 같이 올라감)
            val lift = with(density) { (LIST_H - DRUM_H).toPx() }
            Row(
                Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = EDGE, bottom = EDGE + DRUM_H + 12.dp)
                    .graphicsLayer { translationY = -lift * overviewAnim.value },
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OverviewButton(open = overview, onClick = { overview = !overview })
                MyLocationButton(active = false, onClick = onMyLocation)
            }

            NavDrum(
                overview = { overviewAnim.value },
                overviewOn = overview,
                handle = drum,
                onPick = pickFromOverview,
                onSelect = selectItem,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = EDGE, bottom = EDGE)
                    .fillMaxWidth(DRUM_WIDTH),
            )

            if (routeOpen) {
                RouteSheet(
                    from = routeFrom,
                    reveal = { routeReveal.value },
                    top = { sheetTop.value },
                    expand = { ((thirdTop - sheetTop.value) / (thirdTop - fullTop)).coerceIn(0f, 1f) },
                    onDrag = { dy -> scope.launch { sheetTop.snapTo((sheetTop.value + dy).coerceIn(fullTop, lowestTop)) } },
                    onDragStopped = settleSheet,
                    onExpand = { moveSheet(fullTop) },
                    onClose = closeRoute,
                    start = routeStart,
                    onStartChange = { routeStart = it; onRouteFormChanged() },
                    dest = routeDestination,
                    onDestChange = { routeDestination = it; onRouteFormChanged() },
                    option = routeOption,
                    onOptionChange = { routeOption = it; onRouteFormChanged() },
                    onAnalyzeRoute = onAnalyzeRoute,
                    routeUiState = routeUiState,
                    onStartGuidance = onStartGuidance,
                )
            }

            openPage?.let { page ->
                // 커지는 화면의 바깥 그림자: 화면 레이어는 모양대로 잘리므로 그 밖(뒤)에 따로 그림
                Spacer(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer()
                        .drawBehind {
                            val p = pageProgress.value
                            if (p < 1f) {
                                val rect = lerp(pageFrom, Rect(Offset.Zero, size), p)
                                softShadow.drawRaised(this, rect, alpha = (p * 6f).coerceAtMost(1f) * (1f - p))
                            }
                        }
                )
                PageOverlay(
                    page = page,
                    settings = settings,
                    from = pageFrom,
                    progress = { pageProgress.value },
                    onBack = closePage,
                )
            }
        }
    }
}

// ───────────────────────── 원통 메뉴 ─────────────────────────

private enum class BarRole { Front, Back, Hidden }

/** 원통 메뉴를 바깥(메인 화면)에서 다루기 위한 손잡이 */
private class DrumHandle {
    /** 지금 앞에 있는 막대의 위치 정보 */
    var frontCoords: () -> LayoutCoordinates? = { null }
    /** 고른 메뉴를 앞으로 가져오기 (모아보기가 다 접힌 뒤에) */
    var bringToFront: (NavItem) -> Unit = {}
}

/**
 * 오른쪽 아래 원통 메뉴. 앞 막대 하나에 메뉴 하나, 뒤쪽 막대들은 위로 살짝 겹쳐 보임.
 * 아래로 밀면 뒤(위)의 막대가 앞으로 내려오고, 위로 밀면 앞 막대가 뒤로 넘어감. 끝없이 반복.
 * 성능: 위치·크기·투명도는 레이어 속성으로만 바뀜 (드래그 중 재구성·재배치 없음).
 */
@Composable
private fun NavDrum(
    overview: () -> Float,
    overviewOn: Boolean,
    handle: DrumHandle,
    onPick: (NavItem, LayoutCoordinates) -> Unit,
    onSelect: (NavItem, LayoutCoordinates) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = NavItem.entries
    val n = items.size
    var savedFront by rememberSaveable { mutableIntStateOf(0) }
    val pos = remember { Animatable(savedFront.toFloat()) }   // 앞에 있는 항목 번호 (소수 = 도는 중)
    val scope = rememberCoroutineScope()
    val stepPx = with(LocalDensity.current) { DRUM_STEP.toPx() }
    val barCoords = remember { arrayOfNulls<LayoutCoordinates>(n) }
    // 손가락 상태 (상태가 아니므로 바뀌어도 재구성되지 않음): [0] 도는 중에 붙잡음, [1] 끄는 중
    val touch = remember { BooleanArray(2) }
    // 속도 1(칸/초)로 놓았을 때 관성으로 더 도는 거리
    val unitTravel = remember { exponentialDecay<Float>().calculateTargetValue(0f, 1f) }

    /** 막대에 딱 맞춰 멈추기 */
    val snapTo: suspend (Float, Float) -> Unit = { target, velocity ->
        pos.animateTo(target, spring(dampingRatio = 0.9f, stiffness = 700f), initialVelocity = velocity)
        savedFront = target.roundToInt()
    }
    /** 손을 놓은 뒤: 관성으로 돌다가 느려지면 가장 가까운 막대에 맞춰 멈춤 */
    val settle: suspend (Float) -> Unit = { speed ->
        val now = pos.value
        val natural = now + speed * unitTravel / DRUM_FRICTION
        val target = round(natural.coerceIn(now - MAX_SPIN, now + MAX_SPIN))
        val dist = target - now
        if (abs(speed) > SNAP_SPEED && dist * speed > 0f) {
            // 목표 막대에 정확히 닿도록 마찰을 맞춰 감속
            pos.animateDecay(
                speed,
                exponentialDecay(frictionMultiplier = speed * unitTravel / dist, absVelocityThreshold = SNAP_SPEED),
            )
        }
        snapTo(target, pos.velocity)
    }

    // 모아보기: 펼치기 전에 막대 위치를 정확히 맞춤
    val overviewShown by remember { derivedStateOf { overview() > 0f } }
    LaunchedEffect(overviewOn) { if (overviewOn) pos.snapTo(round(pos.value)) }
    handle.frontCoords = { barCoords[Math.floorMod(round(pos.value).toInt(), n)] }
    handle.bringToFront = { item ->
        scope.launch {
            snapshotFlow { overview() }.first { it <= 0f }
            val base = round(pos.value)
            var diff = Math.floorMod(item.ordinal - base.toInt(), n)
            if (diff > n / 2) diff -= n
            pos.snapTo(base + diff)
            savedFront = (base + diff).roundToInt()
        }
    }

    // 그리는 순서 (먼 막대부터, 앞 막대가 맨 위): 순서가 바뀔 때만 재구성
    val order by remember {
        derivedStateOf(structuralEqualityPolicy()) {
            if (overview() > 0f) (0 until n).sortedByDescending { drumSlot(it - pos.value, n) }
            else (0 until n).sortedByDescending { abs(drumRel(it - pos.value, n)) }
        }
    }

    Box(
        modifier
            // 모아보기 중에는 펼친 목록 높이만큼 (아래 기준이라 막대 위치는 그대로)
            .height(if (overviewShown) LIST_H else DRUM_H)
            // 도는 중에 손을 대면 가장 가까운 막대에 바로 맞춰 멈춤 (흐릿한 중간 상태로 서 있지 않게).
            // 이때 누른 막대는 열리지 않음
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    touch[0] = pos.isRunning
                    if (touch[0]) scope.launch { snapTo(round(pos.value), 0f) }
                }
            }
            .draggable(
                orientation = Orientation.Vertical,
                enabled = !overviewShown,
                state = rememberDraggableState { delta -> scope.launch { pos.snapTo(pos.value + delta / stepPx) } },
                onDragStarted = { touch[1] = true },
                onDragStopped = { velocity ->
                    touch[0] = false
                    touch[1] = false
                    settle(velocity / stepPx)
                },
            )
            .semantics { contentDescription = "메뉴. 위아래로 밀어서 바꾸기" }
    ) {
        for (k in order) {
            key(items[k]) {
                val role by remember { derivedStateOf { drumRole(drumRel(k - pos.value, n)) } }
                DrumBar(
                    item = items[k],
                    rel = { drumRel(k - pos.value, n) },
                    back = { drumBackRel(k - pos.value, n) },
                    slot = { drumSlot(k - pos.value, n) },
                    spread = { overviewStagger(overview(), drumSlot(k - pos.value, n)) },
                    interactive = overviewShown || role != BarRole.Hidden,
                    onClick = {
                        when {
                            overviewShown -> barCoords[k]?.let { onPick(items[k], it) }   // 모아보기에서 바로 고름
                            touch[0] -> touch[0] = false   // 도는 걸 멈추려고 누른 것: 열지 않음
                            role == BarRole.Front -> barCoords[k]?.let { onSelect(items[k], it) }
                            // 뒤쪽 막대를 누르면 그 막대가 앞으로 오도록 돌림
                            else -> scope.launch { snapTo(round(pos.value + drumRel(k - pos.value, n)), 0f) }
                        }
                    },
                    onPlaced = { barCoords[k] = it },
                    modifier = Modifier.align(Alignment.BottomEnd),
                )
            }
        }
    }
}

/** 막대가 앞 막대 기준 몇 칸 뒤(+, 위)/아래(-)에 있는지. (-1.5, n-1.5] 로 감싸서 끝없이 반복 */
private fun drumRel(x: Float, n: Int): Float {
    var v = (x + 1.5f) % n
    if (v <= 0f) v += n
    return v - 1.5f
}

/** 앞 막대 기준 뒤쪽으로만 센 거리 [0, n): 모아보기로 펼칠 때 출발 위치 계산용 */
private fun drumBackRel(x: Float, n: Int): Float {
    val v = x % n
    return if (v < 0f) v + n else v
}

/** 모아보기 목록에서의 자리 (0 = 맨 아래 앞 막대, 1, 2, 3 = 그 위로 차례로) */
private fun drumSlot(x: Float, n: Int): Int = Math.floorMod(round(x).toInt(), n)

/** 모아보기 진행도를 자리마다 조금씩 늦게 → 아래 막대부터 차례로 펼쳐지고, 접을 땐 위에서부터 */
private fun overviewStagger(progress: Float, slot: Int): Float {
    val span = 1f - OVERVIEW_STAGGER * 3f
    return ((progress - slot * OVERVIEW_STAGGER) / span).coerceIn(0f, 1f)
}

private fun drumRole(d: Float): BarRole = when {
    abs(d) < 0.5f -> BarRole.Front
    d >= 0.5f && d <= VISIBLE_BACK + 0.25f -> BarRole.Back
    else -> BarRole.Hidden
}

/** 뒤로 갈수록 작게(오른쪽 아래 기준), 아래로 말려 들어갈 땐 살짝 작게 */
private fun drumScale(d: Float): Float =
    if (d >= 0f) 1f - BACK_SHRINK * min(d, VISIBLE_BACK + 0.5f) else 1f - 0.05f * min(-d, 1f)

/** 뒤로 갈수록 위로 (앞 막대 위로 PEEK 만큼씩 보이게), 아래로 말려 들어갈 땐 아래로 */
private fun drumOffsetY(d: Float, scale: Float, barH: Float, peek: Float, drop: Float): Float =
    if (d >= 0f) -(peek * min(d, VISIBLE_BACK + 0.5f) + (1f - scale) * barH) else drop * min(-d, 1.5f)

/** 보이는 범위를 벗어나는 막대는 서서히 사라짐 (갑자기 튀지 않게) */
private fun drumAlpha(d: Float): Float = when {
    d > VISIBLE_BACK -> (1f - (d - VISIBLE_BACK) / 0.5f).coerceIn(0f, 1f)
    d >= 0f -> 1f
    else -> (1f + 2f * d).coerceIn(0f, 1f)
}

private fun drumShade(d: Float): Float = if (d > 0f) BACK_SHADE * min(d, VISIBLE_BACK) else 0f

/**
 * 막대 안 글자·아이콘: 앞 자리 근처에서는 바로 또렷하게, 뒤로 넘어가거나 아래로 말려 들어가면 금방 흐려짐
 * (중간에 두 글자가 겹치지 않게)
 */
private fun drumContentAlpha(d: Float): Float =
    if (d > 0f) (1f - (d - 0.1f) * 1.8f).coerceIn(0f, 1f) else (1f + (d + 0.05f) * 3.5f).coerceIn(0f, 1f)

@Composable
private fun DrumBar(
    item: NavItem,
    rel: () -> Float,
    back: () -> Float,
    slot: () -> Int,
    spread: () -> Float,
    interactive: Boolean,
    onClick: () -> Unit,
    onPlaced: (LayoutCoordinates) -> Unit,
    modifier: Modifier = Modifier,
) {
    val softShadow = LocalSoftShadow.current
    val interaction = remember { MutableInteractionSource() }
    val press = rememberPress(interaction)
    // 원통 위 위치: 모아보기로 펼치는 중이면 뒤쪽 기준 거리로 (숨은 막대는 맨 뒤에서 나오게)
    val depth: () -> Float = { if (spread() > 0f) back() else rel() }
    Box(
        modifier
            .fillMaxWidth()
            .height(BAR_H)
            .graphicsLayer {
                val e = spread()                         // 0 = 원통, 1 = 모아보기 목록
                val d = depth()
                val s0 = drumScale(d)
                val s = mix(s0, 1f, e)
                scaleX = s
                scaleY = s
                translationY = mix(
                    drumOffsetY(d, s0, BAR_H.toPx(), PEEK.toPx(), DROP.toPx()),
                    -slot() * (BAR_H + LIST_GAP).toPx(),
                    e,
                )
                alpha = mix(drumAlpha(d), 1f, e)
                transformOrigin = TransformOrigin(1f, 1f)
                // 투명도를 그리기마다 적용해서 막대 밖으로 퍼진 그림자가 잘리지 않게
                compositingStrategy = CompositingStrategy.ModulateAlpha
            }
            .onPlaced(onPlaced)
            .drawBehind { softShadow.drawRaised(this, size.toRect(), alpha = 1f - press.value) }
            .clip(CardShape)
            .background(Soft.Base)
            .softPressedInset(CardShape) { press.value }
            .clickable(interactionSource = interaction, indication = null, enabled = interactive, onClick = onClick),
    ) {
        // 뒤로 넘어가면 글자·아이콘은 흐려져서 깔끔한 띠만 보이게 (레이어 투명도만 바뀜)
        BarContent(item, Modifier.graphicsLayer { alpha = mix(drumContentAlpha(depth()), 1f, spread()) })
        // 뒤로 갈수록 살짝 어둡게 (레이어 투명도만 바뀜)
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer { alpha = drumShade(depth()) * (1f - spread()) }
                .background(Soft.Dark)
        )
    }
}

/** 막대 안쪽: 왼쪽에 이름, 오른쪽 끝 오목한 작은 네모 안에 아이콘 */
@Composable
private fun BarContent(item: NavItem, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxSize()
            .padding(start = 20.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(item.title, color = Soft.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(item.subtitle, color = Soft.TextSub, fontSize = 12.sp, maxLines = 1)
        }
        Box(
            Modifier
                .size(44.dp)
                .clip(WellShape)
                .background(Soft.Base)
                .softInset(WellShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(item.icon, contentDescription = null, tint = Soft.Accent, modifier = Modifier.size(22.dp))
        }
    }
}

/** 누름 상태(0 = 볼록, 1 = 눌림)를 짧게 애니메이션 */
@Composable
private fun rememberPress(interaction: MutableInteractionSource): State<Float> {
    val pressed by interaction.collectIsPressedAsState()
    return animateFloatAsState(if (pressed) 1f else 0f, tween(PRESS_MS), label = "press")
}

// ───────────────────────── 경로 설정 창 ─────────────────────────

/**
 * 경로 설정 창: 막대에서 커져서 화면 아래 1/3 을 채우고, 위로 끌어올리면 상세정보까지 보임.
 * 성능: 창 크기는 모양(보이는 영역)과 내용 이동으로만 바뀜 → 끄는 동안 재배치 없음.
 */
@Composable
private fun RouteSheet(
    from: Rect,
    reveal: () -> Float,
    top: () -> Float,
    expand: () -> Float,
    onDrag: (Float) -> Unit,
    onDragStopped: (Float) -> Unit,
    onExpand: () -> Unit,
    onClose: () -> Unit,
    start: String,
    onStartChange: (String) -> Unit,
    dest: String,
    onDestChange: (String) -> Unit,
    option: Int,
    onOptionChange: (Int) -> Unit,
    onAnalyzeRoute: (String, String, Boolean) -> Unit,
    routeUiState: RouteUiState,
    onStartGuidance: () -> Unit,
) {
    val softShadow = LocalSoftShadow.current
    val density = LocalDensity.current
    val sheetCorner = with(density) { SHEET_CORNER.toPx() }
    val barCorner = with(density) { CORNER.toPx() }
    val barW = with(density) { from.width.toDp() }
    val barH = with(density) { from.height.toDp() }
    // 지금 보이는 창 영역 (막대 → 창으로 커지는 중이면 그 사이). 아래쪽 모서리는 화면 밖으로
    fun visible(size: Size): Rect =
        lerp(from, Rect(0f, top(), size.width, size.height + sheetCorner), reveal())

    // 창 바깥 그림자 (창 레이어는 모양대로 잘리므로 그 밖(뒤)에 따로 그림)
    Spacer(
        Modifier
            .fillMaxSize()
            .graphicsLayer()
            .drawBehind { softShadow.drawLifted(this, visible(size)) }
    )
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                shape = RevealShape(visible(size), mix(barCorner, sheetCorner, reveal()))
                clip = true
            }
            .background(Soft.Base)
            .pointerInput(Unit) { detectTapGestures() } // 창 안의 터치가 뒤의 지도로 넘어가지 않게
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { onDrag(it) },
                onDragStopped = { onDragStopped(it) },
            )
    ) {
        // 처음엔 누른 막대 모양 그대로 보이다가 빠르게 사라짐
        BarContent(
            NavItem.Route,
            Modifier
                .offset { IntOffset(from.left.roundToInt(), from.top.roundToInt()) }
                .size(barW, barH)
                .graphicsLayer { alpha = (1f - reveal() * 4f).coerceIn(0f, 1f) },
        )
        SheetContent(
            expand = expand,
            onExpand = onExpand,
            onClose = onClose,
            start = start,
            onStartChange = onStartChange,
            dest = dest,
            onDestChange = onDestChange,
            option = option,
            onOptionChange = onOptionChange,
            onAnalyzeRoute = onAnalyzeRoute,
            routeUiState = routeUiState,
            onStartGuidance = onStartGuidance,
            modifier = Modifier.graphicsLayer {
                val p = reveal()
                translationY = mix(from.top, top(), p)   // 내용은 창 윗변을 따라 움직임
                alpha = ((p - 0.45f) / 0.55f).coerceIn(0f, 1f)
            },
        )
    }
}

@Composable
private fun SheetContent(
    expand: () -> Float,
    onExpand: () -> Unit,
    onClose: () -> Unit,
    start: String,
    onStartChange: (String) -> Unit,
    dest: String,
    onDestChange: (String) -> Unit,
    option: Int,
    onOptionChange: (Int) -> Unit,
    onAnalyzeRoute: (String, String, Boolean) -> Unit,
    routeUiState: RouteUiState,
    onStartGuidance: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        // 손잡이
        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(40.dp, 5.dp)
                    .clip(PillShape)
                    .background(Soft.Base)
                    .softInset(PillShape, offset = 1.dp, blur = 2.dp)
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("경로 설정", color = Soft.Text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            SoftIconButton(onClick = onClose, size = 40.dp) {
                Icon(Icons.Filled.Close, contentDescription = "닫기", tint = Soft.TextSub, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.height(12.dp))

        // 출발·도착을 하나로 합친 오목한 입력칸 (가운데 선으로 구분)
        Column(
            Modifier
                .fillMaxWidth()
                .height(FIELD_H)
                .clip(FieldShape)
                .background(Soft.Base)
                .softInset(FieldShape)
        ) {
            RouteField(start, onStartChange, "출발지를 입력하세요", onFocus = onExpand) {
                Box(
                    Modifier
                        .size(11.dp)
                        .border(2.5.dp, Soft.Accent, CircleShape)
                )
            }
            GrooveDivider()
            RouteField(dest, onDestChange, "도착지를 입력하세요", onFocus = onExpand) {
                Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Soft.Accent, modifier = Modifier.size(18.dp))
            }
        }

        // 위로 올리면 상세정보 (펼칠수록 사라짐)
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .graphicsLayer { alpha = (1f - expand() * 3f).coerceIn(0f, 1f) }
                .clickable(interactionSource = null, indication = null, onClick = onExpand),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = null, tint = Soft.TextHint, modifier = Modifier.size(18.dp))
            Text("위로 올리면 상세정보", color = Soft.TextHint, fontSize = 12.sp)
        }

        // ── 상세정보 (위로 끌어올리면 보임) ──
        SectionTitle("경로 옵션")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("자전거도로 우선", "최단 경로").forEachIndexed { i, label ->
                OptionChip(label, selected = option == i) { onOptionChange(i) }
            }
        }
        Spacer(Modifier.height(14.dp))
        Box(
            Modifier.fillMaxWidth().height(48.dp)
                .softRaised(CardShape)
                .clip(CardShape)
                .background(Soft.Base)
                .clickable(enabled = !routeUiState.loading) { onAnalyzeRoute(start, dest, option == 0) },
            contentAlignment = Alignment.Center,
        ) {
            Text(if (routeUiState.loading) "경로 검색 중…" else "경로 분석", fontSize = 15.sp, color = Soft.Accent,
                fontWeight = FontWeight.SemiBold)
        }
        Text(
            if (routeUiState.distanceMeters != null) "카카오 자전거 경로 조회 결과 · 사고 데이터 회피 기능 미적용"
            else "자전거도로 우선(BIKE_ONLY) / 최단 경로(SHORTEST). 실제 사고 회피 계산은 별도입니다.",
            color = Soft.TextHint, fontSize = 11.sp, modifier = Modifier.padding(top = 9.dp)
        )
        SectionTitle("예상 정보")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricTile("거리", routeUiState.distanceMeters?.let { String.format(Locale.KOREA, "%.1f km", it / 1000.0) } ?: "— km")
            MetricTile("시간", routeUiState.durationSeconds?.let { "${(it + 59) / 60} 분" } ?: "— 분")
            MetricTile("오르막", "— m")
        }
        Text(
            routeUiState.message,
            color = if (routeUiState.error) Color(0xFFDC534A) else Soft.TextHint,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 10.dp),
        )
        // 경로 분석 성공 후 안내 시작 버튼 표시
        if (routeUiState.distanceMeters != null &&
            !routeUiState.loading &&
            !routeUiState.error
        ) {
            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .softRaised(CardShape)
                    .clip(CardShape)
                    .background(Soft.Base)
                    .clickable {
                        onStartGuidance()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "안내 시작",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Soft.Accent
                )
            }
        }
        SectionTitle("즐겨찾기")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FavoriteTile(Icons.Filled.Home, "집")
            FavoriteTile(Icons.Filled.Place, "회사")
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        color = Soft.Text,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(top = 26.dp, bottom = 12.dp),
    )
}

/** 선택하면 오목하게 들어가는 옵션 */
@Composable
private fun RowScope.OptionChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val press by animateFloatAsState(if (selected) 1f else 0f, tween(160), label = "chip")
    Box(
        Modifier
            .weight(1f)
            .height(40.dp)
            .softPressable(PillShape) { press }
            .clickable(interactionSource = null, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (selected) Soft.Accent else Soft.TextSub,
            fontSize = 12.5.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
        )
    }
}

@Composable
private fun RowScope.MetricTile(label: String, value: String) {
    Column(
        Modifier
            .weight(1f)
            .height(76.dp)
            .softRaised(CardShape)
            .clip(CardShape)
            .background(Soft.Base)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = Soft.TextSub, fontSize = 12.sp)
        Text(value, color = Soft.Text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun RowScope.FavoriteTile(icon: ImageVector, title: String) {
    Row(
        Modifier
            .weight(1f)
            .height(64.dp)
            .softRaised(CardShape)
            .clip(CardShape)
            .background(Soft.Base)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(WellShape)
                .background(Soft.Base)
                .softInset(WellShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Soft.Accent, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, color = Soft.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("등록하기", color = Soft.TextSub, fontSize = 12.sp)
        }
    }
}

/** 구분선: 어두운 선 아래 밝은 선을 겹쳐 살짝 파인 홈처럼 보이게 */
@Composable
private fun GrooveDivider(inset: Dp = 16.dp) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = inset)
    ) {
        Spacer(Modifier.fillMaxWidth().height(DIVIDER / 2).background(DividerDark))
        Spacer(Modifier.fillMaxWidth().height(DIVIDER / 2).background(Soft.Light))
    }
}

/** 합친 입력칸 안의 한 줄 (출발 또는 도착). 누르면 창을 끝까지 펼쳐 키보드에 가리지 않게 함 */
@Composable
private fun RouteField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    onFocus: () -> Unit,
    leading: @Composable () -> Unit,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = FieldTextStyle,
        cursorBrush = CursorBrush,
        modifier = Modifier
            .fillMaxWidth()
            .height(ROW)
            .onFocusChanged { if (it.isFocused) onFocus() },
        decorationBox = { inner ->
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 아이콘 폭이 달라도 두 줄의 글자 시작 위치가 맞도록 같은 폭 칸 가운데에 둠
                Box(Modifier.width(20.dp), contentAlignment = Alignment.Center) { leading() }
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) Text(placeholder, color = Soft.TextHint, fontSize = 14.sp)
                    inner()
                }
            }
        },
    )
}

// ───────────────────────── 화면 채우기 (계정/임시화면1/설정) ─────────────────────────

/** 누른 막대가 커지면서 화면 전체를 채우고, 그 안에 해당 화면이 나타남 */
@Composable
private fun PageOverlay(
    page: NavItem,
    settings: AppSettings,
    from: Rect,
    progress: () -> Float,
    onBack: () -> Unit,
) {
    val density = LocalDensity.current
    val barW = with(density) { from.width.toDp() }
    val barH = with(density) { from.height.toDp() }

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                val p = progress()
                shape = RevealShape(
                    lerp(from, Rect(0f, 0f, size.width, size.height), p),
                    CORNER.toPx() * (1f - p),
                )
                clip = true
            }
            .background(Soft.Base)
            .pointerInput(Unit) { detectTapGestures() } // 뒤의 지도/메뉴로 터치가 넘어가지 않게
    ) {
        // 처음엔 누른 막대 모양 그대로 보이다가 빠르게 사라짐
        BarContent(
            page,
            Modifier
                .offset { IntOffset(from.left.roundToInt(), from.top.roundToInt()) }
                .size(barW, barH)
                .graphicsLayer { alpha = (1f - progress() * 4f).coerceIn(0f, 1f) },
        )
        PageContent(
            page = page,
            settings = settings,
            onBack = onBack,
            modifier = Modifier.graphicsLayer { alpha = ((progress() - 0.3f) / 0.7f).coerceIn(0f, 1f) },
        )
    }
}

/** 레이어 안의 [rect] 영역만 보이게 하는 둥근 사각형 모양 */
private class RevealShape(private val rect: Rect, private val radius: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Rounded(RoundRect(rect, CornerRadius(radius)))
}

private fun mix(start: Float, stop: Float, fraction: Float) = start + (stop - start) * fraction

/** 계정(마이페이지)/임시화면1/설정 화면 */
@Composable
private fun PageContent(page: NavItem, settings: AppSettings, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = EDGE, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SoftIconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로", tint = Soft.Text)
            }
            Spacer(Modifier.width(18.dp))
            Text(page.pageTitle, color = Soft.Text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        }
        when (page) {
            NavItem.Account -> MyPageBody()
            NavItem.Settings -> SettingsBody(settings)
            else -> PlaceholderBody(page)
        }
    }
}

/** 아직 내용이 없는 화면 */
@Composable
private fun PlaceholderBody(page: NavItem) {
    Column(
        Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(120.dp)
                .softRaised(CircleShape, offset = 8.dp, blur = 16.dp)
                .clip(CircleShape)
                .background(Soft.Base),
            contentAlignment = Alignment.Center,
        ) {
            Icon(page.icon, contentDescription = null, tint = Soft.Accent, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(32.dp))
        Text(page.pageTitle, color = Soft.Text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(
            "${page.pageTitle} 내용이 들어갈 자리예요",
            color = Soft.TextSub,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
    }
}

/** 마이페이지 (임시 내용) */
@Composable
private fun MyPageBody() {
    PageBody {
        SoftCard {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Soft.Base)
                        .softInset(CircleShape, offset = 3.dp, blur = 5.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Person, contentDescription = null, tint = Soft.Accent, modifier = Modifier.size(30.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("계정 API 연결 전", color = Soft.Text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text("12/9 카카오 로그인·계정 API 연동 예정", color = Soft.TextSub, fontSize = 13.sp)
                }
                SoftTextButton("편집") {}
            }
        }
        SectionHeader("이번 달 기록", tag = "예시")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("거리", "128.4", "km")
            StatTile("주행", "23", "회")
            StatTile("시간", "9:40", "")
        }
        SectionHeader("최근 주행")
        SoftCard {
            RideRow("한강 자전거길", "24.6 km · 1시간 12분", "11.8")
            GrooveDivider()
            RideRow("출퇴근", "8.2 km · 31분", "11.7")
            GrooveDivider()
            RideRow("남산 순환", "12.9 km · 58분", "11.5")
        }
        SectionHeader("내 정보")
        SoftCard {
            MenuRow("즐겨찾는 장소", "집 · 회사")
            GrooveDivider()
            MenuRow("주행 목표", "주 50 km")
            GrooveDivider()
            MenuRow("알림", "켜짐")
            GrooveDivider()
            MenuRow("로그아웃", null)
        }
    }
}

/** 설정 (임시 내용: 값은 화면에서만 바뀜) */
@Composable
private fun SettingsBody(s: AppSettings) {
    PageBody {
        SectionHeader("주행")
        SoftCard {
            SwitchRow("음성 안내 설정 (시연용)", "TTS는 테스트 구현됨 · 이 스위치는 아직 연결 전", s.voiceGuide) { s.voiceGuide = it }
            GrooveDivider()
            SwitchRow("화면 꺼짐 방지", "주행 중 화면을 켜 둬요", s.keepScreenOn) { s.keepScreenOn = it }
            GrooveDivider()
            SwitchRow("자동 일시정지", "멈추면 기록도 잠시 멈춰요", s.autoPause) { s.autoPause = it }
        }
        SectionHeader("지도")
        SoftCard {
            SwitchRow("자전거도로 표시", null, s.bikeLanes) { s.bikeLanes = it }
            GrooveDivider()
            SwitchRow("공공자전거 대여소 표시", null, s.rentalStations) { s.rentalStations = it }
        }
        SectionHeader("일반")
        SoftCard {
            SettingRow("거리 단위") { SoftSegmented(listOf("km", "mi"), s.unit) { s.unit = it } }
            GrooveDivider()
            SwitchRow("블랙 테마", "어두운 화면으로 바꿔요", Soft.isBlack) { Soft.isBlack = it }
            GrooveDivider()
            SettingRow("라이딩 모드") { Text("추후 디자인 적용", color = Soft.TextSub, fontSize = 12.sp) }
            GrooveDivider()
            SettingRow("앱 버전") { Text("0.1", color = Soft.TextSub, fontSize = 14.sp) }
        }
    }
}

/** 설정 화면 값 (페이지를 닫았다 열어도 유지, 지금은 화면에서만 바뀌는 임시 값) */
@Stable
class AppSettings {
    var voiceGuide by mutableStateOf(true)
    var keepScreenOn by mutableStateOf(true)
    var autoPause by mutableStateOf(false)
    var bikeLanes by mutableStateOf(true)
    var rentalStations by mutableStateOf(false)
    var unit by mutableIntStateOf(0)   // 0 = km, 1 = mi
}

/** 페이지 본문: 세로 스크롤 */
@Composable
private fun PageBody(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = EDGE)
            .padding(top = 12.dp, bottom = 32.dp),
        content = content,
    )
}

/** 볼록한 카드 */
@Composable
private fun SoftCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .softRaised(CardShape)
            .clip(CardShape)
            .background(Soft.Base)
            .padding(vertical = 4.dp),
        content = content,
    )
}

@Composable
private fun SectionHeader(text: String, tag: String? = null) {
    Row(
        Modifier.padding(start = 4.dp, top = 28.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, color = Soft.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
        if (tag != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                tag,
                color = Soft.TextHint,
                fontSize = 11.sp,
                modifier = Modifier
                    .border(1.dp, Soft.TextHint.copy(alpha = 0.6f), PillShape)
                    .padding(horizontal = 8.dp, vertical = 1.dp),
            )
        }
    }
}

@Composable
private fun RowScope.StatTile(label: String, value: String, unit: String) {
    Column(
        Modifier
            .weight(1f)
            .height(80.dp)
            .softRaised(CardShape)
            .clip(CardShape)
            .background(Soft.Base)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = Soft.TextSub, fontSize = 12.sp)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, color = Soft.Text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            if (unit.isNotEmpty()) {
                Spacer(Modifier.width(3.dp))
                Text(unit, color = Soft.TextSub, fontSize = 12.sp, modifier = Modifier.padding(bottom = 3.dp))
            }
        }
    }
}

@Composable
private fun RideRow(title: String, detail: String, date: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(WellShape)
                .background(Soft.Base)
                .softInset(WellShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Place, contentDescription = null, tint = Soft.Accent, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Soft.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(detail, color = Soft.TextSub, fontSize = 12.sp)
        }
        Text(date, color = Soft.TextHint, fontSize = 12.sp)
    }
}

@Composable
private fun MenuRow(title: String, value: String?) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(interactionSource = null, indication = null) {}
            .padding(horizontal = 18.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = Soft.Text, fontSize = 15.sp, modifier = Modifier.weight(1f))
        if (value != null) Text(value, color = Soft.TextSub, fontSize = 13.sp)
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Soft.TextHint,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** 줄 전체를 눌러도 켜고 끌 수 있는 스위치 줄 */
@Composable
private fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                interactionSource = null,
                indication = null,
                role = Role.Switch,
                onValueChange = onChange,
            )
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Soft.Text, fontSize = 15.sp)
            if (subtitle != null) Text(subtitle, color = Soft.TextSub, fontSize = 12.sp)
        }
        SoftSwitch(checked)
    }
}

@Composable
private fun SettingRow(title: String, trailing: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = Soft.Text, fontSize = 15.sp, modifier = Modifier.weight(1f))
        trailing()
    }
}

/** 소프트 UI 스위치: 오목한 길 위로 볼록한 손잡이. 켜지면 길이 파란색 */
@Composable
private fun SoftSwitch(checked: Boolean) {
    val t by animateFloatAsState(if (checked) 1f else 0f, tween(180), label = "switch")
    Box(
        Modifier
            .size(50.dp, 28.dp)
            .clip(PillShape)
            .drawBehind { drawRect(lerpColor(Soft.Base, Soft.Accent, t)) }
            .softInset(PillShape, offset = 2.dp, blur = 3.dp)
            .padding(4.dp)
    ) {
        Box(
            Modifier
                .offset { IntOffset((22.dp.toPx() * t).roundToInt(), 0) }
                .size(20.dp)
                .softRaised(CircleShape, offset = 1.5.dp, blur = 3.dp)
                .clip(CircleShape)
                .drawBehind { drawRect(lerpColor(Soft.Base, Color.White, t)) }
        )
    }
}

/** 소프트 UI 선택 버튼 묶음: 고른 쪽이 볼록하게 */
@Composable
private fun SoftSegmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier
            .height(34.dp)
            .clip(PillShape)
            .background(Soft.Base)
            .softInset(PillShape, offset = 2.dp, blur = 3.dp)
            .padding(3.dp)
    ) {
        options.forEachIndexed { i, label ->
            val sel = i == selected
            Box(
                Modifier
                    .fillMaxHeight()
                    .width(48.dp)
                    .then(if (sel) Modifier.softRaised(PillShape, offset = 1.5.dp, blur = 3.dp).clip(PillShape).background(Soft.Base) else Modifier)
                    .clickable(interactionSource = null, indication = null) { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = if (sel) Soft.Accent else Soft.TextSub,
                    fontSize = 13.sp,
                    fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

/** 작은 글자 버튼 (누르면 오목하게) */
@Composable
private fun SoftTextButton(text: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val press = rememberPress(interaction)
    Box(
        Modifier
            .height(36.dp)
            .softPressable(PillShape) { press.value }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Soft.Accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ───────────────────────── 현재 위치 ─────────────────────────

/** 메뉴 모아보기 버튼: 겹쳐진 막대들을 한 번에 펼쳐서 보여줌. 펼친 동안은 닫기(X) */
@Composable
private fun OverviewButton(open: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val press = rememberPress(interaction)
    val color = if (open) Soft.Accent else Soft.TextSub
    Box(
        modifier
            .size(LOC_BTN)
            .softPressable(CircleShape) { press.value }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .semantics { contentDescription = if (open) "모아보기 닫기" else "메뉴 모아보기" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(22.dp)) {
            val sw = 2.dp.toPx()
            if (open) {
                val m = 6.dp.toPx()
                drawLine(color, Offset(m, m), Offset(size.width - m, size.height - m), sw, cap = StrokeCap.Round)
                drawLine(color, Offset(size.width - m, m), Offset(m, size.height - m), sw, cap = StrokeCap.Round)
            } else {
                // 겹쳐진 막대 3개 (오른쪽 정렬, 위로 갈수록 짧게 = 원통 메뉴 모양)
                val right = size.width - 3.dp.toPx()
                drawLine(color, Offset(right - 15.dp.toPx(), 16.dp.toPx()), Offset(right, 16.dp.toPx()), sw, cap = StrokeCap.Round)
                drawLine(color, Offset(right - 12.dp.toPx(), 11.dp.toPx()), Offset(right, 11.dp.toPx()), sw, cap = StrokeCap.Round)
                drawLine(color, Offset(right - 9.dp.toPx(), 6.dp.toPx()), Offset(right, 6.dp.toPx()), sw, cap = StrokeCap.Round)
            }
        }
    }
}

/** 현재 위치 버튼 (작은 볼록 동그라미). 내 위치를 보여주는 중이면 아이콘이 파란색 */
@Composable
private fun MyLocationButton(active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val press = rememberPress(interaction)
    val color = if (active) Soft.Accent else Soft.TextSub
    Box(
        modifier
            .size(LOC_BTN)
            .softPressable(CircleShape) { press.value }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .semantics { contentDescription = "현재 위치" },
        contentAlignment = Alignment.Center,
    ) {
        // 조준선 모양 아이콘
        Canvas(Modifier.size(22.dp)) {
            val c = center
            val sw = 2.dp.toPx()
            drawCircle(color, radius = 6.dp.toPx(), center = c, style = Stroke(sw))
            drawCircle(color, radius = 2.5.dp.toPx(), center = c)
            val r1 = 8.dp.toPx()
            val r2 = 10.dp.toPx()
            drawLine(color, Offset(c.x, c.y - r2), Offset(c.x, c.y - r1), sw, cap = StrokeCap.Round)
            drawLine(color, Offset(c.x, c.y + r1), Offset(c.x, c.y + r2), sw, cap = StrokeCap.Round)
            drawLine(color, Offset(c.x - r2, c.y), Offset(c.x - r1, c.y), sw, cap = StrokeCap.Round)
            drawLine(color, Offset(c.x + r1, c.y), Offset(c.x + r2, c.y), sw, cap = StrokeCap.Round)
        }
    }
}

/** 지도 위 내 위치 점 (지도 붙이기 전 임시: 화면 가운데). 버튼을 누를 때마다 고리가 퍼짐 */
@Composable
private fun MyLocationMarker(pulse: () -> Float, modifier: Modifier = Modifier) {
    Canvas(modifier.graphicsLayer()) {
        val c = Offset(size.width * 0.5f, size.height * 0.42f)
        val p = pulse()
        drawCircle(Soft.Accent.copy(alpha = 0.10f), radius = 34.dp.toPx(), center = c)
        if (p < 1f) {
            drawCircle(Soft.Accent.copy(alpha = 0.30f * (1f - p)), radius = 12.dp.toPx() + 46.dp.toPx() * p, center = c)
        }
        drawCircle(Soft.Dark.copy(alpha = 0.35f), radius = 11.dp.toPx(), center = c + Offset(1.dp.toPx(), 2.dp.toPx()))
        drawCircle(Color.White, radius = 10.dp.toPx(), center = c)
        drawCircle(Soft.Accent, radius = 7.dp.toPx(), center = c)
    }
}

/** 소프트 UI 동그란 아이콘 버튼 (누르면 오목하게 들어감) */
@Composable
private fun SoftIconButton(onClick: () -> Unit, size: Dp = 44.dp, content: @Composable () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val press = rememberPress(interaction)
    Box(
        Modifier
            .size(size)
            .softPressable(CircleShape) { press.value }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

// ───────────────────────── 지도 자리 ─────────────────────────

/** 지도 표시 영역 (실제 지도 SDK 붙이기 전 임시 배경) */
@Composable
private fun MapPlaceholder(modifier: Modifier = Modifier) {
    Box(modifier) {
        // 별도 레이어에 한 번만 그려 둠 → 메뉴 애니메이션 중에도 지도는 다시 그리지 않음
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer()
        ) {
            val w = size.width
            val h = size.height
            drawRect(Soft.Base)
            val step = 44.dp.toPx()
            val thin = 1.dp.toPx()
            var x = step
            while (x < w) {
                drawLine(Soft.MapGrid, Offset(x, 0f), Offset(x, h), thin)
                x += step
            }
            var y = step
            while (y < h) {
                drawLine(Soft.MapGrid, Offset(0f, y), Offset(w, y), thin)
                y += step
            }
            drawRoundRect(
                Soft.MapPark,
                topLeft = Offset(w * 0.56f, h * 0.36f),
                size = Size(w * 0.30f, h * 0.13f),
                cornerRadius = CornerRadius(18.dp.toPx()),
            )
            val road = 9.dp.toPx()
            drawLine(Soft.MapRoad, Offset(-road, h * 0.30f), Offset(w + road, h * 0.23f), road)
            drawLine(Soft.MapRoad, Offset(w * 0.40f, -road), Offset(w * 0.50f, h + road), road)
            drawLine(Soft.MapRoad, Offset(-road, h * 0.64f), Offset(w + road, h * 0.52f), road * 0.7f)
        }
        Text(
            "지도 표시 영역 (임시)",
            color = Soft.TextHint,
            fontSize = 13.sp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 20.dp),
        )
    }
}
