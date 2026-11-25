package com.example.bicyclesafety.navigation;

import java.util.List;

/**
 * 현재 위치를 경로선 위에 투영해서 다음 회전까지 거리, 남은 거리, 경로 이탈 정도, 도착을 계산합니다.
 * 계산만 하고 화면·음성·네트워크는 다루지 않습니다 (그건 {@link NavigationSession}이 함).
 */
public final class RouteTracker {

    /** 위치 한 번에 대한 계산 결과 */
    public static final class Snapshot {
        /** 다음 안내 단계 번호 (없으면 -1) */
        public int nextStepIndex = -1;
        /** 다음 안내 지점까지 경로를 따라 남은 거리 (m). 남은 안내가 없으면 도착까지 거리 */
        public double distanceToNext;
        /** 그다음 안내 단계 번호 (없으면 -1) */
        public int followingStepIndex = -1;
        /** 다음 안내 지점에서 그다음 안내 지점까지 거리 (m) */
        public double followingDistance;
        /** 도착까지 남은 거리 (m) */
        public double remainingMeters;
        /** 경로선에서 떨어진 거리 (m) */
        public double offRouteMeters;
        /** 진행률 0~1 */
        public double progress;
        public boolean arrived;
    }

    private static final double M_PER_DEG = 111320.0;
    /** 거리가 비슷한 후보가 여럿이면(되돌아오는 길 등) 이전 위치와 가까운 구간을 고르는 허용 오차 */
    private static final double TIE_METERS = 12.0;
    /** 도착 판정 거리 */
    public static final double ARRIVE_METERS = 25.0;

    private final double[] lats;
    private final double[] lngs;
    private final double[] cumulative;
    private final double totalMeters;
    private final List<NavRoute.Step> steps;
    private final double[] stepAlong;
    private double lastAlong = 0;

    public RouteTracker(NavRoute route) {
        int n = route.points.size();
        lats = new double[n];
        lngs = new double[n];
        cumulative = new double[n];
        for (int i = 0; i < n; i++) {
            lats[i] = route.points.get(i).latitude;
            lngs[i] = route.points.get(i).longitude;
            if (i > 0) cumulative[i] = cumulative[i - 1] + distance(lats[i - 1], lngs[i - 1], lats[i], lngs[i]);
        }
        totalMeters = cumulative[n - 1];
        steps = route.steps;
        stepAlong = new double[steps.size()];
        int fromSegment = 0;   // 안내 지점은 앞 지점 이후 구간에서만 찾음 (경로 순서 유지)
        for (int i = 0; i < steps.size(); i++) {
            double[] p = project(steps.get(i).latitude, steps.get(i).longitude, fromSegment, Double.NaN);
            stepAlong[i] = p[1];
            fromSegment = (int) p[2];
        }
    }

    public double getTotalMeters() { return totalMeters; }

    /** 임의 좌표(예: 사고다발지역)의 경로상 위치 0~1 */
    public double fractionAlong(double latitude, double longitude) {
        if (totalMeters <= 0) return 0;
        return clamp(project(latitude, longitude, 0, Double.NaN)[1] / totalMeters, 0, 1);
    }

    public Snapshot update(double latitude, double longitude) {
        Snapshot s = new Snapshot();
        double[] p = project(latitude, longitude, 0, lastAlong);
        double along = p[1];
        lastAlong = along;
        s.offRouteMeters = p[0];
        s.remainingMeters = Math.max(0, totalMeters - along);
        s.progress = totalMeters <= 0 ? 1 : clamp(along / totalMeters, 0, 1);
        double toEnd = distance(latitude, longitude, lats[lats.length - 1], lngs[lngs.length - 1]);
        s.arrived = s.remainingMeters <= ARRIVE_METERS || toEnd <= ARRIVE_METERS;

        for (int i = 0; i < steps.size(); i++) {
            if (Maneuver.fromGuidance(steps.get(i).guidance) == Maneuver.START) continue;
            if (stepAlong[i] > along + 3) {
                s.nextStepIndex = i;
                s.distanceToNext = stepAlong[i] - along;
                break;
            }
        }
        if (s.nextStepIndex < 0) {
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

    /** @return {경로선까지 거리, 출발점부터 경로상 거리, 구간 번호} */
    private double[] project(double lat, double lng, int fromSegment, double preferAlong) {
        double best = Double.POSITIVE_INFINITY;
        double[] dists = new double[lats.length];
        double[] alongs = new double[lats.length];
        int start = Math.max(1, fromSegment);
        double scaleX = M_PER_DEG * Math.cos(Math.toRadians(lat));
        for (int i = start; i < lats.length; i++) {
            double ax = (lngs[i - 1] - lng) * scaleX;
            double ay = (lats[i - 1] - lat) * M_PER_DEG;
            double bx = (lngs[i] - lng) * scaleX;
            double by = (lats[i] - lat) * M_PER_DEG;
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

    /** 두 좌표 사이 거리 (m, 하버사인) */
    public static double distance(double lat1, double lng1, double lat2, double lng2) {
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
