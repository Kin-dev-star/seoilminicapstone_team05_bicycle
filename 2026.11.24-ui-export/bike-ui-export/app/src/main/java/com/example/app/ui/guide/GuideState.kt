package com.example.app.ui.guide

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import kotlin.math.abs
import kotlin.math.ceil

/** 위도·경도 (지도 SDK 의 LatLng 과 같은 뜻. 이름이 겹치지 않게 따로 둠) */
data class GeoPoint(val latitude: Double, val longitude: Double)

/** 길안내 방향 = 화살표 아이콘 */
enum class Turn(val label: String) {
    Straight("직진"),
    Left("좌회전"),
    Right("우회전"),
    UTurn("유턴");

    val icon: ImageVector
        get() = when (this) {
            Straight -> NavIcons.Straight
            Left -> NavIcons.TurnLeft
            Right -> NavIcons.TurnRight
            UTurn -> NavIcons.UTurn
        }

    companion object {
        /**
         * 꺾는 각도로 방향 정하기 (오른쪽으로 꺾으면 +, 왼쪽이면 −, 단위 도).
         * [straightBelow] 보다 덜 꺾이면 직진, 150° 이상이면 유턴.
         */
        fun fromAngle(degrees: Double, straightBelow: Double = 30.0): Turn = when {
            abs(degrees) < straightBelow -> Straight
            abs(degrees) >= 150.0 -> UTurn
            degrees > 0 -> Right
            else -> Left
        }
    }
}

/** 안내 하나: [distanceMeters] m 뒤에 [turn]. [road] 는 들어갈 길 이름 (경로 API 가 알려주면) */
data class Maneuver(val turn: Turn, val distanceMeters: Int, val road: String? = null) {
    val distanceText: String get() = formatDistance(distanceMeters)

    /** "80m 후 우회전", 직진은 "300m 직진" */
    val text: String get() = if (turn == Turn.Straight) "$distanceText 직진" else "$distanceText 후 ${turn.label}"
}

/** 거리 글자: 47m, 120m, 1.2km */
fun formatDistance(meters: Int): String = when {
    meters >= 1000 -> "${meters / 1000}.${meters % 1000 / 100}km"
    meters >= 50 -> "${(meters + 5) / 10 * 10}m"
    else -> "${meters}m"
}

/**
 * 길안내 화면에 보이는 값. 여기 값만 바꾸면 핀과 화살표(칸 또는 위쪽 배너)가 알아서 바뀜.
 *
 * - 경로를 받으면: `startRoute(경로 좌표 목록)`
 * - GPS 위치가 올 때마다: `updateLocation(위도, 경도)` → 핀이 그 자리로 부드럽게 이동하고,
 *   경로가 있으면 다음 안내 · 그다음 안내 · 남은 거리를 다시 계산
 * - 안내 끝: `stopRoute()` → 화살표 사라짐
 * - 경로 API 가 방향을 직접 알려주면 계산 없이 `next = Maneuver(Turn.Left, 150, road = "여의서로")` 처럼 넣어도 됨
 */
@Stable
class GuideState {
    /** 내 위치. null 이면 핀을 숨김 */
    var myLocation: GeoPoint? by mutableStateOf(null)

    /** 다음 안내. null 이면 화살표를 숨김 */
    var next: Maneuver? by mutableStateOf(null)

    /** 그다음 안내 (거리는 다음 안내 지점에서부터). 없으면 null */
    var following: Maneuver? by mutableStateOf(null)

    /** 도착까지 남은 거리 (m). 모르면 null */
    var remainingMeters: Int? by mutableStateOf(null)

    /** 남은 시간을 어림할 때 쓰는 평균 속도 (km/h) */
    var speedKmh: Double by mutableStateOf(15.0)

    /** 도착까지 남은 시간 (분, 어림) */
    val remainingMinutes: Int?
        get() = remainingMeters?.let { ceil(it / (speedKmh * 1000.0 / 60.0)).toInt().coerceAtLeast(1) }

    /** 지금 따라가는 경로 (없으면 null) */
    var route: RouteGuide? = null
        private set

    fun startRoute(points: List<GeoPoint>) {
        val guide = RouteGuide(points)
        route = guide
        val here = myLocation
        if (here != null) show(guide.guidance(here)) else show(Guidance(null, null, null))
    }

    fun stopRoute() {
        route = null
        show(Guidance(null, null, null))
    }

    fun updateLocation(latitude: Double, longitude: Double) = updateLocation(GeoPoint(latitude, longitude))

    fun updateLocation(point: GeoPoint) {
        myLocation = point
        route?.let { show(it.guidance(point)) }
    }

    private fun show(g: Guidance) {
        next = g.next
        following = g.following
        remainingMeters = g.remainingMeters
    }
}
