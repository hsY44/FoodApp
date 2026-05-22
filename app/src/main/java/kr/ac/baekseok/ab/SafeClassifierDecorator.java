package kr.ac.baekseok.ab;

import android.graphics.Bitmap;
import android.util.Pair;

import java.io.IOException;

// AOP - 초기화 상태 검증 횡단 관심사 분리
// init() 없이 classify()를 호출하는 실수를 방지하는 방어 로직을 핵심 코드에서 분리
// 기존에는 ClassiferWithModel에만 isInitialized 체크가 있었고 나머지 구현체엔 없었음
// 이 데코레이터로 모든 구현체에 일관된 검증을 적용
public class SafeClassifierDecorator implements IClassifier {

    private final IClassifier delegate;

    public SafeClassifierDecorator(IClassifier delegate) {
        this.delegate = delegate;
    }

    @Override
    public void init() throws IOException {
        delegate.init();
    }

    @Override
    public Pair<String, Float> classify(Bitmap image) {
        // 미초기화 상태에서 classify() 호출 시 명확한 예외로 문제 위치를 즉시 파악 가능
        if (!delegate.isInitialized()) {
            throw new IllegalStateException("분류기가 초기화되지 않았습니다. init()을 먼저 호출하세요.");
        }
        return delegate.classify(image);
    }

    @Override
    public boolean isInitialized() {
        return delegate.isInitialized();
    }

    @Override
    public void finish() {
        delegate.finish();
    }
}
