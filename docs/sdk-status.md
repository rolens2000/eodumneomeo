# Pleos SDK와 에뮬레이터 확인 상태

확인일 2026-11-10~11. 공개 문서 기준이며, SDK 이름이나 API를 추측해 코드에 쓰지 않았습니다. 확인되지 않은 항목은 core의 입력 인터페이스 뒤에 합성 입력을 두었습니다.

| 대상 | 공개 문서에서 확인한 내용 | 확인 안 된 것 | 저장소 처리 |
|---|---|---|---|
| FusedLocation | 위도·경도·진행 방향(LocationData), 위치 업데이트 콜백, 영역 진입·이탈 알림, 권한 `pleos.car.permission.FUSED_LOCATION` (v1.1.3 가이드). My Project 안내에는 "준비 중" | 사용 가능 시점, 실제 호출 결과 | **미확인**. `LocationSource` 합성 구현 |
| Vehicle - Speed | Vehicle SDK 튜토리얼 소개에 `PERF_VEHICLE_SPEED`(m/s) 언급 | 앱 코드에서 읽는 API와 권한 문자열 | **미확인**. 합성 |
| Vehicle - Gear | 에뮬레이터 시험 안내에 `GEAR_SELECTION` 속성 언급, Virtual Console에서 기어 설정 가능 | 앱 코드 API, 권한 | **미확인**. 합성 |
| Vehicle - Light·Wiper·Brake·TurnSignal | Vehicle SDK 도메인 목록에 Light, Wiper, Brake, TurnSignal 존재 (v2.3.2 소개) | 각 값을 읽는 메서드, 권한 | **미확인**. 합성 |
| Vehicle 의존성 | 저장소 `https://nexus-playground.pleos.ai/repository/maven-releases/`, 좌표 예시 `ai.pleos.playground:Vehicle:2.3.1` (Getting Started) | 소개 문서의 v2.3.2와 버전 차이, 실제 빌드 | 의존성 미추가 |
| TextToSpeech (ON_DEVICE) | ON_DEVICE 모드는 네트워크·앱 등록 없이 사용 (v3.3.1). My Project 안내에는 "준비 중". 에뮬레이터에 음성 엔진 `ai.pleos.playground.caas`가 설치되어 있으나 기본 엔진으로 지정되지 않음 | 앱 코드 API, 우선순위 정책, 엔진 연결 조건 | **미확인**. Android 표준 `TextToSpeech`로 위 엔진을 직접 지정해도 연결 실패 (2026-11-11). 녹음 음성으로 대체 |
| NaviHelper | 신청 없이 사용 가능. 내비게이션 정보 조회, 경로·주행 상태 이벤트 | 기본 지도 위 사용자 표시 | 이번 범위에서 미사용 |
| 주행 중 사용 | Distraction Optimization 미선언 앱은 주행 중 사용 불가 | Manifest 선언의 실제 인정 여부 | `distractionOptimized` 메타데이터 선언만 |
| Pleos Connect Emulator | API 34 이미지, v2.1.0 이상, Cold boot 권장 | 차량 신호를 앱에 주입하는 방법 | 2026-11-11 v2.1.0 이미지(RAM 4GB, Cold boot)에서 debug APK 설치·실행, 합성 시나리오 s01·s02 동작 **확인** |

## 출처

- [Getting Started](https://document.pleos.ai/en/docs/pleos-only/getting-started)
- [FusedLocation](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/intro)
- [Vehicle SDK](https://document.pleos.ai/en/api-reference/connect-sdk-pleos/Vehicle/intro)
- [Vehicle SDK 튜토리얼](https://document.pleos.ai/en/docs/pleos-only/tutorial/vehicle-sdk)
- [TextToSpeech](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/TextToSpeech/intro)
- [NaviHelper](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/intro)
- [주행 제한 정책](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/driving-restiction-policy)
- [에뮬레이터 설정](https://document.pleos.ai/en/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator)
- [프로젝트 생성·SDK 신청](https://document.pleos.ai/docs/pleos-only/create-my-project)
