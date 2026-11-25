package com.example.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import com.example.bicyclesafety.R
import com.example.app.ui.theme.Soft
import com.example.bicyclesafety.MainActivity
import com.example.bicyclesafety.RiskPoint

/** Current query only; elevation cannot be inferred from the bicycle route response. */
data class RouteUiState(
    val loading: Boolean = false,
    val distanceMeters: Int? = null,
    val durationSeconds: Int? = null,
    val message: String = "출발지와 도착지를 입력하면 표시돼요",
    val error: Boolean = false,
    val routeHazards: Int? = null,
    val hazardDataSource: String = "",
    /** 경로 미리보기 카드용: "자전거도로 우선" / "최단 경로", 출발·도착 이름 */
    val modeLabel: String = "",
    val startName: String = "",
    val endName: String = "",
)

/**
 * 길 안내(내비게이션) 화면 상태. Java MainActivity가 위치가 바뀔 때마다 갱신합니다.
 * maneuver 값은 NavigationTracker.Maneuver 이름 (LEFT, RIGHT, STRAIGHT, ...).
 */
data class NavUiState(
    val active: Boolean = false,
    /** 안내 시작 후 첫 위치를 받았는지 */
    val hasFix: Boolean = false,
    val maneuver: String = "STRAIGHT",
    val maneuverLabel: String = "",
    val instruction: String = "",
    /** 다음 안내 지점까지 남은 거리 (m) */
    val distanceToNext: Int = 0,
    /** 그다음 안내 (없으면 null) */
    val nextManeuver: String? = null,
    val nextDistance: Int = 0,
    val remainingMeters: Int = 0,
    val remainingSeconds: Int = 0,
    /** 현재 속도 km/h (모르면 -1) */
    val speedKmh: Int = -1,
    /** 진행률 0~1 */
    val progress: Float = 0f,
    /** 경로 주변 사고다발지역의 경로상 위치 0~1 */
    val hazardMarks: List<Float> = emptyList(),
    val offRoute: Boolean = false,
    val rerouting: Boolean = false,
    val destination: String = "",
    /** 화면 아래 잠깐 보이는 알림 ("경로 안내를 시작합니다." 등) */
    val message: String? = null,
    val messageId: Int = 0,
)

/** Only place that links the Java map/feature layer with the new Compose design. */
object BikeUiBridge {
    private var activeRiskPoint by mutableStateOf<RiskPoint?>(null)
    private var safetyRequired by mutableStateOf(false)
    private var routeUiState by mutableStateOf(RouteUiState())
    private var proximityWarning by mutableStateOf<String?>(null)
    private var navUiState by mutableStateOf(NavUiState())

    @JvmStatic
    fun routeStarted() { routeUiState = RouteUiState(loading = true, message = "장소 검색 및 자전거 경로 조회 중…") }

    @JvmStatic
    fun routeSucceeded(distanceMeters: Int, durationSeconds: Int, startName: String, endName: String, safe: Boolean, routeHazards: Int, hazardDataSource: String) {
        val mode = if (safe) "자전거도로 우선" else "최단 경로"
        routeUiState = RouteUiState(
            distanceMeters = distanceMeters, durationSeconds = durationSeconds,
            message = "$mode · $startName → $endName\n" +
                    (if (routeHazards < 0) "사고 데이터가 준비되지 않아 주변 사고 이력을 확인하지 못했습니다.\n실제 안전 회피 계산 미적용 · 첫 번째 장소 검색 결과 사용"
                    else "경로 주변 150m 이내 과거 사고다발지역: ${routeHazards}곳 ($hazardDataSource)\n실제 안전 회피 계산 미적용 · 첫 번째 장소 검색 결과 사용"),
            routeHazards = routeHazards, hazardDataSource = hazardDataSource,
            modeLabel = mode, startName = startName, endName = endName,
        )
    }

    // ── 길 안내 화면 (MainActivity → Compose) ──

    @JvmStatic
    fun navStarted(destination: String, durationSeconds: Int, distanceMeters: Int, hazardMarks: FloatArray) {
        navUiState = NavUiState(
            active = true, destination = destination,
            remainingMeters = distanceMeters, remainingSeconds = durationSeconds,
            hazardMarks = hazardMarks.toList(),
        )
    }

    @JvmStatic
    fun navProgress(
        maneuver: String, maneuverLabel: String, instruction: String, distanceToNext: Int,
        nextManeuver: String?, nextDistance: Int, remainingMeters: Int, remainingSeconds: Int,
        speedKmh: Int, progress: Float, offRoute: Boolean,
    ) {
        if (!navUiState.active) return
        navUiState = navUiState.copy(
            hasFix = true, maneuver = maneuver, maneuverLabel = maneuverLabel, instruction = instruction,
            distanceToNext = distanceToNext, nextManeuver = nextManeuver, nextDistance = nextDistance,
            remainingMeters = remainingMeters, remainingSeconds = remainingSeconds,
            speedKmh = speedKmh, progress = progress, offRoute = offRoute,
        )
    }

