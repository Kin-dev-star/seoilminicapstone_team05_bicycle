package com.example.bicyclesafety.navigation;

import com.example.bicyclesafety.KakaoRouteRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * 카카오 길찾기를 길안내 모듈에 연결합니다.
 * - {@link #toNavRoute}: 앱의 KakaoRouteRepository 결과 → NavRoute (이 메서드만 앱 코드에 의존)
 * - {@link #rerouter}: 경로 이탈 시 현재 위치에서 다시 찾기 ({@link KakaoBicycleRouteClient} 사용)
 * 다른 경로 API 를 쓰면 이 파일 대신 그 API 결과로 NavRoute 를 만들면 됩니다.
 */
public final class KakaoRouteAdapter {
    private KakaoRouteAdapter() {}

    /** 앱의 카카오 길찾기 결과 → NavRoute */
    public static NavRoute toNavRoute(KakaoRouteRepository.Result result) {
        List<NavRoute.Point> points = new ArrayList<>();
        for (KakaoRouteRepository.Point p : result.points) points.add(new NavRoute.Point(p.latitude, p.longitude));
        List<NavRoute.Step> steps = new ArrayList<>();
        for (KakaoRouteRepository.GuideStep s : result.guideSteps) steps.add(new NavRoute.Step(s.guidance, s.latitude, s.longitude));
        return new NavRoute(points, steps, result.distanceMeters, result.durationSeconds,
                result.destinationName, result.endLatitude, result.endLongitude, result.bicycleFirst);
    }

    /** 현재 위치에서 같은 도착지 · 같은 옵션으로 카카오 자전거 길찾기를 다시 부르는 Rerouter */
    public static NavigationSession.Rerouter rerouter(final String restApiKey) {
        return (latitude, longitude, previous) -> KakaoBicycleRouteClient.route(restApiKey, latitude, longitude,
                previous.endLatitude, previous.endLongitude, previous.destinationName, previous.bicycleFirst);
    }
}
