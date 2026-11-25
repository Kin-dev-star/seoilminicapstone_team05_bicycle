# 자전거 길안내 기능 모듈 (화면 없음)

화면(UI) 없이 **길안내 동작만** 담은 모듈입니다. 경로를 넣고 GPS 위치를 계속 넣어 주면 아래를 알려 줍니다.

- 다음 안내 방향과 남은 거리, 그다음 안내
- 도착까지 거리·시간, 진행률, 현재 속도
- 음성으로 읽을 문장 (회전 150m 전 예고, 35m 전 안내, 출발·도착)
- 경로 이탈 감지 → 현재 위치에서 자동 재탐색
- 과거 사고다발지역 150m 접근 알림
- 목적지 25m 안에 들어오면 도착

화면과 음성은 이 모듈을 쓰는 쪽에서 원하는 방식으로 만들면 됩니다. 지금 앱 화면(`BicycleSafety_v2`의 `NavigationScreen.kt`)이나 디자인 팀 화면(`bike-ui-export`의 `GuideState` · `GuideBanner`) 어디에든 붙일 수 있습니다.

> Android·Compose 코드를 쓰지 않아서 PC에서 바로 테스트할 수 있습니다. 실제 앱 빌드·기기 확인은 아직 하지 않았습니다.

---

## 들어 있는 것

| 파일 | 내용 |
|---|---|
| `src/.../navigation/NavigationSession.java` | **길안내 본체.** `start(경로)` → `onLocation(위치)` 반복 → `Listener`로 상태·문장·이벤트 전달 |
| `src/.../navigation/RouteTracker.java` | 위치를 경로선에 투영해 다음 회전 거리·남은 거리·이탈 거리·도착 계산 |
| `src/.../navigation/NavRoute.java` | 경로 데이터 (좌표, 안내 지점, 거리·시간, 도착지) |
| `src/.../navigation/Maneuver.java` | 방향 종류 (좌회전·우회전·○시 방향·유턴·목적지), 카카오 안내 문구로 판별 |
| `src/.../navigation/KakaoBicycleRouteClient.java` | 재탐색용 카카오 자전거 길찾기 (좌표 → 좌표) |
| `src/.../navigation/KakaoRouteAdapter.java` | 앱의 `KakaoRouteRepository` 결과 → `NavRoute`, 카카오 재탐색 연결 |
| `examples/DesignGuideBridge.kt` | 디자인 팀 `GuideState`에 연결하는 예시 |
| `test/` | PC 테스트 (가상 경로 시나리오, 예외 상황) |

패키지는 `com.example.bicyclesafety.navigation`입니다. `src` 아래 폴더를 그대로 `app/src/main/java/` 아래에 복사하면 됩니다.
- `KakaoRouteAdapter.toNavRoute()`만 앱의 `KakaoRouteRepository`(`BicycleSafety_v2` 버전)를 씁니다. 다른 경로 API를 쓰면 이 파일을 빼고 직접 `NavRoute`를 만드세요.
- `KakaoBicycleRouteClient`는 `org.json`(Android 기본 포함)만 씁니다.

---

## 쓰는 방법 (Java, MainActivity 예)

```java
private final ExecutorService routeWorker = Executors.newSingleThreadExecutor();
private NavigationSession nav;

// onCreate
nav = new NavigationSession(new NavigationSession.SimpleListener() {
    @Override public void onUpdate(NavigationSession.State s) {
        // 화면 갱신: s.maneuver, s.distanceToNext, s.nextManeuver, s.remainingMeters,
        //           s.remainingSeconds, s.speedKmh, s.progress, s.offRoute, s.rerouting, s.nearbyHazard
    }
    @Override public void onSpeak(String text) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_ADD, null, "nav");
    }
    @Override public void onRouteChanged(NavRoute r) { /* 지도에 새 경로선 그리기 */ }
    @Override public void onRerouteFailed(String m) { /* "재탐색 실패" 표시 */ }
    @Override public void onEnded() { /* 길안내 화면 닫기, 화면 꺼짐 방지 해제 */ }
},
KakaoRouteAdapter.rerouter(BuildConfig.KAKAO_REST_API_KEY),   // null 이면 재탐색 없이 이탈만 알림
ContextCompat.getMainExecutor(this),                           // 재탐색 결과를 메인 스레드로
routeWorker);                                                  // 재탐색 네트워크는 여기서

// 사고다발지역 (RiskPoint 목록을 Hazard 로)
List<NavigationSession.Hazard> list = new ArrayList<>();
for (RiskPoint p : hazardPoints) list.add(new NavigationSession.Hazard(p.id, p.name, p.latitude, p.longitude));
nav.setHazards(list);

// '안내 시작'
nav.start(KakaoRouteAdapter.toNavRoute(pendingRoute));

// LocationListener: 1초 간격 권장
nav.onLocation(location.getLatitude(), location.getLongitude(),
        location.hasAccuracy() ? location.getAccuracy() : -1f,
        location.hasSpeed() ? location.getSpeed() : -1f,
        location.getTime());

// 재탐색 버튼 / 종료 버튼
nav.reroute();
nav.stop();
```

