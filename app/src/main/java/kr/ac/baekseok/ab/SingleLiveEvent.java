package kr.ac.baekseok.ab;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import java.util.concurrent.atomic.AtomicBoolean;

// 한 번만 전달되는 이벤트용 데이터 클래스
// 일반 LiveData는 화면 회전 시 이전 값을 다시 전달 → 로그인 성공 메시지가 중복 표시됨
// SingleLiveEvent는 이미 전달한 이벤트를 다시 보내지 않음
public class SingleLiveEvent<T> extends MutableLiveData<T> {

    private final AtomicBoolean pending = new AtomicBoolean(false);

    @Override
    public void observe(@NonNull LifecycleOwner owner, @NonNull Observer<? super T> observer) {
        super.observe(owner, value -> {
            // 전달 대기 상태일 때만 이벤트 전달 (한 번만 전달되도록)
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
