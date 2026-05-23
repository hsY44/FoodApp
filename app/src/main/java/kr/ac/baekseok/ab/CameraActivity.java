package kr.ac.baekseok.ab;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

// 카메라 촬영 화면 - UI만 담당, 분류 로직은 ClassifierViewModel에 위임
// ViewModel이 백그라운드 스레드에서 추론을 처리하므로 UI 스레드 차단 없음
public class CameraActivity extends AppCompatActivity {

    private static final String TAG = "[IC]CameraActivity";
    private static final int CAMERA_IMAGE_REQUEST_CODE = 1;
    private static final String KEY_SELECTED_URI = "KEY_SELECTED_URI";
    private static final String KEY_USER_ID = "USER_ID";

    private ClassifierViewModel viewModel;
    private TtsHelper tts;

    private ImageView imageView;
    private TextView textView;
    private Button takeBtn;
    private ProgressBar progressBar;

    private Uri selectedImageUri;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        tts = new TtsHelper(this);
        userId = getIntent().getStringExtra(KEY_USER_ID);

        // ViewModel은 화면 회전 시에도 유지 - 분류 작업이 중단되지 않음
        viewModel = new ViewModelProvider(this).get(ClassifierViewModel.class);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v ->
                tts.speakAndThen("뒤로 가기", this::finish));

        imageView = findViewById(R.id.imageView);
        textView = findViewById(R.id.textView);
        takeBtn = findViewById(R.id.takeBtn);
        progressBar = findViewById(R.id.progressBar);

        tts.bindTouchTts(takeBtn, "사진 촬영");
        takeBtn.setOnClickListener(v -> getImageFromCamera());

        // 모델 준비 완료 시 버튼 활성화
        viewModel.isReady.observe(this, ready -> {
            takeBtn.setEnabled(Boolean.TRUE.equals(ready));
        });

        // 분류 중 ProgressBar 표시 / 버튼 비활성화
        viewModel.isClassifying.observe(this, classifying -> {
            if (Boolean.TRUE.equals(classifying)) {
                progressBar.setVisibility(View.VISIBLE);
                takeBtn.setVisibility(View.GONE);
                textView.setText("분석 중...");
            } else {
                progressBar.setVisibility(View.GONE);
                takeBtn.setVisibility(View.VISIBLE);
            }
        });

        // 분류 결과 관찰 - ViewModel에서 백그라운드 처리 완료 후 전달
        viewModel.result.observe(this, result -> {
            if (result == null) return;

            if (result.isLowConfidence()) {
                textView.setText("인식 불가 (신뢰도 부족)\n다시 촬영해주세요.");
                tts.speak("이미지를 인식하지 못했습니다. 다시 촬영해주세요.");
                return;
            }

            String ingredients = result.foodAllergens.isEmpty()
                    ? "성분 정보 없음"
                    : String.join(", ", result.foodAllergens);

            textView.setText(String.format(Locale.KOREAN,
                    "%s (%.1f%%)\n주요 성분: %s", result.foodName, result.probability * 100, ingredients));

            tts.speak(String.format(Locale.KOREAN,
                    "분류 결과: %s, 확률 %.0f퍼센트", result.foodName, result.probability * 100));

            if (result.hasAllergyWarning()) showAllergyWarning(result);
        });

        if (savedInstanceState != null) {
            Uri uri = savedInstanceState.getParcelable(KEY_SELECTED_URI);
            if (uri != null) selectedImageUri = uri;
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_SELECTED_URI, selectedImageUri);
    }

    private void getImageFromCamera() {
        File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "picture.jpg");
        selectedImageUri = FileProvider.getUriForFile(this, getPackageName(), file);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, selectedImageUri);
        startActivityForResult(intent, CAMERA_IMAGE_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK || requestCode != CAMERA_IMAGE_REQUEST_CODE) return;

        Bitmap bitmap = decodeBitmap(selectedImageUri);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
            // ViewModel에 분류 요청 - 백그라운드 스레드에서 처리
            viewModel.classify(bitmap, userId, "카메라");
        }
    }

    private Bitmap decodeBitmap(Uri uri) {
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                return ImageDecoder.decodeBitmap(
                        ImageDecoder.createSource(getContentResolver(), uri));
            } else {
                return MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            }
        } catch (IOException e) {
            Log.e(TAG, "이미지 읽기 실패", e);
            return null;
        }
    }

    private void showAllergyWarning(ClassificationResult result) {
        String matchedStr = String.join(", ", result.matchedAllergens);
        new AlertDialog.Builder(this)
                .setTitle("⚠️ 알러지 경고")
                .setMessage("이 식품에 내 알러지 성분이 포함되어 있습니다!\n\n주의 성분: " + matchedStr)
                .setPositiveButton("확인", null)
                .show();
        tts.speak("경고! 알러지 성분이 포함되어 있습니다. 주의 성분: " + matchedStr);
    }

    @Override
    protected void onDestroy() {
        // ViewModel.onCleared()에서 classifier.finish()와 executor.shutdown() 처리
        tts.shutdown();
        super.onDestroy();
    }
}
