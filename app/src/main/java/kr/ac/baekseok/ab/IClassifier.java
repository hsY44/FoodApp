package kr.ac.baekseok.ab;

import android.graphics.Bitmap;
import android.util.Pair;

import java.io.IOException;

// 이미지 분류기 공통 인터페이스 - IoC(제어의 역전) 적용을 위한 추상화
// Activity는 구체 구현체(ClassiferWithModel 등)가 아닌 이 인터페이스에만 의존
// Classifer, ClassiferWithModel, ClassiferWithSupport 세 구현체가 모두 이 인터페이스를 구현
public interface IClassifier {

    // 모델 파일과 라벨 파일을 Assets에서 로드하고 분류기를 사용 가능 상태로 초기화
    void init() throws IOException;

    // 입력 비트맵을 분류하여 (클래스명, 확률) 쌍 반환
    // 확률 값은 0.0~1.0 범위 - 퍼센트 표시 시 * 100 필요
    Pair<String, Float> classify(Bitmap image);

    // init() 완료 여부 반환 - SafeClassifierDecorator의 사전 검증에 사용
    boolean isInitialized();

    // 모델/인터프리터 네이티브 자원 해제 - Activity onDestroy()에서 반드시 호출
    void finish();
}
