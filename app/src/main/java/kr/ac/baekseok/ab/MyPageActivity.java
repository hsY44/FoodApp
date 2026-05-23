package kr.ac.baekseok.ab;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// 마이페이지 화면 - UI만 담당, 알러지/비밀번호 로직은 MyPageViewModel에 위임
public class MyPageActivity extends AppCompatActivity {

    public static final String KEY_USER_ID = "USER_ID";

    private static final String[] PREDEFINED = {
            "밀", "우유", "달걀", "대두", "새우", "감자",
            "옥수수", "치즈", "코코넛", "젤라틴", "팜유", "돼지고기"
    };

    private MyPageViewModel viewModel;
    private TtsHelper tts;
    private String userId;

    private ChipGroup predefinedChipGroup, customChipGroup;
    private Chip addChip;
    private EditText etCurrentPw, etNewPw, etConfirmPw;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mypage);

        tts = new TtsHelper(this);
        userId = getIntent().getStringExtra(KEY_USER_ID);

        viewModel = new ViewModelProvider(this).get(MyPageViewModel.class);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v ->
                tts.speakAndThen("뒤로 가기", this::finish));

        boolean isGuest = AppConstants.GUEST_USER_ID.equals(userId);

        TextView tvUserId = findViewById(R.id.tvUserId);
        tvUserId.setText(isGuest ? "게스트" : (userId != null ? userId : "-"));

        // 게스트는 비밀번호 섹션 숨김
        LinearLayout passwordSection = findViewById(R.id.passwordSection);
        if (isGuest) passwordSection.setVisibility(View.GONE);

        predefinedChipGroup = findViewById(R.id.predefinedChipGroup);
        customChipGroup = findViewById(R.id.customChipGroup);
        setupPredefinedChips();
        setupAddChip();

        etCurrentPw = (EditText) findViewById(R.id.etCurrentPw);
        etNewPw = (EditText) findViewById(R.id.etNewPw);
        etConfirmPw = (EditText) findViewById(R.id.etConfirmPw);

        MaterialButton btnSaveAllergy = findViewById(R.id.btnSaveAllergy);
        btnSaveAllergy.setOnClickListener(v -> {
            if (isGuest) viewModel.saveGuestAllergy(getSelectedAllergens());
            else viewModel.saveAllergy(userId, getSelectedAllergens());
        });

        MaterialButton btnChangePw = findViewById(R.id.btnChangePw);
        btnChangePw.setOnClickListener(v ->
                viewModel.changePassword(userId,
                        etCurrentPw.getText().toString().trim(),
                        etNewPw.getText().toString().trim(),
                        etConfirmPw.getText().toString().trim()));

        // 알러지 데이터 로드 → Chip 상태로 복원
        viewModel.allergy.observe(this, this::loadSavedAllergens);
        if (isGuest) viewModel.loadGuestAllergy();
        else viewModel.loadAllergy(userId);

        // 알러지 저장 결과
        viewModel.allergySaveResult.observe(this, success -> {
            if (Boolean.TRUE.equals(success)) speak("알러지 정보가 저장되었습니다.");
        });

        // 비밀번호 변경 결과
        viewModel.passwordChangeState.observe(this, state -> {
            switch (state) {
                case SUCCESS:
                    etCurrentPw.setText(""); etNewPw.setText(""); etConfirmPw.setText("");
                    speak("비밀번호가 변경되었습니다.");
                    break;
                case EMPTY_CURRENT: speak("현재 비밀번호를 입력해주세요."); etCurrentPw.requestFocus(); break;
                case WRONG_CURRENT:
                    speak("현재 비밀번호가 올바르지 않습니다.");
                    etCurrentPw.setText(""); etCurrentPw.requestFocus(); break;
                case EMPTY_NEW: speak("새 비밀번호를 입력해주세요."); etNewPw.requestFocus(); break;
                case NEW_TOO_SHORT: speak("새 비밀번호는 4글자 이상이어야 합니다."); etNewPw.requestFocus(); break;
                case MISMATCH:
                    speak("새 비밀번호가 일치하지 않습니다.");
                    etConfirmPw.setText(""); etConfirmPw.requestFocus(); break;
            }
        });
    }

    private void setupPredefinedChips() {
        for (String allergen : PREDEFINED) {
            Chip chip = new Chip(this);
            chip.setText(allergen);
            chip.setCheckable(true);
            predefinedChipGroup.addView(chip);
        }
    }

    private void setupAddChip() {
        addChip = new Chip(this);
        addChip.setText("+ 기타 직접입력");
        addChip.setCheckable(false);
        addChip.setChipIconResource(R.drawable.ic_add);
        addChip.setChipIconVisible(true);
        addChip.setOnClickListener(v -> showAddAllergenDialog());
        customChipGroup.addView(addChip);
    }

    private void addCustomChip(String allergen) {
        Chip chip = new Chip(this);
        chip.setText(allergen);
        chip.setCloseIconVisible(true);
        chip.setOnCloseIconClickListener(v -> customChipGroup.removeView(chip));
        customChipGroup.addView(chip, customChipGroup.getChildCount() - 1);
    }

    private void loadSavedAllergens(String savedAllergy) {
        if (savedAllergy == null || savedAllergy.equals("없음")) return;
        List<String> predefinedList = Arrays.asList(PREDEFINED);
        for (String part : savedAllergy.split(",")) {
            String allergen = part.trim();
            if (allergen.isEmpty()) continue;
            if (predefinedList.contains(allergen)) {
                for (int i = 0; i < predefinedChipGroup.getChildCount(); i++) {
                    Chip chip = (Chip) predefinedChipGroup.getChildAt(i);
                    if (chip.getText().toString().equals(allergen)) { chip.setChecked(true); break; }
                }
            } else {
                addCustomChip(allergen);
            }
        }
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
                    if (Arrays.asList(PREDEFINED).contains(allergen)) {
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
