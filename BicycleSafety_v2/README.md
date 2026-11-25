# BicycleSafety — 자전거 안전예방 지도 (미니캡스톤 5조)

서울시 자전거 사고 다발지역을 카카오맵에 표시하고, 카카오 자전거 경로 검색과 GPS·TTS 접근 경고를 제공하는 Android 앱입니다.

> **현재 상태: 12월 2일 작업 준비본 (UI v4, S6·S7 최종점검 보완본)**
> 정적 점검까지만 마친 상태이며, **Android Studio 실제 빌드와 기기 실행은 아직 확인 전**입니다.

이 문서는 기존 안내 문서 3개(`README_먼저읽기.md`, `README_통합_안내.md`, `README_경로설정_통합안내.md`)를 하나로 합친 것입니다. 이전 안내 원문은 `docs/참고_기존안내/`에 보관되어 있습니다.

---

## 1. 프로젝트 열기

1. 이 폴더(`app/`과 `settings.gradle.kts`가 함께 있는 폴더)를 `C:\BS`처럼 **경로가 짧고 한글이 없는 곳**에 둡니다. 상위 경로에 한글이 있으면 Gradle 오류가 날 수 있습니다.
2. Android Studio에서 `File > Open`으로 이 폴더를 엽니다. `app` 폴더만 선택하지 마세요.
3. Gradle Sync를 실행합니다. 처음에는 Kotlin/Compose 의존성을 내려받으므로 네트워크가 필요합니다.
   - 특정 PC에서만 쓰던 **JDK 25 강제 설정은 제거**했습니다. Android Studio의 **Gradle JDK(Embedded JDK/JBR, 호환 버전)**를 사용하세요.
   - Gradle JDK 버전 호환 오류가 나오면 오류 메시지부터 확인하고 설치된 JDK 설정을 조정합니다.
4. SDK 37을 설치하고, ARM64 변환을 지원하는 Pixel 에뮬레이터 또는 실제 Android 기기를 준비합니다.
5. 아래 2번의 `local.properties`를 설정한 뒤 `Build > Make Project`를 실행합니다.

## 2. `local.properties` 설정 (필수)

프로젝트 루트에 `local.properties` 파일을 만들고 **본인 PC의 SDK 위치와 카카오 키**를 입력합니다. 이미 파일이 있으면 기존 `sdk.dir=...` 줄은 지우지 말고 키만 추가합니다. `local.properties.example`을 참고하세요.

```properties
sdk.dir=C\:\\Users\\User\\AppData\\Local\\Android\\Sdk
KAKAO_NATIVE_APP_KEY=카카오디벨로퍼스에서_발급한_네이티브앱키
KAKAO_REST_API_KEY=카카오디벨로퍼스에서_발급한_REST_API_키
# TEAM_HAZARD_API_URL=https://팀서버/사고API주소  (팀과 형식 합의 후 사용, HTTPS만 허용)
```

저장 후 `File > Sync Project with Gradle Files` → `Build > Rebuild Project`를 실행합니다.

**보안 주의**
- `local.properties`는 `.gitignore` 대상입니다. 키를 채팅, 스크린샷, 공개 GitHub에 올리지 마세요. 노출된 적이 있는 키는 재발급하세요.
- 키는 `BuildConfig`로 앱에 들어가므로 APK에서 추출될 수 있습니다. 수업용 프로토타입 방식이며, 실제 배포 시에는 팀 백엔드에서 카카오 REST API를 대신 호출해야 합니다.
- 자세한 내용은 `docs/04_보안_기본수칙.md`를 참고하세요.

## 3. 동작 확인 순서

1. 앱 실행 → 로딩 화면 → **주행 전 안전 안내** 확인 → 실제 카카오맵 표시
2. 사고 마커 선택 → **과거 사고 데이터**의 지점명·건수·출처 표시
3. 경로 설정 시트 열기 → 출발지 `서울역`, 도착지 `상계역` 입력
4. `자전거도로 우선` 선택 → **경로 분석** → 거리, 시간, 검색된 장소명과 지도 위 경로선 표시
5. 경로 주변 150m 이내 **사고다발지역 개수** 표시 확인
6. `최단 경로`로 바꿔 다시 분석 → 경로선이 새 경로로 교체되는지 확인
7. 출발지를 비우거나 출발지·도착지를 같게 입력 → 안내 문구 확인
8. 현재 위치 버튼 → 위치 권한 허용/거부 각각 확인
9. 에뮬레이터 위치를 사고다발지역 근처로 설정 → 접근 경고 문구와 한국어 TTS 확인
10. 경로 결과 후 `안내 시작` → 첫 경로 안내 문구 확인

전체 체크리스트와 결과 표는 `docs/03_학교실습_체크리스트.md`에 있습니다. 빌드 오류와 실행 화면은 캡처로 남겨 주세요.

