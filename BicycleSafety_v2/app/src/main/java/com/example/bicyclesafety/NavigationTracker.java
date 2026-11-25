package com.example.bicyclesafety;

import java.util.ArrayList;
import java.util.List;

/**
 * 자전거 길 안내 계산기 (Android 의존성 없음).
 * 현재 위치를 경로선 위에 투영해서 다음 회전까지 거리, 남은 거리, 경로 이탈 여부를 계산합니다.
 * 내비게이션 화면(BikeUiBridge.navProgress)과 TTS 안내 시점 판단에 사용합니다.
 */
public final class NavigationTracker {

    /** 화면 화살표 종류. Kotlin 화면에서 name()으로 받습니다. */
    public enum Maneuver { START, STRAIGHT, LEFT, RIGHT, SLIGHT_LEFT, SLIGHT_RIGHT, UTURN, ARRIVE }

    /** 한 번의 위치 갱신 결과 */
    public static final class Snapshot {
        /** 다음 안내 단계 번호 (없으면 -1) */
        public int nextStepIndex = -1;
        /** 다음 안내 지점까지 경로를 따라 남은 거리 (m) */
        public double distanceToNext;
        /** 그다음 안내 단계 번호 (없으면 -1) */
        public int followingStepIndex = -1;
        /** 다음 안내 지점에서 그다음 안내 지점까지 거리 (m) */
        public double followingDistance;
        /** 목적지까지 남은 거리 (m) */
        public double remainingMeters;
        /** 경로선에서 떨어진 거리 (m) */
        public double offRouteMeters;
        /** 진행률 0~1 */
        public double progress;
        public boolean arrived;
    }

    private static final double EARTH_M_PER_DEG = 111320.0;
    /** 같은 거리 후보 중 이전 위치와 가까운 구간을 고르는 허용 오차 (되돌아오는 길 대비) */
    private static final double TIE_METERS = 12.0;
    /** 목적지 도착 판정 거리 */
    public static final double ARRIVE_METERS = 25.0;

    private final double[] lats;
    private final double[] lngs;
    /** 각 경로 좌표까지 누적 거리 */
    private final double[] cumulative;
    private final double totalMeters;
    private final List<KakaoRouteRepository.GuideStep> steps;
    /** 각 안내 지점의 경로상 위치 (출발점부터 거리) */
    private final double[] stepAlong;
    private double lastAlong = 0;

    public NavigationTracker(List<KakaoRouteRepository.Point> points,
                             List<KakaoRouteRepository.GuideStep> guideSteps) {
        int n = points.size();
        lats = new double[n];
        lngs = new double[n];
        cumulative = new double[n];
        for (int i = 0; i < n; i++) {
            lats[i] = points.get(i).latitude;
            lngs[i] = points.get(i).longitude;
            if (i > 0) cumulative[i] = cumulative[i - 1] + distance(lats[i - 1], lngs[i - 1], lats[i], lngs[i]);
        }
        totalMeters = n == 0 ? 0 : cumulative[n - 1];
        steps = new ArrayList<>(guideSteps);
        stepAlong = new double[steps.size()];
        // 안내 지점은 경로 순서대로 놓이도록 앞 지점 이후 구간에서만 찾습니다.
        int fromSegment = 0;
        for (int i = 0; i < steps.size(); i++) {
            KakaoRouteRepository.GuideStep step = steps.get(i);
            double[] p = project(step.latitude, step.longitude, fromSegment, Double.NaN);
            stepAlong[i] = p[1];
            fromSegment = (int) p[2];
        }
    }

    public double getTotalMeters() { return totalMeters; }

    public List<KakaoRouteRepository.GuideStep> getSteps() { return steps; }

    /** 임의 좌표(예: 사고다발지역)의 경로상 위치를 0~1 비율로 돌려줍니다. */
    public double fractionAlong(double latitude, double longitude) {
        if (totalMeters <= 0) return 0;
        return clamp(project(latitude, longitude, 0, Double.NaN)[1] / totalMeters, 0, 1);
    }

