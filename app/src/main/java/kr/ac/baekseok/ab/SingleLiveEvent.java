package kr.ac.baekseok.ab;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import java.util.concurrent.atomic.AtomicBoolean;

// 단발성 이벤트 전달용 LiveData
// 일반 LiveData는 화면 회전 시 마지막 값을 재전달 → 로그인 성공 메시지가 중복 표시됨
// SingleLiveEvent는 consume된 이벤트를 재전달하지 않아 이를 방지
public class SingleLiveEvent<T> extends MutableLiveData<T> {

    private final AtomicBoolean pending = new AtomicBoolean(false);

    @Override
    public void observe(@NonNull LifecycleOwner owner, @NonNull Observer<? super T> observer) {
        super.observe(owner, value -> {
            // pending이 true일 때만 observer에 전달 (한 번만 소비)
            if (pending.compareAndSet(true, false)) {
                observer.onChanged(value);
            }
        });
    }

    @Override
    public void setValue(@Nullable T value) {
        pending.set(true);
        super.setValue(value);
    }

    // 데이터 없이 이벤트만 발생시킬 때 사용
    public void call() {
        setValue(null);
    }
}
