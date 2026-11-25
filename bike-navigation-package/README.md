# 자전거 길 안내(내비게이션) 연동 가이드 (BicycleSafety)

기준 코드: `BicycleSafety_v2` (커밋 `88b5a4c`, 12/2 작업 준비본 + README 통합)
적용 결과: main에 PR #4로 반영됨 (`BicycleSafety_v2` 폴더는 이미 적용된 상태)
추가 라이브러리: **없음** (카카오맵 SDK, 카카오 자전거 길찾기 REST API, Android 기본 위치·TTS만 사용)

> 작성 환경에 Android SDK가 없어 **실제 Gradle 빌드·기기 실행은 아직 확인하지 못했습니다.**
> 처음 빌드하는 사람이 결과(성공/첫 번째 오류)를 PR #4 댓글이나 Jira에 남겨 주세요.

이 폴더는 **다른 버전의 앱 코드(팀원 개인 작업본 등)에 길 안내 기능만 옮겨 넣을 때** 쓰는 패키지입니다.

---

## 기능 요약

| 화면 | 내용 |
|---|---|
| 경로 미리보기 | 경로 분석이 끝나면 설정 창이 닫히고 지도에 전체 경로 + 아래 요약 카드(예상 시간·거리, 경로 주변 150m 과거 사고다발지역 수, `경로 변경` / `안내 시작`) |
| 길 안내 | 왼쪽 위 회전 카드(화살표·남은 거리·안내 문구·그다음 안내), 현재 속도, 오른쪽 진행 막대(빨간 점 = 사고다발지역), 아래 카드(도착지·도착 예정 시각·남은 거리·재탐색·종료) |

| 동작 | 기준 |
|---|---|
| 위치 갱신 | 안내 중 1초 간격 (평소 2.5초·5m) |
| 음성 안내 | 회전 150m 전 예고("150미터 앞에서 좌회전") 1번 + 35m 전 1번 |
| 경로 이탈 | 경로선에서 40m 넘게 3번 연속 벗어나면 현재 위치에서 자동 재탐색 (15초 간격, GPS 오차 35m 이하일 때만 판단) |
| 도착 | 목적지 25m 이내면 자동 종료 |
| 지도 | 현재 위치를 따라감 (줌 17, 북쪽 위 고정), 안내 중 화면 꺼짐 방지 |

기존 사고다발지역 접근 경고(150m)와 그 TTS 문구는 바꾸지 않았습니다.

---

## 패키지 구성

| 파일 | 설명 |
|---|---|
| `bike_navigation.patch` | 기준 코드 대비 변경 전체 (git apply 용, 7개 파일) |
| `new_files/…` | 새로 추가하는 파일 2개 (그대로 복사) |
| `modified_files/…` | 수정된 파일 5개의 **적용 후 전체본** (직접 비교·붙여넣기용) |
| `README.md` | 이 문서 |

---

## 방법 A. git으로 한 번에 적용 (내 코드가 기준 코드에서 크게 안 바뀐 경우)

프로젝트 루트(`settings.gradle.kts`가 있는 폴더)에서:
```
git apply --check bike_navigation.patch   # 충돌 확인
git apply bike_navigation.patch
```
`--check`에서 오류가 나면 방법 B로 하세요.

## 방법 B. 직접 옮겨 넣기 (내 코드가 많이 바뀐 경우)

### 1) 새 파일 2개 복사 (`new_files` 폴더 구조 그대로)
- `app/src/main/java/com/example/bicyclesafety/NavigationTracker.java` ← **길 안내 계산** (Android 의존성 없음)
- `app/src/main/java/com/example/app/ui/main/NavigationScreen.kt` ← **미리보기 카드 + 길 안내 화면**

### 2) 수정 파일 5개 반영
Android Studio에서 내 파일을 열고 오른쪽 클릭 → `Compare With...`로 `modified_files`의 같은 파일과 비교하면서 아래 부분만 옮기세요.

**`KakaoRouteRepository.java`**
- `GuideStep`에 `type` 필드와 생성자 추가 (기존 3개 인자 생성자는 그대로 동작)
- `Result`에 `destinationName`, `endLatitude`, `endLongitude`, `bicycleFirst` 필드 추가
- `findRoute()` 안의 경로 요청 부분을 `requestRoute()`로 분리, 재탐색용 `rerouteFrom()` 추가

