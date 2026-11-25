# 제보게시판 전달 메모 (자료조사·발표 담당 · 김지우, 2026-11-26)

기준 프로젝트: `BicycleSafety_team_share_20261125.zip` (Jira RGN5-43 / GitHub 같은 파일)

## 1. 파일 목록

| 구분 | 파일 | 내용 |
|---|---|---|
| 새 파일 | `app/src/main/java/com/example/app/ui/main/ReportScreen.kt` | 제보게시판 화면 (목록·상세·글쓰기) |
| 새 파일 | `app/src/main/java/com/example/app/ui/main/ReportSupport.kt` | 현재 위치, 좌표→주소, 종류·시간 표시 도우미 |
| 연결 1줄 | `MainScreen.kt` → `MainScreen_연결_한줄.patch` | `PageContent()`의 `when (page)`에 `NavItem.Report -> ReportScreen()` 추가 |

- **data 패키지, MainActivity.java, BikeUiBridge.kt, AndroidManifest.xml, build.gradle은 수정하지 않았습니다.**
- 새 라이브러리 없음. 이미 있는 Compose, activity-compose, play-services-location만 사용합니다.
- 두 파일 모두 `com.example.app.ui.main` 패키지라서 MainScreen.kt에 import를 추가할 필요가 없습니다.

## 2. 기능

**목록**
- `AppRepositories.reports(context).list(page, 20)`로 최신순 20개씩 보여 주고, 더 있으면 "제보 더 보기"를 표시
- 카드마다 종류 아이콘과 태그, 제목, 위치, 작성자, "n분 전"
- 새로고침 버튼, 빈 목록 안내, 오류 시 다시 시도
- 서버 연결 전(`USE_SERVER_FOR_REPORTS = false`)이면 "이 휴대폰에서 쓴 제보만 보여요" 안내

**상세**
- `.get(id)`로 불러옴
- 종류, 제목, 작성자, 작성 시각, 위치 이름과 좌표, "지도에서 보기"(카카오맵 링크), 내용

**글쓰기**
- 종류 선택: 사고 발생 / 도로 파손 / 공사·장애물 / 위험 구간 / 기타
- 위치(필수), 두 가지 방법:
  - 장소 검색: `KakaoRouteRepository.searchPlacesCombined` 사용, 입력이 0.35초 멈추면 백그라운드에서 검색
  - "지금 있는 곳으로 제보": Fused 위치로 좌표를 받고 카카오 coord2address로 주소로 바꿈. 위치 권한이 없으면 요청
- 제목(필수, 50자), 내용(필수, 1000자)
- `.create(HazardReport("", "[종류] 제목", 내용, 장소명, 위도, 경도, "", 0))`로 저장. 저장 후 바로 상세 화면으로 이동
- 작성 중 뒤로 가기를 누르면 "작성을 그만둘까요?" 확인

## 3. 알아 두실 점

1. **제보 종류는 제목 앞에 붙여 저장합니다.** 예: `"[도로 파손] 맨홀 뚜껑 들림"`. 필드를 늘리지 않으려는 선택이고, 서버 쪽은 그냥 제목 문자열로 저장하면 됩니다.
2. **현재 위치 주소 변환**은 기존 `KAKAO_REST_API_KEY`(local.properties)를 그대로 씁니다. 키가 없으면 "현재 위치"와 좌표만 저장됩니다.
3. 화면 맨 위 ← 버튼(공통 상단바)을 누르면 확인 창 없이 제보 화면 전체가 닫힙니다. 휴대폰 뒤로 가기는 상세 → 목록 → 닫기 순서로 동작합니다.
4. `USE_SERVER_FOR_REPORTS`를 `true`로 바꾸면 코드 수정 없이 서버 게시판으로 동작합니다. 이때 글쓰기는 로그인이 필요하고, 저장소 오류 메시지가 화면에 그대로 표시됩니다.

## 4. 실행 여부

- **Android Studio 빌드·실기기 실행은 아직 못 했습니다.** 작업한 환경에 Android SDK가 없었습니다.
- Kotlin 컴파일러로 두 파일의 문법 오류가 없는 것만 확인했습니다. Android 라이브러리 없이 돌려서 나온 오류는 전부 "참조를 찾을 수 없음"이었습니다.
- 실기기에서 확인할 순서:
  1. 패치 한 줄 적용 후 빌드
  2. 메뉴 '제보하기'에서 빈 목록이 보이는지
  3. 글쓰기: 장소 검색으로 위치 선택
  4. "지금 있는 곳으로 제보"를 누르면 권한 요청 후 주소가 표시되는지
  5. 등록하면 상세 화면으로 가는지
  6. 목록으로 돌아가면 새 글이 맨 위에 있는지
  7. 작성 중 휴대폰 뒤로 가기를 누르면 확인 창이 뜨는지
