package com.example.app.ui.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.app.ui.theme.Soft
import com.example.app.ui.theme.softInset
import com.example.app.ui.theme.softPressable
import com.example.app.ui.theme.softRaised
import com.example.bicyclesafety.BuildConfig
import com.example.bicyclesafety.KakaoRouteRepository
import com.example.bicyclesafety.data.AppRepositories
import com.example.bicyclesafety.data.HazardReport
import com.example.bicyclesafety.data.ResultCallback
import com.example.bicyclesafety.data.SessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/*
 * 제보게시판 화면 (자료조사·발표 담당 · 김지우, 2026-11-26)
 *
 * 목록 → 상세 → 글쓰기 세 화면을 이 파일 하나에서 바꿔 가며 보여 줍니다.
 * 저장·조회는 data 패키지의 AppRepositories.reports(context) 만 사용합니다
 * (로컬/서버 전환은 저장소가 알아서 함, docs/TEAM_INTEGRATION_GUIDE.md 2장).
 *
 * 연결 방법 (MainScreen.kt 의 PageContent() 안 when (page) 에 한 줄):
 *     NavItem.Report -> ReportScreen()
 *
 * data 패키지 모델·인터페이스는 수정하지 않았습니다.
 * - 제보 종류는 제목 앞 "[종류] " 로 저장
 */

private const val PAGE_SIZE = 20
private const val TITLE_MAX = 50
private const val CONTENT_MAX = 1000

private val RpEdge = 20.dp
private val RpCardShape = RoundedCornerShape(18.dp)
private val RpFieldShape = RoundedCornerShape(16.dp)
private val RpWellShape = RoundedCornerShape(12.dp)
private val RpPill = RoundedCornerShape(percent = 50)
private val RpDanger = Color(0xFFE5484D)

// ───────────────────────── 목록 상태 (상세에서 돌아와도 스크롤 유지) ─────────────────────────

@Stable
private class ReportListState {
    var items by mutableStateOf<List<HazardReport>>(emptyList())
    var nextPage by mutableIntStateOf(0)
    var hasMore by mutableStateOf(false)
    var loading by mutableStateOf(false)
    var loadedOnce by mutableStateOf(false)
    var error by mutableStateOf("")
    val scroll = LazyListState()

    fun load(context: Context, reset: Boolean) {
        if (loading) return
        loading = true
        error = ""
        val page = if (reset) 0 else nextPage
        AppRepositories.reports(context).list(page, PAGE_SIZE, object : ResultCallback<List<HazardReport>> {
            override fun onSuccess(result: List<HazardReport>) {
                items = (if (reset) result else items + result).distinctBy { it.id }
                nextPage = page + 1
                hasMore = result.size >= PAGE_SIZE
                loading = false
                loadedOnce = true
            }

            override fun onError(message: String) {
                error = message.ifBlank { "제보를 불러오지 못했어요." }
                loading = false
                loadedOnce = true
            }
        })
    }

    /** 방금 쓴 글을 맨 위에 */
    fun prepend(report: HazardReport) {
        items = (listOf(report) + items).distinctBy { it.id }
    }
}

// ───────────────────────── 진입점 ─────────────────────────

/** 제보게시판 (NavItem.Report 화면 본문) */
@Composable
fun ReportScreen() {
    val context = LocalContext.current
    val list = remember { ReportListState() }
    var writing by rememberSaveable { mutableStateOf(false) }
    var detailId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (!list.loadedOnce) list.load(context, reset = true)
    }
    // 상세 화면에서 뒤로 가기 → 목록 (글쓰기 화면은 자체 BackHandler 로 확인 창을 띄움)
    BackHandler(enabled = !writing && detailId != null) { detailId = null }

    val openId = detailId
    when {
        writing -> ReportWriteBody(
            onClose = { writing = false },
            onSaved = { saved ->
                list.prepend(saved)
                writing = false
                detailId = saved.id
            },
        )
        openId != null -> ReportDetailBody(
            id = openId,
            cached = list.items.firstOrNull { it.id == openId },
            onBack = { detailId = null },
        )
        else -> ReportListBody(list, onOpen = { detailId = it }, onWrite = { writing = true })
    }
}

