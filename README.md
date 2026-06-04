# SafeFood

과자 이미지를 촬영하거나 갤러리에서 선택하면 TensorFlow Lite 모델이 17종 과자를 분류하고,
사용자가 등록한 알러지 성분과 교차 검사하여 경고를 제공하는 Android 앱입니다.

---

## 목차

1. [스크린샷](#스크린샷)
2. [주요 기능](#주요-기능)
3. [기술 스택](#기술-스택)
4. [아키텍처](#아키텍처)
5. [모델 개발 과정](#모델-개발-과정)
6. [프로젝트 구조](#프로젝트-구조)
7. [개발 환경 및 빌드](#개발-환경-및-빌드)

---

## 스크린샷

| | | | |
|:---:|:---:|:---:|:---:|
| ![](images/image.png) | ![](images/image-1.png) | ![](images/image-2.png) | ![](images/image-3.png) |

---

## 주요 기능

- 카메라 촬영 / 갤러리 이미지로 과자 분류 (17종)
- 알러지 성분 교차 검사 및 경고
- 분류 히스토리 (사용자별 저장)
- 마이페이지 (알러지 정보 수정, 비밀번호 변경)
- 회원가입 / 로그인 / 앱 재시작 후 자동 로그인 유지
- 게스트 모드 (로그인 없이 사용, 앱 재시작 시 초기화)
- TTS 음성 안내 지원

### 분류 가능 과자 (17종)

해태 포키 블루베리, 꼬칼콘 고소한맛, 농심 매운새우깡, 콘초, 프링글스, 포테토칩, 포카칩,
빠다코코낫, 몽쉘, 야채크래커, 쁘띠첼 구미젤리, 해태 에이스, 허니버터칩, 농심 알새우칩,
예감 치즈그라탕, 쫄병, 크라운 쵸코하임

---

## 기술 스택

| 분류 | 내용 |
|---|---|
| 언어 | Java 8 (JDK 1.8) |
| 최소 SDK | API 27 (Android 8.1 Oreo) |
| 타겟 SDK | API 34 (Android 14) |
| AI / 추론 | TensorFlow Lite (`Inq1.tflite`) + ML Model Binding |
| UI | Material Design 3, AndroidX AppCompat |
| 아키텍처 | MVVM — ViewModel + LiveData (AndroidX Lifecycle 2.6.1) |
| 데이터 저장 | SQLite (SQLiteOpenHelper) — 사용자 계정, 히스토리 |
| 세션 관리 | SharedPreferences — 로그인 세션, 게스트 알러지 |
| 보안 | SHA-256 비밀번호 단방향 해싱 |
| 음성 | Android TextToSpeech (TTS) |
| 카메라 | CameraX FileProvider (사진 촬영, 갤러리 선택) |

---

## 아키텍처

### 분류기 설계 — Decorator + Factory 패턴

이미지 분류 기능을 인터페이스(`IClassifier`) 기반으로 설계하여
분류 방식이 바뀌어도 화면 코드를 수정하지 않아도 됩니다.

`ClassifierFactory`가 아래 순서로 Decorator 체인을 조립합니다.

```
ClassifierFactory.create(context)
  └── LoggingClassifierDecorator   // 분류 시간 측정 및 로그 출력
        └── SafeClassifierDecorator  // init() 호출 여부 사전 검증
              └── ClassiferWithModel   // 핵심 TFLite 추론 로직
```

| 클래스 | 역할 |
|---|---|
| `IClassifier` | 분류기 공통 인터페이스 (`init`, `classify`, `finish`) |
| `ClassiferWithModel` | TFLite 모델 로드 및 실제 추론 수행 |
| `SafeClassifierDecorator` | `init()` 없이 `classify()` 호출 시 즉시 예외 발생 |
| `LoggingClassifierDecorator` | 추론 소요 시간 측정 및 Logcat 출력 |
| `ClassifierFactory` | Decorator 체인 조립 — 구현체 교체 시 이 클래스만 수정 |

### MVVM 흐름

```
Activity / Fragment
    ↕ observe / call
  ViewModel  (ClassifierViewModel, AuthViewModel, HistoryViewModel, MyPageViewModel)
    ↕
  Repository  (UserRepository, HistoryRepository)
    ↕
  SQLite (SQLiteOpenHelper)
```

---

## 모델 개발 과정

### 실험한 모델

| 파일 | 설명 |
|---|---|
| `model.tflite` | 초기 실험 모델 |
| `model2.tflite` | 구조 변경 실험 |
| `modelM.tflite` | MobileNet 기반 실험 |
| `Inception1.tflite` | InceptionV3 기반 실험 |
| `model_quant_f16.tflite` | Float16 양자화 실험 |
| `Inq1.tflite` | **최종 채택 — INT8 양자화** |

### 최적화 결과

| 항목 | 원본 모델 | 최종 모델 (`Inq1.tflite`) |
|---|---|---|
| 모델 크기 | 200 MB | 52 MB |
| 분류 정확도 | 92% | 88% |
| 크기 감소 | — | 약 4배 감소 |
| 정확도 하락 | — | 4% 하락 |

용량을 4배 줄이면서 정확도 하락은 4%에 그쳤습니다.

### 이미지 전처리 파이프라인

`ImageProcessor`를 통해 추론 전 전처리를 순서대로 적용합니다.

```
Bitmap 입력
  → ARGB_8888 변환 (포맷 통일)
  → 짧은 변 기준 정사각형 크롭 (ResizeWithCropOrPadOp)
  → 모델 입력 크기로 리사이즈 (ResizeOp, NEAREST_NEIGHBOR)
  → 카메라 센서 방향 보정 회전 (Rot90Op)
  → 픽셀값 정규화 0.0 ~ 1.0 (NormalizeOp)
  → TFLite 추론
```

---

## 프로젝트 구조

```
app/src/main/
├── java/kr/ac/baekseok/ab/
│   │
│   ├── [화면]
│   ├── MainActivity.java             # 로그인 화면
│   ├── SubActivity.java              # 메인 메뉴
│   ├── CameraActivity.java           # 카메라 분류
│   ├── GalleryActivity.java          # 갤러리 분류
│   ├── HistoryActivity.java          # 분류 히스토리
│   ├── MyPageActivity.java           # 마이페이지
│   ├── Join.java                     # 회원가입
│   │
│   ├── [분류기 — Decorator 패턴]
│   ├── IClassifier.java              # 분류기 공통 인터페이스
│   ├── ClassiferWithModel.java       # TFLite 추론 핵심 구현체 (현재 사용)
│   ├── Classifer.java                # 초기 분류기 구현체
│   ├── ClassiferWithSupport.java     # Support Library 기반 구현체
│   ├── SafeClassifierDecorator.java  # 초기화 검증 Decorator
│   ├── LoggingClassifierDecorator.java # 시간 측정/로그 Decorator
│   ├── ClassifierFactory.java        # Decorator 체인 조립
│   ├── ClassificationResult.java     # 분류 결과 모델
│   │
│   ├── [ViewModel]
│   ├── ClassifierViewModel.java      # 이미지 분류 로직
│   ├── AuthViewModel.java            # 로그인/회원가입 로직
│   ├── HistoryViewModel.java         # 히스토리 로직
│   ├── MyPageViewModel.java          # 마이페이지 로직
│   │
│   ├── [Repository / DB]
│   ├── UserRepository.java           # 사용자 DB
│   ├── HistoryRepository.java        # 히스토리 DB
│   ├── FoodAllergyDatabase.java      # 과자별 알러지 데이터 (22종 기준)
│   ├── HistoryRecord.java            # 히스토리 레코드 모델
│   ├── HistoryAdapter.java           # 히스토리 RecyclerView 어댑터
│   │
│   ├── [상태 / 세션]
│   ├── SessionManager.java           # 로그인 세션 유지
│   ├── LoginState.java               # 로그인 상태
│   ├── IdCheckState.java             # 아이디 중복 확인 상태
│   ├── PasswordChangeState.java      # 비밀번호 변경 상태
│   ├── SingleLiveEvent.java          # 한 번만 소비되는 LiveData
│   ├── AppConstants.java             # 전역 상수 (GUEST_USER_ID 등)
│   │
│   └── [유틸]
│       ├── TtsHelper.java            # TTS 헬퍼 (speakAndThen 콜백 지원)
│       └── PasswordUtil.java         # SHA-256 해싱
│
├── assets/
│   ├── Inq1.tflite                   # 최종 분류 모델 (INT8 양자화, 52MB)
│   └── labels.txt                    # 과자 클래스 목록 (17종)
│
└── res/layout/                       # XML 레이아웃
```

---

## 개발 환경 및 빌드

- Android Studio Hedgehog (2023.1.1) 이상
- JDK 8
- Gradle 8.x
- 실기기 또는 에뮬레이터 (카메라 기능은 실기기 권장)

```bash
# Android Studio에서 열기
# File > Open > SafeFood 폴더 선택
# Gradle Sync 완료 후 Run (Shift+F10)
```

> `local.properties`는 Android SDK 경로를 자동 생성하므로 별도 설정 불필요합니다.
