package com.example.app.ui.guide

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt

internal const val METERS_PER_DEGREE = 6_371_000.0 * PI / 180.0   // 위도 1° 의 길이

private const val LOOK_M = 15.0       // 꺾인 정도를 잴 때 앞뒤로 보는 거리 (점이 촘촘한 곡선도 한 번의 꺾임으로 봄)
private const val PASSED_M = 5.0      // 안내 지점을 이만큼 지나면 다음 안내로 넘어감
private const val ARRIVED_M = 10.0    // 도착까지 이보다 가까우면 안내 끝

/** 경로 위 안내 지점: 시작점부터 [at] m 되는 곳에서 [turn] */
data class TurnPoint(val at: Double, val turn: Turn)

/** 지금 위치에서 본 안내: [next] 다음 안내, [following] 그다음 안내 (거리는 다음 안내 지점부터), [remainingMeters] 도착까지 */
data class Guidance(val next: Maneuver?, val following: Maneuver?, val remainingMeters: Int?)

/**
 * 경로(좌표 목록)를 따라갈 때 다음 안내(방향 + 남은 거리)를 계산.
 * 지도·경로 API 와 상관없이 좌표 목록만 있으면 됨 (경로 API 가 주는 좌표를 그대로 넣기).
 * - 꺾이는 각도가 [turnMinDegrees] 이상인 곳만 안내 (완만하게 굽은 길은 직진)
 * - 마지막 안내를 지나면 도착까지 직진, 도착하면 null
 */
class RouteGuide(points: List<GeoPoint>, private val turnMinDegrees: Double = 30.0) {
    private val origin: GeoPoint
    private val metersPerLng: Double
    private val xs: DoubleArray        // 시작점 기준 동쪽 거리 (m)
    private val ys: DoubleArray        // 시작점 기준 북쪽 거리 (m)
    private val along: DoubleArray     // 시작점부터 각 점까지 경로 길이 (m)

    /** 경로 전체 길이 (m) */
    val length: Double

    /** 안내 지점들 (가까운 순) */
    val turns: List<TurnPoint>

    init {
        require(points.size >= 2) { "경로에는 좌표가 2개 이상 있어야 해요" }
        origin = points.first()
        metersPerLng = METERS_PER_DEGREE * cos(Math.toRadians(origin.latitude))
        xs = DoubleArray(points.size) { (points[it].longitude - origin.longitude) * metersPerLng }
        ys = DoubleArray(points.size) { (points[it].latitude - origin.latitude) * METERS_PER_DEGREE }
        along = DoubleArray(points.size)
        for (i in 1 until points.size) along[i] = along[i - 1] + hypot(xs[i] - xs[i - 1], ys[i] - ys[i - 1])
        length = along.last()
        turns = findTurns()
    }

    /** [me] 에서 본 다음 안내. 도착했으면 null */
    fun next(me: GeoPoint): Maneuver? = guidance(me).next

    /** [me] 에서 본 안내: 다음 안내, 그다음 안내, 도착까지 남은 거리 (도착했으면 모두 null) */
    fun guidance(me: GeoPoint): Guidance {
        val s = progressOf(me)
        val left = length - s
        if (left <= ARRIVED_M) return Guidance(null, null, null)
        val i = turns.indexOfFirst { it.at > s - PASSED_M }
        if (i < 0) return Guidance(Maneuver(Turn.Straight, left.roundToInt()), null, left.roundToInt())   // 도착까지 직진
        val turn = turns[i]
        val following = turns.getOrNull(i + 1)?.let { Maneuver(it.turn, (it.at - turn.at).roundToInt()) }
        return Guidance(Maneuver(turn.turn, max(0.0, turn.at - s).roundToInt()), following, left.roundToInt())
    }

    /** [me] 를 경로에서 가장 가까운 곳으로 옮겼을 때 시작점부터 거리 (m) */
    fun progressOf(me: GeoPoint): Double {
        val px = (me.longitude - origin.longitude) * metersPerLng
        val py = (me.latitude - origin.latitude) * METERS_PER_DEGREE
        var best = Double.MAX_VALUE
        var bestAt = 0.0
        for (i in 0 until xs.size - 1) {
            val dx = xs[i + 1] - xs[i]
            val dy = ys[i + 1] - ys[i]
            val len2 = dx * dx + dy * dy
            val t = if (len2 == 0.0) 0.0 else (((px - xs[i]) * dx + (py - ys[i]) * dy) / len2).coerceIn(0.0, 1.0)
            val qx = xs[i] + dx * t - px
            val qy = ys[i] + dy * t - py
            val d2 = qx * qx + qy * qy
            if (d2 < best) {
                best = d2
                bestAt = along[i] + (along[i + 1] - along[i]) * t
            }
        }
        return bestAt
    }

