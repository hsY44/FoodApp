package kr.ac.baekseok.ab;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// 회원가입 화면 - UI만 담당, 인증 로직은 AuthViewModel에 위임
public class Join extends AppCompatActivity {

    private static final String[] PREDEFINED = {
            "밀", "우유", "달걀", "대두", "새우", "감자",
            "옥수수", "치즈", "코코넛", "젤라틴", "팜유", "돼지고기"
    };

    private AuthViewModel viewModel;
    private TtsHelper tts;

    private EditText j_Id, j_Pw;
    private TextView tvIdCheckResult;
    private ChipGroup predefinedChipGroup, customChipGroup;
    private Chip addChip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.join);

        tts = new TtsHelper(this);
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v ->
                tts.speakAndThen("뒤로 가기", this::finish));

        j_Id = (EditText) findViewById(R.id.jId);
        j_Pw = (EditText) findViewById(R.id.jPw);
        tvIdCheckResult = findViewById(R.id.tvIdCheckResult);
        predefinedChipGroup = findViewById(R.id.predefinedChipGroup);
        customChipGroup = findViewById(R.id.customChipGroup);

        setupChips();

        // 아이디 변경 감지 → ViewModel에 변경 통보하여 확인 상태 초기화
        j_Id.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.onIdTextChanged(s.toString().trim());
                tvIdCheckResult.setVisibility(View.GONE);
            }
        });

        Button btnCheckId = findViewById(R.id.btnCheckId);
        tts.bindTouchTts(btnCheckId, "아이디 중복 확인");
        btnCheckId.setOnClickListener(v ->
                viewModel.checkId(j_Id.getText().toString().trim()));

        Button j_btn1 = (Button) findViewById(R.id.jRegistration);
        tts.bindTouchTts(j_btn1, "회원가입");
        j_btn1.setOnClickListener(v -> {
            String id = j_Id.getText().toString().trim();
            String pw = j_Pw.getText().toString().trim();
            viewModel.register(id, pw, getSelectedAllergens());
        });

        // 중복 확인 결과 관찰
        viewModel.idCheckState.observe(this, state -> {
            switch (state) {
                case AVAILABLE:
                    tvIdCheckResult.setText("✓ 사용 가능한 아이디입니다.");
                    tvIdCheckResult.setTextColor(ContextCompat.getColor(this, R.color.color_success));
                    tvIdCheckResult.setVisibility(View.VISIBLE);
                    speak("사용 가능한 아이디입니다.");
                    break;
                case TAKEN:
                    tvIdCheckResult.setText("✗ 이미 사용 중인 아이디입니다.");
                    tvIdCheckResult.setTextColor(ContextCompat.getColor(this, R.color.color_error));
                    tvIdCheckResult.setVisibility(View.VISIBLE);
                    speak("이미 사용 중인 아이디입니다.");
                    break;
                case EMPTY:
                    if (!viewModel.isIdAvailable()) speak("아이디 중복 확인을 해주세요.");
                    break;
                case TOO_SHORT:
                    speak("아이디는 3글자 이상이어야 합니다.");
                    break;
            }
        });

        // 비밀번호 길이 검증 피드백 (loginState 재사용)
        viewModel.loginState.observe(this, state -> {
            if (state == LoginState.EMPTY_PASSWORD) speak("비밀번호를 입력해주세요.");
            else if (state == LoginState.WRONG_PASSWORD) speak("비밀번호는 4글자 이상 입력해주세요.");
        });

        // 회원가입 성공 - TTS 발화 완료 후에 화면 전환 (발화 중 끊김 방지)
        viewModel.registerSuccess.observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                Toast.makeText(this, "회원가입이 완료되었습니다.", Toast.LENGTH_SHORT).show();
                tts.speakAndThen("회원가입이 완료되었습니다. 로그인해주세요.", () -> {
                    if (!isFinishing() && !isDestroyed()) {
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    }
                });
            }
        });
    }

    private void setupChips() {
        for (String allergen : PREDEFINED) {
            Chip chip = new Chip(this);
            chip.setText(allergen);
            chip.setCheckable(true);
            predefinedChipGroup.addView(chip);
        }
        addChip = new Chip(this);
        addChip.setText("+ 기타 직접입력");
        addChip.setCheckable(false);
        addChip.setChipIconResource(R.drawable.ic_add);
        addChip.setChipIconVisible(true);
        addChip.setOnClickListener(v -> showAddAllergenDialog());
        customChipGroup.addView(addChip);
    }

    private void showAddAllergenDialog() {
        EditText input = new EditText(this);
        input.setHint("알러지 성분 입력");
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setPadding(48, 24, 48, 24);
        new AlertDialog.Builder(this)
                .setTitle("기타 알러지 직접 입력")
                .setView(input)
                .setPositiveButton("추가", (dialog, which) -> {
                    String allergen = input.getText().toString().trim();
                    if (allergen.isEmpty()) return;
                    List<String> predefinedList = Arrays.asList(PREDEFINED);
                    if (predefinedList.contains(allergen)) {
                        for (int i = 0; i < predefinedChipGroup.getChildCount(); i++) {
                            Chip chip = (Chip) predefinedChipGroup.getChildAt(i);
                            if (chip.getText().toString().equals(allergen)) chip.setChecked(true);
                        }
                        Toast.makeText(this, "'" + allergen + "'은(는) 위 목록에서 선택되었습니다.", Toast.LENGTH_SHORT).show();
                    } else {
                        addCustomChip(allergen);
                    }
                })
                .setNegativeButton("취소", null).show();
    }

    private void addCustomChip(String allergen) {
        Chip chip = new Chip(this);
        chip.setText(allergen);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> customChipGroup.removeView(chip));
        customChipGroup.addView(chip, customChipGroup.getChildCount() - 1);
    }

    private String getSelectedAllergens() {
        List<String> selected = new ArrayList<>();
        for (int i = 0; i < predefinedChipGroup.getChildCount(); i++) {
            Chip chip = (Chip) predefinedChipGroup.getChildAt(i);
            if (chip.isChecked()) selected.add(chip.getText().toString());
        }
        for (int i = 0; i < customChipGroup.getChildCount() - 1; i++) {
            selected.add(((Chip) customChipGroup.getChildAt(i)).getText().toString());
        }
        return selected.isEmpty() ? "없음" : String.join(", ", selected);
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
