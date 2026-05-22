package kr.ac.baekseok.ab;

import android.content.Context;

// 분류기 생성과 데코레이터 체이닝을 담당하는 팩토리
// Activity에서 구체 구현체를 직접 new 하지 않고 이 팩토리를 통해 IClassifier를 얻음 - DI 적용
// 모델 교체(ClassiferWithModel → Classifer 등) 또는 데코레이터 추가/제거 시 이 클래스 한 곳만 수정
public class ClassifierFactory {

    // 데코레이터 체이닝: 핵심 로직 → 초기화 검증(Safe) → 로깅(Logging) 순으로 감싸기
    // AOP 관점: 핵심 분류 로직(ClassiferWithModel) 앞뒤로 횡단 관심사를 삽입
    //   - SafeClassifierDecorator : classify() 호출 전 초기화 여부 검증
    //   - LoggingClassifierDecorator : 추론 시간 측정 및 결과 로그 출력
    public static IClassifier create(Context context) {
        IClassifier core = new ClassiferWithModel(context);         // 핵심 분류 로직
        IClassifier safe = new SafeClassifierDecorator(core);       // 초기화 검증 래핑
        return new LoggingClassifierDecorator(safe);                // 로깅 래핑
    }
}