    /** 시작점부터 경로를 따라 [meters] m 간 곳 */
    fun pointAt(meters: Double): GeoPoint {
        val (x, y) = xyAt(meters)
        return GeoPoint(origin.latitude + y / METERS_PER_DEGREE, origin.longitude + x / metersPerLng)
    }

    private fun xyAt(meters: Double): Pair<Double, Double> {
        val s = meters.coerceIn(0.0, length)
        var i = along.binarySearch(s)
        if (i >= 0) return xs[i] to ys[i]
        i = -i - 2                                   // s 가 들어 있는 구간의 시작 점
        val t = (s - along[i]) / (along[i + 1] - along[i])
        return (xs[i] + (xs[i + 1] - xs[i]) * t) to (ys[i] + (ys[i + 1] - ys[i]) * t)
    }

    /**
     * 꺾이는 곳 찾기: 점마다 앞뒤 [LOOK_M] 안에서 꺾인 각도를 재고, 크게 꺾인 점들이 가까이 붙어 있으면
     * 한 번의 꺾임으로 묶어서 묶음 전체에서 꺾인 각도로 방향을 정함
     * (꺾이는 곳이 점 여러 개로 나뉘어 있어도, 짧게 건넜다가 돌아오는 유턴도 한 번으로 안내)
     */
    private fun findTurns(): List<TurnPoint> {
        val sharp = (1 until xs.size - 1).filter { i ->
            abs(turningBetween(along[i] - LOOK_M, along[i] + LOOK_M)) >= turnMinDegrees
        }
        val out = ArrayList<TurnPoint>()
        var k = 0
        while (k < sharp.size) {
            var end = k
            while (end + 1 < sharp.size && along[sharp[end + 1]] - along[sharp[end]] < LOOK_M * 2) end++
            val first = along[sharp[k]]
            val angle = turningBetween(first - LOOK_M, along[sharp[end]] + LOOK_M)
            val turn = Turn.fromAngle(angle, turnMinDegrees)
            if (turn != Turn.Straight) out += TurnPoint(first, turn)
            k = end + 1
        }
        return out
    }

    /** 경로의 [from] m ~ [to] m 구간에서 방향이 바뀐 각도의 합 (오른쪽 +, 왼쪽 −) */
    private fun turningBetween(from: Double, to: Double): Double {
        val a = from.coerceIn(0.0, length)
        val b = to.coerceIn(0.0, length)
        val pts = ArrayList<Pair<Double, Double>>()
        pts += xyAt(a)
        var i = along.binarySearch(a).let { if (it >= 0) it + 1 else -it - 1 }   // a 다음 점부터
        while (i < along.size && along[i] < b) pts += xs[i] to ys[i++]
        pts += xyAt(b)
        var total = 0.0
        var prevX = 0.0
        var prevY = 0.0
        var hasPrev = false
        for (j in 1 until pts.size) {
            val dx = pts[j].first - pts[j - 1].first
            val dy = pts[j].second - pts[j - 1].second
            if (hypot(dx, dy) < 0.01) continue                  // 같은 자리 점은 건너뜀
            if (hasPrev) total += turnAngle(prevX, prevY, dx, dy)
            prevX = dx
            prevY = dy
            hasPrev = true
        }
        return total
    }
}

/** 방향 (dx 동쪽, dy 북쪽) 에서 다음 방향으로 꺾인 각도. 오른쪽 +, 왼쪽 − (−180 ~ 180) */
private fun turnAngle(inX: Double, inY: Double, outX: Double, outY: Double): Double {
    val a = Math.toDegrees(atan2(outX, outY) - atan2(inX, inY))
    return ((a + 540.0) % 360.0) - 180.0
}

/** 두 위치 사이 거리 (m, 가까운 거리용 근사) */
fun distanceMeters(a: GeoPoint, b: GeoPoint): Double {
    val dy = (b.latitude - a.latitude) * METERS_PER_DEGREE
    val dx = (b.longitude - a.longitude) * METERS_PER_DEGREE * cos(Math.toRadians((a.latitude + b.latitude) / 2))
    return hypot(dx, dy)
}