// ───────────────────────── 목록 ─────────────────────────

@Composable
private fun ReportListBody(list: ReportListState, onOpen: (String) -> Unit, onWrite: () -> Unit) {
    val context = LocalContext.current
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = list.scroll,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = RpEdge, end = RpEdge, top = 8.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "intro") {
                ListIntro(loading = list.loading, onRefresh = { list.load(context, reset = true) })
            }
            when {
                list.items.isEmpty() && (list.loading || !list.loadedOnce) -> item(key = "loading") {
                    CenterNote("제보를 불러오는 중…")
                }
                list.items.isEmpty() && list.error.isNotEmpty() -> item(key = "error") {
                    Column(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(list.error, color = RpDanger, fontSize = 13.sp, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(12.dp))
                        RpSoftButton("다시 시도", Icons.Filled.Refresh) { list.load(context, reset = true) }
                    }
                }
                list.items.isEmpty() -> item(key = "empty") {
                    EmptyState()
                }
            }
            items(list.items, key = { it.id }) { report ->
                ReportCard(report) { onOpen(report.id) }
            }
            if (list.items.isNotEmpty() && list.hasMore) item(key = "more") {
                Box(Modifier.fillMaxWidth().padding(top = 4.dp), contentAlignment = Alignment.Center) {
                    if (list.loading) Text("불러오는 중…", color = Soft.TextHint, fontSize = 13.sp)
                    else RpSoftButton("제보 더 보기", null) { list.load(context, reset = false) }
                }
            }
            if (list.items.isNotEmpty() && list.error.isNotEmpty()) item(key = "moreError") {
                Text(list.error, color = RpDanger, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
        }
        RpPrimaryButton(
            text = "제보 작성",
            icon = Icons.Filled.Add,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            onClick = onWrite,
        )
    }
}

