package kr.ac.baekseok.ab;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

// 로그인/회원가입 로직 담당 ViewModel
// MainActivity(로그인), Join(회원가입) 에서 공유
// 화면은 결과만 받아서 표시 - 로그인/회원가입 로직은 이 클래스에만 존재
public class AuthViewModel extends AndroidViewModel {

    private final UserRepository userRepository;

    // 화면 회전 시 같은 이벤트가 다시 전달되지 않는 이벤트 타입
    private final SingleLiveEvent<LoginState> _loginState = new SingleLiveEvent<>();
    public final LiveData<LoginState> loginState = _loginState;

    private final SingleLiveEvent<IdCheckState> _idCheckState = new SingleLiveEvent<>();
    public final LiveData<IdCheckState> idCheckState = _idCheckState;

    private final SingleLiveEvent<Boolean> _registerSuccess = new SingleLiveEvent<>();
    public final LiveData<Boolean> registerSuccess = _registerSuccess;

    // 중복 확인 통과 여부와 확인된 ID를 함께 저장
    // ID를 바꾸면 다시 확인해야 하므로 저장된 ID와 비교
    private boolean isIdAvailable = false;
    private String checkedId = "";

    public AuthViewModel(@NonNull Application application) {
        super(application);
        userRepository = new UserRepository(application);
    }

    // 로그인 처리 - 비밀번호를 암호화한 뒤 DB에 저장된 값과 비교
    public void login(String id, String pw) {
        if (id.isEmpty()) { _loginState.setValue(LoginState.EMPTY_ID); return; }
        if (pw.isEmpty()) { _loginState.setValue(LoginState.EMPTY_PASSWORD); return; }

        if (!userRepository.isIdRegistered(id)) {
            _loginState.setValue(LoginState.ID_NOT_FOUND);
        } else if (!userRepository.login(id, pw)) {
            _loginState.setValue(LoginState.WRONG_PASSWORD);
        } else {
            _loginState.setValue(LoginState.SUCCESS);
        }
    }

    // 아이디 중복 확인
    public void checkId(String id) {
        if (id.isEmpty()) {
            isIdAvailable = false; checkedId = "";
            _idCheckState.setValue(IdCheckState.EMPTY);
            return;
        }
        if (id.length() < 3) {
            isIdAvailable = false; checkedId = "";
            _idCheckState.setValue(IdCheckState.TOO_SHORT);
            return;
        }

        if (userRepository.isIdRegistered(id)) {
            isIdAvailable = false; checkedId = "";
            _idCheckState.setValue(IdCheckState.TAKEN);
        } else {
            isIdAvailable = true; checkedId = id;
            _idCheckState.setValue(IdCheckState.AVAILABLE);
        }
    }

    // 입력 중인 아이디가 확인된 ID와 다르면 확인 상태 초기화
    public void onIdTextChanged(String currentId) {
        if (!currentId.equals(checkedId)) {
            isIdAvailable = false;
        }
    }

    // 회원가입 처리
    public void register(String id, String pw, String allergy) {
        if (!isIdAvailable) { _idCheckState.setValue(IdCheckState.EMPTY); return; }
        if (pw.isEmpty()) { _loginState.setValue(LoginState.EMPTY_PASSWORD); return; }
        if (pw.length() < 4) {
            _loginState.setValue(LoginState.WRONG_PASSWORD); // 재사용 (별도 상태 추가 가능)
            return;
        }
        userRepository.register(id, pw, allergy);
        _registerSuccess.setValue(true);
    }

    public boolean isIdAvailable() {
        return isIdAvailable;
    }
}