- `start` · `onLocation` · `reroute` · `stop`은 같은 스레드(메인 스레드)에서 부르세요.
- 진행 막대에 사고다발지역을 표시하려면 `nav.hazardFractions(150)`를 쓰면 됩니다. 경로 주변 150m 안 지점의 위치가 0~1로 나옵니다.

## 디자인 팀 화면에 붙이기

`examples/DesignGuideBridge.kt`의 `GuideStateListener`를 Listener로 넣으면 끝입니다. 디자인 팀 `GuideState`의 `next` · `following` · `remainingMeters`에 모듈이 계산한 값이 들어가서, 위쪽 배너나 작은 칸이 그대로 안내를 보여 줍니다. 방향 변환은 `LEFT`·`SLIGHT_LEFT` → `Turn.Left`, `RIGHT`·`SLIGHT_RIGHT` → `Turn.Right`, `UTURN` → `Turn.UTurn`, 나머지 → `Turn.Straight`입니다.

음성은 둘 중 하나만 켜세요.
- 모듈 문장을 쓰려면 `speak`에 TTS를 넘기고, 디자인 팀 설정의 "음성 안내"는 끕니다.
- 디자인 팀 `VoiceGuide`(300·100·30m)를 쓰려면 `speak = null`로 둡니다.

## 기준 값 바꾸기

`NavigationSession.Options`에서 바꿀 수 있습니다. 기본값은 지금 앱(`MainActivity`)과 같습니다.

| 값 | 기본 | 의미 |
|---|---|---|
| `offRouteMeters` | 40 | 경로선에서 이만큼 멀면 이탈 |
| `offRouteCount` | 3 | 연속 이탈 횟수 → 자동 재탐색 |
| `rerouteIntervalMs` | 15000 | 자동 재탐색 최소 간격 |
| `offRouteMaxAccuracy` | 35 | GPS 오차가 이보다 크면 이탈 판단 안 함 |
| `hazardRadiusMeters` | 150 | 사고다발지역 접근 알림 반경 |
| `hazardMaxAccuracy` | 80 | GPS 오차가 이보다 크면 접근 판단 안 함 |
| `hazardRepeatMs` | 90000 | 같은 지점 재알림 간격 |
| `preAnnounceMin` ~ `preAnnounceMax` | 60 ~ 160 | 회전 예고 범위 ("150미터 앞에서 좌회전") |
| `nearAnnounce` | 35 | 회전 직전 안내 거리 |
| `startText` 등 | | 출발·이탈·도착·사고 접근 문장 |

## PC에서 테스트

`bike-navigation-core` 폴더에서 아래를 실행하면 됩니다. JDK만 있으면 되고 Android는 필요 없습니다.
```
sh test/run_test.sh
```
가상 경로(북쪽 500m → 좌회전 300m → 우회전 300m)를 1초마다 5m/s로 달리면서, 사고지점 접근 → 경로 이탈 → 자동 재탐색 → 도착까지 음성 문장과 이벤트를 출력합니다. GPS 오차가 큰 경우, 재탐색 중 종료, 재탐색 실패, 재탐색 없는 경우도 확인합니다.

## 알려진 제한

- 회전 방향은 카카오 `guidance` 문구로 판별합니다. 실제 문구가 다르면 `Maneuver.fromGuidance()`를 고치세요.
- 남은 시간은 전체 예상 시간 × 남은 거리 비율입니다.
- 앱이 화면에 있을 때 위치를 넣어 주는 방식입니다 (백그라운드 위치 수집은 모듈 밖).
- 시연용 기능이며 실제 도로 내비게이션을 대신하지 않습니다. 사고 정보는 과거 사고 이력입니다.