@Composable
private fun ListIntro(loading: Boolean, onRefresh: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .softRaised(RpCardShape)
            .clip(RpCardShape)
            .background(Soft.Base)
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconWell(Icons.Filled.ReportProblem, size = 44.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("자전거 길 위험을 함께 알려요", color = Soft.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text("사고 현장이나 위험한 곳을 위치와 함께 제보해 주세요", color = Soft.TextSub, fontSize = 12.sp)
            }
        }
        if (!AppRepositories.USE_SERVER_FOR_REPORTS) {
            Spacer(Modifier.height(10.dp))
            Text(
                "지금은 서버 연결 전이라 이 휴대폰에서 쓴 제보만 보여요",
                color = Soft.TextHint, fontSize = 11.sp,
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("최근 제보", color = Soft.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            RpSoftButton(if (loading) "불러오는 중…" else "새로고침", Icons.Filled.Refresh, enabled = !loading, onClick = onRefresh)
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        Modifier.fillMaxWidth().padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconWell(Icons.Filled.Place, size = 72.dp, iconSize = 30.dp)
        Spacer(Modifier.height(16.dp))
        Text("아직 제보가 없어요", color = Soft.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(
            "아래 '제보 작성'을 눌러 첫 위험 지점을 알려 주세요",
            color = Soft.TextSub, fontSize = 13.sp, textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ReportCard(report: HazardReport, onClick: () -> Unit) {
    val (kind, title) = remember(report.title) { splitReportTitle(report.title) }
    Row(
        Modifier
            .fillMaxWidth()
            .softRaised(RpCardShape)
            .clip(RpCardShape)
            .background(Soft.Base)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(64.dp)
                .clip(RpWellShape)
                .background(Soft.Base)
                .softInset(RpWellShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(kindIcon(kind), contentDescription = null, tint = kindColor(kind), modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            if (kind != null) KindTag(kind)
            Text(
                title.ifBlank { "(제목 없음)" }, color = Soft.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = if (kind != null) 4.dp else 0.dp),
            )
            Text(
                report.placeName.ifBlank { "위치 정보 없음" }, color = Soft.TextSub, fontSize = 12.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                listOf(report.authorNickname.ifBlank { "익명" }, reportTimeAgo(report.createdAt))
                    .filter { it.isNotBlank() }.joinToString(" · "),
                color = Soft.TextHint, fontSize = 11.sp,
            )
        }
    }
}

// ───────────────────────── 상세 ─────────────────────────

@Composable
private fun ReportDetailBody(id: String, cached: HazardReport?, onBack: () -> Unit) {
    val context = LocalContext.current
    var report by remember(id) { mutableStateOf(cached) }
    var error by remember(id) { mutableStateOf("") }
    LaunchedEffect(id) {
        AppRepositories.reports(context).get(id, object : ResultCallback<HazardReport> {
            override fun onSuccess(result: HazardReport) {
                report = result
                error = ""
            }

            override fun onError(message: String) {
                if (report == null) error = message.ifBlank { "제보를 불러오지 못했어요." }
            }
        })
    }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = RpEdge)
            .padding(top = 4.dp, bottom = 32.dp)
    ) {
        RpSoftButton("목록", Icons.AutoMirrored.Filled.ArrowBack, onClick = onBack)
        Spacer(Modifier.height(18.dp))

        val r = report
        if (r == null) {
            CenterNote(error.ifEmpty { "불러오는 중…" }, color = if (error.isEmpty()) Soft.TextHint else RpDanger)
            return@Column
        }
        val (kind, title) = splitReportTitle(r.title)

        if (kind != null) KindTag(kind)
        Text(
            title.ifBlank { "(제목 없음)" }, color = Soft.Text, fontSize = 21.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            listOf(r.authorNickname.ifBlank { "익명" }, reportFullTime(r.createdAt)).filter { it.isNotBlank() }.joinToString(" · "),
            color = Soft.TextHint, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp),
        )

        // 위치
        Spacer(Modifier.height(18.dp))
        RpCard {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconWell(Icons.Filled.Place, size = 40.dp, iconSize = 20.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(r.placeName.ifBlank { "위치 이름 없음" }, color = Soft.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    if (hasCoords(r.latitude, r.longitude)) {
                        Text(coordText(r.latitude, r.longitude), color = Soft.TextSub, fontSize = 12.sp)
                    }
                }
            }
            if (hasCoords(r.latitude, r.longitude)) {
                Box(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp)) {
                    RpSoftButton("지도에서 보기", Icons.Filled.Map) { openInMap(context, r.placeName, r.latitude, r.longitude) }
                }
            }
        }

        // 내용
        Spacer(Modifier.height(16.dp))
        RpCard {
            Text(
                r.content.ifBlank { "(내용 없음)" }, color = Soft.Text, fontSize = 15.sp, lineHeight = 22.sp,
                modifier = Modifier.padding(18.dp),
            )
        }
    }
}

// ───────────────────────── 글쓰기 ─────────────────────────

@Composable
private fun ReportWriteBody(onClose: () -> Unit, onSaved: (HazardReport) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    val restKey = BuildConfig.KAKAO_REST_API_KEY
    val session = remember { SessionStore.load(context) }

    var kind by rememberSaveable { mutableStateOf<String?>(REPORT_KINDS[0]) }
    var title by rememberSaveable { mutableStateOf("") }
    var content by rememberSaveable { mutableStateOf("") }
    // 선택한 위치 (장소 검색 또는 현재 위치)
    var placeName by rememberSaveable { mutableStateOf("") }
    var placeDetail by rememberSaveable { mutableStateOf("") }
    var lat by rememberSaveable { mutableStateOf<Double?>(null) }
    var lng by rememberSaveable { mutableStateOf<Double?>(null) }
    // 장소 검색
    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<KakaoRouteRepository.PlaceOption>>(emptyList()) }
    var searchStatus by remember { mutableStateOf("") }
    var locating by remember { mutableStateOf(false) }
    // 등록
    var submitting by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var confirmLeave by remember { mutableStateOf(false) }

    val hasPlace = lat != null && lng != null && placeName.isNotBlank()
    val dirty = title.isNotBlank() || content.isNotBlank() || hasPlace

    val leave: () -> Unit = {
        if (!submitting) {
            if (dirty) confirmLeave = true else onClose()
        }
    }
    BackHandler(onBack = leave)

    // 장소 검색 (입력 후 0.35초 멈추면 검색)
    LaunchedEffect(query) {
        val q = query.trim()
        results = emptyList()
        searchStatus = ""
        if (q.length < 2) return@LaunchedEffect
        if (restKey.isBlank()) {
            searchStatus = "카카오 REST 키가 없어 검색할 수 없어요 (local.properties 확인)"
            return@LaunchedEffect
        }
        delay(350)
        searchStatus = "검색 중…"
        val found = withContext(Dispatchers.IO) {
            runCatching { KakaoRouteRepository.searchPlacesCombined(restKey, q) }
        }
        found.onSuccess {
            results = it.take(6)
            searchStatus = if (it.isEmpty()) "검색 결과가 없어요. 다른 이름이나 주소로 검색해 보세요." else ""
        }.onFailure {
            searchStatus = "검색 실패 · 네트워크를 확인해 주세요"
        }
    }

    fun pickPlace(p: KakaoRouteRepository.PlaceOption) {
        placeName = p.name
        placeDetail = if (p.address.isNotBlank() && p.address != p.name) p.address else coordText(p.latitude, p.longitude)
        lat = p.latitude
        lng = p.longitude
        query = ""
        message = ""
        focus.clearFocus()
    }

    fun clearPlace() {
        placeName = ""; placeDetail = ""; lat = null; lng = null
    }

    fun useMyLocation() {
        locating = true
        message = ""
        fetchCurrentLocation(context) { loc ->
            if (loc == null) {
                locating = false
                message = "현재 위치를 가져오지 못했어요. GPS를 켜고 다시 시도하거나 장소를 검색해 주세요."
                return@fetchCurrentLocation
            }
            scope.launch {
                val address = withContext(Dispatchers.IO) { reverseGeocode(restKey, loc.latitude, loc.longitude) }
                lat = loc.latitude
                lng = loc.longitude
                placeName = address ?: "현재 위치"
                placeDetail = "현재 위치 · " + coordText(loc.latitude, loc.longitude)
                query = ""
                locating = false
                focus.clearFocus()
            }
        }
    }

    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { it }) useMyLocation()
        else message = "위치 권한이 없어 현재 위치를 쓸 수 없어요. 장소 검색을 이용해 주세요."
    }

    fun submit() {
        focus.clearFocus()
        val problem = when {
            !hasPlace -> "제보 위치를 선택해 주세요."
            title.isBlank() -> "제목을 입력해 주세요."
            content.isBlank() -> "내용을 입력해 주세요."
            else -> ""
        }
        message = problem
        if (problem.isNotEmpty() || submitting) return
        submitting = true
        val draft = HazardReport(
            "", joinReportTitle(kind, title), content.trim(), placeName.trim(),
            lat ?: 0.0, lng ?: 0.0, "", 0L,
        )
        AppRepositories.reports(context).create(draft, object : ResultCallback<HazardReport> {
            override fun onSuccess(result: HazardReport) {
                submitting = false
                onSaved(result)
            }

            override fun onError(error: String) {
                submitting = false
                message = error.ifBlank { "제보를 등록하지 못했어요. 잠시 후 다시 시도해 주세요." }
            }
        })
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = RpEdge)
            .padding(top = 4.dp, bottom = 32.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RpSoftButton("목록", Icons.AutoMirrored.Filled.ArrowBack, onClick = leave)
            Spacer(Modifier.weight(1f))
            Text("새 제보", color = Soft.TextSub, fontSize = 13.sp)
        }

        // 1. 종류
        FormLabel("어떤 위험인가요?")
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            REPORT_KINDS.forEach { k -> RpChip(k, selected = kind == k) { kind = if (kind == k) null else k } }
        }

        // 2. 위치
        FormLabel("제보 위치", required = true)
        if (hasPlace) {
            RpCard {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconWell(Icons.Filled.Place, size = 40.dp, iconSize = 20.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(placeName, color = Soft.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (placeDetail.isNotBlank())
                            Text(placeDetail, color = Soft.TextSub, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.width(8.dp))
                    RpSoftButton("변경", null) { clearPlace() }
                }
            }
        } else {
            RpField(
                value = query,
                onChange = { query = it },
                placeholder = "장소 이름이나 주소 검색 (예: 여의도 한강공원)",
                singleLine = true,
                leading = { Icon(Icons.Filled.Search, contentDescription = null, tint = Soft.TextHint, modifier = Modifier.size(20.dp)) },
                imeAction = ImeAction.Search,
            )
            if (results.isNotEmpty() || searchStatus.isNotEmpty()) {
                Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                    results.forEach { place ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RpWellShape)
                                .clickable { pickPlace(place) }
                                .padding(horizontal = 10.dp, vertical = 9.dp)
                        ) {
                            Text(place.name, color = Soft.Accent, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (place.address.isNotBlank() && place.address != place.name)
                                Text(place.address, color = Soft.TextSub, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    if (searchStatus.isNotEmpty())
                        Text(searchStatus, color = Soft.TextHint, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            RpSoftButton(
                if (locating) "현재 위치 찾는 중…" else "지금 있는 곳으로 제보",
                Icons.Filled.MyLocation,
                enabled = !locating,
            ) {
                if (hasLocationPermission(context)) useMyLocation()
                else locationPermission.launch(arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                ))
            }
        }

        // 3. 제목
        FormLabel("제목", required = true, hint = "${title.length}/$TITLE_MAX")
        RpField(
            value = title,
            onChange = { if (it.length <= TITLE_MAX) title = it.replace("\n", " ") },
            placeholder = "예) 자전거도로 위 맨홀 뚜껑 들림",
            singleLine = true,
            imeAction = ImeAction.Next,
        )

        // 4. 내용
        FormLabel("내용", required = true, hint = "${content.length}/$CONTENT_MAX")
        RpField(
            value = content,
            onChange = { if (it.length <= CONTENT_MAX) content = it },
            placeholder = "언제, 무엇이 위험했는지 적어 주세요.\n예) 오후 6시쯤 내리막 끝 맨홀 뚜껑이 들려 있어 넘어질 뻔했어요.",
            singleLine = false,
            minHeight = 150.dp,
        )

        Spacer(Modifier.height(14.dp))
        Text(
            when {
                AppRepositories.USE_SERVER_FOR_REPORTS && (session == null || !session.serverVerified) ->
                    "서버에 올리려면 마이페이지에서 카카오 로그인이 필요해요"
                session != null -> "작성자: ${session.nickname}"
                else -> "로그인하지 않으면 '익명'으로 올라가요"
            },
            color = Soft.TextHint, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp),
        )
        if (message.isNotEmpty()) {
            Text(message, color = RpDanger, fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp, top = 8.dp))
        }
        Spacer(Modifier.height(20.dp))
        RpPrimaryButton(
            text = if (submitting) "등록 중…" else "제보 등록",
            icon = null,
            enabled = !submitting,
            modifier = Modifier.fillMaxWidth(),
            onClick = { submit() },
        )
    }

    if (confirmLeave) {
        Dialog(onDismissRequest = { confirmLeave = false }) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RpCardShape)
                    .background(Soft.Base)
                    .padding(22.dp)
            ) {
                Text("작성을 그만둘까요?", color = Soft.Text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text("입력한 내용이 사라져요.", color = Soft.TextSub, fontSize = 13.sp)
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    RpSoftButton("계속 쓰기", null) { confirmLeave = false }
                    Spacer(Modifier.width(12.dp))
                    RpSoftButton("나가기", null, textColor = RpDanger) {
                        confirmLeave = false
                        onClose()
                    }
                }
            }
        }
    }
}

