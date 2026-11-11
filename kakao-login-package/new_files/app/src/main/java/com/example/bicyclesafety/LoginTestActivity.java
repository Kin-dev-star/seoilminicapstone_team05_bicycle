package com.example.bicyclesafety;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 카카오 로그인이 제대로 동작하는지 확인하는 테스트 화면.
 * 지도 화면 > 설정(톱니바퀴) > "카카오 로그인 테스트" 로 들어옵니다.
 * 실제 로그인 처리는 KakaoLoginManager 가 하고, 이 화면은 결과만 보여줍니다.
 */
public class LoginTestActivity extends AppCompatActivity {

    private static final String TAG = "KakaoLogin";

    private TextView tvStatus;
    private TextView tvUserId;
    private TextView tvNickname;
    private TextView tvLog;
    private Button btnLogout;
    private Button btnRefresh;

    /** 로그인/자동 로그인/정보 조회 결과 공통 처리 */
    private final KakaoLoginManager.LoginCallback resultCallback = new KakaoLoginManager.LoginCallback() {
        @Override
        public void onSuccess(KakaoLoginManager.KakaoUser user) {
            appendLog("로그인 성공: 회원번호=" + user.id + ", 닉네임=" + user.nickname);
            showLoggedIn(user);
            // TODO: 서버(MySQL users 테이블)에 회원 저장 API가 생기면 여기서 호출
        }

        @Override
        public void onFailure(String message) {
            appendLog("로그인 안 됨: " + message);
            showLoggedOut();
        }

        @Override
        public void onCancel() {
            appendLog("사용자가 로그인을 취소했습니다");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login_test);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login_root), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        tvStatus = findViewById(R.id.tv_login_status);
        tvUserId = findViewById(R.id.tv_user_id);
        tvNickname = findViewById(R.id.tv_nickname);
        tvLog = findViewById(R.id.tv_log);
        btnLogout = findViewById(R.id.btn_logout);
        btnRefresh = findViewById(R.id.btn_refresh_user);
        TextView tvKeyHash = findViewById(R.id.tv_key_hash);
        TextView tvTalkAvailable = findViewById(R.id.tv_talk_available);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_login_talk).setOnClickListener(v -> {
            appendLog("카카오 로그인 시도 (카카오톡 우선)");
            KakaoLoginManager.login(this, resultCallback);
        });
        findViewById(R.id.btn_login_account).setOnClickListener(v -> {
            appendLog("카카오계정(웹)으로 로그인 시도");
            KakaoLoginManager.loginWithKakaoAccount(this, resultCallback);
        });
        btnRefresh.setOnClickListener(v -> KakaoLoginManager.fetchUser(resultCallback));
        btnLogout.setOnClickListener(v -> KakaoLoginManager.logout(() -> {
            appendLog("로그아웃 완료");
            showLoggedOut();
        }));

        // 키 해시: 카카오 디벨로퍼스 > 앱 > 플랫폼 키 > 네이티브 앱 키 에 등록해야 로그인 가능
        String keyHash = KakaoLoginManager.getKeyHash(this);
        tvKeyHash.setText(keyHash);
        Log.d(TAG, "keyHash=" + keyHash);

        tvTalkAvailable.setText(KakaoLoginManager.isKakaoTalkAvailable(this)
                ? "설치됨 (카카오톡으로 로그인)"
                : "없음 (카카오계정 웹 로그인)");

        appendLog("카카오 SDK 초기화 완료");
        showLoggedOut();

        // 저장된 토큰이 있으면 자동 로그인
        KakaoLoginManager.checkAutoLogin(resultCallback);
    }

    private void showLoggedIn(KakaoLoginManager.KakaoUser user) {
        tvStatus.setText("로그인됨");
        tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_ok));
        tvUserId.setText(String.valueOf(user.id));
        tvNickname.setText(user.nickname != null ? user.nickname : "(닉네임 동의 안 함)");
        setButtonEnabled(btnLogout, true);
        setButtonEnabled(btnRefresh, true);
    }

    private void showLoggedOut() {
        tvStatus.setText("로그인 안 됨");
        tvStatus.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        tvUserId.setText("-");
        tvNickname.setText("-");
        setButtonEnabled(btnLogout, false);
        setButtonEnabled(btnRefresh, false);
    }

    private void setButtonEnabled(Button button, boolean enabled) {
        button.setEnabled(enabled);
        button.setAlpha(enabled ? 1f : 0.4f);
    }

    /** 화면 아래 로그 영역 + Logcat(태그: KakaoLogin)에 기록 */
    private void appendLog(String message) {
        String time = new SimpleDateFormat("HH:mm:ss", Locale.KOREA).format(new Date());
        String line = "[" + time + "] " + message;
        Log.d(TAG, line);

        CharSequence old = tvLog.getText();
        tvLog.setText(old.length() == 0 ? line : line + "\n" + old);
    }
}
