package kr.ac.baekseok.ab;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

// 마이페이지 비즈니스 로직 담당 ViewModel
// 알러지 정보 로드/저장, 비밀번호 변경 처리
public class MyPageViewModel extends AndroidViewModel {

    private final UserRepository userRepository;

    private final MutableLiveData<String> _allergy = new MutableLiveData<>();
    public final LiveData<String> allergy = _allergy;

    private final SingleLiveEvent<Boolean> _allergySaveResult = new SingleLiveEvent<>();
    public final LiveData<Boolean> allergySaveResult = _allergySaveResult;

    private final SingleLiveEvent<PasswordChangeState> _passwordChangeState = new SingleLiveEvent<>();
    public final LiveData<PasswordChangeState> passwordChangeState = _passwordChangeState;

    public MyPageViewModel(@NonNull Application application) {
        super(application);
        userRepository = new UserRepository(application);
    }

    // 사용자 알러지 정보를 DB에서 로드하여 LiveData로 전달
    public void loadAllergy(String userId) {
        if (userId == null) return;
        _allergy.setValue(userRepository.getAllergy(userId));
    }

    // 알러지 정보 저장
    public void saveAllergy(String userId, String allergyStr) {
        if (userId == null) return;
        userRepository.updateAllergy(userId, allergyStr);
        _allergySaveResult.setValue(true);
    }

    // 비밀번호 변경 - 단계별 검증 후 처리
    public void changePassword(String userId, String currentPw, String newPw, String confirmPw) {
        if (currentPw.isEmpty()) {
            _passwordChangeState.setValue(PasswordChangeState.EMPTY_CURRENT); return;
        }
        if (newPw.isEmpty()) {
            _passwordChangeState.setValue(PasswordChangeState.EMPTY_NEW); return;
        }
        if (newPw.length() < 4) {
            _passwordChangeState.setValue(PasswordChangeState.NEW_TOO_SHORT); return;
        }
        if (!newPw.equals(confirmPw)) {
            _passwordChangeState.setValue(PasswordChangeState.MISMATCH); return;
        }
        // 현재 비밀번호 검증 - UserRepository.login()이 내부적으로 해싱 처리
        if (!userRepository.login(userId, currentPw)) {
            _passwordChangeState.setValue(PasswordChangeState.WRONG_CURRENT); return;
        }

        userRepository.updatePassword(userId, newPw);
        _passwordChangeState.setValue(PasswordChangeState.SUCCESS);
    }
}
