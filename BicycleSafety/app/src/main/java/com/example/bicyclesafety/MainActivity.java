
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
import android.graphics.Color;
import com.kakao.vectormap.label.LabelOptions;
import com.kakao.vectormap.label.LabelStyle;
import com.kakao.vectormap.label.LabelTextBuilder;


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
        // 설정 버튼 클릭 이벤트
        findViewById(R.id.btn_settings).setOnClickListener(v -> {
            showSettingsDialog();
        });

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
                        // 첫 번째 샘플 위험지점 (실제 사고 데이터 아님)
                        RiskPoint point1 = new RiskPoint();

                        point1.id = "R001";
                        point1.name = "서울시청 주변";
                        point1.riskLevel = "높음";
                        point1.description = "교차로 통행 시 주변 차량에 주의하세요.";
                        point1.latitude = 37.5665;
                        point1.longitude = 126.9780;
                        // 공통 메서드로 첫 번째 위험지점 표시
                        addRiskMarker(kakaoMap, point1);
                        // 두 번째 샘플 위험지점 (실제 사고 데이터 아님)
                        RiskPoint point2 = new RiskPoint();

                        point2.id = "R002";
                        point2.name = "덕수궁 주변";
                        point2.riskLevel = "보통";
                        point2.description = "보행자와 자전거 이동 시 주변을 확인하세요.";
                        point2.latitude = 37.5681;
                        point2.longitude = 126.9759;

                        addRiskMarker(kakaoMap, point2);


                        // 세 번째 샘플 위험지점 (실제 사고 데이터 아님)
                        RiskPoint point3 = new RiskPoint();

                        point3.id = "R003";
                        point3.name = "광화문역 주변";
                        point3.riskLevel = "높음";
                        point3.description = "교차로와 차량 진입 구간에 주의하세요.";
                        point3.latitude = 37.5716;
                        point3.longitude = 126.9769;

                        addRiskMarker(kakaoMap, point3);
                        // 위험지점 마커 클릭 이벤트
                        kakaoMap.setOnLabelClickListener((map, layer, label) -> {

                            // 클릭한 마커에 연결된 데이터 가져오기
                            Object data = label.getTag();

                            if (data instanceof RiskPoint) {

                                RiskPoint clickedPoint = (RiskPoint) data;

                                // 클릭한 위험지점 이름 표시
                                // 클릭한 위험지점의 상세 정보 팝업 표시
                                runOnUiThread(() -> showRiskDetailDialog(clickedPoint));
                            }

                            return true;
                        });
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
    // 위험지점 마커를 지도에 추가하는 공통 메서드
    private void addRiskMarker(KakaoMap kakaoMap, RiskPoint point) {

        LabelOptions marker = LabelOptions.from(
                        point.id,
                        LatLng.from(point.latitude, point.longitude)
                )
                .setStyles(
                        LabelStyle.from().setTextStyles(36, Color.RED)
                )
                .setTexts(new LabelTextBuilder().setTexts("●"))
                .setClickable(true)
                .setTag(point);

        kakaoMap.getLabelManager().getLayer().addLabel(marker);
    }
    // 위험지점 상세 정보 팝업
    private void showRiskDetailDialog(RiskPoint point) {

        new AlertDialog.Builder(MainActivity.this)
                .setTitle("위험지점 안내 - " + point.name)
                .setMessage(
                        "위험도: " + point.riskLevel
                                + "\n\n"
                                + point.description
                )
                .setPositiveButton("확인", (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }
    // 설정 팝업
    private void showSettingsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("설정")
                .setItems(new String[]{"테마 설정"}, (dialog, which) -> {
                    showThemeDialog();
                })
                .setNegativeButton("닫기", null)
                .show();
    }

    // 테마 설정 팝업 (임시 UI)
    private void showThemeDialog() {
        String[] themes = {"기본 모드", "다크 모드", "라이딩 모드"};

        new AlertDialog.Builder(this)
                .setTitle("테마 설정")
                .setSingleChoiceItems(themes, 0, (dialog, which) -> {
                    // 실제 테마 적용 기능은 디자인 확정 후 구현
                })
                .setPositiveButton("확인", (dialog, which) -> {
                    dialog.dismiss();
                })
                .setNegativeButton("취소", null)
                .show();
    }
}
