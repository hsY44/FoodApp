package kr.ac.baekseok.ab;

import android.content.Context;

// 분류기를 단계적으로 감싸서 생성하는 클래스
// 사용하는 쪽에서 구체 구현체를 직접 만들지 않고 이 클래스를 통해 분류기를 얻음
// 모델 교체나 기능 추가/제거 시 이 클래스 한 곳만 수정
public class ClassifierFactory {

    // 분류기를 순서대로 감싸기: 핵심 분류 → 초기화 확인 → 시간 측정/로그
    public static IClassifier create(Context context) {
        IClassifier core = new ClassiferWithModel(context);         // 핵심 분류 로직
        IClassifier safe = new SafeClassifierDecorator(core);       // 초기화 확인 추가
        return new LoggingClassifierDecorator(safe);                // 시간 측정/로그 추가
    }
}