## 4. 어떤 기능이 어디에 있나

| 대상 | 파일 | 역할 |
|---|---|---|
| 앱 초기화 | `app/src/main/java/com/example/bicyclesafety/MapApplication.java` | 카카오맵 네이티브 키 초기화 |
| 지도·마커·GPS·TTS·경로 제어 | `.../bicyclesafety/MainActivity.java` | Java 기능 연결의 중심, 안전 확인 게이트 |
| 카카오 검색·경로 API | `.../bicyclesafety/KakaoRouteRepository.java` | 출발지/도착지 검색, 경로 좌표, 안내 문구 |
| 사고 데이터 입력 | `.../bicyclesafety/RiskDataRepository.java` | 앱 동봉 데이터 / 선택적 HTTPS 팀 서버 API 변환 |
| 사고 모델 | `.../bicyclesafety/RiskPoint.java` | 사고 건수, 사상자, 좌표, 출처 |
| Java–Kotlin 연결 | `.../app/ui/BikeUiBridge.kt` | 화면 상태와 Java 함수, Kakao MapView의 유일한 연결점 |
| 앱 화면 구성 | `.../app/ui/BikeApp.kt` | 로딩 → 메인 화면 전환, 느린 PC 로딩 탈출 처리 |
| 메인 화면 | `.../app/ui/main/MainScreen.kt` | 원통 메뉴, 경로 시트, GPS·경고 표시, 테마 |
| 로딩 화면 | `.../app/ui/loading/BikeLoadingScreen.kt` | 자전거 로딩 애니메이션 + 로고 |
| 디자인 스타일 | `.../app/ui/theme/Soft.kt` | 디자이너 제공 뉴모피즘 스타일 |
| 동봉 사고 기록 | `app/src/main/assets/seoul_bicycle_accidents.json` | KOROAD 데이터를 팀이 가공한 CSV 2,079행 |
| 화면 레이아웃 | `app/src/main/res/layout/activity_main.xml` | ComposeView 한 개 (다른 XML 레이아웃은 참조용 보관) |

```text
BicycleSafety_v2/
  README.md                       <- 이 문서
  local.properties.example        <- 키 설정 예시
  app/
    build.gradle.kts
    src/main/
      java/com/example/bicyclesafety/
        MainActivity.java
        KakaoRouteRepository.java
        RiskDataRepository.java
        RiskPoint.java
        MapApplication.java
      java/com/example/app/ui/
        BikeUiBridge.kt
        BikeApp.kt
        main/MainScreen.kt
        loading/BikeLoadingScreen.kt
        theme/Soft.kt
      assets/seoul_bicycle_accidents.json
  docs/
    01_일정과_구현현황.md
    02_팀API_연동계약.md
    03_학교실습_체크리스트.md
    04_보안_기본수칙.md
    05_새UI_카카오로그인_통합대기.md
    06~11 (질문, 점검 결과, 수정 내역)
    참고_기존안내/                <- 이전 README 원문
    학교제출_초안/                <- S6·S7 점검 PDF 초안
  tests/                          <- 정적 검사 스크립트
  gradle/, settings.gradle.kts, build.gradle.kts
```

## 5. 경로 검색 데이터 흐름

```text
MainScreen.kt (출발지 / 도착지 / 옵션 UI)
  ↓
BikeUiBridge.kt → MainActivity.java (메인 스레드에서 클릭 처리)
  ↓ 별도 백그라운드 스레드
KakaoRouteRepository.java
  ① 카카오 키워드 장소 검색 API (출발지)
  ② 카카오 키워드 장소 검색 API (도착지)
  ③ 카카오 자전거 경로 API (BIKE_ONLY / SHORTEST)
  ↓
route.legs[].steps[].path.points[] 순서대로 처리
  ↓ 메인 스레드
MainActivity.java: KakaoMap RouteLine 추가 + 카메라 이동
  ↓
BikeUiBridge.kt → MainScreen.kt (거리·예상 시간 표시)
```

## 6. 반드시 구분해야 하는 것