    public Snapshot update(double latitude, double longitude) {
        Snapshot s = new Snapshot();
        if (lats.length < 2) {
            s.arrived = true;
            return s;
        }
        double[] p = project(latitude, longitude, 0, lastAlong);
        double along = p[1];
        lastAlong = along;
        s.offRouteMeters = p[0];
        s.remainingMeters = Math.max(0, totalMeters - along);
        s.progress = totalMeters <= 0 ? 1 : clamp(along / totalMeters, 0, 1);
        double toEnd = distance(latitude, longitude, lats[lats.length - 1], lngs[lngs.length - 1]);
        s.arrived = s.remainingMeters <= ARRIVE_METERS || toEnd <= ARRIVE_METERS;

        // 현재 위치보다 앞에 있는 첫 안내 지점 (출발 안내는 건너뜀)
        for (int i = 0; i < steps.size(); i++) {
            if (maneuverOf(steps.get(i).guidance) == Maneuver.START) continue;
            if (stepAlong[i] > along + 3) {
                s.nextStepIndex = i;
                s.distanceToNext = stepAlong[i] - along;
                break;
            }
        }
        if (s.nextStepIndex < 0) {
            // 남은 안내가 없으면 목적지까지 직진 안내
            s.distanceToNext = s.remainingMeters;
        } else {
            for (int j = s.nextStepIndex + 1; j < steps.size(); j++) {
                if (stepAlong[j] > stepAlong[s.nextStepIndex] + 3) {
                    s.followingStepIndex = j;
                    s.followingDistance = stepAlong[j] - stepAlong[s.nextStepIndex];
                    break;
                }
            }
        }
        return s;
    }

    /**
     * 안내 문구로 화살표 종류를 고릅니다. 카카오 자전거 경로의 guidance 문구 기준
     * (예: "좌회전", "오른쪽 방향", "2시 방향", "유턴", "목적지").
     */
    public static Maneuver maneuverOf(String guidance) {
        String g = guidance == null ? "" : guidance.replace(" ", "");
        if (g.contains("목적지") || g.contains("도착")) return Maneuver.ARRIVE;
        if (g.contains("출발")) return Maneuver.START;
        if (g.contains("유턴") || g.contains("회전교차로6시")) return Maneuver.UTURN;
        if (g.contains("12시")) return Maneuver.STRAIGHT;
        if (g.contains("10시") || g.contains("11시")) return Maneuver.SLIGHT_LEFT;
        if (g.contains("1시") || g.contains("2시")) return Maneuver.SLIGHT_RIGHT;
        if (g.contains("좌회전") || g.contains("왼쪽") || g.contains("좌측") || g.contains("9시")) return Maneuver.LEFT;
        if (g.contains("우회전") || g.contains("오른쪽") || g.contains("우측") || g.contains("3시")) return Maneuver.RIGHT;
        return Maneuver.STRAIGHT;
    }

    /** 화면 큰 글씨에 쓰는 짧은 안내 */
    public static String labelOf(Maneuver maneuver) {
        switch (maneuver) {
            case LEFT: return "좌회전";
            case RIGHT: return "우회전";
            case SLIGHT_LEFT: return "왼쪽 방향";
            case SLIGHT_RIGHT: return "오른쪽 방향";
            case UTURN: return "유턴";
            case ARRIVE: return "목적지";
            case START: return "출발";
            default: return "직진";
        }
    }

    /**
     * 좌표를 경로선에 투영합니다.
     * @param fromSegment 이 구간부터 찾기
     * @param preferAlong NaN이 아니면 거리가 비슷한 후보 중 이 위치와 가까운 구간을 고름
     * @return {경로선까지 거리, 출발점부터 경로상 거리, 구간 번호}
     */
    private double[] project(double lat, double lng, int fromSegment, double preferAlong) {
        double best = Double.POSITIVE_INFINITY;
        double[] dists = new double[lats.length];
        double[] alongs = new double[lats.length];
        int start = Math.max(1, fromSegment);
        double scaleX = EARTH_M_PER_DEG * Math.cos(Math.toRadians(lat));
        for (int i = start; i < lats.length; i++) {
            double ax = (lngs[i - 1] - lng) * scaleX;
            double ay = (lats[i - 1] - lat) * EARTH_M_PER_DEG;
            double bx = (lngs[i] - lng) * scaleX;
            double by = (lats[i] - lat) * EARTH_M_PER_DEG;
            double vx = bx - ax, vy = by - ay;
            double len2 = vx * vx + vy * vy;
            double t = len2 < 0.0001 ? 0 : clamp(-(ax * vx + ay * vy) / len2, 0, 1);
            dists[i] = Math.hypot(ax + t * vx, ay + t * vy);
            alongs[i] = cumulative[i - 1] + t * (cumulative[i] - cumulative[i - 1]);
            if (dists[i] < best) best = dists[i];
        }
        int chosen = -1;
        double chosenScore = Double.POSITIVE_INFINITY;
        for (int i = start; i < lats.length; i++) {
            if (dists[i] > best + TIE_METERS) continue;
            double score = Double.isNaN(preferAlong) ? dists[i] : Math.abs(alongs[i] - preferAlong);
            if (score < chosenScore) {
                chosenScore = score;
                chosen = i;
            }
        }
        if (chosen < 0) return new double[]{best, lastAlong, start};
        return new double[]{dists[chosen], alongs[chosen], chosen};
    }

    static double distance(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.pow(Math.sin(dLat / 2), 2) + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2)) * Math.pow(Math.sin(dLng / 2), 2);
        return 6371000.0 * 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
