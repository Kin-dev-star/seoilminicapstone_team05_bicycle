package com.example.bicyclesafety;

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
import java.util.Collections;
import java.util.List;

/**
 * Prototype REST client. Do not call on the main thread.
 * For production move REST authentication behind the team's Node.js backend:
 * a key embedded via BuildConfig can still be extracted from an APK.
 */
public final class KakaoRouteRepository {
    private KakaoRouteRepository() {}

    private static final String PLACE_URL =
            "https://dapi.kakao.com/v2/local/search/keyword.json";
    private static final String BICYCLE_URL =
            "https://dapi.kakao.com/v2/routing/bicycle";

    public static final class Point {
        public final double latitude;
        public final double longitude;
        public Point(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }
    public static final class GuideStep {
        public final String guidance;
        public final double latitude;
        public final double longitude;
        /** 카카오 응답의 안내 종류 코드(없으면 -1). 화살표는 안내 문구로 판별합니다. */
        public final int type;

        public GuideStep(String guidance, double latitude, double longitude) {
            this(guidance, latitude, longitude, -1);
        }

        public GuideStep(String guidance, double latitude, double longitude, int type) {
            this.guidance = guidance;
            this.latitude = latitude;
            this.longitude = longitude;
            this.type = type;
        }
    }

    public static final class Result {
        public final int distanceMeters;
        public final int durationSeconds;
        public final String matchedOrigin;
        public final String matchedDestination;
        public final List<Point> points;
        public final List<GuideStep> guideSteps;
        /** 내비게이션 화면 하단과 재탐색에 쓰는 도착지 정보 */
        public final String destinationName;
        public final double endLatitude;
        public final double endLongitude;
        public final boolean bicycleFirst;

        Result(int distanceMeters, int durationSeconds, String matchedOrigin,
               String matchedDestination, List<Point> points,
               List<GuideStep> guideSteps, String destinationName,
               double endLatitude, double endLongitude, boolean bicycleFirst) {
            this.distanceMeters = distanceMeters;
            this.durationSeconds = durationSeconds;
            this.matchedOrigin = matchedOrigin;
            this.matchedDestination = matchedDestination;
            this.points = Collections.unmodifiableList(new ArrayList<>(points));
            this.guideSteps = Collections.unmodifiableList(new ArrayList<>(guideSteps));
            this.destinationName = destinationName;
            this.endLatitude = endLatitude;
            this.endLongitude = endLongitude;
            this.bicycleFirst = bicycleFirst;
        }
    }

    private static final class Place {
        final String name;
        final String address;
        final double longitude;
        final double latitude;
        Place(String name, String address, double longitude, double latitude) {
            this.name = name;
            this.address = address;
            this.longitude = longitude;
            this.latitude = latitude;
        }
        String displayName() {
            return address.isEmpty() ? name : name + " (" + address + ")";
        }
    }

    /** Calls two place searches and one bicycle route query sequentially. */
    public static Result findRoute(String key, String origin, String destination,
                                   boolean bicycleFirst) throws IOException, JSONException {
        if (key == null || key.trim().isEmpty()) {
            throw new IOException("REST API 키가 설정되지 않았습니다. local.properties를 확인하세요.");
        }
        // Prototype: use top ranked search result. Add explicit place selection before release.
        Place start = searchFirstPlace(key, origin);
        Place end = searchFirstPlace(key, destination);
        return requestRoute(key, start, end, bicycleFirst);
    }

    /**
     * 안내 중 경로를 벗어났을 때 현재 위치에서 같은 도착지로 다시 찾습니다.
     * 장소 검색 없이 자전거 경로 API 한 번만 호출합니다.
     */
    public static Result rerouteFrom(String key, double latitude, double longitude,
                                     Result previous) throws IOException, JSONException {
        if (key == null || key.trim().isEmpty()) {
            throw new IOException("REST API 키가 설정되지 않았습니다. local.properties를 확인하세요.");
        }
        Place start = new Place("현재 위치", "", longitude, latitude);
        Place end = new Place(previous.destinationName, "", previous.endLongitude, previous.endLatitude);
        Result result = requestRoute(key, start, end, previous.bicycleFirst);
        return new Result(result.distanceMeters, result.durationSeconds, "현재 위치",
                previous.matchedDestination, result.points, result.guideSteps,
                previous.destinationName, previous.endLatitude, previous.endLongitude,
                previous.bicycleFirst);
    }

