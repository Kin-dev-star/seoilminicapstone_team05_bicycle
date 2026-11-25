# 제보게시판 적용 방법

기준 프로젝트: `BicycleSafety_team_share_20261125.zip` 안의 `BicycleSafety_v2`
걸리는 시간: 약 5분 (빌드 시간 제외)

---

## 1단계. 새 파일 두 개 복사

이 폴더의 파일 두 개를 프로젝트의 같은 위치에 복사합니다.

```
제보게시판_김지우/app/src/main/java/com/example/app/ui/main/ReportScreen.kt
제보게시판_김지우/app/src/main/java/com/example/app/ui/main/ReportSupport.kt
        ↓ 복사
BicycleSafety_v2/app/src/main/java/com/example/app/ui/main/
```

- `MainScreen.kt`, `NavigationScreen.kt`가 들어 있는 폴더입니다.
- 이 폴더에 원래 없던 새 파일이라 기존 파일을 덮어쓰지 않습니다.

## 2단계. MainScreen.kt에 한 줄 추가

1. `MainScreen.kt`를 엽니다.
2. `Ctrl + F`로 `PlaceholderBody(page)`를 찾습니다. `PageContent()` 함수 안의 `when (page)` 부분입니다.
3. 가운데 줄을 추가합니다.

```kotlin
        when (page) {
            NavItem.Account -> MyPageBody(account, onKakaoLogin, onKakaoLogout)
            NavItem.Settings -> SettingsBody(settings)
            NavItem.Report -> ReportScreen()   // 제보게시판 (ReportScreen.kt)   ← 이 줄 추가
            else -> PlaceholderBody(page)
        }
```

- import는 추가하지 않아도 됩니다. 두 파일이 `MainScreen.kt`와 같은 패키지(`com.example.app.ui.main`)에 있습니다.
- Git을 쓰면 줄을 직접 넣는 대신 패치를 적용해도 됩니다. 프로젝트 폴더(`BicycleSafety_v2`)에서 실행하세요.
  ```
  git apply MainScreen_연결_한줄.patch
  ```

## 3단계. 키 확인

`BicycleSafety_v2/local.properties`에 카카오 REST 키가 있는지 확인합니다. 지도·경로 검색에 이미 쓰고 있다면 그대로 두면 됩니다.

```
KAKAO_REST_API_KEY=발급받은_REST_키
```

- 키가 없으면 장소 검색이 안 되고, "지금 있는 곳으로 제보"는 주소 대신 좌표만 저장됩니다.
- `local.properties`는 팀원에게 공유하지 마세요.

## 4단계. 빌드하고 실행

1. Android Studio 메뉴에서 **File → Sync Project with Gradle Files**를 누릅니다.
2. **Run ▶**으로 폰에 설치합니다.
3. 오른쪽 아래 메뉴에서 **제보하기**를 누릅니다.

## 5단계. 동작 확인

| 확인할 것 | 정상이면 |
|---|---|
| 제보하기 화면 | "아직 제보가 없어요"와 아래쪽 **제보 작성** 버튼이 보임 |
| 장소 검색 | 두 글자 이상 입력하면 아래에 검색 결과가 뜸 |
| 지금 있는 곳으로 제보 | 위치 권한을 묻고, 허용하면 현재 주소가 들어감 |
| 제보 등록 | 상세 화면으로 이동 |
| 목록 | 방금 쓴 글이 맨 위에 보임 |
| 작성 중 휴대폰 뒤로 가기 | "작성을 그만둘까요?" 창이 뜸 |

---

## 문제가 생기면

| 증상 | 원인과 해결 |
|---|---|
| `Unresolved reference: ReportScreen` | 1단계 파일이 다른 폴더에 들어갔습니다. `ui/main` 폴더에 있는지 확인하세요. |
| `Conflicting overloads` / `Redeclaration` | 같은 파일을 두 번 복사했습니다. 예: `ReportScreen (1).kt`. 하나를 지우세요. |
| 장소 검색 결과가 안 나옴 | 3단계 REST 키를 확인하세요. 와이파이·데이터가 연결돼 있는지도 확인하세요. |
| 현재 위치를 못 가져옴 | 폰의 위치(GPS)를 켜고, 앱 설정에서 위치 권한을 허용하세요. |
| 다른 폰에서 내 제보가 안 보임 | 정상입니다. 서버 연결 전이라 글은 쓴 폰에만 저장됩니다. 아래 '서버 연결 후'를 보세요. |
| 그 밖의 빌드 오류 | 오류 메시지 전체를 캡처해서 김지우에게 보내 주세요. |

## 서버 연결 후 (백엔드 API 완성 시)

백엔드 담당이 `/api/reports`를 만들면 아래 한 곳만 바꿉니다. 화면 코드는 수정하지 않습니다.

```java
// app/src/main/java/com/example/bicyclesafety/data/AppRepositories.java
public static boolean USE_SERVER_FOR_REPORTS = true;   // false → true
```

- 이후 목록·상세는 로그인 없이 보이고, 글쓰기는 카카오 로그인이 필요합니다.
- 서버 주소 `TEAM_API_BASE_URL`도 `local.properties`에 있어야 합니다.

## 팀 규칙 관련

- `MainScreen.kt`는 공통 파일입니다. 팀에 낼 때는 파일 전체 말고 **새 파일 두 개와 한 줄 패치**만 전달하세요.
- data 패키지, `MainActivity.java`, `BikeUiBridge.kt`는 수정하지 않았습니다.
- 기능 설명과 실행 여부는 `README_제보게시판_전달.md`에 있습니다.