    @JvmStatic
    fun navRerouting(rerouting: Boolean) {
        if (navUiState.active) navUiState = navUiState.copy(rerouting = rerouting)
    }

    @JvmStatic
    fun navRouteReplaced(durationSeconds: Int, distanceMeters: Int, hazardMarks: FloatArray) {
        if (!navUiState.active) return
        navUiState = navUiState.copy(
            remainingMeters = distanceMeters, remainingSeconds = durationSeconds,
            hazardMarks = hazardMarks.toList(), offRoute = false, progress = 0f,
        )
    }

    @JvmStatic
    fun navMessage(message: String) {
        navUiState = navUiState.copy(message = message, messageId = navUiState.messageId + 1)
    }

    @JvmStatic
    fun navEnded() { navUiState = NavUiState() }

    @JvmStatic
    fun routeFailed(message: String) { routeUiState = RouteUiState(message = message, error = true) }

    @JvmStatic
    fun routeCleared() { routeUiState = RouteUiState() }

    @JvmStatic
    fun attach(activity: MainActivity, composeView: ComposeView, requireSafety: Boolean) {
        safetyRequired = requireSafety
        activeRiskPoint = null
        routeCleared()
        composeView.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        composeView.setContent {
            BikeApp(
                mapContent = { modifier ->
                    AndroidView(
                        modifier = modifier,
                        factory = { activity.acquireMapView() }
                    )
                },
                onAnalyzeRoute = { origin, destination, safe ->
                    activity.onAnalyzeRoute(origin, destination, safe)
                },
                onMyLocation = { activity.onCurrentLocationRequested() },
                onRouteFormChanged = { activity.onRouteFormChanged() },
                routeUiState = routeUiState,
                onStartGuidance = { activity.onStartGuidanceRequested() },
                proximityWarning = proximityWarning,
                navUiState = navUiState,
                onEndGuidance = { activity.onEndGuidanceRequested() },
                onReroute = { activity.onRerouteRequested() },
                onClearRoute = { activity.onRouteFormChanged() },
            )
            activeRiskPoint?.let { point ->
                HazardDialog(point = point, onDismiss = { activeRiskPoint = null })
            }
            if (safetyRequired) {
                SafetyDialog(
                    onConfirm = {
                        safetyRequired = false
                        activity.onSafetyConfirmed()
                    },
                    onExit = {
                        safetyRequired = false
                        activity.onSafetyExit()
                    }
                )
            }
        }
    }

    @JvmStatic
    fun displayRiskPoint(point: RiskPoint) { activeRiskPoint = point }

    @JvmStatic
    fun showProximityWarning(warning: String?) { proximityWarning = warning }
}

@Composable
private fun HazardDialog(point: RiskPoint, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Soft.Base)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text("위험지점 안내", fontSize = 13.sp, color = Soft.Accent)
            Text(point.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Soft.Text)
            Text("위험도: ${point.riskLevel}", fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                color = if (point.riskLevel == "높음") Color(0xFFDA413E) else Soft.Text)
            Text(point.description, fontSize = 15.sp, color = Soft.TextSub)
            Text("자료: ${point.source} · 과거 사고 이력으로 실시간 위험을 보장하지 않습니다.",
                fontSize = 11.sp, color = Soft.TextHint)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text("확인", modifier = Modifier.clickable(onClick = onDismiss)
                    .semantics { contentDescription = "위험지점 안내 닫기" }.padding(12.dp),
                    fontSize = 15.sp, color = Soft.Accent, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** New-design equivalent of the original blocking pre-ride safety alert. */
@Composable
private fun SafetyDialog(onConfirm: () -> Unit, onExit: () -> Unit) {
    Dialog(onDismissRequest = { /* explicit acknowledgment is mandatory */ }) {
        Column(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Soft.Base)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(stringResource(R.string.safety_title), fontSize = 21.sp,
                fontWeight = FontWeight.Bold, color = Soft.Text)
            Text(stringResource(R.string.safety_message), fontSize = 14.sp, color = Soft.TextSub)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(stringResource(R.string.safety_exit),
                    modifier = Modifier.clickable(onClick = onExit).padding(10.dp),
                    fontSize = 14.sp, color = Soft.TextSub)
                Text(stringResource(R.string.safety_confirm),
                    modifier = Modifier.clickable(onClick = onConfirm).padding(10.dp),
                    fontSize = 14.sp, color = Soft.Accent, fontWeight = FontWeight.Bold)
            }
        }
    }
}
