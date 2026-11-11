# 카카오 로그인 연동 가이드 (BicycleSafety)

기준 코드: RGN5-42 첨부 `BicycleSafety_26.11.04.zip`
사용 SDK: Kakao SDK v2-user **2.26.0** (카카오맵과 같은 네이티브 앱 키 사용)

> 아직 실제 기기/에뮬레이터 빌드는 확인하지 못했습니다. 처음 적용하는 사람이 빌드 결과를 이 이슈 댓글로 남겨 주세요.

---

## 패키지 구성
| 파일 | 설명 |
|---|---|
| `kakao_login.patch` | 26.11.04 기준 변경 전체 (git apply 용) |
| `new_files/…` | 새로 추가하는 파일 4개 (그대로 복사) |
| `README.md` | 이 문서 (연동 가이드) |

---

## 방법 A. git 으로 한 번에 적용 (내 코드가 26.11.04에서 크게 안 바뀐 경우)
프로젝트 루트(settings.gradle.kts 있는 폴더)에서:
```
git apply --check kakao_login.patch   # 충돌 확인
git apply kakao_login.patch
```
`--check`에서 오류가 나면 방법 B로 하세요.

## 방법 B. 직접 붙여넣기 (내 코드가 많이 바뀐 경우)

### 1) 새 파일 4개 복사 (`new_files` 폴더 구조 그대로)
- `app/src/main/java/com/example/bicyclesafety/KakaoLoginManager.java` ← **로그인 공통 클래스**
- `app/src/main/java/com/example/bicyclesafety/LoginTestActivity.java` ← 동작 확인 화면
- `app/src/main/res/layout/activity_login_test.xml`
- `app/src/main/res/drawable/bg_button_kakao.xml`

### 2) `settings.gradle.kts` — repositories 안에 추가
```kotlin
// 카카오 로그인 SDK 저장소
maven { url = uri("https://devrepo.kakao.com/nexus/content/groups/public/") }
```

### 3) `app/build.gradle.kts` — dependencies 안에 추가
```kotlin
// 카카오 로그인 (Kakao SDK v2)
implementation("com.kakao.sdk:v2-user:2.26.0")
```

### 4) `MapApplication.java` — SDK 초기화 추가
```java
import com.kakao.sdk.common.KakaoSdk;

public static final String KAKAO_NATIVE_APP_KEY = "YOUR_KAKAO_NATIVE_APP_KEY";

// onCreate() 안, KakaoMapSdk.init 아래에
KakaoSdk.init(this, KAKAO_NATIVE_APP_KEY);
```

### 5) `AndroidManifest.xml` — `<application>` 안에 추가
```xml
<!-- 카카오 로그인 테스트 화면 -->
<activity
    android:name=".LoginTestActivity"
    android:exported="false" />

<!-- 카카오 로그인 Redirect URI: kakao{네이티브 앱 키}://oauth -->
<activity
    android:name="com.kakao.sdk.auth.AuthCodeHandlerActivity"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data
            android:host="oauth"
            android:scheme="kakaoYOUR_KAKAO_NATIVE_APP_KEY" />
    </intent-filter>
</activity>
```
⚠ scheme 은 `kakao` + 네이티브 앱 키를 **직접** 적어야 합니다 (변수/리소스로 넣으면 동작 안 하는 사례가 많음).

### 6) `res/values/colors.xml` — 색상 1개 추가
```xml
<color name="status_ok">#FF1E8E3E</color>
```

### 7) (선택) `MainActivity.java` — 설정 메뉴에 테스트 화면 연결
```java
import android.content.Intent;

// showSettingsDialog() 의 setItems 부분
.setItems(new String[]{"테마 설정", "카카오 로그인 테스트"}, (dialog, which) -> {
    if (which == 0) {
        showThemeDialog();
    } else {
        startActivity(new Intent(this, LoginTestActivity.class));
    }
})
```

---

## 내 화면에서 로그인 쓰기 (계정 화면 등)
```java
// 로그인 버튼
KakaoLoginManager.login(this, new KakaoLoginManager.LoginCallback() {
    @Override public void onSuccess(KakaoLoginManager.KakaoUser user) {
        // user.id (카카오 회원번호 → users.kakao_id), user.nickname, user.profileImageUrl
        // → 여기서 서버 회원 저장 API 호출
    }
    @Override public void onFailure(String message) { /* 오류 안내 */ }
    @Override public void onCancel() { /* 사용자가 취소 */ }
});

// 앱 시작 시 자동 로그인 (토큰 없거나 만료 → onFailure → 로그인 버튼 보여주기)
KakaoLoginManager.checkAutoLogin(callback);

// 로그아웃
KakaoLoginManager.logout(() -> { /* 로그아웃 후 화면 처리 */ });
```
콜백은 항상 UI 스레드에서 호출되므로 바로 화면을 바꿔도 됩니다.

| 메서드 | 설명 |
|---|---|
| `login(activity, cb)` | 카카오톡 있으면 카카오톡, 없거나 실패하면 카카오계정(웹) |
| `loginWithKakaoAccount(activity, cb)` | 카카오계정(웹)으로만 로그인 |
| `checkAutoLogin(cb)` | 저장된 토큰으로 자동 로그인 |
| `fetchUser(cb)` | 현재 사용자 정보 다시 조회 |
| `logout(runnable)` | 로그아웃 |
| `hasToken()` | 토큰 저장 여부 |
| `getKeyHash(context)` | 카카오 디벨로퍼스에 등록할 키 해시 |

---

## 실행 전 카카오 디벨로퍼스 설정 (팀 앱 = 카카오맵 쓰는 앱, 앱 관리자가 1회)
1. **카카오 로그인 활성화** ON
2. **동의항목**: 닉네임(프로필 정보) 설정
3. **플랫폼 > Android**: 패키지명 `com.example.bicyclesafety` 등록
4. **키 해시 등록** — 디버그 키 해시는 **PC마다 다르므로 팀원 각자** 등록 필요
   - 앱 실행 → 설정(톱니바퀴) → 카카오 로그인 테스트 화면에 키 해시가 표시됨 (길게 눌러 복사)
   - 또는 Logcat 에서 태그 `KakaoLogin` 검색

## 동작 확인 순서
1. Gradle Sync → 실행 (카카오맵 때문에 ARM 기기/에뮬레이터 필요)
2. 안전 안내 확인 → 설정(톱니바퀴) → **카카오 로그인 테스트**
3. **카카오 로그인** → "로그인됨" + 회원번호·닉네임 표시되면 성공
4. 뒤로 갔다가 다시 들어오면 자동 로그인되는지 확인
5. **로그아웃** → "로그인 안 됨"으로 바뀌면 성공

## 자주 나는 오류
| 증상 | 확인할 것 |
|---|---|
| 키 해시 관련 오류 | 테스트 화면의 키 해시를 카카오 디벨로퍼스에 등록했는지 |
| 로그인 후 앱으로 안 돌아옴 / 반응 없음 | Manifest `android:scheme="kakao{앱키}"` 오타, 앱 키 일치 여부 |
| KOE 로 시작하는 오류 페이지 | 카카오 로그인 활성화, 패키지명 등록 여부 |
| Gradle Sync 실패 (kakao sdk 못 찾음) | settings.gradle.kts 저장소 추가 여부 |

## 다음 단계
- 서버(MySQL `users` 테이블)에 회원 저장 API가 생기면 `onSuccess` 에서 `user.id`, `user.nickname` 을 보내면 됩니다.
- 즐겨찾기·커뮤니티는 이 회원 정보를 기준으로 연결합니다.
