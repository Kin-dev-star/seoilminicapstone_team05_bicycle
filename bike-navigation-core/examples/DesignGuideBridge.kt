package com.example.app.ui.guide

// 예시: 길안내 모듈(NavigationSession)을 디자인 팀 bike-ui-export 의 GuideState 에 연결.
// 디자인 팀 화면(위쪽 배너 · 작은 칸 · 내 위치 핀)이 그대로 길안내를 보여 주게 됩니다.
// 디자인 팀 코드(GuideState, Maneuver, Turn, GeoPoint)가 들어 있는 프로젝트에 넣어서 쓰세요.

import com.example.bicyclesafety.navigation.NavRoute
import com.example.bicyclesafety.navigation.NavigationSession
import com.example.bicyclesafety.navigation.Maneuver as NavManeuver

/** 길안내 모듈 방향 → 디자인 팀 화살표 */
fun NavManeuver.toTurn(): Turn = when (this) {
    NavManeuver.LEFT, NavManeuver.SLIGHT_LEFT -> Turn.Left
    NavManeuver.RIGHT, NavManeuver.SLIGHT_RIGHT -> Turn.Right
    NavManeuver.UTURN -> Turn.UTurn
    else -> Turn.Straight
}

/**
 * NavigationSession 의 안내 상태를 GuideState 에 넣는 Listener.
 * GuideState.startRoute() 의 자체 계산은 쓰지 않고, 모듈이 계산한 값을 next · following · remainingMeters 에 직접 넣음
 * (디자인 팀 README: "경로 API 가 방향을 직접 알려주면 next = Maneuver(...) 처럼 넣어도 됨").
 *
 * @param speak TTS 로 읽기 (예: TextToSpeech.speak). 디자인 팀 VoiceGuide 를 쓰면 null 로 두고 설정에서 음성 안내를 켜기
 * @param onRouteDrawn 재탐색으로 경로가 바뀌면 지도에 새 경로선 그리기
 */
class GuideStateListener(
    private val guide: GuideState,
    private val speak: ((String) -> Unit)? = null,
    private val onRouteDrawn: (NavRoute) -> Unit = {},
    private val onFinished: () -> Unit = {},
) : NavigationSession.SimpleListener() {

    override fun onUpdate(state: NavigationSession.State) {
        if (!state.hasFix) return
        guide.next = Maneuver(state.maneuver.toTurn(), state.distanceToNext, road = null)
        guide.following = state.nextManeuver?.let { Maneuver(it.toTurn(), state.nextDistance) }
        guide.remainingMeters = state.remainingMeters
    }

    override fun onSpeak(text: String) { speak?.invoke(text) }

    override fun onRouteChanged(newRoute: NavRoute) = onRouteDrawn(newRoute)

    override fun onEnded() {
        guide.stopRoute()     // 배너 · 작은 칸 사라짐
        onFinished()
    }
}

/*
 * Activity 에서 쓰기 (디자인 팀 README 의 MainActivity 기준):
 *
 *   private val app = AppState(searchRoute = ::searchRoute, ...)
 *   private val routeWorker = Executors.newSingleThreadExecutor()
 *   private val nav = NavigationSession(
 *       GuideStateListener(app.guide, speak = { tts.speak(it, TextToSpeech.QUEUE_ADD, null, "nav") }),
 *       KakaoRouteAdapter.rerouter(BuildConfig.KAKAO_REST_API_KEY),
 *       ContextCompat.getMainExecutor(this),
 *       routeWorker,
 *   )
 *
 *   // 경로 설정 창의 '안내 시작' → 카카오 결과로 안내 시작
 *   nav.start(KakaoRouteAdapter.toNavRoute(kakaoResult))
 *
 *   // GPS 위치가 올 때마다 (LocationListener)
 *   app.guide.myLocation = GeoPoint(location.latitude, location.longitude)   // 내 위치 핀
 *   nav.onLocation(location.latitude, location.longitude,
 *       if (location.hasAccuracy()) location.accuracy else -1f,
 *       if (location.hasSpeed()) location.speed else -1f,
 *       location.time)
 */
