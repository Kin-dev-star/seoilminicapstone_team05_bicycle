package com.example.bicyclesafety;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.kakao.sdk.auth.AuthApiClient;
import com.kakao.sdk.common.model.ClientError;
import com.kakao.sdk.common.model.ClientErrorCause;
import com.kakao.sdk.common.util.Utility;
import com.kakao.sdk.user.UserApiClient;
import com.kakao.sdk.user.model.Account;
import com.kakao.sdk.user.model.Profile;
import com.kakao.sdk.user.model.User;

import kotlin.Unit;

/**
 * 카카오 로그인 공통 클래스. 어느 화면에서든 아래처럼 호출하면 됩니다.
 *
 * <pre>
 * KakaoLoginManager.login(this, new KakaoLoginManager.LoginCallback() {
 *     public void onSuccess(KakaoLoginManager.KakaoUser user) { ... user.id, user.nickname ... }
 *     public void onFailure(String message) { ... }
 * });
 * </pre>
 *
 * 콜백은 항상 메인(UI) 스레드에서 호출되므로 바로 화면을 바꿔도 됩니다.
 * 사전 조건: MapApplication 에서 KakaoSdk.init(...) 호출, AndroidManifest 에 AuthCodeHandlerActivity 등록.
 */
public final class KakaoLoginManager {

    private static final String TAG = "KakaoLogin";
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private KakaoLoginManager() {
    }

    /** 로그인한 카카오 사용자 정보 (서버 users 테이블에 저장할 값) */
    public static final class KakaoUser {
        public final long id;                // 카카오 회원번호 (users.kakao_id)
        public final String nickname;        // 동의 안 했으면 null
        public final String profileImageUrl; // 동의 안 했으면 null

        KakaoUser(long id, String nickname, String profileImageUrl) {
            this.id = id;
            this.nickname = nickname;
            this.profileImageUrl = profileImageUrl;
        }
    }

    public interface LoginCallback {
        void onSuccess(KakaoUser user);

        void onFailure(String message);

        /** 사용자가 로그인 화면에서 취소함 (기본: 아무것도 안 함) */
        default void onCancel() {
        }
    }

    /** 기기에 카카오 토큰이 저장돼 있는지 (만료 여부는 확인 안 함) */
    public static boolean hasToken() {
        return AuthApiClient.getInstance().hasToken();
    }

    public static boolean isKakaoTalkAvailable(Context context) {
        return UserApiClient.getInstance().isKakaoTalkLoginAvailable(context);
    }

    /** 카카오 디벨로퍼스에 등록해야 하는 이 PC/키스토어의 키 해시 */
    public static String getKeyHash(Context context) {
        return Utility.INSTANCE.getKeyHash(context);
    }

    /**
     * 자동 로그인: 저장된 토큰이 유효하면 사용자 정보까지 조회해 onSuccess.
     * 토큰이 없거나 만료됐으면 onFailure (→ 로그인 버튼을 보여주면 됨).
     */
    public static void checkAutoLogin(LoginCallback callback) {
        if (!hasToken()) {
            post(() -> callback.onFailure("저장된 토큰 없음"));
            return;
        }
        UserApiClient.getInstance().accessTokenInfo((tokenInfo, error) -> {
            if (error != null) {
                post(() -> callback.onFailure("토큰 만료/무효: " + error.getMessage()));
            } else {
                fetchUser(callback);
            }
            return Unit.INSTANCE;
        });
    }

    /** 카카오톡이 있으면 카카오톡으로, 없거나 실패하면 카카오계정(웹)으로 로그인 */
    public static void login(Activity activity, LoginCallback callback) {
        UserApiClient client = UserApiClient.getInstance();

        if (!client.isKakaoTalkLoginAvailable(activity)) {
            loginWithKakaoAccount(activity, callback);
            return;
        }

        client.loginWithKakaoTalk(activity, (token, error) -> {
            if (error != null) {
                Log.w(TAG, "카카오톡 로그인 실패", error);
                if (isCancelled(error)) {
                    post(callback::onCancel);
                } else {
                    // 카카오톡에 연결된 계정이 없는 경우 등 → 카카오계정으로 재시도
                    loginWithKakaoAccount(activity, callback);
                }
            } else if (token != null) {
                fetchUser(callback);
            }
            return Unit.INSTANCE;
        });
    }

    /** 카카오계정(웹 브라우저)으로 로그인 */
    public static void loginWithKakaoAccount(Activity activity, LoginCallback callback) {
        UserApiClient.getInstance().loginWithKakaoAccount(activity, (token, error) -> {
            if (error != null) {
                Log.w(TAG, "카카오계정 로그인 실패", error);
                if (isCancelled(error)) {
                    post(callback::onCancel);
                } else {
                    post(() -> callback.onFailure(String.valueOf(error.getMessage())));
                }
            } else if (token != null) {
                fetchUser(callback);
            }
            return Unit.INSTANCE;
        });
    }

    /** 현재 로그인한 사용자 정보 조회 */
    public static void fetchUser(LoginCallback callback) {
        UserApiClient.getInstance().me((user, error) -> {
            if (error != null) {
                post(() -> callback.onFailure("사용자 정보 조회 실패: " + error.getMessage()));
            } else if (user != null) {
                KakaoUser kakaoUser = toKakaoUser(user);
                Log.d(TAG, "로그인 사용자: id=" + kakaoUser.id + ", nickname=" + kakaoUser.nickname);
                post(() -> callback.onSuccess(kakaoUser));
            }
            return Unit.INSTANCE;
        });
    }

    /** 로그아웃. 오류가 나도 기기에 저장된 토큰은 SDK가 삭제하므로 항상 onComplete 호출 */
    public static void logout(Runnable onComplete) {
        UserApiClient.getInstance().logout(error -> {
            if (error != null) {
                Log.w(TAG, "로그아웃 중 오류 (로컬 토큰은 삭제됨)", error);
            }
            post(onComplete);
            return Unit.INSTANCE;
        });
    }

    private static KakaoUser toKakaoUser(User user) {
        String nickname = null;
        String imageUrl = null;
        Account account = user.getKakaoAccount();
        if (account != null) {
            Profile profile = account.getProfile();
            if (profile != null) {
                nickname = profile.getNickname();
                imageUrl = profile.getProfileImageUrl();
            }
        }
        Long id = user.getId();
        return new KakaoUser(id == null ? 0L : id, nickname, imageUrl);
    }

    private static boolean isCancelled(Throwable error) {
        return error instanceof ClientError
                && ((ClientError) error).getReason() == ClientErrorCause.Cancelled;
    }

    private static void post(Runnable r) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            MAIN.post(r);
        }
    }
}
