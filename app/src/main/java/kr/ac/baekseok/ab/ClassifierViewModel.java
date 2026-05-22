package kr.ac.baekseok.ab;

import android.app.Application;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// 이미지 분류 비즈니스 로직 담당 ViewModel
// CameraActivity / GalleryActivity 에서 공유
// 핵심: 분류 추론을 백그라운드 스레드(ExecutorService)에서 실행하여 ANR 방지
// Activity는 LiveData만 관찰 - 직접 분류 로직에 관여하지 않음
public class ClassifierViewModel extends AndroidViewModel {

    private static final String TAG = "[ClassifierVM]";

    private final IClassifier classifier;
    private final HistoryRepository historyRepository;
    private final UserRepository userRepository;

    // 단일 스레드 풀 - 분류 작업은 순차 처리 (동시 추론 방지)
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // 모델 초기화 완료 여부 - Activity에서 버튼 활성화 제어에 사용
    private final MutableLiveData<Boolean> _isReady = new MutableLiveData<>(false);
    public final LiveData<Boolean> isReady = _isReady;

    // 분류 진행 중 여부 - Activity에서 ProgressBar 및 버튼 비활성화 제어에 사용
    private final MutableLiveData<Boolean> _isClassifying = new MutableLiveData<>(false);
    public final LiveData<Boolean> isClassifying = _isClassifying;

    // 분류 결과 - 식품명, 확률, 알러지 성분, 사용자 알러지 매칭 결과 포함
    private final SingleLiveEvent<ClassificationResult> _result = new SingleLiveEvent<>();
    public final LiveData<ClassificationResult> result = _result;

    public ClassifierViewModel(@NonNull Application application) {
        super(application);
        classifier = ClassifierFactory.create(application);
        historyRepository = new HistoryRepository(application);
        userRepository = new UserRepository(application);

        // 모델 초기화도 백그라운드에서 수행 - 모델 로드가 수백ms 소요될 수 있음
        initClassifierAsync();
    }

    private void initClassifierAsync() {
        executor.execute(() -> {
            try {
                classifier.init();
                mainHandler.post(() -> _isReady.setValue(true));
            } catch (IOException e) {
                Log.e(TAG, "모델 초기화 실패", e);
            }
        });
    }

    // 이미지 분류 실행 - 백그라운드 스레드에서 추론, 결과는 메인 스레드로 전달
    public void classify(Bitmap bitmap, String userId, String source) {
        if (!Boolean.TRUE.equals(_isReady.getValue())) {
            Log.w(TAG, "모델 초기화 전 classify() 호출 무시");
            return;
        }

        _isClassifying.setValue(true);

        executor.execute(() -> {
            Pair<String, Float> output = classifier.classify(bitmap);
            String foodName = output.first;
            float prob = output.second;

            // 알러지 교차 검사 - 백그라운드에서 DB 조회 포함
            String userAllergy = (userId != null) ? userRepository.getAllergy(userId) : "없음";
            List<String> foodAllergens = FoodAllergyDatabase.getAllergens(foodName);
            List<String> matched = FoodAllergyDatabase.matchingAllergens(foodName, userAllergy);

            // 히스토리 저장 (백그라운드에서 처리)
            historyRepository.save(source, foodName, prob);

            ClassificationResult classificationResult =
                    new ClassificationResult(foodName, prob, foodAllergens, matched);

            // UI 업데이트는 반드시 메인 스레드에서
            mainHandler.post(() -> {
                _result.setValue(classificationResult);
                _isClassifying.setValue(false);
            });
        });
    }

    // ViewModel이 소멸될 때 자원 해제
    // Activity가 완전히 종료될 때만 호출 - 화면 회전에서는 호출되지 않음
    @Override
    protected void onCleared() {
        executor.shutdown();     // 스레드 풀 종료
        classifier.finish();     // TFLite 네이티브 자원 해제
        super.onCleared();
    }
}
