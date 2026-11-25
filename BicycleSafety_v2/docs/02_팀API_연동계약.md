# 팀 서버 사고 API 연동 계약(협의용)

## 백엔드 담당자에게 필요한 확인 사항
1. 실제 서버 접근 주소 (학교 PC/에뮬레이터에서 접근 가능해야 함): `https://...`
2. HTTP 메서드 `GET`, 응답 MIME `application/json; charset=utf-8`
3. 사고 자료 결과 형식 및 컬럼 이름, 페이징/조회 범위/좌표계(WGS84) 정의
4. 위험 판별 API의 요청 좌표, 응답 위험 기준, 안내 문구, 오류 메시지 정의
5. 공공데이터 기준 연도, 업데이트 날짜, 사고다발지역 ID 중복 가능 여부
6. 인증 여부 및 발급 방식(테스트 비밀 키는 GitHub에 올리지 말기)

## 프론트가 지금 받을 수 있도록 만든 **제안 JSON 형식**

```json
{
  "items": [
    {
      "id": "161307",
      "name": "서울특별시 종로구 경운동(안국역 부근)",
      "latitude": 37.576533,
      "longitude": 126.985544,
      "accidentCount": 4,
      "casualtyCount": 4
    }
  ]
}
```

- **이 형식은 합의 전 제안**이지, 백엔드에서 이미 제공하는 API가 아닙니다.
- 서버가 최종 형식을 변경하면 **`RiskDataRepository.java`**의 `parse()`만 수정합니다.
- `local.properties`에 `TEAM_HAZARD_API_URL=https://...`을 입력할 때만 팀 서버 조회를 시도합니다.
- 서버 실패 시 오프라인 CSV 가공 데이터를 사용하고, `Logcat`에 오류를 남기며 결과 표시에도 자료 출처를 붙입니다.
- `http://localhost:8088/1`은 현재 안드로이드에서 올바른 원격 서버 주소가 아니고, 이전 Java 서버 반환 텍스트는 JSON 계약과 다릅니다.
- API 기본주소·비밀키는 공개 GitHub에 올리지 않습니다.

## 경로 API 데이터 흐름

```
UI-03 출발지/도착지 + BIKE_ONLY/SHORTEST
   -> BikeUiBridge
   -> MainActivity
   -> KakaoRouteRepository
   -> 카카오 장소 검색 2회 (좌표)
   -> 카카오 자전거 경로 조회 (distance/time/steps/path.points)
   -> 지도 경로선 + 예상거리/시간 + 사고이력 150m 반경 건수
```

실제 **위험 회피**는 별도 공통 API와 알고리즘이 준비되어야 합니다.