**`MainActivity.java`**
- `import android.view.WindowManager;`
- 필드: `navTracker`, `lastPreAnnouncedStep`, `lastSpokenStep`, `offRouteCount`, `lastRerouteAt`, `rerouting`, `previousNavLocation`, `OFF_ROUTE_METERS`, `NAV_ZOOM` (기존 `nextGuideStep`은 삭제)
- `onStartGuidanceRequested()` 교체, 새 메서드 `onEndGuidanceRequested()`, `onRerouteRequested()`, `stopGuidance()`, `updateNavigation()`, `announceGuidance()`, `requestReroute()`, `speakQueued()` 등 추가
- `onLocationChanged()`: 정확도 검사 **앞에** `if (guidanceActive) updateNavigation(location);` 추가, 맨 아래 기존 55m 안내 블록 삭제
- `startLocationUpdates()`: 안내 중 1초 간격
- `showRouteOnMap(result, fitCamera)`, `moveCameraToLocation(location, zoom)` 오버로드
- `onRouteFormChanged()`: 안내 중이면 `stopGuidance(null)`

**`BikeUiBridge.kt`**
- `RouteUiState`에 `modeLabel`, `startName`, `endName` 추가
- `NavUiState` 데이터 클래스 추가
- `navStarted / navProgress / navRerouting / navRouteReplaced / navMessage / navEnded` 함수 추가
- `BikeApp(...)` 호출에 `navUiState`, `onEndGuidance`, `onReroute`, `onClearRoute` 전달

**`BikeApp.kt`**
- 파라미터 `navUiState`, `onEndGuidance`, `onReroute`, `onClearRoute` 추가 후 `MainScreen`에 그대로 전달

**`MainScreen.kt`**
- `import com.example.app.ui.NavUiState`
- 파라미터 4개 추가 (`BikeApp.kt`와 동일)
- `BackHandler` 바로 위에 미리보기·안내 상태 블록 추가 (`routeReady`, `navActive`, 두 개의 `LaunchedEffect`, `openRouteFromPreview`, `showPreview`)
- 지도 위 화면을 `if (navActive) { NavigationOverlay(...) } else { 기존 화면 }`으로 감싸고, 기존 화면 안에서 미리보기 중에는 원통 메뉴를 숨기고 `RoutePreviewPanel`을 표시

---

## 동작 확인 순서

1. `local.properties`에 `KAKAO_NATIVE_APP_KEY`, `KAKAO_REST_API_KEY` 설정 (키는 팀 채널에서 공유, git에 올리지 않기)
2. Gradle Sync → `Build > Make Project`
3. 경로 설정에서 출발지·도착지 입력 → `경로 분석` → 창이 닫히고 미리보기 카드가 나오는지
4. `안내 시작` → 위치 권한 허용 → 길 안내 화면으로 바뀌고 "경로 안내를 시작합니다." 표시
5. 에뮬레이터 `Extended Controls > Location > Routes`에서 같은 출발지·도착지로 경로를 만들고 재생
   - 회전 카드 거리 감소, 150m·35m 전 음성 안내, 진행 막대 이동
6. 경로에서 먼 곳으로 위치 이동 → "경로를 벗어났어요" → 자동 재탐색
7. 도착지 근처로 이동 → 자동 종료 / ✕ 버튼 → 종료 확인창

## 알려진 제한

- 회전 방향은 카카오 응답의 `guidance` 문구(좌회전·우회전·○시 방향·유턴·목적지 등)로 판별합니다. 실제 문구가 다르면 `NavigationTracker.maneuverOf()`를 고치세요.
- 남은 시간은 전체 예상 시간 × 남은 거리 비율로 계산한 추정값입니다.
- 지도 진행 방향 회전(헤딩 업)은 아직 없습니다.
- 앱이 화면에 있을 때만 동작합니다(백그라운드 안내 없음).
- 시연용 기능이며 실제 도로 내비게이션을 대신하지 않습니다. 안내되는 사고 정보는 과거 사고 이력입니다.
