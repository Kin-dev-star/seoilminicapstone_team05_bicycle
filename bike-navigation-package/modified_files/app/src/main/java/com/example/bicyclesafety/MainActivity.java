package com.example.bicyclesafety;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.os.SystemClock;
import com.kakao.vectormap.label.Label;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import java.util.Comparator;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.compose.ui.platform.ComposeView;

import com.example.app.ui.BikeUiBridge;
import com.kakao.vectormap.KakaoMap;
import com.kakao.vectormap.KakaoMapReadyCallback;
import com.kakao.vectormap.LatLng;
import com.kakao.vectormap.MapLifeCycleCallback;
import com.kakao.vectormap.MapView;
import com.kakao.vectormap.camera.CameraUpdateFactory;
import com.kakao.vectormap.route.RouteLineLayer;
import com.kakao.vectormap.route.RouteLineOptions;
import com.kakao.vectormap.route.RouteLineSegment;
import com.kakao.vectormap.route.RouteLineStyle;
import com.kakao.vectormap.route.RouteLineStyles;
import com.kakao.vectormap.route.RouteLineStylesSet;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.kakao.vectormap.label.LabelOptions;
import com.kakao.vectormap.label.LabelStyle;
import com.kakao.vectormap.label.LabelTextBuilder;

/**
 * Java feature layer retained from the tested 11/11 project.
 * Compose owns the screen; Kakao MapView and hazard callbacks remain here.
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "BicycleSafety";
    private MapView mapView;
    private boolean isMapStarted = false;
    private boolean isSafetyConfirmed = false;
    private KakaoMap activeMap;
    private RouteLineLayer routeLayer;
    private KakaoRouteRepository.Result pendingRoute;
    private final ExecutorService routeWorker = Executors.newSingleThreadExecutor();
    private final ExecutorService hazardWorker = Executors.newSingleThreadExecutor();
    private final List<RiskPoint> hazardPoints = new ArrayList<>();
    // 경로 검색 후에는 GPS 갱신이 있어도 경로 주변 사고 마커를 유지합니다.
    private final List<RiskPoint> routeHazardsVisible = new ArrayList<>();
    private boolean lastRouteBicycleFirst = true;
    private String hazardSource = "미수신";
    private Label currentLocationLabel;
    private Location lastLocation;
    private Location markerCenterLocation;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private boolean trackingLocation = false;
    private boolean centerOnNextLocation = false;
    private boolean pendingGuidancePermission = false;
    private TextToSpeech textToSpeech;
    private boolean ttsReady = false;
    private boolean guidanceActive = false;
    // ── 내비게이션 화면 상태 ──
    private NavigationTracker navTracker;
    private int lastPreAnnouncedStep = -1;   // "150미터 앞에서 ~" 안내를 한 단계
    private int lastSpokenStep = -1;         // 회전 직전 안내를 한 단계
    private int offRouteCount = 0;           // 연속으로 경로를 벗어난 위치 수
    private long lastRerouteAt = 0;
    private boolean rerouting = false;
    private Location previousNavLocation;
    private static final double OFF_ROUTE_METERS = 40.0;
    private static final int NAV_ZOOM = 17;
    private final Map<String, Long> warningHistory = new HashMap<>();
    private static final int LOCATION_REQUEST = 501;
    private static final double WARNING_DISTANCE_METERS = 150.0;
    private static final double ROUTE_HAZARD_METERS = 150.0;
    private int routeRequestRevision = 0;
    private boolean routeRequestInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState != null) {
            isSafetyConfirmed = savedInstanceState.getBoolean("safety_confirmed", false);
        }
        BikeUiBridge.attach(this, (ComposeView) findViewById(R.id.compose_root),
                !isSafetyConfirmed);
        // 지도/팝업/안내 UI가 만든 직후 실제 사고 기록을 백그라운드에서 읽습니다.
        loadRiskData();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = location -> runOnUiThread(() -> onLocationChanged(location));
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS && textToSpeech != null) {
                int languageResult = textToSpeech.setLanguage(Locale.KOREAN);
                ttsReady = languageResult != TextToSpeech.LANG_MISSING_DATA
                        && languageResult != TextToSpeech.LANG_NOT_SUPPORTED;
            }
        });
        // MapView는 안전 팝업을 확인하고 화면에 붙은 후에만 시작합니다.
    }

    /** Called by AndroidView once the new screen is ready to host the real map. */
    public View acquireMapView() {
        if (mapView == null) {
            mapView = new MapView(this);
            // start() after AndroidView attaches the MapView; avoids early-start races.
            mapView.post(this::startMapAfterSafety);
        }
        return mapView;
    }

    /** The Compose-designed safety dialog calls these; map gate is unchanged. */
    public void onSafetyConfirmed() {
        isSafetyConfirmed = true;
        startMapAfterSafety();
    }

    public void onSafetyExit() {
        finish();
    }

    private void startMapAfterSafety() {
        if (!isSafetyConfirmed || mapView == null || isMapStarted) return;
        if (BuildConfig.KAKAO_NATIVE_APP_KEY.isEmpty()) {
            Toast.makeText(this, "카카오 네이티브 앱 키가 없습니다. local.properties를 확인해 주세요.", Toast.LENGTH_LONG).show();
            return;
        }
        isMapStarted = true;
        mapView.start(new MapLifeCycleCallback() {
            @Override
            public void onMapDestroy() {
                Log.d(TAG, "지도 종료");
                runOnUiThread(() -> {
                    isMapStarted = false;
                    activeMap = null;
                    routeLayer = null;
                });
            }

            @Override
            public void onMapError(Exception error) {
                Log.e(TAG, "카카오맵 시작 오류", error);
                runOnUiThread(() -> {
                    isMapStarted = false;
                    Toast.makeText(MainActivity.this,
                            "카카오맵을 불러오지 못했습니다. 인증과 네트워크를 확인하세요.",
                            Toast.LENGTH_LONG).show();
                });
            }
        }, new KakaoMapReadyCallback() {
            @Override
            public void onMapReady(KakaoMap kakaoMap) {
                Log.d(TAG, "카카오맵 연결 성공");
                activeMap = kakaoMap;
                routeLayer = kakaoMap.getRouteLineManager().getLayer();
                // Render a route retrieved before map initialization, if any.
                if (pendingRoute != null && !showRouteOnMap(pendingRoute)) {
                    pendingRoute = null;
                    routeHazardsVisible.clear();
                    BikeUiBridge.routeFailed("경로선 표시 실패: 지도 로그를 확인해 주세요.");
                }
                refreshHazardMarkers(pendingRoute == null ? null : routeHazardsVisible);

                kakaoMap.setOnLabelClickListener((map, layer, label) -> {
                    Object tag = label.getTag();
                    if (tag instanceof RiskPoint) {
                        RiskPoint clickedPoint = (RiskPoint) tag;
                        runOnUiThread(() -> BikeUiBridge.displayRiskPoint(clickedPoint));
                        return true;
                    }
                    return false;
                });
            }

            @Override
            public LatLng getPosition() {
                return LatLng.from(37.5665, 126.9780);
            }

            @Override
            public int getZoomLevel() { return 15; }
        });
    }

    /** 서울 자전거 사고다발지역 공공데이터. 팀 서버 준비 전에는 동봉된 CSV 가공본 사용. */
    private void loadRiskData() {
        hazardWorker.execute(() -> {
            List<RiskPoint> loaded = new ArrayList<>();
            String source = "오프라인 공공데이터";
            try {
                loaded = RiskDataRepository.loadBundled(getApplicationContext());
                String api = BuildConfig.TEAM_HAZARD_API_URL.trim();
                if (!api.isEmpty()) {
                    try {
                        List<RiskPoint> response = RiskDataRepository.loadFromTeamServer(api);
                        if (response.isEmpty()) throw new IllegalStateException("사고 API 결과가 비어 있습니다.");
                        loaded = response;
                        source = "팀 서버 API";
                    } catch (Exception error) {
                        Log.w(TAG, "팀 사고 API 조회 실패, 오프라인 자료 사용", error);
                        source = "오프라인 공공데이터(서버 연결 실패)";
                    }
                }
            } catch (Exception error) {
                Log.e(TAG, "사고 자료 로딩 실패", error);
                source = "데이터 로딩 실패";
            }
            final List<RiskPoint> result = loaded;
            final String dataSource = source;
            runOnUiThread(() -> {
                if (isDestroyed()) return;
                hazardPoints.clear();
                hazardPoints.addAll(result);
                hazardSource = dataSource;
                Log.i(TAG, "사고 자료: " + hazardSource + " / " + hazardPoints.size() + "건");
                // 경로가 먼저 조회되고 사고 자료가 나중에 로딩된 경우에도 결과를 갱신합니다.
                if (pendingRoute != null) {
                    routeHazardsVisible.clear();
                    routeHazardsVisible.addAll(hazardsAlongRoute(pendingRoute.points));
                    BikeUiBridge.routeSucceeded(pendingRoute.distanceMeters, pendingRoute.durationSeconds,
                            pendingRoute.matchedOrigin, pendingRoute.matchedDestination,
                            lastRouteBicycleFirst, hazardPoints.isEmpty() ? -1 : routeHazardsVisible.size(), hazardSource);
                }
                if (activeMap != null) {
                    refreshHazardMarkers(pendingRoute == null ? null : routeHazardsVisible);
                    markerCenterLocation = lastLocation == null ? null : new Location(lastLocation);
                }
            });
        });
    }

    /** 지도 렌더링 부하를 낮추기 위해 현재 지도나 조회 경로 주변 최대 80개만 표시. */
    private void refreshHazardMarkers(List<RiskPoint> nearRoute) {
        if (activeMap == null) return;
        try {
            activeMap.getLabelManager().getLayer().removeAll();
            currentLocationLabel = null;
            List<RiskPoint> targets = new ArrayList<>();
            if (nearRoute != null) {
                targets.addAll(nearRoute);
            } else {
                double centerLat = lastLocation == null ? 37.5665 : lastLocation.getLatitude();
                double centerLng = lastLocation == null ? 126.9780 : lastLocation.getLongitude();
                for (RiskPoint point : hazardPoints) {
                    if (distanceMeters(centerLat, centerLng, point.latitude, point.longitude) < 3500) targets.add(point);
                }
                targets.sort(Comparator.comparingDouble(p ->
                        distanceMeters(centerLat, centerLng, p.latitude, p.longitude)));
            }
            int shown = 0;
            for (RiskPoint point : targets) {
                if (shown++ >= 80) break;
                addRiskMarker(activeMap, point);
            }
            if (lastLocation != null) drawCurrentLocation(lastLocation);
        } catch (Exception error) {
            Log.e(TAG, "사고 마커 표시 오류", error);
        }
    }

    private static double distanceMeters(double lat1, double lng1, double lat2, double lng2) {
        // 근거리 계산용 구면거리(Haversine), 미터 단위
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.pow(Math.sin(dLat/2),2) + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2)) * Math.pow(Math.sin(dLng/2),2);
        return 6371000.0 * 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    }

    /** 지도에서 선분까지의 최소 거리; 단순 위험 정보 표시용이며 회피 경로 계산이 아님. */
    private static double distanceToRoute(RiskPoint p, List<KakaoRouteRepository.Point> path) {
        double best = Double.POSITIVE_INFINITY;
        double scaleX = 111320.0 * Math.cos(Math.toRadians(p.latitude));
        for (int i = 1; i < path.size(); i++) {
            KakaoRouteRepository.Point first = path.get(i-1);
            KakaoRouteRepository.Point second = path.get(i);
            double ax = (first.longitude - p.longitude) * scaleX;
            double ay = (first.latitude - p.latitude) * 111320;
            double bx = (second.longitude - p.longitude) * scaleX;
            double by = (second.latitude - p.latitude) * 111320;
            double vx = bx - ax, vy = by - ay;
            double t = (vx*vx+vy*vy) < 0.001 ? 0 : Math.max(0, Math.min(1, -(ax*vx+ay*vy)/(vx*vx+vy*vy)));
            best = Math.min(best, Math.hypot(ax + t*vx, ay + t*vy));
        }
        return best;
    }

    private List<RiskPoint> hazardsAlongRoute(List<KakaoRouteRepository.Point> path) {
        List<RiskPoint> result = new ArrayList<>();
        if (path.size() < 2) return result;
        for (RiskPoint point : hazardPoints) {
            if (distanceToRoute(point, path) <= ROUTE_HAZARD_METERS) result.add(point);
        }
        return result;
    }

    private void addRiskMarker(KakaoMap kakaoMap, RiskPoint point) {
        LabelOptions marker = LabelOptions.from(
                        point.id, LatLng.from(point.latitude, point.longitude))
                .setStyles(LabelStyle.from().setTextStyles(36, Color.RED))
                .setTexts(new LabelTextBuilder().setTexts("●"))
                .setClickable(true)
                .setTag(point);
        kakaoMap.getLabelManager().getLayer().addLabel(marker);
    }

    /** Called whenever a user edits either address or switches route mode. */
    public void onRouteFormChanged() {
        routeRequestRevision++; // Invalidate callbacks from an outdated query.
        routeRequestInProgress = false;
        pendingRoute = null;
        routeHazardsVisible.clear();
        if (guidanceActive) stopGuidance(null);
        if (routeLayer != null) routeLayer.removeAll();
        refreshHazardMarkers(null);
        BikeUiBridge.routeCleared();
    }

    /** Runs REST requests off the UI thread, then draws the real route on KakaoMap. */
    public void onAnalyzeRoute(String origin, String destination, boolean safeRoute) {
        final String start = origin.trim();
        final String end = destination.trim();
        if (start.isEmpty() || end.isEmpty()) {
            BikeUiBridge.routeFailed("출발지와 도착지를 모두 입력해 주세요.");
            Toast.makeText(this, "출발지와 도착지를 입력해 주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (start.equals(end)) {
            BikeUiBridge.routeFailed("출발지와 도착지가 동일합니다. 서로 다른 장소를 입력해 주세요.");
            return;
        }
        if (routeRequestInProgress) return;
        if (BuildConfig.KAKAO_REST_API_KEY.isEmpty() ||
                BuildConfig.KAKAO_REST_API_KEY.equals("PASTE_NEW_REST_API_KEY_HERE")) {
            BikeUiBridge.routeFailed("REST API 키가 없습니다. 프로젝트 local.properties에 KAKAO_REST_API_KEY를 설정하고 다시 빌드하세요.");
            return;
        }

        routeRequestInProgress = true;
        final int revision = ++routeRequestRevision;
        pendingRoute = null;
        routeHazardsVisible.clear();
        if (routeLayer != null) routeLayer.removeAll();
        refreshHazardMarkers(null);
        BikeUiBridge.routeStarted();

        final List<RiskPoint> currentHazards = new ArrayList<>(hazardPoints);
        final String currentSource = hazardSource;
        routeWorker.execute(() -> {
            try {
                KakaoRouteRepository.Result result = KakaoRouteRepository.findRoute(
                        BuildConfig.KAKAO_REST_API_KEY, start, end, safeRoute);
                // 대용량 사고 지점과 경로 거리 계산은 UI 스레드에서 실행하지 않습니다.
                List<RiskPoint> nearby = new ArrayList<>();
                for (RiskPoint hazard : currentHazards) {
                    if (distanceToRoute(hazard, result.points) <= ROUTE_HAZARD_METERS) nearby.add(hazard);
                }
                runOnUiThread(() -> {
                    if (isDestroyed() || revision != routeRequestRevision) return;
                    routeRequestInProgress = false;
                    pendingRoute = result;
                    lastRouteBicycleFirst = safeRoute;
                    routeHazardsVisible.clear();
                    routeHazardsVisible.addAll(nearby);
                    refreshHazardMarkers(routeHazardsVisible);
                    Log.d(TAG, "수신된 안내 단계: " + result.guideSteps.size());

                    if (!result.guideSteps.isEmpty())
                        Log.d(TAG, "첫 번째 안내: " + result.guideSteps.get(0).guidance);
                    if (showRouteOnMap(result)) {
                        BikeUiBridge.routeSucceeded(result.distanceMeters, result.durationSeconds,
                                result.matchedOrigin, result.matchedDestination, safeRoute,
                                currentHazards.isEmpty() ? -1 : nearby.size(), currentSource);
                    } else {
                        pendingRoute = null;
                        routeHazardsVisible.clear();
                        refreshHazardMarkers(null);
                        BikeUiBridge.routeFailed("경로 데이터를 받았지만 지도에 선을 표시하지 못했습니다.");
                    }
                });
            } catch (Exception error) {
                Log.w(TAG, "경로 조회 실패", error);
                runOnUiThread(() -> {
                    if (isDestroyed() || revision != routeRequestRevision) return;
                    routeRequestInProgress = false;
                    String message = error.getMessage() == null
                            ? "경로 조회에 실패했습니다. 네트워크와 API 설정을 확인하세요."
                            : error.getMessage();
                    BikeUiBridge.routeFailed(message);
                    Toast.makeText(MainActivity.this, "경로 조회 실패", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /** RouteLine 레이어만 갱신: 사고 마커는 Label 레이어에 따로 유지됩니다. */
    private boolean showRouteOnMap(KakaoRouteRepository.Result result) {
        return showRouteOnMap(result, true);
    }

    /** @param fitCamera false면 안내 중 재탐색처럼 카메라를 현재 위치에 둔 채 선만 바꿉니다. */
    private boolean showRouteOnMap(KakaoRouteRepository.Result result, boolean fitCamera) {
        // Map loading is asynchronous: keep the result until onMapReady() calls us.
        if (activeMap == null || routeLayer == null) return true;
        List<LatLng> points = new ArrayList<>();
        double minLat = 90.0;
        double maxLat = -90.0;
        double minLng = 180.0;
        double maxLng = -180.0;
        for (KakaoRouteRepository.Point p : result.points) {
            points.add(LatLng.from(p.latitude, p.longitude));
            minLat = Math.min(minLat, p.latitude);
            maxLat = Math.max(maxLat, p.latitude);
            minLng = Math.min(minLng, p.longitude);
            maxLng = Math.max(maxLng, p.longitude);
        }
        if (points.size() < 2) return false;
        try {
            routeLayer.removeAll();
            RouteLineStylesSet styles = RouteLineStylesSet.from(
                    RouteLineStyles.from(RouteLineStyle.from(
                            11f, Color.rgb(44, 114, 235), 2f, Color.WHITE)));
            RouteLineSegment segment = RouteLineSegment.from(points)
                    .setStyles(styles.getStyles(0));
            routeLayer.addRouteLine(RouteLineOptions.from(segment).setStylesSet(styles));
            // Show the entire result after the sheet is closed. Include map padding.
            if (fitCamera) {
                LatLng southWest = LatLng.from(minLat, minLng);
                LatLng northEast = LatLng.from(maxLat, maxLng);
                activeMap.moveCamera(CameraUpdateFactory.fitMapPoints(
                        new LatLng[]{southWest, northEast}, 110));
            }
            return true;
        } catch (Exception error) {
            Log.e(TAG, "경로선 표시 오류", error);
            return false;
        }
    }

    /** 위치 기능은 화면 사용 중에만 작동합니다. 위치 권한이 없으면 먼저 권한 요청. */
    public void onCurrentLocationRequested() {
        if (!hasLocationPermission()) {
            centerOnNextLocation = true;
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_REQUEST);
            return;
        }
        centerOnNextLocation = true;
        trackingLocation = true;
        startLocationUpdates();
        if (lastLocation != null) {
            // 이미 받은 좌표가 있다면 사용자 요청에 즉시 이동합니다.
            drawCurrentLocation(lastLocation);
            moveCameraToLocation(lastLocation);
            centerOnNextLocation = false;
        }
    }

    private boolean hasLocationPermission() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != LOCATION_REQUEST) return;
        if (hasLocationPermission()) {
            if (pendingGuidancePermission) {
                pendingGuidancePermission = false;
                onStartGuidanceRequested();
            } else {
                onCurrentLocationRequested();
            }
        } else {
            pendingGuidancePermission = false;
            guidanceActive = false;
            centerOnNextLocation = false;
            Toast.makeText(this, "위치 권한이 없어 현재 위치와 접근 알림을 사용할 수 없습니다.", Toast.LENGTH_LONG).show();
        }
    }

    @SuppressWarnings("MissingPermission")
    private void startLocationUpdates() {
        if (!hasLocationPermission() || locationManager == null || locationListener == null) return;
        try {
            locationManager.removeUpdates(locationListener);
            boolean hasFine = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED;
            String provider = hasFine && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    ? LocationManager.GPS_PROVIDER : LocationManager.NETWORK_PROVIDER;
            if (!locationManager.isProviderEnabled(provider)) {
                Toast.makeText(this, "기기의 위치 서비스(GPS)를 켜주세요.", Toast.LENGTH_LONG).show();
                return;
            }
            // 길 안내 중에는 1초마다, 평소에는 2.5초·5m마다 위치를 받습니다.
            locationManager.requestLocationUpdates(provider, guidanceActive ? 1000L : 2500L,
                    guidanceActive ? 0f : 5f, locationListener, Looper.getMainLooper());
            Toast.makeText(this, "현재 위치 확인 중 · 에뮬레이터는 위치 설정으로 테스트", Toast.LENGTH_SHORT).show();
        } catch (SecurityException error) {
            Toast.makeText(this, "위치 권한을 다시 확인해 주세요.", Toast.LENGTH_LONG).show();
        } catch (Exception error) {
            Log.e(TAG, "GPS 시작 오류", error);
            Toast.makeText(this, "현재 위치 기능을 시작할 수 없습니다.", Toast.LENGTH_LONG).show();
        }
    }

    private void onLocationChanged(Location location) {
        if (!trackingLocation || !Double.isFinite(location.getLatitude()) || !Double.isFinite(location.getLongitude())) return;
        lastLocation = location;
        if (activeMap != null) {
            // 현재 위치가 지도 마커 범위에서 멀어지면 그 주변 사고 이력을 다시 표시합니다.
            if (markerCenterLocation == null || markerCenterLocation.distanceTo(location) > 900f) {
                refreshHazardMarkers(pendingRoute == null ? null : routeHazardsVisible);
                markerCenterLocation = new Location(location);
            } else {
                drawCurrentLocation(location);
            }
            if (centerOnNextLocation) {
                moveCameraToLocation(location);
                centerOnNextLocation = false;
            }
        }
        if (guidanceActive) updateNavigation(location);
        // 정확도가 확인되고 오차 80m 이하인 좌표에만 시연용 근접 경고를 적용합니다.
        if (!location.hasAccuracy() || location.getAccuracy() > 80f) {
            BikeUiBridge.showProximityWarning("GPS 정확도를 확인할 수 없거나 오차가 커서 위험 접근 판별을 보류했습니다.");
            return;
        }
        RiskPoint nearest = null;
        double nearestDistance = WARNING_DISTANCE_METERS;
        for (RiskPoint p : hazardPoints) {
            double distance = distanceMeters(location.getLatitude(),location.getLongitude(),p.latitude,p.longitude);
            if (distance < nearestDistance) { nearest = p; nearestDistance = distance; }
        }
        if (nearest != null) {
            BikeUiBridge.showProximityWarning("과거 사고다발지역 접근: " + nearest.name + " (약 "
                    + Math.round(nearestDistance) + "m) · 실시간 위험 판단 아님");
            long now = SystemClock.elapsedRealtime();
            Long previous = warningHistory.get(nearest.id);
            if (previous == null || now - previous > 90000) {
                warningHistory.put(nearest.id, now);
                speak("전방에 과거 자전거 사고 다발지역이 있습니다. 주변을 주의해 주세요.");
            }
        } else {
            BikeUiBridge.showProximityWarning(null);
        }
    }

    private void drawCurrentLocation(Location location) {
        if (activeMap == null) return;
        LatLng coordinate = LatLng.from(location.getLatitude(), location.getLongitude());
        if (currentLocationLabel == null) {
            LabelOptions marker = LabelOptions.from("CURRENT_GPS", coordinate)
                    .setStyles(LabelStyle.from().setTextStyles(42, Color.BLUE))
                    .setTexts(new LabelTextBuilder().setTexts("●"));
            currentLocationLabel = activeMap.getLabelManager().getLayer().addLabel(marker);
        } else {
            currentLocationLabel.moveTo(coordinate);
        }
    }

    /** 사용자가 현재 위치 버튼을 누른 경우에만 지도 중심 이동; 안내 중 지도 자동 추적 아님. */
    private void moveCameraToLocation(Location location) {
        moveCameraToLocation(location, 15);
    }

    private void moveCameraToLocation(Location location, int zoomLevel) {
        if (activeMap == null) return;
        try {
            activeMap.moveCamera(CameraUpdateFactory.newCenterPosition(
                    LatLng.from(location.getLatitude(), location.getLongitude()), zoomLevel));
        } catch (Exception error) {
            Log.w(TAG, "현재 위치로 지도 이동 실패", error);
        }
    }

    /**
     * 안내 시작: 내비게이션 화면으로 전환하고 현재 위치를 1초마다 추적합니다.
     * 회전 안내·남은 거리·도착 판단은 NavigationTracker가 계산합니다. (시연용, 실도로 내비 아님)
     */
    public void onStartGuidanceRequested() {
        if (pendingRoute == null || pendingRoute.points.size() < 2) {
            Toast.makeText(this, "먼저 경로를 분석해 주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!hasLocationPermission()) {
            pendingGuidancePermission = true;
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_REQUEST);
            return;
        }
        guidanceActive = true;
        navTracker = new NavigationTracker(pendingRoute.points, pendingRoute.guideSteps);
        resetNavAnnouncements();
        offRouteCount = 0;
        rerouting = false;
        previousNavLocation = null;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        BikeUiBridge.navStarted(pendingRoute.destinationName, pendingRoute.durationSeconds,
                pendingRoute.distanceMeters, hazardFractions());
        BikeUiBridge.navMessage("경로 안내를 시작합니다.");
        speak("자전거 경로 안내를 시작합니다. "
                + (pendingRoute.guideSteps.isEmpty() ? "" : pendingRoute.guideSteps.get(0).guidance));
        trackingLocation = true;
        centerOnNextLocation = false;
        startLocationUpdates();
        if (lastLocation != null) updateNavigation(lastLocation);
    }

    /** 내비게이션 화면의 종료 버튼 */
    public void onEndGuidanceRequested() {
        if (guidanceActive) stopGuidance("경로 안내를 종료합니다.");
    }

    /** 내비게이션 화면의 재탐색 버튼 */
    public void onRerouteRequested() {
        if (!guidanceActive) return;
        if (lastLocation == null) {
            BikeUiBridge.navMessage("현재 위치를 확인하는 중입니다.");
            return;
        }
        requestReroute(lastLocation, false);
    }

    private void stopGuidance(String spoken) {
        guidanceActive = false;
        navTracker = null;
        rerouting = false;
        offRouteCount = 0;
        previousNavLocation = null;
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        BikeUiBridge.navEnded();
        if (spoken != null) speak(spoken);
        // 안내가 끝나면 전체 경로가 보이게 되돌리고, 위치 갱신 간격도 평소대로
        if (pendingRoute != null) showRouteOnMap(pendingRoute, true);
        if (trackingLocation && hasLocationPermission()) startLocationUpdates();
    }

    private void resetNavAnnouncements() {
        lastPreAnnouncedStep = -1;
        lastSpokenStep = -1;
    }

    /** 경로 주변 사고다발지역의 경로상 위치 (오른쪽 진행 막대 표시용, 0~1) */
    private float[] hazardFractions() {
        if (navTracker == null) return new float[0];
        float[] result = new float[routeHazardsVisible.size()];
        for (int i = 0; i < result.length; i++) {
            RiskPoint p = routeHazardsVisible.get(i);
            result[i] = (float) navTracker.fractionAlong(p.latitude, p.longitude);
        }
        return result;
    }

    private void updateNavigation(Location location) {
        if (navTracker == null || pendingRoute == null) return;
        moveCameraToLocation(location, NAV_ZOOM);
        NavigationTracker.Snapshot snap = navTracker.update(location.getLatitude(), location.getLongitude());
        boolean accurate = location.hasAccuracy() && location.getAccuracy() <= 35f;
        boolean offRoute = accurate && snap.offRouteMeters > OFF_ROUTE_METERS;

        if (snap.arrived && !offRoute) {
            stopGuidance("목적지에 도착했습니다. 경로 안내를 종료합니다.");
            Toast.makeText(this, "목적지에 도착했습니다.", Toast.LENGTH_LONG).show();
            return;
        }
        // GPS 튐을 피하려고 3번 연속 벗어났을 때만 자동 재탐색 (15초 간격)
        offRouteCount = offRoute ? offRouteCount + 1 : 0;
        if (offRouteCount >= 3 && !rerouting
                && SystemClock.elapsedRealtime() - lastRerouteAt > 15000) {
            requestReroute(location, true);
        }

        List<KakaoRouteRepository.GuideStep> steps = navTracker.getSteps();
        NavigationTracker.Maneuver maneuver = NavigationTracker.Maneuver.ARRIVE;
        String instruction = "목적지까지 이동";
        if (snap.nextStepIndex >= 0) {
            instruction = steps.get(snap.nextStepIndex).guidance;
            maneuver = NavigationTracker.maneuverOf(instruction);
        }
        String following = null;
        if (snap.followingStepIndex >= 0) {
            following = NavigationTracker.maneuverOf(steps.get(snap.followingStepIndex).guidance).name();
        }
        double total = Math.max(1.0, navTracker.getTotalMeters());
        int remainingSeconds = (int) Math.round(pendingRoute.durationSeconds * (snap.remainingMeters / total));
        BikeUiBridge.navProgress(maneuver.name(), NavigationTracker.labelOf(maneuver), instruction,
                (int) Math.round(snap.distanceToNext), following, (int) Math.round(snap.followingDistance),
                (int) Math.round(snap.remainingMeters), remainingSeconds, speedKmh(location),
                (float) snap.progress, offRoute);
        announceGuidance(snap, steps);
        previousNavLocation = new Location(location);
    }

    /** 회전 150m 전 예고 1번 + 30m 전 1번. 같은 문구를 반복하지 않습니다. */
    private void announceGuidance(NavigationTracker.Snapshot snap,
                                  List<KakaoRouteRepository.GuideStep> steps) {
        if (snap.nextStepIndex < 0 || rerouting) return;
        int index = snap.nextStepIndex;
        String guidance = steps.get(index).guidance;
        double d = snap.distanceToNext;
        if (d <= 35 && lastSpokenStep != index) {
            lastSpokenStep = index;
            lastPreAnnouncedStep = index;
            speakQueued(guidance);
        } else if (d > 60 && d <= 160 && lastPreAnnouncedStep != index) {
            lastPreAnnouncedStep = index;
            long rounded = Math.round(d / 10.0) * 10;
            speakQueued(rounded + "미터 앞에서 " + guidance);
        }
    }

    private int speedKmh(Location location) {
        if (location.hasSpeed()) return Math.max(0, Math.round(location.getSpeed() * 3.6f));
        if (previousNavLocation == null) return -1;
        long dtMs = location.getTime() - previousNavLocation.getTime();
        if (dtMs <= 0) return -1;
        double kmh = previousNavLocation.distanceTo(location) / (dtMs / 1000.0) * 3.6;
        return kmh > 80 ? -1 : (int) Math.round(kmh);
    }

    /** 경로를 벗어났을 때 현재 위치에서 같은 도착지로 다시 찾습니다. */
    private void requestReroute(Location from, boolean automatic) {
        if (rerouting || pendingRoute == null) return;
        if (BuildConfig.KAKAO_REST_API_KEY.isEmpty()) {
            BikeUiBridge.navMessage("REST API 키가 없어 재탐색할 수 없습니다.");
            return;
        }
        rerouting = true;
        lastRerouteAt = SystemClock.elapsedRealtime();
        BikeUiBridge.navRerouting(true);
        if (automatic) speakQueued("경로를 벗어났습니다. 경로를 다시 탐색합니다.");
        final KakaoRouteRepository.Result previous = pendingRoute;
        final double lat = from.getLatitude();
        final double lng = from.getLongitude();
        final int revision = routeRequestRevision;
        final List<RiskPoint> currentHazards = new ArrayList<>(hazardPoints);
        routeWorker.execute(() -> {
            try {
                KakaoRouteRepository.Result result = KakaoRouteRepository.rerouteFrom(
                        BuildConfig.KAKAO_REST_API_KEY, lat, lng, previous);
                List<RiskPoint> nearby = new ArrayList<>();
                for (RiskPoint hazard : currentHazards) {
                    if (distanceToRoute(hazard, result.points) <= ROUTE_HAZARD_METERS) nearby.add(hazard);
                }
                runOnUiThread(() -> {
                    rerouting = false;
                    BikeUiBridge.navRerouting(false);
                    if (isDestroyed() || revision != routeRequestRevision || !guidanceActive) return;
                    offRouteCount = 0;
                    pendingRoute = result;
                    routeHazardsVisible.clear();
                    routeHazardsVisible.addAll(nearby);
                    refreshHazardMarkers(routeHazardsVisible);
                    showRouteOnMap(result, false);
                    navTracker = new NavigationTracker(result.points, result.guideSteps);
                    resetNavAnnouncements();
                    BikeUiBridge.navRouteReplaced(result.durationSeconds, result.distanceMeters, hazardFractions());
                    BikeUiBridge.navMessage("새 경로로 안내합니다.");
                    if (lastLocation != null) updateNavigation(lastLocation);
                });
            } catch (Exception error) {
                Log.w(TAG, "경로 재탐색 실패", error);
                runOnUiThread(() -> {
                    rerouting = false;
                    BikeUiBridge.navRerouting(false);
                    if (isDestroyed() || !guidanceActive) return;
                    BikeUiBridge.navMessage("재탐색 실패 · 네트워크를 확인해 주세요.");
                });
            }
        });
    }

    /** 길 안내 문구는 앞 안내를 끊지 않고 이어서 읽습니다. */
    private void speakQueued(String message) {
        if (ttsReady && textToSpeech != null) {
            textToSpeech.speak(message, TextToSpeech.QUEUE_ADD, null, "nav_" + SystemClock.uptimeMillis());
        }
    }

    private void speak(String message) {
        if (ttsReady && textToSpeech != null) {
            textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, "bike_" + SystemClock.uptimeMillis());
        } else {
            Toast.makeText(this, "음성 엔진 준비 중: " + message, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("safety_confirmed", isSafetyConfirmed);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        routeRequestRevision++;
        routeWorker.shutdownNow();
        hazardWorker.shutdownNow();
        if (locationManager != null && locationListener != null) locationManager.removeUpdates(locationListener);
        if (textToSpeech != null) { textToSpeech.stop(); textToSpeech.shutdown(); }
        activeMap = null;
        routeLayer = null;
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null && isMapStarted) mapView.resume();
        if (trackingLocation && hasLocationPermission()) startLocationUpdates();
    }

    @Override
    protected void onPause() {
        if (mapView != null && isMapStarted) mapView.pause();
        if (locationManager != null && locationListener != null) locationManager.removeUpdates(locationListener);
        super.onPause();
    }
}
