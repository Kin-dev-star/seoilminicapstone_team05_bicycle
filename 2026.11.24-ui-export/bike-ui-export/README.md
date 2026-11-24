# 자전거 앱 화면 — 다른 프로젝트에 넣기

## 들어 있는 것
| 파일 | 내용 |
|---|---|
| `ui/BikeApp.kt` | 로딩화면 → 메인 화면 전체 흐름 |
| `ui/loading/BikeLoadingScreen.kt` | 자전거 로딩 애니메이션 |
| `ui/main/MainScreen.kt` | 지도 영역, 원통 메뉴, 모아보기, 현재 위치 버튼, 경로 설정 창, 마이페이지, 설정 |
| `ui/guide/GuideState.kt` | 길안내 값: 내 위치(핀), 다음·그다음 안내, 남은 거리, 방향 종류 |
| `ui/guide/RouteGuide.kt` | 경로 좌표 + 내 위치 → 다음 안내(방향·남은 거리) 계산 |
| `ui/guide/GuideViews.kt` | 내 위치 핀, 위쪽 길안내 배너, 오른쪽 위 작은 칸 |
| `ui/guide/NavIcons.kt` | 핀 · 좌회전 · 우회전 · 직진 · 유턴 아이콘 |
| `ui/guide/TempMap.kt` | 임시 지도 그림 · 좌표 변환 · 데모 값 (지도 SDK 붙이기 전) |
| `ui/theme/Soft.kt` | 소프트 UI 색·그림자 (밝은 테마 / 블랙 테마) |
| `res/values/bike_colors.xml` | (선택) 앱 켤 때 깜빡임 방지용 파란색 |

## 1. 파일 복사
이 폴더의 `app` 폴더를 프로젝트 폴더에 그대로 덮어 넣으면 같은 경로로 들어가요.
패키지 이름(`com.example.app.ui`)은 그대로 둬도 동작해요.
바꾸고 싶으면 Android Studio에서 `ui` 패키지를 우클릭 → **Refactor → Move** 로 옮기면 import 까지 같이 바뀌어요.

## 2. `app/build.gradle.kts`
```kotlin
plugins {
    id("org.jetbrains.kotlin.plugin.compose")   // Kotlin 2.0 이상 (루트 build.gradle.kts 에도 같은 버전으로 등록)
}

android {
    defaultConfig { minSdk = 23 }               // 23 이상
    buildFeatures { compose = true }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.12.01"))   // 2025.08 이상 필요 (그림자 기능)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.activity:activity-compose:1.10.1")           // 1.10 이상 필요
}
```

## 3. Activity 에서 쓰기
```kotlin
class MainActivity : ComponentActivity() {
    private val guide = GuideState()        // 길안내 값: 여기 값만 바꾸면 핀·화살표가 따라 바뀜

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TempMap.fillDemo(guide)             // (임시) 데모 값. 실제 위치를 넣기 시작하면 지우기
        setContent { BikeApp(guide = guide) }
    }
}
```
길안내 값 바꾸기:
```kotlin
guide.startRoute(경로좌표)              // List<GeoPoint>: 경로 API 가 준 좌표 목록
guide.updateLocation(위도, 경도)        // GPS 위치가 올 때마다: 핀 이동 + 다음 안내 계산
guide.stopRoute()                       // 안내 끝 → 배너(또는 작은 칸) 사라짐
guide.next = Maneuver(Turn.Left, 150, road = "여의서로")   // 경로 API 가 방향·길 이름을 직접 주면 바로 넣어도 됨
```
- 길안내 표시: 앱의 설정 → 주행 → 길안내 표시 (배너 / 작은 칸, 기본 배너).
  코드에서 정하려면 `MainScreen(guide = guide, settings = AppSettings().apply { guideStyle = GuideStyle.Box })`
- 실제 로딩 작업 넣기: `BikeApp(loadInitialData = { /* 지도 초기화 등 */ }, guide = guide)`
- 메인 화면만 쓰기: `setContent { MaterialTheme { MainScreen(guide = guide) } }`
- 블랙 테마: `Soft.isBlack = true` (설정 화면의 블랙 테마 스위치와 같고, 바꾸면 자동 저장)
- 실제 지도를 붙일 때: `MainScreen.kt` 의 `TempMapView` 자리에 지도 화면을 넣고, `MyLocationPin` 의 `toScreen` 을
  지도 SDK 의 좌표 변환으로 바꾸기 (예: 네이버 `naverMap.projection.toScreenLocation`)

## 4. (선택) 켤 때 흰 화면 깜빡임 막기
앱 테마(`res/values/themes.xml`)에 한 줄 추가:
```xml
<item name="android:windowBackground">@color/bike_blue</item>
```

## 참고
- 지도와 데모 경로·위치, 검색, 기록 숫자, 설정 값은 아직 임시예요 (지도 SDK·GPS 연결 전).
- 확인한 환경: AGP 8.13.2 · Kotlin 2.3.21 · Compose BOM 2025.12.01 · activity-compose 1.10.1 · compileSdk 36
