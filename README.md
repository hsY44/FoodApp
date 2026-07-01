# SafeFood

![Android](https://img.shields.io/badge/Android-API%2027%2B-3DDC84?logo=android&logoColor=white)
![Java](https://img.shields.io/badge/Java-8-ED8B00?logo=openjdk&logoColor=white)
![TensorFlow Lite](https://img.shields.io/badge/TFLite-INT8-FF6F00?logo=tensorflow&logoColor=white)
![Project](https://img.shields.io/badge/프로젝트-개인-blue)

> 개인 프로젝트 | TFLite INT8 양자화 기반 Android 과자 분류 앱

---

## 📌 프로젝트 소개

**SafeFood**는 과자 이미지를 촬영하거나 갤러리에서 선택하면 TensorFlow Lite 모델이 17종 과자를 분류하고,
사용자가 등록한 알러지 성분(22종 기준)과 교차 검사하여 경고를 제공하는 Android 앱입니다.

INT8 양자화로 모델 크기를 **200 MB → 52 MB(약 4배 감소)**, 최종 분류 정확도 **88%**로 모바일 환경에 최적화했습니다.

---

## 목차

1. [프로젝트 소개](#📌-프로젝트-소개)
2. [스크린샷](#스크린샷)
3. [주요 기능](#주요-기능)
4. [기술 스택](#🛠-기술-스택)
5. [아키텍처](#🏗-아키텍처)
6. [모델 개발 과정](#🤖-모델-개발-과정)


---

## 스크린샷

| 로그인 | 메인 / 과자 분류 | 분류 결과 / 알러지 경고 | 분류 히스토리 |
|:---:|:---:|:---:|:---:|
| ![로그인](images/image.png) | ![메인](images/image-1.png) | ![결과](images/image-2.png) | ![히스토리](images/image-3.png) |

---

## 주요 기능

- 카메라 촬영 / 갤러리 이미지로 과자 분류 (17종)
- 알러지 성분 교차 검사 및 경고 (`FoodAllergyDatabase` — 22종 성분 기준)
- 분류 히스토리 (사용자별 저장)
- 마이페이지 (알러지 정보 수정, 비밀번호 변경)
- 회원가입 / 로그인 / 앱 재시작 후 자동 로그인 유지
- 게스트 모드 (로그인 없이 사용, 앱 재시작 시 초기화)
- TTS 음성 안내 지원

### 분류 가능 과자 (17종)

해태 포키 블루베리, 꼬칼콘 고소한맛, 농심 매운새우깡, 콘초, 프링글스, 포테토칩, 포카칩,
빠다코코낫, 몽쉘, 야채크래커, 쁘띠첼 구미젤리, 해태 에이스, 허니버터칩, 농심 알새우칩,
예감 치즈그라탕, 쫄병, 크라운 쵸코하임

![메인 화면 — 과자 분류](images/image-1.png)

---

## 🛠 기술 스택

| 분류 | 내용 |
|------|------|
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

## 🏗 아키텍처

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
|--------|------|
| `IClassifier` | 분류기 공통 인터페이스 (`init`, `classify`, `finish`) |
| `ClassiferWithModel` | TFLite 모델 로드 및 실제 추론 수행 |
| `SafeClassifierDecorator` | `init()` 없이 `classify()` 호출 시 즉시 예외 발생 |
| `LoggingClassifierDecorator` | 추론 소요 시간 측정 및 Logcat 출력 |
| `ClassifierFactory` | Decorator 체인 조립 — 구현체 교체 시 이 클래스만 수정 |

### 주요 구현 포인트

**알러지 교차 검사**

- 분류 결과(`label`)를 `FoodAllergyDatabase`에 조회 → 해당 과자의 성분 목록 반환
- 사용자 등록 알러지와 교집합이 존재하면 경고 다이얼로그 표시
- 게스트 모드 알러지는 `SharedPreferences`에 임시 저장, 재시작 시 초기화

```java
// 분류 후 알러지 교차 검사
List<String> ingredients = FoodAllergyDatabase.getIngredients(label);
List<String> matched = userAllergies.stream()
    .filter(ingredients::contains)
    .collect(Collectors.toList());
if (!matched.isEmpty()) showAllergyWarning(matched);
```

**세션 유지 / 게스트 모드 분기**

- 로그인 성공 시 `SharedPreferences`에 `empCode` 저장 → 앱 재시작 시 자동 로그인
- 게스트 진입 시 `isGuest=true` 플래그로 분기 — DB 저장 없이 메모리만 사용
- 로그아웃 / 앱 종료 시 게스트 데이터 전체 초기화

**분류기 초기화 안전성 (`SafeClassifierDecorator`)**

- `classify()` 호출 전 `init()` 미실행 상태를 즉시 감지하여 예외 발생
- Activity 생명주기(`onResume` / `onPause`)에서 `init` / `finish` 쌍을 보장

![분류 결과 및 알러지 경고 화면](images/image-2.png)

---

## 🤖 모델 개발 과정

### 실험한 모델

| 파일 | 설명 |
|------|------|
| `model.tflite` | 초기 실험 모델 |
| `model2.tflite` | 구조 변경 실험 |
| `modelM.tflite` | MobileNet 기반 실험 |
| `Inception1.tflite` | InceptionV3 기반 실험 |
| `model_quant_f16.tflite` | Float16 양자화 실험 |
| `Inq1.tflite` | **최종 채택 — INT8 양자화** |

### 최적화 결과

| 항목 | 원본 모델 | 최종 모델 (`Inq1.tflite`) |
|------|-----------|--------------------------|
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

![분류 히스토리 화면](images/image-3.png)

---
