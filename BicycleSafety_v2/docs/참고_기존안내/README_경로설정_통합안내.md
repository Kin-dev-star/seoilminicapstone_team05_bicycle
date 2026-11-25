# BicycleSafety — 카카오 자전거 경로 설정 통합본 (2026-11-11)

기준: **Pixel 10에서 정상 실행이 확인됐던 Java + Kotlin/Compose 디자인 통합본**.
원본 지도, 주행 전 안전 팝업, 가상 위험지점 마커 3개, 설정·테마, 원통 메뉴는 그대로 유지하고 **경로 분석 버튼**에 실제 카카오 API를 연결했습니다.

## 1. Android Studio에서 열기

1. 압축을 해제해 **`BicycleSafety_UI_Integrated` 폴더**를 만듭니다.
2. `File → Open`에서 해당 폴더를 선택합니다. **상위 경로에 한글이 있으면 Gradle 오류가 발생할 수 있으니** `C:\Users\soft65\AndroidStudioProjects\BicycleSafety_UI_Integrated`처럼 영어 경로를 사용하세요.
3. Gradle Sync를 완료합니다.

## 2. REST API 키 한 줄 설정 (필수)

- Android Studio 왼쪽 프로젝트 보기에서 프로젝트 최상단의 **`local.properties`** 파일을 엽니다. (없으면 프로젝트 루트에 생성합니다. 기존 `sdk.dir=...` 내용은 지우지 마세요.)
- 아래 줄을 추가합니다. **예시 문자열은 실제 카카오디벨로퍼스 REST API 키로 로컬 PC에서 교체합니다.**

```properties
KAKAO_REST_API_KEY=여기에_재발급한_REST_API_키
```

- 저장하고 `File → Sync Project with Gradle Files` (또는 Sync Now)한 뒤 **Build → Rebuild Project**로 빌드해 주세요.
- **키를 채팅/스크린샷/공개 GitHub에 올리지 마세요.** 기존 스크린샷에 보였던 REST API 키는 재발급을 권장합니다.
- 본 ZIP에는 REST API 키를 넣지 않았습니다. `local.properties`는 Git ignore 대상입니다.
- 단, 이 방식은 **수업용 프로토타입**을 위한 것으로, `BuildConfig` 안에 포함된 키가 APK에서 추출될 수 있습니다. 실제 서비스/배포 시에는 팀 백엔드(Node.js)에서 REST API를 대리 호출하여 키를 보호해야 합니다.

## 3. 실제 작동 검증

1. 앱 실행 → 주행 안전 안내 확인 → 카카오맵 표시.
2. 경로 설정 시트를 열어 **출발지: `서울시청` / 도착지: `광화문역`** 입력.
3. `안전 우회` 선택 → **경로 분석** 클릭.
4. 잠시 `경로 검색 중…` 표시 후 **거리, 시간, 검색된 장소명**이 나타나는지 확인합니다.
5. 시트를 X로 닫고 **지도에 파란색 실제 경로선**이 나타나는지 확인합니다. 빨간색 위험지점 마커 3개는 그대로 남아야 합니다.
6. 시트를 다시 열어 `최단 경로` 선택 후 재분석 → 경로선이 교체되는지 확인합니다.
7. 출발지를 공백으로 지우고 경로 분석 → 빈 입력 안내가 나오는지 확인합니다.

## 4. 사용 기술과 데이터 흐름

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
MainActivity.java: KakaoMap RouteLine 추가 + 카메라 위치 변경
  ↓
BikeUiBridge.kt → MainScreen.kt (거리·분 단위 예상시간 표시)
```

## 5. 현재 제한 사항 (의도적인 미구현)

- 화면에 표시된 **'안전 우회'는 현재 `BIKE_ONLY` = 자전거도로 우선 모드**에 연결되어 있습니다. **사고 다발 지역을 피해 계산하는 기능은 아직 없습니다.** 따라서 실제 안전도를 보장하는 경로로 안내해서는 안 됩니다.
- **장소 검색 결과는 첫 번째 결과만 자동 선택**합니다. 이름이 같은 여러 장소가 있을 수 있으므로 표시된 검색 장소명을 반드시 확인하세요. 정확한 장소 선택 목록은 다음 단계 과제입니다.
- 오르막 높이는 카카오 경로 응답에 제공되지 않아 **`— m`로 유지**합니다.
- GPS, TTS, 사고 공공데이터 실연동, 로그인/DB 및 즐겨찾기 저장은 이번 작업에 포함하지 않았습니다.
- 위험지점 마커 3개는 여전히 테스트용 가상 데이터입니다.
- API 키 사용량/요금 및 권한은 카카오디벨로퍼스 콘솔에서 확인하세요.

## 6. 오류가 있을 때

- **경로 분석에 'REST API 키가 없습니다'** → `local.properties`의 키 이름/값, Gradle Sync + Rebuild 확인.
- **HTTP 401 / 403** → 키 종류(REST API 키), 재발급 키, 카카오맵 서비스 사용 설정/권한 확인.
- **장소 검색 결과 없음** → 주소나 장소명을 더 구체적으로 입력.
- **200 OK지만 경로가 없음** → 두 지점이 너무 멀거나 자전거 길찾기 미지원 지역인지 확인.
- **빌드 실패** → Android Studio의 `Build Output` 첫 번째 오류를 확인.
- **카카오맵은 보이지만 경로선 실패** → `Logcat`에서 `BicycleSafety` 필터로 지도 경로선 오류 확인.

## 실제 검증 여부

- Java 신규/수정 코드: Android 및 Kakao SDK API 모양에 맞춘 **로컬 컴파일 스텁 검사 통과** (실제 SDK 빌드와는 다름).
- Kotlin/Compose 및 전체 Android 프로젝트: **Android SDK가 없는 현재 제작 환경에서 Gradle 빌드/에뮬레이터 테스트 불가**. Pixel 10에서 반드시 직접 확인 필요.

참조: https://developers.kakao.com/docs/ko/kakaomap/rest-api , https://apis.map.kakao.com/android_v2/docs/api-guide/routeline/
