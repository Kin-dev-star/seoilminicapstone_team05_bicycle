# BicycleSafety - 12월 2일 작업 준비본

> 기준: `BicycleSafety_UI_Integrated_중복파일15개제거.zip`에서 다시 정리했습니다. **학교 Android Studio에서 실제 빌드·실행 전인 검증 준비본**입니다. 12/2 과제가 자동으로 통과했다는 뜻은 아닙니다.

## 0. 프로젝트 열기 (가장 중요)

1. ZIP을 `C:\BS` 등 **경로가 짧고 한글이 없는 곳**에 풀어 주세요. `app/`과 `settings.gradle.kts`가 함께 보이는 폴더를 `File > Open`으로 엽니다.
2. Gradle Sync. SDK 37 설치 및 ARM64 변환 지원 Pixel 에뮬레이터 또는 실제 Android 기기 준비.
   - 재검증 수정본은 특정 PC에서만 쓰던 **JDK 25 강제 설정을 제거**했습니다. Android Studio의 **Gradle JDK(Embedded JDK/JBR, 호환 버전)**를 사용하세요. Gradle JDK 버전이 호환되지 않는다는 오류가 나오면 오류 메시지부터 확인하고 설치된 JDK 설정을 조정합니다.
3. 프로젝트 루트에 `local.properties` 파일을 만들고, **본인 PC에 맞는 SDK 위치와 키**를 입력합니다. 예시:

   ```properties
   sdk.dir=C\:\\Users\\User\\AppData\\Local\\Android\\Sdk
   KAKAO_NATIVE_APP_KEY=카카오디벨로퍼스에서_발급한_네이티브앱키
   KAKAO_REST_API_KEY=카카오디벨로퍼스에서_발급한_REST_API_키
   # TEAM_HAZARD_API_URL=https://팀서버/사고API주소  (팀과 형식 합의 후 사용)
   ```

4. `Build > Make Project` 실행. 첫 오류가 나면 **Build Output의 첫 번째 에러**를 확인합니다.
5. 앱 실행 → 안전 안내 확인 → 실제 카카오맵 표시 → 서울역→상계역 경로 검색.
6. 오래된 느린 에뮬레이터는 지도 SDK와 충돌할 수 있어 **WHPX·시스템 이미지·그래픽 렌더링**도 확인합니다. 앱 코드 오류라고 먼저 단정하지 마세요.

> `local.properties`는 배포 ZIP에 포함하지 않았습니다. 다른 컴퓨터의 SDK 경로와 비공개 키를 그대로 넣으면 안 됩니다.

## 1. 어떤 기능이 어디에 있나?

| 대상 | 파일 | 역할 |
|---|---|---|
| 앱 초기화 | `app/src/main/java/com/example/bicyclesafety/MapApplication.java` | 카카오맵 네이티브 키 초기화 |
| 지도·마커·GPS·TTS·경로 제어 | `.../bicyclesafety/MainActivity.java` | Java 기능 연결의 중심 |
| 카카오 검색·경로 API | `.../bicyclesafety/KakaoRouteRepository.java` | 출발지/도착지 검색, 경로 좌표, 안내 문구 |
| 사고 데이터 입력 | `.../bicyclesafety/RiskDataRepository.java` | 앱 동봉 데이터 / **선택적 HTTPS 팀 서버 API** 변환 |
| 사고 모델 | `.../bicyclesafety/RiskPoint.java` | 사고 건수, 사상자, 좌표, 출처 |
| Java-Kotlin 연결 | `.../app/ui/BikeUiBridge.kt` | 화면 상태와 Java 함수 연결 |
| 메인 화면 | `.../app/ui/main/MainScreen.kt` | 디자인 유지, 경로 UI, GPS·경고 표시 |
| 로딩 및 앱 화면 | `.../app/ui/BikeApp.kt` | 화면 전환, 느린 PC 로딩 탈출 처리 |
| 동봉 사고 기록 | `app/src/main/assets/seoul_bicycle_accidents.json` | KOROAD 팀 가공 CSV 2,079행 |
| 문서 | `docs/` | 일정, 팀 API 협의, 테스트, 보안 |

### 프로젝트 폴더

```text
BicycleSafety/
  README_먼저읽기.md            <- 여기부터
  app/
    src/main/
      java/com/example/bicyclesafety/
        MainActivity.java
        KakaoRouteRepository.java
        RiskDataRepository.java       <- 추가
        RiskPoint.java
        MapApplication.java
      java/com/example/app/ui/
        BikeUiBridge.kt
        BikeApp.kt
        main/MainScreen.kt
      assets/seoul_bicycle_accidents.json  <- 추가
  docs/
    01_일정과_구현현황.md
    02_팀API_연동계약.md
    03_학교실습_체크리스트.md
    04_보안_기본수칙.md
  gradle/
  app/build.gradle.kts
  settings.gradle.kts
```

## 2. 디자인 v4 반영 범위

- `2026-11-11_ui_v4.zip`의 최신 원통형 메뉴/경로창/페이지/테마를 기반으로 구성했으며, `BikeUiBridge.kt`를 통해 Java 실제 기능을 연결했습니다.
- v4의 **평지 위주**는 대응 API 확정 전이므로 빼고 `자전거도로 우선`과 `최단 경로`만 남겼습니다.
- 카카오 로그인 패치는 11/4 기준이며 12/9 일정이므로 현재 빌드에는 포함하지 않았습니다. 검토 내용: `docs/05_새UI_카카오로그인_통합대기.md`.

## 3. 반드시 구분해야 하는 것

- **실제 자전거 경로**: 카카오 장소검색 + 자전거 경로 REST 조회가 성공하면 지도에 표시됩니다. `BIKE_ONLY`는 **자전거도로 우선**, 사고를 피하는 안전 알고리즘이 아닙니다.
- **사고 데이터**: 2,079건은 과거 공공데이터를 팀이 CSV로 가공한 **오프라인 자료**입니다. 앱은 지도 성능을 위해 현재 중심/경로 주변 **최대 80개 마커만 표시**합니다. 서버가 연결된 경우에만 서버 자료로 대체됩니다.
- **경로 위험 표시**: 경로선 주변 **150m 이내** 과거 사고다발지역 개수만 단순 비교해 표시합니다. 위험도를 추정하거나 경로를 변경하지 않습니다.
- **GPS·TTS**: 권한을 받은 후 **앱을 사용하는 동안**만 위치를 요청합니다. 150m 반경 접근 경고·안내 시작 첫 문구 및 GPS 기반 일부 안내는 시연용입니다. 위치 정확도와 TTS 엔진에 따라 달라집니다. 실도로 안전 내비게이션으로 사용하면 안 됩니다.
- **계정 기능**: 12/9 작업 대상. 아직 로그인·회원가입 API가 없으며 화면은 **미구현** 표시로 유지됩니다.

## 4. 오늘 확인 순서

`docs/03_학교실습_체크리스트.md`대로 확인하고, 빌드 오류와 실행 화면을 스크린샷으로 남기세요.

**원본 ZIP 및 원본 GitHub 프로젝트는 수정하지 않았습니다. 이 준비본은 별도 작업 폴더입니다.**

### 검증 상태

- XML/JSON/ZIP 정적 점검을 수행했지만 **Android Studio 실제 Gradle Build와 기기 실행은 내일 학교 실습실에서 확인해야 합니다.**
- GitHub / Jira에 이 작업본을 자동 커밋하거나 상태를 바꾸지 않았습니다.
