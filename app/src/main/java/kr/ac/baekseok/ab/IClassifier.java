package kr.ac.baekseok.ab;

import android.graphics.Bitmap;
import android.util.Pair;

import java.io.IOException;

// 이미지 분류기 공통 규칙 정의 - 구현체가 바뀌어도 사용하는 쪽 코드는 변경 불필요
// 화면 코드는 이 인터페이스만 알면 됨 - 분류 방식이 바뀌어도 화면 코드는 수정 불필요
// Classifer, ClassiferWithModel, ClassiferWithSupport 세 구현체가 모두 이 인터페이스를 구현
public interface IClassifier {

    // 모델 파일과 라벨 파일을 불러와 분류 가능한 상태로 준비
    void init() throws IOException;

    // 이미지를 분류하여 (과자 이름, 확률) 반환
    // 확률은 0.0~1.0 범위 - 퍼센트로 표시할 때는 × 100
    Pair<String, Float> classify(Bitmap image);

    // 초기화 완료 여부 반환 - 분류 전 준비 확인에 사용
    boolean isInitialized();

    // 분류 모델 자원 해제 - 화면 종료 시 반드시 호출
    void finish();
}
