#!/bin/sh
# PC에서 길안내 모듈만 테스트 (Android 필요 없음). bike-navigation-core 폴더에서 실행: sh test/run_test.sh
# org.json 은 Android 에 들어 있어서, PC 테스트용으로만 빈 클래스(test/stubs)를 씁니다.
set -e
rm -rf test/out && mkdir -p test/out
javac -encoding UTF-8 -nowarn -d test/out test/stubs/org/json/*.java \
  src/com/example/bicyclesafety/navigation/NavRoute.java \
  src/com/example/bicyclesafety/navigation/Maneuver.java \
  src/com/example/bicyclesafety/navigation/RouteTracker.java \
  src/com/example/bicyclesafety/navigation/NavigationSession.java \
  src/com/example/bicyclesafety/navigation/KakaoBicycleRouteClient.java \
  test/CoreTest.java test/EdgeTest.java
echo "== 시나리오: 직진 → 좌회전 → 사고지점 접근 → 경로 이탈 → 자동 재탐색 → 우회전 → 도착"
java -Dstdout.encoding=UTF-8 -Dfile.encoding=UTF-8 -cp test/out CoreTest | grep -v "state:"
echo "== 예외 상황: GPS 오차 큼 / 재탐색 중 종료 / 재탐색 실패 / 재탐색 없음"
java -Dstdout.encoding=UTF-8 -Dfile.encoding=UTF-8 -cp test/out EdgeTest
