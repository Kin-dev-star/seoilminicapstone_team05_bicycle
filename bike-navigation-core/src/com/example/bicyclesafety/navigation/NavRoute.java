package com.example.bicyclesafety.navigation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 길안내에 쓰는 경로 하나. 경로 API(카카오 자전거 길찾기 등) 결과를 이 모양으로 바꿔서 넣습니다.
 * 카카오 결과는 {@link KakaoRouteAdapter#toNavRoute}로 바로 바꿀 수 있습니다.
 */
public final class NavRoute {

    /** 위도·경도 한 점 */
    public static final class Point {
        public final double latitude;
        public final double longitude;

        public Point(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    /** 안내 지점: 이 위치에서 guidance 문구로 안내 (예: "좌회전", "2시 방향", "목적지") */
    public static final class Step {
        public final String guidance;
        public final double latitude;
        public final double longitude;

        public Step(String guidance, double latitude, double longitude) {
            this.guidance = guidance;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    /** 경로선 좌표 (출발 → 도착 순서) */
    public final List<Point> points;
    /** 안내 지점 (경로 순서) */
    public final List<Step> steps;
    /** 전체 거리 (m) · 예상 시간 (초) */
    public final int distanceMeters;
    public final int durationSeconds;
    /** 도착지 이름과 좌표 (재탐색할 때 씀) */
    public final String destinationName;
    public final double endLatitude;
    public final double endLongitude;
    /** 자전거도로 우선이면 true, 최단 경로면 false (재탐색할 때 같은 옵션으로) */
    public final boolean bicycleFirst;

    public NavRoute(List<Point> points, List<Step> steps, int distanceMeters, int durationSeconds,
                    String destinationName, double endLatitude, double endLongitude, boolean bicycleFirst) {
        if (points == null || points.size() < 2) {
            throw new IllegalArgumentException("경로 좌표가 2개 이상 있어야 해요");
        }
        this.points = Collections.unmodifiableList(new ArrayList<>(points));
        this.steps = Collections.unmodifiableList(new ArrayList<>(steps == null ? Collections.<Step>emptyList() : steps));
        this.distanceMeters = distanceMeters;
        this.durationSeconds = durationSeconds;
        this.destinationName = destinationName == null ? "" : destinationName;
        this.endLatitude = endLatitude;
        this.endLongitude = endLongitude;
        this.bicycleFirst = bicycleFirst;
    }
}