    private static Result requestRoute(String key, Place start, Place end,
                                       boolean bicycleFirst) throws IOException, JSONException {
        String params = "?start_x=" + start.longitude
                + "&start_y=" + start.latitude
                + "&end_x=" + end.longitude
                + "&end_y=" + end.latitude
                + "&input_coord=WGS84&output_coord=WGS84"
                + "&route_mode=" + (bicycleFirst ? "BIKE_ONLY" : "SHORTEST")
                + "&s_name=" + encode(start.name)
                + "&e_name=" + encode(end.name);
        JSONObject response = requestJson(BICYCLE_URL + params, key);
        JSONObject route = response.optJSONObject("route");
        if (route == null) {
            throw new IOException("경로를 찾지 못했습니다. 장소나 이동 범위를 확인하세요.");
        }

        JSONArray legs = route.optJSONArray("legs");
        if (legs == null || legs.length() == 0) {
            throw new IOException("경로 정보가 없습니다. 출발지·도착지를 바꿔보세요.");
        }
        List<Point> points = new ArrayList<>();
        List<GuideStep> guideSteps = new ArrayList<>();
        int legDistance = 0;
        int legTime = 0;
        for (int i = 0; i < legs.length(); i++) {
            JSONObject leg = legs.getJSONObject(i);
            JSONObject legProps = leg.optJSONObject("properties");
            if (legProps != null) {
                legDistance += legProps.optInt("distance", 0);
                legTime += legProps.optInt("time", 0);
            }
            JSONArray steps = leg.optJSONArray("steps");
            if (steps == null) continue;
            for (int j = 0; j < steps.length(); j++) {
                JSONObject step = steps.getJSONObject(j);
                JSONObject stepProps = step.optJSONObject("properties");

                if (stepProps != null) {
                    String guidance = stepProps.optString("guidance", "").trim();
                    double longitude = stepProps.optDouble("x", Double.NaN);
                    double latitude = stepProps.optDouble("y", Double.NaN);

                    if (!guidance.isEmpty() && isValidCoordinate(latitude, longitude)) {
                        guideSteps.add(new GuideStep(guidance, latitude, longitude,
                                stepProps.optInt("type", -1)));
                    }
                }
                JSONObject path = step.optJSONObject("path");
                if (path == null) continue;
                JSONArray pairs = path.optJSONArray("points");
                if (pairs == null) continue;
                for (int k = 0; k < pairs.length(); k++) {
                    JSONArray pair = pairs.optJSONArray(k);
                    if (pair == null || pair.length() < 2) continue;
                    double lng = pair.getDouble(0);
                    double lat = pair.getDouble(1);
                    if (!isValidCoordinate(lat, lng)) continue;
                    if (!points.isEmpty()) {
                        Point last = points.get(points.size() - 1);
                        if (last.latitude == lat && last.longitude == lng) continue;
                    }
                    points.add(new Point(lat, lng));
                }
            }
        }
        if (points.size() < 2) {
            throw new IOException("경로 좌표가 부족해 지도에 표시할 수 없습니다.");
        }
        JSONObject properties = route.optJSONObject("properties");
        int meters = properties != null ? properties.optInt("totalDistance", legDistance) : legDistance;
        int seconds = properties != null ? properties.optInt("totalTime", legTime) : legTime;
        return new Result(meters, seconds, start.displayName(), end.displayName(), points, guideSteps,
                end.name, end.latitude, end.longitude, bicycleFirst);
    }

    private static boolean isValidCoordinate(double lat, double lng) {
        return !Double.isNaN(lat) && !Double.isNaN(lng)
                && lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180;
    }

    private static Place searchFirstPlace(String key, String text) throws IOException, JSONException {
        JSONObject response = requestJson(PLACE_URL + "?query=" + encode(text) + "&size=5", key);
        JSONArray documents = response.optJSONArray("documents");
        if (documents == null || documents.length() == 0) {
            throw new IOException("'" + text + "' 검색 결과가 없습니다. 정확한 장소명을 입력해 주세요.");
        }
        JSONObject found = documents.getJSONObject(0);
        return new Place(found.optString("place_name", text),
                found.optString("road_address_name", found.optString("address_name", "")),
                Double.parseDouble(found.getString("x")),
                Double.parseDouble(found.getString("y")));
    }

    private static String encode(String value) throws IOException {
        // String overload is supported on the Android project's minSdk 26.
        return URLEncoder.encode(value, "UTF-8");
    }

    private static JSONObject requestJson(String url, String restApiKey) throws IOException, JSONException {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "KakaoAK " + restApiKey);
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(12000);
            connection.setReadTimeout(20000);
            int status = connection.getResponseCode();
            InputStream stream = status >= 200 && status < 300
                    ? connection.getInputStream() : connection.getErrorStream();
            String text = readBody(stream);
            if (status < 200 || status >= 300) {
                String message = "카카오 API 응답 오류 (HTTP " + status + ")";
                try {
                    JSONObject error = new JSONObject(text);
                    String detail = error.optString("msg", error.optString("message", ""));
                    if (!detail.isEmpty()) message += ": " + detail;
                } catch (JSONException ignored) {
                    // An HTML or empty error body is possible.
                }
                if (status == 401 || status == 403) {
                    message += " · REST API 키 및 카카오맵 사용 권한을 확인하세요.";
                }
                throw new IOException(message);
            }
            return new JSONObject(text);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static String readBody(InputStream stream) throws IOException {
        if (stream == null) return "";
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
                // Avoid unbounded memory use from unexpectedly large/error responses.
                if (result.length() > 10_000_000) {
                    throw new IOException("서버 응답 크기가 너무 큽니다.");
                }
            }
        }
        return result.toString();
    }
}
