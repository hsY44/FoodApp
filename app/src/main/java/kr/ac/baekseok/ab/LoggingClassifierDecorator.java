package kr.ac.baekseok.ab;

import android.graphics.Bitmap;
import android.util.Log;
import android.util.Pair;

import java.io.IOException;

// 분류 시간 측정과 로그 출력을 핵심 분류 로직과 분리한 래퍼 클래스
// 실제 분류 작업은 내부에 보관한 delegate 객체에 전달
public class LoggingClassifierDecorator implements IClassifier {

    private static final String TAG = "[Classifier]";

    // 실제 분류를 처리할 대상 객체
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
        // 분류 시작 시간 기록 - 성능 확인용
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
        Log.d(TAG, "finish() - 분류기 자원 해제");
        delegate.finish();
    }
}
