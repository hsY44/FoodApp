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

    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sub);

        userId = getIntent().getStringExtra(KEY_USER_ID);

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
        galleryBtn.setOnClickListener(view -> {
            Intent i = new Intent(this, GalleryActivity.class);
            i.putExtra(KEY_USER_ID, userId);
            startActivity(i);
        });

        Button cameraBtn = findViewById(R.id.cameraBtn);
        cameraBtn.setOnClickListener(view -> {
            Intent i = new Intent(this, CameraActivity.class);
            i.putExtra(KEY_USER_ID, userId);
            startActivity(i);
        });

        Button myPageBtn = findViewById(R.id.myPageBtn);
        myPageBtn.setOnClickListener(view -> {
            Intent i = new Intent(this, MyPageActivity.class);
            i.putExtra(KEY_USER_ID, userId);
            startActivity(i);
        });

        Button historyBtn = findViewById(R.id.historyBtn);
        historyBtn.setOnClickListener(view ->
                startActivity(new Intent(this, HistoryActivity.class)));
    }

    // 로그아웃 확인 다이얼로그
    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("로그아웃")
                .setMessage("로그아웃 하시겠습니까?")
                .setPositiveButton("로그아웃", (dialog, which) -> logout())
                .setNegativeButton("취소", null)
                .show();
    }

    private void logout() {
        // FLAG_ACTIVITY_CLEAR_TASK: SubActivity를 포함한 백스택 전체 제거
        // 뒤로가기를 눌러도 SubActivity로 돌아오지 않음
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    // 시스템 뒤로가기 버튼도 로그아웃 확인 다이얼로그로 처리
    @Override
    public void onBackPressed() {
        showLogoutDialog();
    }
}
