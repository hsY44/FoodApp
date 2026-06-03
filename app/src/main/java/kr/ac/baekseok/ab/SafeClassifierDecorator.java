package kr.ac.baekseok.ab;

import android.graphics.Bitmap;
import android.util.Pair;

import java.io.IOException;

// 분류 전 초기화 여부를 자동으로 확인하는 래퍼 클래스
// init() 없이 분류를 시도하는 실수를 방지하는 방어 로직을 핵심 코드에서 분리
// 기존에는 ClassiferWithModel에만 초기화 확인이 있었고 나머지 구현체엔 없었음
// 이 클래스로 모든 구현체에 동일하게 초기화 확인을 적용
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
        // 초기화 없이 분류를 시도하면 즉시 오류를 던져 문제 원인을 바로 알 수 있음
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