// ───────────────────────── 작은 부품 ─────────────────────────

@Composable
private fun FormLabel(text: String, required: Boolean = false, hint: String? = null) {
    Row(
        Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 24.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, color = Soft.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        if (required) Text(" *", color = RpDanger, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        if (hint != null) Text(hint, color = Soft.TextHint, fontSize = 11.sp)
    }
}

/** 볼록한 카드 */
@Composable
private fun RpCard(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .softRaised(RpCardShape)
            .clip(RpCardShape)
            .background(Soft.Base)
    ) { content() }
}

/** 오목한 입력칸 */
@Composable
private fun RpField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean,
    minHeight: Dp = 0.dp,
    leading: (@Composable () -> Unit)? = null,
    imeAction: ImeAction = ImeAction.Default,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RpFieldShape)
            .background(Soft.Base)
            .softInset(RpFieldShape)
            .heightIn(min = maxOf(50.dp, minHeight))
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(10.dp))
        }
        BasicTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.weight(1f),
            singleLine = singleLine,
            textStyle = TextStyle(color = Soft.Text, fontSize = 14.sp, lineHeight = 20.sp),
            cursorBrush = SolidColor(Soft.Accent),
            keyboardOptions = KeyboardOptions(imeAction = imeAction),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) Text(placeholder, color = Soft.TextHint, fontSize = 14.sp, lineHeight = 20.sp)
                    inner()
                }
            },
        )
    }
}

