package com.example.bicyclesafety.navigation;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 좌표 → 좌표 카카오 자전거 길찾기 (재탐색용). 장소 검색 없이 길찾기 API 한 번만 부릅니다.
 * 앱의 KakaoRouteRepository 와 같은 API · 같은 응답 해석. 메인 스레드에서 부르지 마세요.
 * 키는 local.properties → BuildConfig 로 받아서 넘기세요 (코드에 직접 적지 않기).
 */
public final class KakaoBicycleRouteClient {
    private KakaoBicycleRouteClient() {}

    private static final String BICYCLE_URL = "https://dapi.kakao.com/v2/routing/bicycle";

    public static NavRoute route(String restApiKey, double startLat, double startLng,
                                 double endLat, double endLng, String endName,
                                 boolean bicycleFirst) throws IOException, JSONException {
        if (restApiKey == null || restApiKey.trim().isEmpty()) {
            throw new IOException("REST API 키가 없습니다. local.properties를 확인하세요.");
        }
        String params = "?start_x=" + startLng + "&start_y=" + startLat
                + "&end_x=" + endLng + "&end_y=" + endLat
                + "&input_coord=WGS84&output_coord=WGS84"
                + "&route_mode=" + (bicycleFirst ? "BIKE_ONLY" : "SHORTEST")
                + "&s_name=" + URLEncoder.encode("현재 위치", "UTF-8")
                + "&e_name=" + URLEncoder.encode(endName == null ? "" : endName, "UTF-8");
        JSONObject response = requestJson(BICYCLE_URL + params, restApiKey);
        JSONObject route = response.optJSONObject("route");
        if (route == null) throw new IOException("경로를 찾지 못했습니다.");
        JSONArray legs = route.optJSONArray("legs");
        if (legs == null || legs.length() == 0) throw new IOException("경로 정보가 없습니다.");

        List<NavRoute.Point> points = new ArrayList<>();
        List<NavRoute.Step> steps = new ArrayList<>();
        int legDistance = 0, legTime = 0;
        for (int i = 0; i < legs.length(); i++) {
            JSONObject leg = legs.getJSONObject(i);
            JSONObject legProps = leg.optJSONObject("properties");
            if (legProps != null) {
                legDistance += legProps.optInt("distance", 0);
                legTime += legProps.optInt("time", 0);
            }
            JSONArray stepArray = leg.optJSONArray("steps");
            if (stepArray == null) continue;
            for (int j = 0; j < stepArray.length(); j++) {
                JSONObject step = stepArray.getJSONObject(j);
                JSONObject props = step.optJSONObject("properties");
                if (props != null) {
                    String guidance = props.optString("guidance", "").trim();
                    double x = props.optDouble("x", Double.NaN), y = props.optDouble("y", Double.NaN);
                    if (!guidance.isEmpty() && valid(y, x)) steps.add(new NavRoute.Step(guidance, y, x));
                }
                JSONObject path = step.optJSONObject("path");
                JSONArray pairs = path == null ? null : path.optJSONArray("points");
                if (pairs == null) continue;
                for (int k = 0; k < pairs.length(); k++) {
                    JSONArray pair = pairs.optJSONArray(k);
                    if (pair == null || pair.length() < 2) continue;
                    double lng = pair.getDouble(0), lat = pair.getDouble(1);
                    if (!valid(lat, lng)) continue;
                    if (!points.isEmpty()) {
                        NavRoute.Point last = points.get(points.size() - 1);
                        if (last.latitude == lat && last.longitude == lng) continue;
                    }
                    points.add(new NavRoute.Point(lat, lng));
                }
            }
        }
        if (points.size() < 2) throw new IOException("경로 좌표가 부족합니다.");
        JSONObject props = route.optJSONObject("properties");
        int meters = props != null ? props.optInt("totalDistance", legDistance) : legDistance;
        int seconds = props != null ? props.optInt("totalTime", legTime) : legTime;
        return new NavRoute(points, steps, meters, seconds, endName, endLat, endLng, bicycleFirst);
    }

    private static boolean valid(double lat, double lng) {
        return !Double.isNaN(lat) && !Double.isNaN(lng) && lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180;
    }

    private static JSONObject requestJson(String url, String key) throws IOException, JSONException {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "KakaoAK " + key);
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(12000);
            connection.setReadTimeout(20000);
            int status = connection.getResponseCode();
            InputStream stream = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream();
            StringBuilder body = new StringBuilder();
            if (stream != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        body.append(line);
                        if (body.length() > 10_000_000) throw new IOException("서버 응답 크기가 너무 큽니다.");
                    }
                }
            }
            if (status < 200 || status >= 300) {
                throw new IOException("카카오 API 응답 오류 (HTTP " + status + ")"
                        + (status == 401 || status == 403 ? " · REST API 키 및 권한을 확인하세요." : ""));
            }
            return new JSONObject(body.toString());
        } finally {
            if (connection != null) connection.disconnect();
        }
    }
}
