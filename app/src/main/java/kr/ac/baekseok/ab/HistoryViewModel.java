package kr.ac.baekseok.ab;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;

// 분류 히스토리 비즈니스 로직 담당 ViewModel
// 목록 로드, 전체 삭제 후 즉시 LiveData 갱신
public class HistoryViewModel extends AndroidViewModel {

    private final HistoryRepository historyRepository;

    private final MutableLiveData<List<HistoryRecord>> _historyList = new MutableLiveData<>();
    public final LiveData<List<HistoryRecord>> historyList = _historyList;

    public HistoryViewModel(@NonNull Application application) {
        super(application);
        historyRepository = new HistoryRepository(application);
    }

    // deleteAll() 시 userId 참조를 위해 loadHistory()에서 저장
    private String currentUserId;

    // 사용자별 히스토리만 조회 - userId로 DB 필터링
    public void loadHistory(String userId) {
        currentUserId = userId;
        _historyList.setValue(historyRepository.getAll(userId));
    }

    // 전체 삭제 후 빈 리스트로 LiveData 즉시 갱신 → Activity의 observer가 UI를 바로 업데이트
    public void deleteAll() {
        historyRepository.deleteAll(currentUserId);
        _historyList.setValue(new ArrayList<>());
    }
}
