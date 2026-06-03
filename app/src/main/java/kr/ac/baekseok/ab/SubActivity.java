package kr.ac.baekseok.ab;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

// 로그인 성공 후 메뉴 화면 - 기능 선택 및 로그아웃
public class SubActivity extends AppCompatActivity {

    public static final String KEY_USER_ID = "USER_ID";
    public static final String GUEST_USER_ID = AppConstants.GUEST_USER_ID;

    private String userId;
    private TtsHelper tts;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sub);

        tts = new TtsHelper(this);
        sessionManager = new SessionManager(this);

        userId = getIntent().getStringExtra(KEY_USER_ID);

        // 앱이 완전히 종료됐다가 다시 실행될 때 userId가 없으면 저장된 로그인 정보로 복구
        if (userId == null) {
            userId = sessionManager.getSavedUserId();
        }
        if (userId == null) {
            // 세션도 없으면 로그인 화면으로
            startActivity(new Intent(this, MainActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
            return;
        }

        // 툴바 overflow 메뉴에서 로그아웃 처리
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_logout) {
                showLogoutDialog();
                return true;
            }
            return false;
        });

        Button galleryBtn = findViewById(R.id.galleryBtn);
        Button cameraBtn = findViewById(R.id.cameraBtn);
        Button myPageBtn = findViewById(R.id.myPageBtn);
        Button historyBtn = findViewById(R.id.historyBtn);

        tts.bindTouchTts(galleryBtn, "갤러리에서 선택");
        tts.bindTouchTts(cameraBtn, "카메라로 촬영");
        tts.bindTouchTts(myPageBtn, "마이페이지");
        tts.bindTouchTts(historyBtn, "분류 히스토리 보기");

        galleryBtn.setOnClickListener(view -> {
            Intent i = new Intent(this, GalleryActivity.class);
            i.putExtra(KEY_USER_ID, userId);
            startActivity(i);
        });

        cameraBtn.setOnClickListener(view -> {
            Intent i = new Intent(this, CameraActivity.class);
            i.putExtra(KEY_USER_ID, userId);
            startActivity(i);
        });

        myPageBtn.setOnClickListener(view -> {
            Intent i = new Intent(this, MyPageActivity.class);
            i.putExtra(KEY_USER_ID, userId);
            startActivity(i);
        });

        historyBtn.setOnClickListener(view -> {
            Intent i = new Intent(this, HistoryActivity.class);
            i.putExtra(KEY_USER_ID, userId);
            startActivity(i);
        });
    }

    // 로그아웃 확인 다이얼로그
    private void showLogoutDialog() {
        String title = GUEST_USER_ID.equals(userId) ? "종료" : "로그아웃";
        String message = GUEST_USER_ID.equals(userId) ? "메인 화면으로 돌아가시겠습니까?" : "로그아웃 하시겠습니까?";
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(title, (dialog, which) -> logout())
                .setNegativeButton("취소", null)
                .show();
    }

    private void logout() {
        sessionManager.clear();
        // 로그아웃 후 이전 화면 전부 닫기 - 뒤로가기 눌러도 메뉴 화면으로 돌아오지 않도록
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    // 시스템 뒤로가기 버튼도 로그아웃 확인 다이얼로그로 처리
    @Override
    public void onBackPressed() {
        showLogoutDialog();
    }

    @Override
    protected void onDestroy() {
        tts.shutdown();
        super.onDestroy();
    }
}