- **경로:** `자전거도로 우선`은 카카오 `BIKE_ONLY` 모드입니다. **사고 다발지역을 피해서 계산하는 안전 알고리즘이 아닙니다.** 실제 안전도를 보장하는 경로로 안내하면 안 됩니다.
- **장소 검색:** 검색 결과 중 **첫 번째 장소만 자동 선택**합니다. 같은 이름의 장소가 여러 곳일 수 있으니 표시된 장소명을 꼭 확인하세요.
- **오르막:** 카카오 경로 응답에 높이 정보가 없어 `— m`로 표시됩니다.
- **사고 데이터:** 2,079건은 과거 공공데이터를 가공한 **오프라인 자료**입니다. 지도 성능을 위해 현재 화면 중심 또는 경로 주변 **최대 80개 마커만** 표시합니다. 팀 서버가 연결된 경우에만 서버 자료로 바뀝니다.
- **경로 위험 표시:** 경로선 주변 **150m 이내** 과거 사고다발지역 개수를 단순히 세어 보여 줄 뿐, 위험도를 추정하거나 경로를 바꾸지 않습니다.
- **GPS·TTS:** 권한을 받은 뒤 **앱을 사용하는 동안에만** 위치를 요청합니다. 위치 오차가 80m 이하일 때만 150m 반경 접근 경고를 적용하며, 접근 경고와 안내 시작 문구는 시연용입니다. 위치 정확도와 TTS 엔진에 따라 결과가 달라지므로 실제 도로 내비게이션으로 사용하면 안 됩니다. 안내는 **"과거 사고 이력"** 기준이며 실시간 사고나 안전을 보장하지 않습니다.
- **디자인 v4:** `2026-11-11_ui_v4.zip`의 원통형 메뉴·경로창·페이지·테마를 기반으로 했습니다. v4의 `평지 위주` 옵션은 대응 API가 확정되지 않아 제외했습니다.
- **계정 기능:** 12/9 작업 대상입니다. 로그인·회원가입 API가 아직 없어 화면에는 **미구현** 안내가 표시됩니다. 카카오 로그인 패치(11/4 기준)는 현재 빌드에 포함하지 않았으며, 검토 내용은 `docs/05_새UI_카카오로그인_통합대기.md`에 있습니다.

## 7. 오류가 날 때

| 증상 | 확인할 것 |
|---|---|
| 빌드 실패 | `Build Output`의 **첫 번째 오류** 전문 확인 |
| Gradle JDK 호환 오류 | Android Studio의 Gradle JDK를 Embedded JDK/JBR(호환 버전)로 설정 |
| 경로 분석 시 "REST API 키가 없습니다" | `local.properties`의 키 이름과 값 확인 → Gradle Sync + Rebuild |
| HTTP 401 / 403 | REST API 키 종류, 재발급 여부, 카카오맵 서비스 사용 설정·권한 확인 |
| 장소 검색 결과 없음 | 주소나 장소명을 더 구체적으로 입력 |
| 200 OK인데 경로 없음 | 두 지점이 너무 멀거나 자전거 길찾기 미지원 지역인지 확인 |
| 지도는 보이는데 경로선 실패 | Logcat에서 `BicycleSafety` 필터로 오류 확인 |
| `System UI isn't responding`, `Component activation exception` | 앱 코드 오류로 단정하기 전에 에뮬레이터 상태(WHPX, 시스템 이미지, 그래픽 렌더링)와 카카오맵 ANR 확인. Logcat에서 `FATAL EXCEPTION`, `ANR`, `BicycleSafety`를 함께 확인 |

오래되고 느린 에뮬레이터는 카카오맵 SDK와 충돌할 수 있습니다. 카카오맵은 공식 ARM ABI 실행 환경이 필요합니다.

## 8. 버전 이력

| 버전 | 주요 내용 |
|---|---|
| 11/11 최종 UI 통합 작업본 | Java 카카오맵 SDK 연결에 Kotlin/Compose 디자인(로딩 애니메이션, 원통 메뉴, 경로 시트, 설정·마이페이지, 밝은/블랙 테마)을 통합. 실제 Kakao `MapView`를 Compose `AndroidView`로 넣고, 안전 확인 게이트를 새 디자인 팝업으로 다시 그림. 로고를 런처 아이콘과 로딩 화면에 반영 |
| 11/11 카카오 자전거 경로 설정 통합본 | 경로 분석 버튼에 카카오 장소 검색 + 자전거 경로 REST API를 연결해 실제 경로선 표시. REST API 키를 `local.properties`로 분리 |
| **12/2 작업 준비본 (현재)** | KOROAD 사고 데이터 2,079건 동봉, 선택적 팀 서버 API, GPS 현재 위치와 150m 접근 경고 TTS, 경로 주변 사고다발지역 개수 표시, UI v4 반영, JDK 25 강제 설정 제거 |

이전 버전의 안내 원문은 `docs/참고_기존안내/`에 있습니다.

## 9. 검증 상태

- XML·JSON·ZIP 정적 점검과 Java 코드 로컬 컴파일 스텁 검사를 했습니다. 실제 Android SDK 빌드와는 다릅니다.
- **Android Studio 실제 Gradle 빌드와 기기 실행은 학교 실습실에서 확인해야 합니다.**
- 협업은 `docs/04_보안_기본수칙.md`대로 Jira 이슈별 브랜치 → 커밋 → PR → 팀원 승인 순서로 진행합니다.

참고: [카카오 로컬·길찾기 REST API](https://developers.kakao.com/docs/ko/kakaomap/rest-api), [카카오맵 Android v2 RouteLine](https://apis.map.kakao.com/android_v2/docs/api-guide/routeline/)
