package com.example.bicyclesafety;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * S6 사고 데이터 입력부.
 * - 기본: 실제 도로교통공단 공공데이터의 팀 CSV 가공본(오프라인 파일)
 * - 팀 서버: TEAM_HAZARD_API_URL 을 설정했을 때만 JSON 조회(HTTPS)
 * 이 클래스의 응답 변환만 바꾸면 지도/화면 코드를 손댈 필요가 없습니다.
 */
public final class RiskDataRepository {
    private RiskDataRepository() {}

    public static List<RiskPoint> loadBundled(Context context) throws IOException, JSONException {
        try (InputStream input = context.getAssets().open("seoul_bicycle_accidents.json")) {
            return parse(new String(readAll(input), StandardCharsets.UTF_8), "오프라인 공공데이터");
        }
    }

    /** 팀원과 합의한 예시 계약: {"items":[{"id":"...","name":"...","latitude":37.5,
     * "longitude":127.0,"accidentCount":5,"casualtyCount":6}]} (배열 루트도 허용).
     * 운영 주소는 HTTPS만 허용. 로컬 HTTP 서버를 바로 개방하지 않습니다. */
    public static List<RiskPoint> loadFromTeamServer(String endpoint) throws IOException, JSONException {
        if (endpoint == null || !endpoint.startsWith("https://")) {
            throw new IOException("팀 API는 HTTPS 주소가 필요합니다. TEAM_HAZARD_API_URL 설정을 확인하세요.");
        }
        HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
        try {
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("Accept", "application/json");
            if (conn.getResponseCode() != 200) throw new IOException("팀 사고 API HTTP " + conn.getResponseCode());
            try (InputStream input = conn.getInputStream()) {
                return parse(new String(readAll(input), StandardCharsets.UTF_8), "팀 서버 API");
            }
        } finally { conn.disconnect(); }
    }

    static List<RiskPoint> parse(String json, String source) throws JSONException {
        String trimmed = json.trim();
        JSONArray array = trimmed.startsWith("[") ? new JSONArray(trimmed) : new JSONObject(trimmed).getJSONArray("items");
        List<RiskPoint> points = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject row = array.optJSONObject(i);
            if (row == null) continue;
            double lat = row.optDouble("latitude", Double.NaN);
            double lng = row.optDouble("longitude", Double.NaN);
            if (!Double.isFinite(lat) || !Double.isFinite(lng) || lat < 33 || lat > 39 || lng < 124 || lng > 132) continue;
            RiskPoint point = new RiskPoint();
            point.id = row.optString("id", String.valueOf(i));
            point.name = row.optString("name", "사고다발지역");
            point.latitude = lat;
            point.longitude = lng;
            point.accidentCount = Math.max(0, row.optInt("accidentCount", 0));
            point.casualtyCount = Math.max(0, row.optInt("casualtyCount", 0));
            point.riskLevel = "사고 이력"; // 팀의 위험도 등급 산식 확정 전에는 등급을 임의로 계산하지 않음
            point.description = "사고 " + point.accidentCount + "건 · 사상자 " + point.casualtyCount
                    + "명 (과거 공공데이터, 실시간 위험도 아님)";
            point.source = source;
            points.add(point);
        }
        return Collections.unmodifiableList(points);
    }

    private static byte[] readAll(InputStream input) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int count;
            while ((count = input.read(buf)) != -1) {
                out.write(buf, 0, count);
                if (out.size() > 5_000_000) throw new IOException("사고 데이터 용량 제한(5MB) 초과");
            }
            return out.toByteArray();
        }
    }
}
