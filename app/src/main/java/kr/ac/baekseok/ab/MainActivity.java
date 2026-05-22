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
    private EditText mainId, mainPw;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tts = new TtsHelper(this);

        // AndroidViewModel은 별도 Factory 없이 ViewModelProvider로 생성 가능
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        mainId = (EditText) findViewById(R.id.edtId);
        mainPw = (EditText) findViewById(R.id.edtPw);
        Button mainLoginBtn = (Button) findViewById(R.id.mainLoginBtn);
        Button mainJoinBtn = (Button) findViewById(R.id.mainJoinBtn);

        mainJoinBtn.setOnClickListener(v ->
                startActivity(new Intent(this, Join.class)));

        mainLoginBtn.setOnClickListener(v ->
                viewModel.login(
                        mainId.getText().toString().trim(),
                        mainPw.getText().toString().trim()
                ));

        // LiveData 관찰 - ViewModel에서 전달된 상태에 따라 UI/TTS 처리
        viewModel.loginState.observe(this, state -> {
            switch (state) {
                case SUCCESS:
                    speak("로그인 성공! 이미지 분류를 시작하세요.");
                    Intent intent = new Intent(this, SubActivity.class);
                    intent.putExtra(SubActivity.KEY_USER_ID, mainId.getText().toString().trim());
                    startActivity(intent);
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
        mainId.setText("");
        mainPw.setText("");
    }

    private void speak(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        tts.speak(msg);
    }

    @Override
    protected void onDestroy() {
        tts.shutdown();
        super.onDestroy();
    }
}
