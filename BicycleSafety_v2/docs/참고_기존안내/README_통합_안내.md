# BicycleSafety 2026-11-11 — 최종 UI 통합 작업본

## 무엇이 달라졌나요?
- 원본 `BicycleSafety_26.11.11.zip`의 Java 카카오맵 SDK 연결, 위험지점 3건, 클릭 처리를 유지했습니다.
- `bike-ui-export.zip`의 Kotlin/Jetpack Compose 디자인(로딩 애니메이션, 원통 메뉴, 경로 설정 시트, 설정·마이페이지)을 통합했습니다.
- 가짜 지도 자리 `MapPlaceholder` 대신 실제 Kakao `MapView`를 Compose의 `AndroidView`로 넣었습니다.
- 마커 클릭 정보는 새 소프트 UI 위험 안내 팝업으로 표시합니다.
- 기존 **주행 전 안전 확인 게이트**도 새 디자인의 팝업으로 다시 그렸으며 확인/종료 기능을 Java 쪽에 연결했습니다.
- 새 경로 시트의 출발지·도착지 입력, **안전 우회/최단 경로** 선택, **경로 분석** 버튼을 기존 임시 처리 로직에 연결했습니다. 텍스트는 시트를 닫았다 열어도 유지됩니다.
- UX/UI 담당자가 보낸 블랙/화이트 로고를 리소스로 포함하고 앱 런처 아이콘 및 로딩 화면에 반영했습니다.
- 밝은/블랙 테마는 디자이너 구현을 사용했습니다. 라이딩 모드는 아직 디자인 대기입니다.

## Windows Android Studio에서 실행
1. 기존 11/11 프로젝트는 보관하고, 이 ZIP의 `BicycleSafety_UI_Integrated` 폴더를 별도로 압축 해제합니다. **한글 경로 없이** 사용하는 편이 안전합니다.
2. Android Studio의 `File → Open`에서 `BicycleSafety_UI_Integrated`를 엽니다. `app`만 선택하지 마세요.
3. Gradle Sync 실행. 새 Kotlin/Compose 의존성을 처음 다운로드하므로 네트워크가 필요합니다.
4. 기존에 실행된 Pixel 10 에뮬레이터를 선택하고 `Run app` 실행.
5. 주행 전 안전 안내 확인 → 새 로딩 화면 → 실제 지도 표시 → 마커 3개 클릭 → 경로 시트 입력·모드 변경·경로 분석 → 설정/블랙 테마 순서로 확인합니다.
6. Gradle/컴파일 문제가 발생하면 `Build Output`의 **첫 번째 오류** 전문을 기록하세요.

## 파일별 연결 구조
- `app/src/main/java/com/example/bicyclesafety/MainActivity.java`: 기존 카카오맵 SDK 시작·일시정지·마커, 경로 분석 임시 메시지, 안전 확인 게이트.
- `app/src/main/java/com/example/app/ui/BikeUiBridge.kt`: Java MainActivity ↔ Compose UI 및 Kakao MapView의 유일한 연결점.
- `app/src/main/java/com/example/app/ui/BikeApp.kt`: 새 로딩→메인 화면 구성.
- `app/src/main/java/com/example/app/ui/main/MainScreen.kt`: 새 원통 메뉴/경로 시트/테마와 지도 자리.
- `app/src/main/java/com/example/app/ui/theme/Soft.kt`: 디자이너 제공 뉴모피즘 스타일.
- `app/src/main/java/com/example/app/ui/loading/BikeLoadingScreen.kt`: 디자이너 제공 자전거 로딩 애니메이션 + 로고.
- `app/src/main/res/layout/activity_main.xml`: ComposeView 한 개. 이전 XML 레이아웃은 참조용으로 보관했으며 실행 화면에서는 사용하지 않습니다.

## 반드시 구분할 사항
- 실제 GPS, TTS, 경로 계산, 데이터베이스/계정 API는 **아직 연결되지 않았습니다.** 현재 위험지점 3개는 **가상 샘플**입니다.
- 마이페이지 숫자와 사용자는 디자이너가 넣은 **예시 데이터**입니다.
- 실기기/Pixel 10 실행 및 Gradle 전체 빌드는 이 작업 환경에서 직접 수행하지 못했습니다. 소스 연결과 리소스 문법을 점검했으며, 실제 런타임 확인이 필요합니다.
- 배포 또는 공개 GitHub 업로드 전 `MapApplication.java`에 있는 카카오 네이티브 앱 키를 개인 로컬 설정으로 분리하고, 개발자 콘솔 등록 패키지/키 해시를 확인하세요.
- 앱 테마의 지도 색(카카오맵 타일)은 블랙 테마 전환으로 자동 변경되지 않습니다. 현재는 UI 패널과 메뉴만 블랙 전환됩니다.

## 테스트 체크리스트
- [ ] Gradle Sync 및 Debug 빌드 성공
- [ ] Pixel 10 앱 실행, 로딩 애니메이션과 로고
- [ ] 주행 전 안전 팝업 → 확인 후 실제 카카오맵 표시
- [ ] 빨간 마커 3개 및 이름별 위험도/주의 팝업
- [ ] 지도를 드래그·핀치 확대
- [ ] 오른쪽 원통 메뉴·모아보기 및 경로 시트 열기/닫기
- [ ] 출발·도착 한글 입력, 안전 우회/최단 경로 선택
- [ ] 분석 버튼 → API 미연결 메시지 (입력값 유지)
- [ ] 설정 화면 → 블랙 테마 → 복귀·재실행 후 유지
- [ ] 마이페이지는 예시 데이터로 표시됨