/** 종류 선택 칩: 선택되면 오목하게 */
@Composable
private fun RpChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shapeMod = if (selected) {
        Modifier.clip(RpPill).background(Soft.Base).softInset(RpPill)
    } else {
        Modifier.softRaised(RpPill, offset = 3.dp, blur = 6.dp).clip(RpPill).background(Soft.Base)
    }
    Box(
        Modifier
            .height(36.dp)
            .then(shapeMod)
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label, color = if (selected) kindColor(label) else Soft.TextSub, fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

/** 누르면 오목해지는 작은 버튼 */
@Composable
private fun RpSoftButton(
    text: String,
    icon: ImageVector?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    textColor: Color = Soft.Accent,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 1f else 0f, tween(110), label = "rpPress")
    Row(
        modifier
            .height(38.dp)
            .softPressable(RpPill) { press }
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val color = if (enabled) textColor else Soft.TextHint
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** 파란 주 버튼 (제보 작성 · 제보 등록) */
@Composable
private fun RpPrimaryButton(
    text: String,
    icon: ImageVector?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .height(52.dp)
            .softRaised(RpPill)
            .clip(RpPill)
            .background(Soft.Accent.copy(alpha = if (enabled) 1f else 0.5f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 26.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun IconWell(icon: ImageVector, size: Dp, iconSize: Dp = 22.dp, tint: Color = Soft.Accent) {
    Box(
        Modifier
            .size(size)
            .clip(RpWellShape)
            .background(Soft.Base)
            .softInset(RpWellShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
    }
}

@Composable
private fun KindTag(kind: String) {
    val c = kindColor(kind)
    Text(
        kind, color = c, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RpPill)
            .background(c.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Composable
private fun CenterNote(text: String, color: Color = Soft.TextHint) {
    Text(
        text, color = color, fontSize = 13.sp, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
    )
}

// ───────────────────────── 도우미 ─────────────────────────

private fun kindIcon(kind: String?): ImageVector = when (kind) {
    "사고 발생" -> Icons.Filled.Warning
    "도로 파손" -> Icons.Filled.Report
    "공사·장애물" -> Icons.Filled.Construction
    "위험 구간" -> Icons.Filled.ReportProblem
    else -> Icons.Filled.Place
}

private fun kindColor(kind: String?): Color = when (kind) {
    "사고 발생" -> RpDanger
    "도로 파손", "공사·장애물" -> Color(0xFFE08A00)
    else -> Soft.Accent
}

/** 0,0 은 좌표 없음으로 봄 (서울 범위 앱) */
private fun hasCoords(lat: Double, lng: Double): Boolean = !(lat == 0.0 && lng == 0.0)

private fun coordText(lat: Double, lng: Double): String = String.format(Locale.US, "%.5f, %.5f", lat, lng)

/** 카카오맵(앱 또는 웹)에서 위치 보기 */
private fun openInMap(context: Context, name: String, lat: Double, lng: Double) {
    val uri = Uri.parse("https://map.kakao.com/link/map/${Uri.encode(name.ifBlank { "제보 위치" })},$lat,$lng")
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}
