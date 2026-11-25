package com.example.bicyclesafety.navigation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * 자전거 길안내 기능 (화면 없음).
 *
 * <p>쓰는 방법: 경로를 {@link #start(NavRoute)}로 넣고, GPS 위치가 올 때마다 {@link #onLocation}을 부르면
 * {@link Listener}로 안내 상태 · 읽어 줄 문장 · 경로 이탈 재탐색 · 사고다발지역 접근 · 도착을 알려 줍니다.
 * 화면(배너, 화살표)과 음성(TTS)은 Listener 를 받는 쪽에서 원하는 방식으로 만들면 됩니다.
 *
 * <p>Android·Compose 코드를 쓰지 않아서 어떤 화면에도 붙일 수 있고, PC에서 바로 테스트할 수 있습니다.
 * {@link #start}, {@link #onLocation}, {@link #reroute}, {@link #stop}은 같은 스레드(보통 메인 스레드)에서 부르세요.
 * 재탐색 네트워크 요청은 worker 에서 하고, 결과는 callback(메인 스레드 Executor)으로 돌아옵니다.
 */
public final class NavigationSession {

    /** 안내 상태 · 이벤트를 받는 쪽. 필요한 것만 고쳐 쓰려면 {@link SimpleListener}를 상속하세요. */
    public interface Listener {
        /** 위치가 바뀔 때마다 새 안내 상태 */
        void onUpdate(State state);
        /** 음성으로 읽어 줄 문장 (TTS 에 그대로 넣기) */
        void onSpeak(String text);
        /** 과거 사고다발지역 반경 안에 들어옴 (같은 지점은 일정 시간 동안 다시 알리지 않음) */
        void onHazardApproach(Hazard hazard, double distanceMeters);
        /** 재탐색 시작 (automatic = 경로 이탈로 자동 시작) */
        void onRerouteStarted(boolean automatic);
        /** 재탐색 성공: 새 경로로 안내를 이어 감 (지도에 새 경로선을 그리기) */
        void onRouteChanged(NavRoute newRoute);
        /** 재탐색 실패 */
        void onRerouteFailed(String message);
        /** 목적지 도착 (이어서 onEnded 도 불림) */
        void onArrived();
        /** 안내 끝 (도착 또는 stop) */
        void onEnded();
    }

    /** 아무것도 안 하는 Listener. 필요한 메서드만 override 해서 쓰기 */
    public static class SimpleListener implements Listener {
        @Override public void onUpdate(State state) {}
        @Override public void onSpeak(String text) {}
        @Override public void onHazardApproach(Hazard hazard, double distanceMeters) {}
        @Override public void onRerouteStarted(boolean automatic) {}
        @Override public void onRouteChanged(NavRoute newRoute) {}
        @Override public void onRerouteFailed(String message) {}
        @Override public void onArrived() {}
        @Override public void onEnded() {}
    }

    /** 현재 위치에서 같은 도착지로 경로를 다시 찾는 방법 (카카오는 {@link KakaoRouteAdapter#rerouter}) */
    public interface Rerouter {
        NavRoute reroute(double latitude, double longitude, NavRoute previous) throws Exception;
    }

    /** 과거 사고다발지역 (경로 주변 표시 · 접근 알림용) */
    public static final class Hazard {
        public final String id;
        public final String name;
        public final double latitude;
        public final double longitude;

        public Hazard(String id, String name, double latitude, double longitude) {
            this.id = id;
            this.name = name;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    /** 화면에 그릴 안내 상태 */
    public static final class State {
        /** 안내 시작 후 위치를 한 번이라도 받았는지 (false 면 "현재 위치 확인 중") */
        public boolean hasFix;
        public Maneuver maneuver = Maneuver.STRAIGHT;
        /** 짧은 안내 ("좌회전") 와 카카오 원래 문구 */
        public String maneuverLabel = "";
        public String instruction = "";
        /** 다음 안내 지점까지 (m) */
        public int distanceToNext;
        /** 그다음 안내 (없으면 null) 와 거리 (다음 안내 지점부터, m) */
        public Maneuver nextManeuver;
        public int nextDistance;
        /** 도착까지 거리 (m) · 시간 (초, 예상 시간 × 남은 거리 비율) */
        public int remainingMeters;
        public int remainingSeconds;
        /** 현재 속도 km/h (모르면 -1) */
        public int speedKmh = -1;
        /** 진행률 0~1 */
        public float progress;
        /** 경로선에서 떨어진 거리 (m) 와 이탈 여부 */
        public double offRouteMeters;
        public boolean offRoute;
        public boolean rerouting;
        /** 가장 가까운 사고다발지역 (반경 밖이면 null) 과 거리 */
        public Hazard nearbyHazard;
        public double hazardDistance;
    }

    /** 바꿀 수 있는 기준 값 (기본값은 앱 MainActivity 와 같음) */
    public static final class Options {
        /** 이 거리보다 경로선에서 멀면 이탈 */
        public double offRouteMeters = 40;
        /** 연속으로 이만큼 이탈하면 자동 재탐색 */
        public int offRouteCount = 3;
        /** 자동 재탐색 최소 간격 */
        public long rerouteIntervalMs = 15000;
        /** GPS 오차가 이보다 크면 이탈 판단을 하지 않음 */
        public float offRouteMaxAccuracy = 35f;
        /** 사고다발지역 접근 알림 반경 / GPS 오차 한도 / 같은 지점 재알림 간격 */
        public double hazardRadiusMeters = 150;
        public float hazardMaxAccuracy = 80f;
        public long hazardRepeatMs = 90000;
        /** 회전 예고 범위 ("150미터 앞에서 좌회전") 와 회전 직전 안내 거리 */
        public double preAnnounceMin = 60;
        public double preAnnounceMax = 160;
        public double nearAnnounce = 35;
        /** 음성 문장 */
        public String startText = "자전거 경로 안내를 시작합니다.";
        public String offRouteText = "경로를 벗어났습니다. 경로를 다시 탐색합니다.";
        public String arriveText = "목적지에 도착했습니다. 경로 안내를 종료합니다.";
        public String hazardText = "전방에 과거 자전거 사고 다발지역이 있습니다. 주변을 주의해 주세요.";
    }

    private final Listener listener;
    private final Rerouter rerouter;
    private final Executor callback;
    private final Executor worker;
    private final Options options;

    private NavRoute route;
    private RouteTracker tracker;
    private boolean active;
    private int revision;
    private boolean rerouting;
    private int offRouteCount;
    private long lastRerouteAt = Long.MIN_VALUE / 2;
    private int lastPreAnnounced = -1;
    private int lastSpoken = -1;
    private double lastLat = Double.NaN, lastLng = Double.NaN;
    private long lastTime;
    private State lastState = new State();
    private List<Hazard> hazards = Collections.emptyList();
    private final Map<String, Long> hazardHistory = new HashMap<>();

    /**
     * @param rerouter 재탐색 방법 (null 이면 재탐색 없이 이탈만 알림)
     * @param callback 재탐색 결과를 돌려줄 스레드 (Android: {@code ContextCompat.getMainExecutor(context)})
     * @param worker   재탐색 네트워크 요청을 할 스레드 (예: {@code Executors.newSingleThreadExecutor()})
     */
    public NavigationSession(Listener listener, Rerouter rerouter, Executor callback, Executor worker, Options options) {
        this.listener = listener;
        this.rerouter = rerouter;
        this.callback = callback;
        this.worker = worker;
        this.options = options == null ? new Options() : options;
    }

    public NavigationSession(Listener listener, Rerouter rerouter, Executor callback, Executor worker) {
        this(listener, rerouter, callback, worker, null);
    }

    /** 사고다발지역 목록 (안내 중에도 바꿀 수 있음) */
    public void setHazards(List<Hazard> list) {
        hazards = list == null ? Collections.<Hazard>emptyList() : new ArrayList<>(list);
    }

    public boolean isActive() { return active; }

    public NavRoute getRoute() { return route; }

    public State getLastState() { return lastState; }

    /** 경로 주변 사고다발지역의 경로상 위치 0~1 (진행 막대 표시용) */
    public float[] hazardFractions(double withinMeters) {
        if (tracker == null) return new float[0];
        List<Float> out = new ArrayList<>();
        for (Hazard h : hazards) {
            if (distanceToRoute(h.latitude, h.longitude) <= withinMeters) {
                out.add((float) tracker.fractionAlong(h.latitude, h.longitude));
            }
        }
        float[] result = new float[out.size()];
        for (int i = 0; i < result.length; i++) result[i] = out.get(i);
        return result;
    }

    /** 안내 시작 */
    public void start(NavRoute newRoute) {
        revision++;
        route = newRoute;
        tracker = new RouteTracker(newRoute);
        active = true;
        rerouting = false;
        offRouteCount = 0;
        lastPreAnnounced = -1;
        lastSpoken = -1;
        lastLat = Double.NaN;
        hazardHistory.clear();
        State s = new State();
        s.remainingMeters = newRoute.distanceMeters;
        s.remainingSeconds = newRoute.durationSeconds;
        lastState = s;
        listener.onUpdate(s);
        listener.onSpeak(options.startText);
    }

    /**
     * GPS 위치 넣기.
     * @param accuracyMeters 위치 오차 (모르면 음수)
     * @param speedMps       속도 m/s (모르면 음수 → 앞 위치와 거리로 계산)
     * @param timeMillis     위치 시각 (System.currentTimeMillis 와 같은 기준)
     */
    public void onLocation(double latitude, double longitude, float accuracyMeters, float speedMps, long timeMillis) {
        if (!active || tracker == null || Double.isNaN(latitude) || Double.isNaN(longitude)) return;
        RouteTracker.Snapshot snap = tracker.update(latitude, longitude);
        boolean accurate = accuracyMeters >= 0 && accuracyMeters <= options.offRouteMaxAccuracy;
        boolean offRoute = accurate && snap.offRouteMeters > options.offRouteMeters;

        if (snap.arrived && !offRoute) {
            listener.onSpeak(options.arriveText);
            listener.onArrived();
            finish();
            return;
        }

        offRouteCount = offRoute ? offRouteCount + 1 : 0;
        // 재탐색은 이번 위치 처리를 다 끝낸 뒤에 시작 (처리 중에 경로가 바뀌지 않게)
        boolean needReroute = offRouteCount >= options.offRouteCount && !rerouting
                && timeMillis - lastRerouteAt > options.rerouteIntervalMs;

        State s = new State();
        s.hasFix = true;
        List<NavRoute.Step> steps = route.steps;
        if (snap.nextStepIndex >= 0) {
            s.instruction = steps.get(snap.nextStepIndex).guidance;
            s.maneuver = Maneuver.fromGuidance(s.instruction);
        } else {
            s.instruction = "목적지까지 이동";
            s.maneuver = Maneuver.ARRIVE;
        }
        s.maneuverLabel = s.maneuver.label;
        s.distanceToNext = (int) Math.round(snap.distanceToNext);
        if (snap.followingStepIndex >= 0) {
            s.nextManeuver = Maneuver.fromGuidance(steps.get(snap.followingStepIndex).guidance);
            s.nextDistance = (int) Math.round(snap.followingDistance);
        }
        s.remainingMeters = (int) Math.round(snap.remainingMeters);
        double total = Math.max(1.0, tracker.getTotalMeters());
        s.remainingSeconds = (int) Math.round(route.durationSeconds * (snap.remainingMeters / total));
        s.speedKmh = speedKmh(latitude, longitude, speedMps, timeMillis);
        s.progress = (float) snap.progress;
        s.offRouteMeters = snap.offRouteMeters;
        s.offRoute = offRoute;
        s.rerouting = rerouting;
        checkHazards(s, latitude, longitude, accuracyMeters, timeMillis);
        lastState = s;
        listener.onUpdate(s);

        if (!rerouting && !offRoute) announce(snap, steps);
        lastLat = latitude;
        lastLng = longitude;
        lastTime = timeMillis;
        if (needReroute) startReroute(latitude, longitude, timeMillis, true);
    }

    /** 재탐색 버튼 */
    public void reroute() {
        if (!active || Double.isNaN(lastLat)) return;
        startReroute(lastLat, lastLng, lastTime, false);
    }

    /** 안내 종료 (종료 문장은 읽지 않음, 필요하면 onEnded 에서 읽기) */
    public void stop() {
        if (active) finish();
    }

    // ───────────────────────── 내부 ─────────────────────────

    private void finish() {
        active = false;
        revision++;
        rerouting = false;
        tracker = null;
        listener.onEnded();
    }

    /** 회전 예고 1번 + 회전 직전 1번. 같은 안내를 반복하지 않음 */
    private void announce(RouteTracker.Snapshot snap, List<NavRoute.Step> steps) {
        if (snap.nextStepIndex < 0) return;
        int index = snap.nextStepIndex;
        if (Maneuver.fromGuidance(steps.get(index).guidance) == Maneuver.ARRIVE) return;  // 도착은 도착 문장으로
        String guidance = steps.get(index).guidance;
        double d = snap.distanceToNext;
        if (d <= options.nearAnnounce && lastSpoken != index) {
            lastSpoken = index;
            lastPreAnnounced = index;
            listener.onSpeak(guidance);
        } else if (d > options.preAnnounceMin && d <= options.preAnnounceMax && lastPreAnnounced != index) {
            lastPreAnnounced = index;
            listener.onSpeak(Math.round(d / 10.0) * 10 + "미터 앞에서 " + guidance);
        }
    }

    private void checkHazards(State s, double lat, double lng, float accuracy, long now) {
        if (hazards.isEmpty() || accuracy < 0 || accuracy > options.hazardMaxAccuracy) return;
        Hazard nearest = null;
        double best = options.hazardRadiusMeters;
        for (Hazard h : hazards) {
            double d = RouteTracker.distance(lat, lng, h.latitude, h.longitude);
            if (d < best) {
                best = d;
                nearest = h;
            }
        }
        if (nearest == null) return;
        s.nearbyHazard = nearest;
        s.hazardDistance = best;
        Long previous = hazardHistory.get(nearest.id);
        if (previous == null || now - previous > options.hazardRepeatMs) {
            hazardHistory.put(nearest.id, now);
            listener.onHazardApproach(nearest, best);
            listener.onSpeak(options.hazardText);
        }
    }

    private int speedKmh(double lat, double lng, float speedMps, long time) {
        if (speedMps >= 0) return Math.round(speedMps * 3.6f);
        if (Double.isNaN(lastLat) || time <= lastTime) return -1;
        double kmh = RouteTracker.distance(lastLat, lastLng, lat, lng) / ((time - lastTime) / 1000.0) * 3.6;
        return kmh > 80 ? -1 : (int) Math.round(kmh);
    }

    private void startReroute(final double lat, final double lng, long now, boolean automatic) {
        if (rerouter == null || rerouting || route == null) return;
        rerouting = true;
        lastRerouteAt = now;
        lastState.rerouting = true;
        listener.onRerouteStarted(automatic);
        if (automatic) listener.onSpeak(options.offRouteText);
        final NavRoute previous = route;
        final int startedRevision = revision;
        worker.execute(() -> {
            try {
                final NavRoute next = rerouter.reroute(lat, lng, previous);
                callback.execute(() -> {
                    if (startedRevision != revision || !active) return;
                    rerouting = false;
                    offRouteCount = 0;
                    route = next;
                    tracker = new RouteTracker(next);
                    lastPreAnnounced = -1;
                    lastSpoken = -1;
                    listener.onRouteChanged(next);
                    if (!Double.isNaN(lastLat)) onLocation(lastLat, lastLng, -1f, -1f, lastTime);
                });
            } catch (final Exception error) {
                callback.execute(() -> {
                    if (startedRevision != revision || !active) return;
                    rerouting = false;
                    listener.onRerouteFailed(error.getMessage() == null ? "재탐색에 실패했어요" : error.getMessage());
                });
            }
        });
    }

    private double distanceToRoute(double lat, double lng) {
        double best = Double.POSITIVE_INFINITY;
        double scaleX = 111320.0 * Math.cos(Math.toRadians(lat));
        List<NavRoute.Point> pts = route.points;
        for (int i = 1; i < pts.size(); i++) {
            double ax = (pts.get(i - 1).longitude - lng) * scaleX, ay = (pts.get(i - 1).latitude - lat) * 111320.0;
            double bx = (pts.get(i).longitude - lng) * scaleX, by = (pts.get(i).latitude - lat) * 111320.0;
            double vx = bx - ax, vy = by - ay, len2 = vx * vx + vy * vy;
            double t = len2 < 0.0001 ? 0 : Math.max(0, Math.min(1, -(ax * vx + ay * vy) / len2));
            best = Math.min(best, Math.hypot(ax + t * vx, ay + t * vy));
        }
        return best;
    }
}
