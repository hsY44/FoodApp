package kr.ac.baekseok.ab;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

// 로그인 화면 - UI만 담당, 로그인 로직은 AuthViewModel에 위임
public class MainActivity extends AppCompatActivity {

    private AuthViewModel viewModel;
    private TtsHelper tts;
    private SessionManager sessionManager;
    private EditText mainId, mainPw;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        String savedUserId = sessionManager.getSavedUserId();
        if (savedUserId != null) {
            Intent intent = new Intent(this, SubActivity.class);
            intent.putExtra(SubActivity.KEY_USER_ID, savedUserId);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        tts = new TtsHelper(this);

        // AndroidViewModel은 별도 Factory 없이 ViewModelProvider로 생성 가능
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        mainId = (EditText) findViewById(R.id.edtId);
        mainPw = (EditText) findViewById(R.id.edtPw);
        Button mainLoginBtn = (Button) findViewById(R.id.mainLoginBtn);
        Button mainJoinBtn = (Button) findViewById(R.id.mainJoinBtn);
        Button mainGuestBtn = (Button) findViewById(R.id.mainGuestBtn);

        tts.bindTouchTts(mainLoginBtn, "로그인");
        tts.bindTouchTts(mainJoinBtn, "회원가입");
        tts.bindTouchTts(mainGuestBtn, "게스트로 시작");

        mainJoinBtn.setOnClickListener(v ->
                startActivity(new Intent(this, Join.class)));

        mainLoginBtn.setOnClickListener(v ->
                viewModel.login(
                        mainId.getText().toString().trim(),
                        mainPw.getText().toString().trim()
                ));

        mainGuestBtn.setOnClickListener(v -> {
            // 이전 게스트 세션의 알러지 데이터 초기화 - 게스트는 매 세션 새로 시작
            new UserRepository(this).clearGuestAllergy();
            sessionManager.save(AppConstants.GUEST_USER_ID);
            Intent intent = new Intent(this, SubActivity.class);
            intent.putExtra(SubActivity.KEY_USER_ID, AppConstants.GUEST_USER_ID);
            startActivity(intent);
        });

        // LiveData 관찰 - ViewModel에서 전달된 상태에 따라 UI/TTS 처리
        viewModel.loginState.observe(this, state -> {
            switch (state) {
                case SUCCESS:
                    String userId = mainId.getText().toString().trim();
                    sessionManager.save(userId);
                    speak("로그인 성공! 이미지 분류를 시작하세요.");
                    Intent intent = new Intent(this, SubActivity.class);
                    intent.putExtra(SubActivity.KEY_USER_ID, userId);
                    startActivity(intent);
                    finish(); // 백스택에서 MainActivity 제거 - SubActivity만 남겨야 복원 시 충돌 없음
                    break;
                case EMPTY_ID:
                    speak("아이디를 입력해주세요.");
                    mainId.requestFocus();
                    break;
                case EMPTY_PASSWORD:
                    speak("비밀번호를 입력해주세요.");
                    mainPw.requestFocus();
                    break;
                case ID_NOT_FOUND:
                    speak("아이디가 없습니다. 회원가입해 주세요.");
                    break;
                case WRONG_PASSWORD:
                    speak("비밀번호 오류입니다.");
                    mainPw.setText("");
                    mainPw.requestFocus();
                    break;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // setContentView가 호출되지 않은 경우(세션 자동 이동) null 방지
        if (mainId != null) mainId.setText("");
        if (mainPw != null) mainPw.setText("");
    }

    private void speak(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        tts.speak(msg);
    }

    @Override
    protected void onDestroy() {
        if (tts != null) tts.shutdown();
        super.onDestroy();
    }
}
