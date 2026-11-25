# -*- coding: utf-8 -*-
"""Java 실행 테스트가 아닌 S6/S7 연결 흐름의 정적 회귀 검사입니다."""
from pathlib import Path
s=(Path(__file__).parents[1]/'app/src/main/java/com/example/bicyclesafety/MainActivity.java').read_text(encoding='utf-8')
required = {
  '경로 마커 캐시': 'private final List<RiskPoint> routeHazardsVisible = new ArrayList<>();',
  'GPS 갱신 시 경로 마커 보존': 'refreshHazardMarkers(pendingRoute == null ? null : routeHazardsVisible);\n                markerCenterLocation',
  '사고 데이터 지연 로딩 재계산': 'routeHazardsVisible.addAll(hazardsAlongRoute(pendingRoute.points));',
  '지연 로딩 후 UI 결과 갱신': 'lastRouteBicycleFirst, hazardPoints.isEmpty() ? -1 : routeHazardsVisible.size(), hazardSource)',
  '새 경로 요청 때 마커 초기화': 'final int revision = ++routeRequestRevision;\n        pendingRoute = null;\n        routeHazardsVisible.clear();',
  '입력 변경 시 마커 초기화': 'pendingRoute = null;\n        routeHazardsVisible.clear();\n        guidanceActive',
}
for name, fragment in required.items():
    assert fragment in s, 'FAIL: ' + name
print('PASS: 경로 마커·지연 사고 데이터·요청 초기화 연결 흐름 정적 검사 (실제 앱 실행 검증 아님)')
