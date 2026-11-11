
package com.example.bicyclesafety;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.kakao.vectormap.KakaoMap;
import com.kakao.vectormap.KakaoMapReadyCallback;
import com.kakao.vectormap.LatLng;
import com.kakao.vectormap.MapLifeCycleCallback;
import com.kakao.vectormap.MapView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "KakaoMap";

    // 카카오맵 객체
    private MapView mapView;
    private BottomSheetBehavior<View> routeSheet;

    // 지도 시작 여부
    private boolean isMapStarted = false;

    // 안전 수칙 확인 여부
    private boolean isSafetyConfirmed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // 상태바 및 내비게이션 바 영역 처리
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (v, insets) -> {

                    Insets systemBars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );

                    v.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );

                    return insets;
                }
        );

        View sheet = findViewById(R.id.sheet_route_setting);
        routeSheet = BottomSheetBehavior.from(sheet);
        routeSheet.setState(BottomSheetBehavior.STATE_COLLAPSED);
        findViewById(R.id.btn_route_setting).setOnClickListener(
                v -> routeSheet.setState(BottomSheetBehavior.STATE_EXPANDED));
        findViewById(R.id.btn_sheet_close).setOnClickListener(
                v -> routeSheet.setState(BottomSheetBehavior.STATE_COLLAPSED));
        findViewById(R.id.btn_voice_toggle).setOnClickListener(v ->
                Toast.makeText(this, "음성 안내 기능은 추후 연결됩니다.", Toast.LENGTH_SHORT).show());
        findViewById(R.id.btn_analyze_route).setOnClickListener(v -> {
            EditText origin = findViewById(R.id.et_origin);
            EditText destination = findViewById(R.id.et_destination);
            if (origin.getText().toString().trim().isEmpty()
                    || destination.getText().toString().trim().isEmpty()) {
                Toast.makeText(this, "출발지와 도착지를 입력해 주세요.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "경로 탐색 API 연결 전입니다. 입력값은 유지됩니다.", Toast.LENGTH_LONG).show();
            }
        });
        findViewById(R.id.btn_zoom_in).setOnClickListener(v ->
                Toast.makeText(this, "지도 확대 기능은 추후 연결됩니다.", Toast.LENGTH_SHORT).show());
        findViewById(R.id.btn_zoom_out).setOnClickListener(v ->
                Toast.makeText(this, "지도 축소 기능은 추후 연결됩니다.", Toast.LENGTH_SHORT).show());
        findViewById(R.id.btn_my_location).setOnClickListener(v ->
                Toast.makeText(this, "현재 위치 기능은 추후 연결됩니다.", Toast.LENGTH_SHORT).show());

        // 화면 회전 시 안전 수칙 확인 상태 복원
        if (savedInstanceState != null) {
            isSafetyConfirmed = savedInstanceState.getBoolean(
                    "safety_confirmed",
                    false
            );
        }

        // 앱 실행 시 안전경고 팝업
        if (!isSafetyConfirmed) {
            showSafetyDialog();
        } else {
            startMapAfterSafety();
        }
    }

    // 안전경고 팝업
    private void showSafetyDialog() {

        new AlertDialog.Builder(this)
                .setTitle(R.string.safety_title)
                .setMessage(R.string.safety_message)

                // 확인 버튼
                .setPositiveButton(
                        R.string.safety_confirm,
                        (dialog, which) -> {

                            isSafetyConfirmed = true;

                            dialog.dismiss();

                            Log.d(
                                    TAG,
                                    "안전 수칙 확인 완료"
                            );

                            // 안전 수칙 확인 후 지도 시작
                            startMapAfterSafety();
                        }
                )

                // 종료 버튼
                .setNegativeButton(
                        R.string.safety_exit,
                        (dialog, which) -> {

                            dialog.dismiss();
                            finish();
                        }
                )

                // 팝업 외부 터치로 닫기 방지
                .setCancelable(false)
                .show();
    }

    // 안전 수칙 확인 후 지도 실행
    private void startMapAfterSafety() {

        // 안전 수칙 확인 전에는 지도 시작 금지
        if (!isSafetyConfirmed) {
            return;
        }

        // 중복 실행 방지
        if (isMapStarted) {
            Log.d(TAG, "지도는 이미 시작되었습니다.");
            return;
        }

        // 안전 안내 확인 뒤 실제 지도 객체를 지도 영역에 삽입
        if (mapView == null) {
            FrameLayout container = findViewById(R.id.map_container);
            mapView = new MapView(this);
            container.addView(mapView, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT));
        }

        // 지도 시작
        isMapStarted = true;

        Log.d(TAG, "안전 수칙 확인 완료 - 지도 시작");

        startMap();
    }

    // 카카오맵 시작
    private void startMap() {

        mapView.start(

                new MapLifeCycleCallback() {

                    @Override
                    public void onMapDestroy() {

                        Log.d(TAG, "지도 종료");

                        runOnUiThread(() -> {
                            isMapStarted = false;
                        });
                    }

                    @Override
                    public void onMapError(Exception error) {

                        Log.e(TAG, "지도 실행 오류", error);

                        runOnUiThread(() -> {

                            isMapStarted = false;

                            Toast.makeText(
                                    MainActivity.this,
                                    "카카오맵을 불러오지 못했습니다.",
                                    Toast.LENGTH_LONG
                            ).show();
                        });
                    }
                },

                new KakaoMapReadyCallback() {

                    @Override
                    public void onMapReady(KakaoMap kakaoMap) {

                        Log.d(TAG, "카카오맵 연결 성공");
                    }

                    // 지도 시작 위치: 서울시청
                    @Override
                    public LatLng getPosition() {

                        return LatLng.from(
                                37.5665,
                                126.9780
                        );
                    }

                    // 지도 확대 수준
                    @Override
                    public int getZoomLevel() {
                        return 15;
                    }
                }
        );
    }

    // 화면 회전 시 상태 저장
    @Override
    protected void onSaveInstanceState(Bundle outState) {

        outState.putBoolean(
                "safety_confirmed",
                isSafetyConfirmed
        );

        super.onSaveInstanceState(outState);
    }

    // 앱 활성화
    @Override
    protected void onResume() {
        super.onResume();

        // 지도 시작 전 resume 호출 방지
        if (mapView != null && isMapStarted) {

            Log.d(TAG, "지도 resume");

            mapView.resume();
        }
    }

    // 앱 일시정지
    @Override
    protected void onPause() {

        // 지도 시작 전 pause 호출 방지
        if (mapView != null && isMapStarted) {

            Log.d(TAG, "지도 pause");

            mapView.pause();
        }

        super.onPause();
    }
}
