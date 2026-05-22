package kr.ac.baekseok.ab;

import android.graphics.Bitmap;
import android.util.Log;
import android.util.Pair;

import java.io.IOException;

// AOP - 로깅/성능 측정 횡단 관심사 분리
// classify() 호출 전후의 로그 출력과 실행 시간 측정을 핵심 분류 로직과 완전히 분리
// 데코레이터 패턴으로 IClassifier를 구현하고, 실제 작업은 delegate(위임 대상)에 전달
public class LoggingClassifierDecorator implements IClassifier {

    private static final String TAG = "[Classifier]";

    // 실제 분류 작업을 위임할 대상 - SafeClassifierDecorator 또는 실제 구현체
    private final IClassifier delegate;

    public LoggingClassifierDecorator(IClassifier delegate) {
        this.delegate = delegate;
    }

    @Override
    public void init() throws IOException {
        Log.d(TAG, "init() 시작");
        long start = System.currentTimeMillis();
        delegate.init();
        Log.d(TAG, "init() 완료 - " + (System.currentTimeMillis() - start) + "ms");
    }

    @Override
    public Pair<String, Float> classify(Bitmap image) {
        // 추론 시작 시간 기록 - 성능 측정(모델 최적화 시 기준 지표로 활용)
        long start = System.currentTimeMillis();

        Pair<String, Float> result = delegate.classify(image);

        long elapsed = System.currentTimeMillis() - start;
        Log.d(TAG, String.format("classify() 완료 - 결과: %s (%.2f%%), 소요: %dms",
                result.first, result.second * 100, elapsed));

        return result;
    }

    @Override
    public boolean isInitialized() {
        return delegate.isInitialized();
    }

    @Override
    public void finish() {
        Log.d(TAG, "finish() - 자원 해제");
        delegate.finish();
    }
}
