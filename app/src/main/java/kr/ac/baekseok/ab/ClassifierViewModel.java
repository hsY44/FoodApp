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

// 이미지 분류 로직 담당 ViewModel
// CameraActivity / GalleryActivity 에서 공유
// 핵심: 분류 작업을 별도로 처리해서 앱 화면이 멈추지 않도록 함
// 화면은 결과만 받아서 표시 - 분류 로직에 관여하지 않음
public class ClassifierViewModel extends AndroidViewModel {

    private static final String TAG = "[ClassifierVM]";

    private final IClassifier classifier;
    private final HistoryRepository historyRepository;
    private final UserRepository userRepository;

    // 분류 작업은 한 번에 하나씩 차례대로 처리
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // 모델 준비 완료 여부 - 버튼 활성화에 사용
    private final MutableLiveData<Boolean> _isReady = new MutableLiveData<>(false);
    public final LiveData<Boolean> isReady = _isReady;

    // 분류 중 여부 - 로딩 표시와 버튼 비활성화에 사용
    private final MutableLiveData<Boolean> _isClassifying = new MutableLiveData<>(false);
    public final LiveData<Boolean> isClassifying = _isClassifying;

    // 분류 결과 (식품명, 확률, 식품 알러지 성분, 내 알러지와 일치하는 성분)
    private final SingleLiveEvent<ClassificationResult> _result = new SingleLiveEvent<>();
    public final LiveData<ClassificationResult> result = _result;

    public ClassifierViewModel(@NonNull Application application) {
        super(application);
        classifier = ClassifierFactory.create(application);
        historyRepository = new HistoryRepository(application);
        userRepository = new UserRepository(application);

        // 모델 파일 로드는 시간이 걸리므로 앱이 멈추지 않도록 뒤에서 처리
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

    // 이미지 분류 실행 - 별도 작업으로 처리 후 결과를 화면으로 전달
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

            // 알러지 정보 가져오기 - 게스트는 임시 저장소, 일반 사용자는 DB
            String userAllergy;
            if (AppConstants.GUEST_USER_ID.equals(userId)) {
                userAllergy = userRepository.getGuestAllergy();
            } else {
                userAllergy = (userId != null) ? userRepository.getAllergy(userId) : "없음";
            }
            List<String> foodAllergens = FoodAllergyDatabase.getAllergens(foodName);
            List<String> matched = FoodAllergyDatabase.matchingAllergens(foodName, userAllergy);

            // 분류 기록 저장
            historyRepository.save(userId, source, foodName, prob);

            ClassificationResult classificationResult =
                    new ClassificationResult(foodName, prob, foodAllergens, matched);

            // 화면 업데이트는 반드시 화면을 담당하는 메인 스레드에서 실행
            mainHandler.post(() -> {
                _result.setValue(classificationResult);
                _isClassifying.setValue(false);
            });
        });
    }

    // 화면이 완전히 종료될 때 실행 - 화면 회전 시에는 실행되지 않음
    @Override
    protected void onCleared() {
        executor.shutdown();     // 별도 작업 실행기 종료
        classifier.finish();     // 분류 모델 자원 해제
        super.onCleared();
    }
}
