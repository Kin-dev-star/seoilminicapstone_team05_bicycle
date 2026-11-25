package com.example.app.ui.main

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/*
 * 제보게시판 보조 기능 (자료조사·발표 담당 · 김지우)
 * - 현재 위치 가져오기, 좌표 → 주소 변환
 * - 제보 종류(제목 앞 "[종류] ")와 시간 표시 도우미
 */

// ───────────────────────── 제보 종류 ─────────────────────────

/** 제보 종류. data 모델에 필드를 늘리지 않으려고 제목 앞에 "[종류] " 로 붙여 저장 */
internal val REPORT_KINDS = listOf("사고 발생", "도로 파손", "공사·장애물", "위험 구간", "기타")

private val KIND_PREFIX = Regex("^\\[([^\\]]{1,10})]\\s*")

/** "[도로 파손] 맨홀 뚜껑 들림" → ("도로 파손", "맨홀 뚜껑 들림"). 종류가 없으면 (null, 제목) */
internal fun splitReportTitle(title: String): Pair<String?, String> {
    val m = KIND_PREFIX.find(title) ?: return null to title
    return m.groupValues[1] to title.substring(m.range.last + 1)
}

internal fun joinReportTitle(kind: String?, title: String): String =
    if (kind.isNullOrBlank()) title.trim() else "[$kind] ${title.trim()}"

// ───────────────────────── 시간 표시 ─────────────────────────

/** 방금 전 / 5분 전 / 3시간 전 / 2일 전 / 11.20 / 2025.11.20 */
internal fun reportTimeAgo(createdAt: Long, now: Long = System.currentTimeMillis()): String {
    if (createdAt <= 0) return ""
    val diff = (now - createdAt) / 1000
    return when {
        diff < 60 -> "방금 전"
        diff < 3600 -> "${diff / 60}분 전"
        diff < 86400 -> "${diff / 3600}시간 전"
        diff < 86400 * 7 -> "${diff / 86400}일 전"
        else -> {
            val sameYear = SimpleDateFormat("yyyy", Locale.KOREA).let { it.format(Date(createdAt)) == it.format(Date(now)) }
            SimpleDateFormat(if (sameYear) "MM.dd" else "yyyy.MM.dd", Locale.KOREA).format(Date(createdAt))
        }
    }
}

internal fun reportFullTime(createdAt: Long): String =
    if (createdAt <= 0) "" else SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA).format(Date(createdAt))

// ───────────────────────── 현재 위치 ─────────────────────────

/** 위치 권한이 있는지 */
internal fun hasLocationPermission(context: Context): Boolean =
    context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED ||
        context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED

/**
 * 지금 위치 한 번 가져오기 (Google Fused 위치 · MainActivity 와 같은 라이브러리).
 * 못 가져오면 마지막으로 알려진 위치, 그것도 없으면 null. 결과는 메인 스레드에서 옴.
 */
@SuppressLint("MissingPermission")
internal fun fetchCurrentLocation(context: Context, onResult: (Location?) -> Unit) {
    if (!hasLocationPermission(context)) { onResult(null); return }
    val client = LocationServices.getFusedLocationProviderClient(context)
    runCatching {
        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
            .addOnSuccessListener { loc ->
                if (loc != null) onResult(loc)
                else client.lastLocation
                    .addOnSuccessListener { last -> onResult(last) }
                    .addOnFailureListener { onResult(null) }
            }
            .addOnFailureListener { onResult(null) }
    }.onFailure { onResult(null) }
}

/**
 * 좌표 → 주소 (카카오 로컬 coord2address). 도로명 주소 우선, 없으면 지번.
 * 네트워크를 쓰므로 반드시 백그라운드 스레드에서 호출. 실패하면 null.
 */
internal fun reverseGeocode(restKey: String, latitude: Double, longitude: Double): String? {
    if (restKey.isBlank()) return null
    var connection: HttpURLConnection? = null
    return runCatching {
        val url = URL("https://dapi.kakao.com/v2/local/geo/coord2address.json?x=$longitude&y=$latitude")
        connection = (url.openConnection() as HttpURLConnection).apply {
            setRequestProperty("Authorization", "KakaoAK $restKey")
            connectTimeout = 8000
            readTimeout = 8000
        }
        val c = connection!!
        if (c.responseCode !in 200..299) return@runCatching null
        val body = c.inputStream.bufferedReader().use { it.readText() }
        val doc = JSONObject(body).optJSONArray("documents")?.optJSONObject(0) ?: return@runCatching null
        val road = doc.optJSONObject("road_address")?.optString("address_name").orEmpty()
        val jibun = doc.optJSONObject("address")?.optString("address_name").orEmpty()
        road.ifBlank { jibun }.ifBlank { null }
    }.getOrNull().also { connection?.disconnect() }
}
