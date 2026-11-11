# 어둠너머

어둠과 구조물에 가려 차량 센서와 운전자의 눈이 닿지 않는 비신호 교차로를, 차량 화면과 음성으로 미리 알려 주는 Pleos Connect 앱입니다.

> **고지: 실차·실센서 미검증.** 이 저장소의 모든 차량 신호, GPS 경로, 반사경 이벤트는 합성 입력입니다. 교차로 좌표는 시험용 샘플이며 실제 교차로나 설치 지점이 아닙니다. 실제 차량과 실제 센서로는 시험하지 않았습니다.

## 구조

| 단계 | 내용 | 이번 저장소 |
|---|---|---|
| 1단계 | 단말에 저장한 시야 제한 비신호 교차로 DB로, 진행 방향 앞 교차로 50m 전에 한 번 음성 안내 | 판단 로직, 합성 입력, 앱 화면·음성 구현 |
| 2단계 | 스마트 반사경 키트의 접근 이벤트를 운전자 기준 좌·우 경보로 변환 | 이벤트 형식, 좌우 판정, 합성 생성기만 포함 |

```
core/   순수 Kotlin 판단 로직. Android 의존 없음, JVM에서 단위 테스트
  GuidanceEngine   1단계 안내 규칙
  SpeechQueue      한 번에 한 문장, 지나친 안내 취소
  SmartMirror      2단계 이벤트 형식과 좌우 판정 (인터페이스)
  Data             샘플 DB·시나리오 읽기
app/    Android 앱. 입력 연결, 화면, 음성만 담당
data/   샘플 교차로 DB, 안내 설정값, 합성 시나리오 (core 테스트와 앱이 같은 파일 사용)
docs/   안내 규칙, 이벤트 형식, SDK 확인 상태, 시연 영상 체크리스트
```

입력은 `LocationSource`(위치·진행 방향)와 `VehicleSignalSource`(속도·기어·전조등·와이퍼·브레이크·방향지시등)로 나뉩니다. 지금은 두 입력 모두 시나리오 파일이 공급하며, Pleos SDK API가 확인되면 같은 인터페이스의 실제 구현으로 바꿉니다.

## 실행 방법

1. Android Studio를 설치하고 이 저장소를 엽니다. 경로에 한글이 없는 폴더를 권장합니다.
2. Android SDK가 잡히면 `app` 모듈이 자동으로 포함됩니다(`local.properties`의 `sdk.dir`).
3. Pleos Connect Emulator v2.1.0 이상을 설치합니다. 설치 절차: [에뮬레이터 설정](https://document.pleos.ai/en/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator)
4. `app`을 에뮬레이터에서 실행합니다.

## 시나리오 재현

앱 하단의 시나리오 버튼을 누르면 `data/scenarios/`의 CSV를 시간에 따라 재생합니다. 화면 맨 위에는 "합성 입력" 표시가 항상 보입니다.

| 파일 | 재현 내용 | 기대 결과 |
|---|---|---|
| `s01_night_basic.csv` | 120m 전에서 시속 30km 북진, 전조등 켜짐 | 50m에서 1회 안내, 통과 후 종료 |
| `s02_day_no_light.csv` | 전조등·와이퍼 꺼짐 | 안내 생략 |
| `s03_hotspot_day.csv` | 사고다발 교차로, 주간 | 안내 |
| `s04_slow_turn_signal.csv` | 시속 15km, 왼쪽 방향지시등 | 안내 |
| `s05_brake.csv` | 브레이크를 밟은 채 진입 | 안내 생략 |
| `s06_cluster.csv` | 100m 간격 교차로 2곳 | 한 번만 안내 |

CSV 열: `t_ms,lat,lon,speed_kph,gear,headlight,wiper,brake,turn`. 키프레임 사이는 선형 보간하며, 진행 방향은 이동 방향으로 계산합니다.

## 테스트 방법

```bash
./gradlew :core:test
```

Windows에서는 `gradlew.bat :core:test`. JDK 17이 필요합니다.

2026-11-11 실행 결과: JDK 17, Gradle 8.7에서 **47개 테스트 모두 통과**. 거리·방위 계산, 진행 방향 앞 판정, 안내 규칙 각각, 음성 순서·취소, 좌우 판정(±45°·±135° 경계 포함), 샘플 DB·설정 파일, 시나리오 6개 재생을 확인합니다.

## Pleos SDK 확인 상태

자세한 내용은 [docs/sdk-status.md](docs/sdk-status.md)에 있습니다. 요약하면 다음과 같습니다.

| 대상 | 상태 |
|---|---|
| FusedLocation | 문서 확인, API 연결 **미확인**. 합성 입력 사용 |
| Vehicle (Speed·Gear·Light·Wiper·Brake·TurnSignal) | 도메인과 의존성 좌표 확인, 메서드·권한 **미확인**. 합성 입력 사용 |
| TextToSpeech (ON_DEVICE) | 문서 확인, API **미확인**. Android 표준 TextToSpeech 사용 |
| NaviHelper | 문서 확인. 이번 범위에서 사용하지 않음 |
| Pleos Connect Emulator | 개발 PC 미설치. 앱 실행 **미검증** |

## 화면 캡처

| 장면 | 이미지 |
|---|---|
| 시나리오 선택 | `docs/images/01-scenarios.png` (촬영 예정) |
| 50m 안내 | `docs/images/02-announce.png` (촬영 예정) |
| 안내 생략 기록 | `docs/images/03-skip.png` (촬영 예정) |

## 저장소에 넣지 않는 것

스마트 반사경 키트의 회로, 펌웨어, 레이더 설정값, 부착 도면, 서버 인증키, 실제 설치 지점 좌표는 별도 자산이며 이 저장소에 포함하지 않습니다.

## 문서

- [안내 규칙](docs/guidance-rules.md)
- [반사경 이벤트 형식](docs/event-format.md)
- [SDK 확인 상태](docs/sdk-status.md)
- [시연 영상 촬영 체크리스트](docs/demo-video-checklist.md)
