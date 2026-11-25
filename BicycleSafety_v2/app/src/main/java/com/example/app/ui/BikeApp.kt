package com.example.app.ui

import android.content.Context
import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import com.example.app.ui.loading.BikeLoadingScreen
import com.example.app.ui.main.MainScreen
import com.example.app.ui.theme.Soft
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop

private val AppColors = lightColorScheme(primary = Color(0xFF1A5FD6))

private const val PREFS = "bike_settings"
private const val KEY_BLACK = "black_theme"

/**
 * 앱 전체 화면: 로딩화면 → (로딩 완료 + 자전거가 화면 밖으로 나가면) → 메인 화면.
 * Activity 에서 `setContent { BikeApp() }` 한 줄로 씀.
 *
 * @param loadInitialData 앱 시작 시 할 로딩 작업 (지도 초기화, 데이터 불러오기 등). 기본값은 임시로 1.5초 대기
 */
@Composable
fun BikeApp(
    mapContent: @Composable (Modifier) -> Unit,
    onAnalyzeRoute: (String, String, Boolean) -> Unit,
    onMyLocation: () -> Unit,
    onRouteFormChanged: () -> Unit,
    routeUiState: RouteUiState,
    onStartGuidance: () -> Unit,
    proximityWarning: String?,
    loadInitialData: suspend () -> Unit = { delay(1500) },
) {
    var dataReady by remember { mutableStateOf(false) }
    var showMain by rememberSaveable { mutableStateOf(false) }
    val activity = LocalActivity.current as? ComponentActivity

    // 지난번에 고른 테마 불러오기 (처음 한 번)
    remember {
        activity?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.let { Soft.isBlack = it.getBoolean(KEY_BLACK, false) }
        true
    }

    LaunchedEffect(Unit) {
        try { loadInitialData() } finally { dataReady = true }
    }

    // 느린 에뮬레이터에서 로딩 애니메이션 완료 이벤트가 누락되어도 메인 화면으로 진행.
    LaunchedEffect(dataReady, showMain) {
        if (dataReady && !showMain) {
            delay(2500)
            showMain = true
        }
    }

    // 상태바 아이콘 색: 로딩(파란 배경)·블랙 테마는 흰색, 밝은 테마 메인은 검은색
    val lightBars = showMain && !Soft.isBlack
    SideEffect { activity?.let { applySystemBars(it, lightBackground = lightBars) } }

    // 테마를 바꾸면 저장해서 다음에 앱을 켤 때도 유지
    LaunchedEffect(Unit) {
        snapshotFlow { Soft.isBlack }.drop(1).collect { value ->
            activity?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()?.putBoolean(KEY_BLACK, value)?.apply()
        }
    }

    MaterialTheme(colorScheme = AppColors) {
        Crossfade(targetState = showMain, animationSpec = tween(450), label = "screen") { main ->
            if (main) {
                MainScreen(
                    mapContent = mapContent,
                    onAnalyzeRoute = onAnalyzeRoute,
                    onMyLocation = onMyLocation,
                    onRouteFormChanged = onRouteFormChanged,
                    routeUiState = routeUiState,
                    onStartGuidance = onStartGuidance,
                    proximityWarning = proximityWarning,
                )
            } else {
                BikeLoadingScreen(isLoading = !dataReady, onFinished = { showMain = true })
            }
        }
    }
}

private fun applySystemBars(activity: ComponentActivity, lightBackground: Boolean) {
    val style = if (lightBackground) {
        SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
    } else {
        SystemBarStyle.dark(AndroidColor.TRANSPARENT)
    }
    activity.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
}
